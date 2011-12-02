package com.jayway.forest.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.AttributeInfo;
import javassist.bytecode.ConstPool;
import javassist.bytecode.ParameterAnnotationsAttribute;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.StringMemberValue;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Application;

import com.jayway.forest.roles.Resource;

public class ForestApplication extends Application {
	
	private final Application delegate;
	private Set<Class<?>> delegateClasses;
	private Set<Object> delegateSingletons;
	
	public ForestApplication(Application delegate) {
		this.delegate = delegate;
		delegateClasses = getDelegateClasses();
		delegateSingletons = getDelegateSingletons();
	}
	
	@Override
	public Set<Class<?>> getClasses() {
		return delegateClasses;
	}

	@Override
	public Set<Object> getSingletons() {
		return delegateSingletons;
	}

	private Set<Object> getDelegateSingletons() {
		Set<Object> classes = delegate.getSingletons();
		Set<Object> result = new HashSet<Object>();
		for (Object object : classes) {
			result.add(proxy(object));
		}
		return result;
	}

	private Set<Class<?>> getDelegateClasses() {
		Set<Class<?>> classes = delegate.getClasses();
		Set<Class<?>> result = new HashSet<Class<?>>();
		for (Class<?> clazz : classes) {
			try {
				result.add(createProxy(clazz));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return result;
	}

	private Class<?> createProxy(Class<?> clazz) throws Exception {
		if (!Resource.class.isAssignableFrom(clazz)) {
			return clazz;
		}
		ClassPool pool = ClassPool.getDefault();
		CtClass sourceClass = pool.get(clazz.getName());
		CtClass targetClass = createSubclass(pool, sourceClass);
		copyPublicMethods(sourceClass, targetClass);
		prepareCommandQuery(targetClass);

		return targetClass.toClass();
	}

	private void copyPublicMethods(CtClass sourceClass, CtClass targetClass) throws CannotCompileException, NotFoundException {
		for (CtMethod sourceMethod : sourceClass.getMethods()) {
			if (shouldCopy(sourceMethod)) {
				CtMethod targetMethod = new CtMethod(sourceMethod, targetClass, null);
				copyAttributeInfo(sourceMethod, targetMethod, AnnotationsAttribute.visibleTag);
				copyAttributeInfo(sourceMethod, targetMethod, ParameterAnnotationsAttribute.visibleTag);
				String returnStatement = "";
				if (!sourceMethod.getReturnType().equals(CtClass.voidType)) {
					returnStatement = "return ";
				}
				String body = "{"+ returnStatement +"super."+ sourceMethod.getName() +"($$);}";
				targetMethod.setBody(body);
				targetClass.addMethod(targetMethod);
			}
		}
	}

	private void copyAttributeInfo(CtMethod sourceMethod, CtMethod targetMethod, String name) {
		AttributeInfo info = sourceMethod.getMethodInfo().getAttribute(name);
		if (info != null) {
			targetMethod.getMethodInfo().addAttribute(info.copy(targetMethod.getDeclaringClass().getClassFile().getConstPool(), null));
		}
	}

	private CtClass createSubclass(ClassPool pool, CtClass sourceClass) throws CannotCompileException {
		CtClass targetClass = pool.makeClass(sourceClass.getName() + "_Proxy");
		AnnotationsAttribute sourceInfo = (AnnotationsAttribute) sourceClass.getClassFile().getAttribute(AnnotationsAttribute.visibleTag);
		if (sourceInfo != null) {
			targetClass.getClassFile().addAttribute(sourceInfo.copy(targetClass.getClassFile().getConstPool(), null));
		}
		targetClass.setSuperclass(sourceClass);
		return targetClass;
	}

	private boolean shouldCopy(CtMethod m) {
		return 	!m.getDeclaringClass().getName().equals("java.lang.Object") &&
				!Modifier.isFinal(m.getModifiers()) && 
				!Modifier.isAbstract(m.getModifiers()) && 
				!Modifier.isStatic(m.getModifiers()) && 
				!Modifier.isNative(m.getModifiers());
	}

	private void prepareCommandQuery(CtClass targetClass) throws Exception {
		for (CtMethod m : targetClass.getMethods()) {
			if (m.getDeclaringClass().equals(targetClass)) {
				if (CtClass.voidType.equals(m.getReturnType())) {
					handleCommand(m);
					if (m.getParameterTypes().length > 1 || (m.getParameterTypes().length == 1 && isSimpleType(m.getParameterTypes()[0]))) {
						prepareParameterAnnotations(m, FormParam.class.getName());
					}
				} else {
					handleQuery(m);
					prepareParameterAnnotations(m, QueryParam.class.getName());
				}
			}
		}
	}

	private boolean isSimpleType(CtClass clazz) {
		if (clazz.isPrimitive() || clazz.getName().equals("java.lang.String")) {
			return true;
		}
		return false;
	}

	private void handleQuery(CtMethod m) throws Exception {
		addAnnotations(m, GET.class);
	}

	private void handleCommand(CtMethod m) throws Exception {
		addAnnotations(m, PUT.class);
	}

	private void addAnnotations(CtMethod m, Class<?> httpMethodAnnotation) throws Exception {
		ConstPool constPool = m.getDeclaringClass().getClassFile().getConstPool();
		AnnotationsAttribute info = (AnnotationsAttribute) m.getMethodInfo().getAttribute(AnnotationsAttribute.visibleTag);
		if (info == null) {
			info = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
		}
		if (!hasAnnotation(info.getAnnotations(), HttpMethod.class)) {
			Annotation get = new Annotation(httpMethodAnnotation.getName(), constPool);
			info.addAnnotation(get);
		}
		if (!m.hasAnnotation(Path.class)) {
			Annotation path = new Annotation(Path.class.getName(), constPool);
			path.addMemberValue("value", new StringMemberValue(m.getName(), constPool));
			info.addAnnotation(path);
		}
		m.getMethodInfo().addAttribute(info);
	}

	private void prepareParameterAnnotations(CtMethod m, String annotationClassName) throws Exception {
		if (m.getParameterTypes().length == 0) {
			return;
		}
		ConstPool constPool = m.getMethodInfo().getConstPool();
		int parameterCount = m.getParameterTypes().length;
		ParameterAnnotationsAttribute info = (ParameterAnnotationsAttribute) m.getMethodInfo().getAttribute(ParameterAnnotationsAttribute.visibleTag);
		Annotation[][] annotations;
		if (info == null) {
			info = new ParameterAnnotationsAttribute(constPool, ParameterAnnotationsAttribute.visibleTag);
			annotations = new Annotation[parameterCount][];
		} else {
			annotations = info.getAnnotations();
		}
		for (int indx=0; indx<parameterCount; indx++) {
			List<Annotation> paramAnnotations;
			if (annotations[indx] == null) {
				paramAnnotations = new LinkedList<Annotation>();
			} else {
				paramAnnotations = new ArrayList<Annotation>(Arrays.asList(annotations[indx]));
			}
			if (!contains(paramAnnotations, annotationClassName)) { 
				Annotation param = new Annotation(annotationClassName, constPool);
				param.addMemberValue("value", new StringMemberValue("argument" + (indx+1), constPool));
				paramAnnotations.add(param);
			}
			annotations[indx] = paramAnnotations.toArray(new Annotation[paramAnnotations.size()]);
		}
		info.setAnnotations(annotations);
		m.getMethodInfo().addAttribute(info);
		
	}

	private boolean contains(List<Annotation> paramAnnotations, String annotationClassName) {
		for (Annotation annotation : paramAnnotations) {
			if (annotation.getTypeName().equals(annotationClassName)) {
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private boolean hasAnnotation(Annotation[] annotations, Class annotationClass) throws Exception {
		for (Annotation annotation : annotations) {
			if (Class.forName(annotation.getTypeName()).getAnnotation(annotationClass) != null) {
				return true;
			}
		}
		return false;
	}

	private Object proxy(Object object) {
		return object;
	}
}
