package com.jayway.forest.reflection.impl;

import java.io.InputStream;
import java.util.Map;

import com.jayway.forest.core.MediaTypeHandler;
import com.jayway.forest.exceptions.MethodNotAllowedRenderTemplateException;
import com.jayway.forest.roles.DeletableResource;

public class CapabilityDeleteCommand extends CapabilityCommand {

    private DeletableResource deletable;

    public CapabilityDeleteCommand(DeletableResource resource, String documentation ) {
		super(DeletableResource.class.getDeclaredMethods()[0], resource, documentation, "delete");
        this.deletable = resource;
	}

    @Override
    public String httpMethod() {
        return "DELETE";
    }

    @Override
    public void delete() {
        deletable.delete();
    }

    @Override
    public void post(Map<String, String[]> formParams, InputStream stream, MediaTypeHandler mediaTypeHandler) {
        if ( mediaTypeHandler.acceptHtml() ) {
            super.post(formParams, stream, mediaTypeHandler);
        } else {
            throw new MethodNotAllowedRenderTemplateException( this );
        }
    }
}
