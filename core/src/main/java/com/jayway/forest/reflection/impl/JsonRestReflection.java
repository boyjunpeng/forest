package com.jayway.forest.reflection.impl;

import com.jayway.forest.core.JSONHelper;
import com.jayway.forest.core.RoleManager;
import com.jayway.forest.reflection.Capabilities;
import com.jayway.forest.reflection.CapabilityReference;
import com.jayway.forest.reflection.ReflectionUtil;
import com.jayway.forest.reflection.RestReflection;
import com.jayway.forest.reflection.impl.LinkCapabilityReference;
import com.jayway.forest.reflection.impl.PagedSortedListResponse;
import com.jayway.forest.roles.Linkable;
import com.jayway.forest.roles.Resource;
import com.jayway.forest.roles.Template;
import com.jayway.forest.roles.UriInfo;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

public final class JsonRestReflection implements RestReflection {
	
	public static final RestReflection INSTANCE = new JsonRestReflection();
	
	private JsonRestReflection() {
	}

	@Override
	public Object renderCapabilities(Capabilities capabilities) {
        StringBuilder results = new StringBuilder( );
        results.append("{");
        List<CapabilityReference> all = new LinkedList<CapabilityReference>();
        all.addAll( capabilities.getQueries() );
        all.addAll( capabilities.getCommands() );
        all.addAll( capabilities.getResources() );
        // todo append the whole paging result
        for (Linkable link : capabilities.getDiscoveredLinks()) {
            all.add(new LinkCapabilityReference(link));
        }
        if ( !all.isEmpty() ) {
            toMapEntries(all, results);
        }
        results.append("}");
		return results.toString();
	}

    private void appendMethod( StringBuilder sb, CapabilityReference method ) {
        sb.append("{\"name\":").append("\"").append(method.name() ).append("\",");
        sb.append("\"href\": \"").append(method.href()).append("\",");
        sb.append("\"method\":\"").append(method.httpMethod()).append("\"");
        if ( method.rel() != null ) {
            sb.append(", \"rel\":\"").append( method.rel() ).append("\"");
        }
        // todo JSONTemplate
        // sb.append(",\"jsonTemplate\": generateTemplate( method ) );
    }

    private void toMapEntries(List<CapabilityReference> list, StringBuilder results) {
        boolean first = true;
        for (CapabilityReference method : list) {
            if ( !first ) results.append( ",\n");
            else first = false;
            appendMethod(results, method);
        }
    }

    // TODO pass in Resource
	@Override
	public Object renderCommandForm(Method method) {
		return createForm(method, "POST", null);
	}

	@Override
	public Object renderQueryForm(Method method) {
		return createForm(method, "GET", null);
	}

    @Override
    public Object renderListResponse(PagedSortedListResponse response) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        appendLink( sb, "next", response.getNext() );
        appendLink(sb, "previous", response.getPrevious());
        if ( response.getOrderByAsc() != null ) {
            // TODO
        }
        if ( response.getOrderByDesc() != null ) {
            // TODO
        }
        sb.append("\"page\":").append( response.getPage() ).append(",");
        sb.append("\"pageSize\":").append( response.getPageSize() ).append(",");
        sb.append("\"totalPages\":").append( response.getTotalPages() ).append(",");
        sb.append("\"totalElements\":").append( response.getTotalElements() ).append(",");
        sb.append("\"list\":");
        appendList(sb, (List<Object>) response.getList());
        sb.append("}");
        return sb.toString();
    }

    @Override
    public Object renderQueryResponse(Object responseObject) {
        if (responseObject instanceof String) {
            return "\"" + responseObject + "\"";
        } else {
            return new JSONHelper().toJSON(responseObject);
        }
    }

    private void appendLink( StringBuilder sb, String name, String link ) {
        if ( link!= null) {
            sb.append("\"").append(name).append("\":\"").append(link).append("\",");
        }
    }

    private void appendList( final StringBuilder sb, List<Object> list ) {
        if ( list == null || list.size() == 0 ) {
            sb.append("[]");
            return;
        }
        sb.append("[");
        final Map<String, Field> fields = new HashMap<String, Field>();
        // what if first element extends list type -> rte
        iterateFields( list.get(0).getClass(), list.get(0), new HtmlRestReflection.FieldIterator() {
            public void field(Field field) {
                field.setAccessible( true );
                fields.put(field.getName(), field);
            }
        });
        IterableCallback.element( sb, list, new Callback<Object>() {
            public void callback(final Object element) {
                sb.append("{\"");
                IterableCallback.element( sb, fields.entrySet(), new Callback<Map.Entry<String,Field>>() {
                    public void callback(Map.Entry<String, Field> entry) {
                        appendListElement( sb, entry.getKey(), entry.getValue(), element );
                    }
                });
                sb.append("}");
            }
        });
        sb.append("]");
    }

    protected String createForm( Method method, String httpMethod, Resource resource ) {
        if ( method.getParameterTypes().length == 0 ) return "";
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        List<Parameter> parameters = new LinkedList<Parameter>();
        for ( int i =0; i< method.getParameterTypes().length; i++) {
            Class<?> aClass = method.getParameterTypes()[i];
            Annotation[] annotations = parameterAnnotations[i];
            for (Annotation annotation : annotations) {
                if ( annotation instanceof Template ) {
                    String methodName = ((Template) annotation).value();
                    Parameter parameter = null;
                    try {
                        Method template = resource.getClass().getMethod(methodName);
                        if ( aClass.isAssignableFrom( template.getReturnType() ) && Modifier.isPrivate( template.getModifiers() ) ) {
                            template.setAccessible( true );
                            parameters.add( new Parameter( aClass, template.invoke(resource) ));
                        }
                    } catch (Throwable e) {
                        // ignore parameter will be without template
                    } finally {
                        if ( parameter == null ) {
                            parameters.add( new Parameter( aClass ));
                        } else {
                            parameters.add( parameter );
                        }
                    }

                }
            }
        }
        final StringBuilder sb = new StringBuilder();
        IterableCallback.element( sb, parameters, new Callback<Parameter>(){
            public void callback(Parameter parameter) {
                // TODO check for parameter template and use it
                jsonTemplateForParameter(sb, parameter.clazz, parameter.clazz);
            }
        });
        return sb.toString();
    }

    class Parameter {
        private Class<?> clazz;
        private Object template;

        Parameter( Class<?> clazz ) {
            this.clazz = clazz;
        }

        Parameter( Class<?> clazz, Object template) {
            this.clazz = clazz;
            this.template = template;
        }
    }

    private void jsonTemplateForParameter(StringBuilder sb, Class<?> clazz, Type genericType) {
        if ( ReflectionUtil.basicTypes.contains( clazz ) ) {
            defaultInstanceBasic(sb, clazz);
        } else {
            sb.append("{");
            defaultInstanceComposed(sb, clazz, genericType);
            sb.append("}");
        }
    }

    private void defaultInstanceComposed(StringBuilder sb, Class<?> clazz, Type genericType ) {
        if ( genericType instanceof ParameterizedType ) {
            Type rawType = ((ParameterizedType) genericType).getRawType();
            if (  List.class.isAssignableFrom( (Class) rawType ) ) {
                sb.append("[");
                Type type = ((ParameterizedType) genericType).getActualTypeArguments()[0];
                jsonTemplateForParameter(sb, (Class) type, type);
                sb.append("]");
            } else if ( Map.class.isAssignableFrom( (Class) rawType ) ) {
                sb.append("{");
                Type type = ((ParameterizedType) genericType).getActualTypeArguments()[1];
                jsonTemplateForParameter(sb, (Class) type, type);
                sb.append("}");
            }
            // TODO maybe support general parameterized types
            return;
        }

        if ( clazz == Object.class ) {
            sb.append("{}");
            return;
        }

        for (Field field : clazz.getDeclaredFields()) {
            if ( Modifier.isStatic( field.getModifiers() )) continue;
            if ( Modifier.isFinal(field.getModifiers())) continue;
            sb.append("\"").append(field.getName()).append("\":");
            jsonTemplateForParameter(sb, field.getType(), field.getGenericType());
        }
        if ( clazz.getSuperclass() != null && clazz.getSuperclass() != Object.class ) {
            defaultInstanceComposed(sb, clazz.getSuperclass(), clazz.getSuperclass());
        }
    }

    private void defaultInstanceBasic(StringBuilder sb, Class<?> clazz) {
        if ( clazz == String.class ) {
            sb.append("\"\"");
        } else if ( clazz == Long.class || clazz == Integer.class ) {
            sb.append("0");
        } else if ( clazz == Double.class ) {
            sb.append("0.0");
        } else if ( clazz == Boolean.class ) {
            sb.append("false");
        }
    }

    private void appendListElement( StringBuilder sb, String name, Field field, Object element ) {
        try {
            sb.append( name ).append("\":\"").append(field.get(element));
            if ( element instanceof Linkable && name.equals("href") ) {
                sb.append("/");
            }
            sb.append("\"");
        } catch (IllegalAccessException e) {
            sb.append( "\"\"" );
        }
    }

    private void iterateFields( Class clazz, Object instance, HtmlRestReflection.FieldIterator callback ) {
        for (Field field : clazz.getDeclaredFields()) {
            if ( Modifier.isFinal(field.getModifiers())) continue;
            if ( Modifier.isStatic( field.getModifiers() )) continue;
            if ( instance instanceof Linkable && field.getName().equals( "rel") ) continue;
            field.setAccessible(true);
            try {
                callback.field( field );
            } catch (IllegalAccessException e) {
                // ignore
            }
        }
    }


    static class IterableCallback<T> {
        static <T> void element( StringBuilder sb, Iterable<T> iterable, Callback<T> callback ) {
            boolean first = true;
            for (T element : iterable) {
                if ( !first ) sb.append(",");
                else first = false;
                callback.callback( element );
            }
        }
    }

    interface Callback<T> {
        void callback( T element );
    }
}