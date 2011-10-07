package com.jayway.forest.reflection;

import com.jayway.forest.roles.Linkable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public final class JsonRestReflection implements RestReflection {
	
	public static final RestReflection INSTANCE = new JsonRestReflection();
	
	private JsonRestReflection() {
	}

	@Override
	public Object renderCapabilities(Capabilities capabilities) {
        StringBuilder results = new StringBuilder( );
        results.append("[");
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
        results.append("]");
		return results.toString();
	}

    private void appendMethod( StringBuilder sb, CapabilityReference method ) {
        sb.append("{\"href\": \"").append( method.name()).append( "\"");
        sb.append(",\"method\":\"").append(method.httpMethod()).append("\"}");
        // todo JSONTemplate
        // sb.append(",\"jsonTemplate\": generateTemplate( method ) );
    }

    private void toMapEntries(List<CapabilityReference> list, StringBuilder results) {
        boolean first = true;
        for (CapabilityReference method : list) {
            if ( !first ) results.append( ",");
            appendMethod( results, method );
            first = false;
        }
    }

	@Override
	public Object renderCommandForm(Method method) {
		return createForm(method, "POST");
	}

	@Override
	public Object renderQueryForm(Method method) {
		return createForm(method, "GET");
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
        iterateFields( list.get(0).getClass(), list.get(0), new FieldIterator() {
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

    protected String createForm( Method method, String httpMethod ) {
    	return "N/A";
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

    private void iterateFields( Class clazz, Object instance, FieldIterator callback ) {
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

    interface FieldIterator {
        void field( Field field ) throws IllegalAccessException;
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
