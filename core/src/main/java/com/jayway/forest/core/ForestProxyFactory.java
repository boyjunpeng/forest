package com.jayway.forest.core;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import com.jayway.forest.Body;
import com.jayway.forest.roles.Resource;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.AttributeInfo;
import javassist.bytecode.ConstPool;
import javassist.bytecode.ParameterAnnotationsAttribute;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.StringMemberValue;

public class ForestProxyFactory {
	private final ClassPool pool = ClassPool.getDefault();

	public Object proxy(Object object) throws Exception {
		Class<?> clazz = getProxyClass(object.getClass());
		Constructor<?> constructor = clazz.getConstructor(object.getClass());
		return constructor.newInstance(object);
	}

	public Class<?> getProxyClass(Class<?> clazz) throws Exception {
		try {
			return this.pool.get(getProxyName(clazz.getName())).toClass();
		} catch (NotFoundException e) {
			return createProxy(clazz);
		}
	}
	
	private Class<?> createProxy(Class<?> clazz) throws Exception {
		if (!Resource.class.isAssignableFrom(clazz)) {
			return clazz;
		}
		CtClass sourceClass = pool.get(clazz.getName());
		CtClass targetClass = createProxyClass(pool, sourceClass);
		copyPublicMethods(sourceClass, targetClass);

		return targetClass.toClass();
	}

	private void copyPublicMethods(CtClass sourceClass, CtClass targetClass) throws Exception {
		for (CtMethod sourceMethod : sourceClass.getMethods()) {
			if (shouldCopy(sourceMethod)) {
				CtMethod targetMethod;
				boolean isCommand = sourceMethod.getReturnType().equals(CtClass.voidType);
				if (isCommand) {
					targetMethod = createCommand(targetClass, sourceMethod);
				} else {
					targetMethod = createQuery(targetClass, sourceMethod);
				}
				String returnStatement = "";
				String afterStatement = "";
				if (isCommand) {
					afterStatement = "return \"Operation completed successfully\";";
				} else {
					returnStatement = "return ";
				}
				String body = "{"+ returnStatement +"delegate."+ sourceMethod.getName() +"($$); "+ afterStatement +"}";
				targetMethod.setBody(body);
				targetClass.addMethod(targetMethod);
			}
		}
	}

	private CtMethod createQuery(CtClass targetClass, CtMethod sourceMethod) throws CannotCompileException, Exception {
		CtMethod targetMethod;
		targetMethod = new CtMethod(sourceMethod, targetClass, null);
		copyAttributeInfo(sourceMethod, targetMethod, AnnotationsAttribute.visibleTag);
		copyAttributeInfo(sourceMethod, targetMethod, ParameterAnnotationsAttribute.visibleTag);
		addAnnotations(sourceMethod.getName(), targetMethod, GET.class);
		prepareParameterAnnotations(targetMethod, QueryParam.class.getName());
		return targetMethod;
	}

	private CtMethod createCommand(CtClass targetClass, CtMethod sourceMethod) throws NotFoundException, Exception {
		CtMethod targetMethod;
		CtClass returnType = targetClass.getClassPool().get("java.lang.String");
		targetMethod = new CtMethod(returnType, sourceMethod.getName() + "_proxy", sourceMethod.getParameterTypes(), targetClass);
		copyAttributeInfo(sourceMethod, targetMethod, AnnotationsAttribute.visibleTag);
		copyAttributeInfo(sourceMethod, targetMethod, ParameterAnnotationsAttribute.visibleTag);
		addAnnotations(sourceMethod.getName(), targetMethod, PUT.class);
		if (targetMethod.getParameterTypes().length > 1 || (targetMethod.getParameterTypes().length == 1 && isSimpleType(targetMethod.getParameterTypes()[0]) && !hasBodyAnnotation(targetMethod))) {
			prepareParameterAnnotations(targetMethod, FormParam.class.getName());
		}
		return targetMethod;
	}

	private boolean hasBodyAnnotation(CtMethod targetMethod) throws Exception {
		Object[] objects = targetMethod.getParameterAnnotations()[0];
		for (Object object : objects) {
			if (object instanceof Body) {
				return true;
			}
		}
		return false;
	}

	private void copyAttributeInfo(CtMethod sourceMethod, CtMethod targetMethod, String name) {
		AttributeInfo info = sourceMethod.getMethodInfo().getAttribute(name);
		if (info != null) {
			targetMethod.getMethodInfo().addAttribute(info.copy(targetMethod.getDeclaringClass().getClassFile().getConstPool(), null));
		}
	}

	private CtClass createProxyClass(ClassPool pool, CtClass sourceClass) throws Exception {
		CtClass targetClass = pool.makeClass(getProxyName(sourceClass.getName()));
		copyAnnotations(sourceClass, targetClass);
		createConstructors(sourceClass, targetClass);
		return targetClass;
	}

	private void createConstructors(CtClass sourceClass, CtClass targetClass) throws Exception {
		targetClass.addField(new CtField(sourceClass, "delegate", targetClass));
		
		if (hasNoArgConstructor(sourceClass)) {
			CtConstructor noArgConstructor = new CtConstructor(new CtClass[] {}, targetClass);
			noArgConstructor.setBody(String.format("delegate = new %s();", sourceClass.getName()));
			targetClass.addConstructor(noArgConstructor);
		}

		CtConstructor delegateConstructor = new CtConstructor(new CtClass[] {sourceClass}, targetClass);
		delegateConstructor.setBody("delegate = $1;");
		targetClass.addConstructor(delegateConstructor);
}

	private boolean hasNoArgConstructor(CtClass sourceClass) throws NotFoundException {
		CtConstructor[] constructors = sourceClass.getConstructors();
		for (CtConstructor ctConstructor : constructors) {
			if (ctConstructor.getParameterTypes().length == 0) {
				return true;
			}
		}
		return false;
	}

	private void copyAnnotations(CtClass sourceClass, CtClass targetClass) {
		AnnotationsAttribute sourceInfo = (AnnotationsAttribute) sourceClass.getClassFile().getAttribute(AnnotationsAttribute.visibleTag);
		if (sourceInfo != null) {
			targetClass.getClassFile().addAttribute(sourceInfo.copy(targetClass.getClassFile().getConstPool(), null));
		}
	}

	private String getProxyName(String sourceClassName) {
		return sourceClassName + "_Proxy";
	}

	private boolean shouldCopy(CtMethod m) {
		return 	!m.getDeclaringClass().getName().equals("java.lang.Object") &&
				!Modifier.isFinal(m.getModifiers()) && 
				!Modifier.isAbstract(m.getModifiers()) && 
				!Modifier.isStatic(m.getModifiers()) && 
				!Modifier.isNative(m.getModifiers());
	}

	private boolean isSimpleType(CtClass clazz) {
		if (clazz.isPrimitive() || clazz.getName().equals("java.lang.String")) {
			return true;
		}
		return false;
	}

	private void addAnnotations(String pathValue, CtMethod m, Class<?> httpMethodAnnotation) throws Exception {
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
			path.addMemberValue("value", new StringMemberValue(pathValue, constPool));
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
}
