package com.jayway.forest.legacy.service;

import com.jayway.forest.legacy.constraint.RolesInContext;
import com.jayway.forest.legacy.dto.IntegerDTO;
import com.jayway.forest.legacy.dto.StringDTO;
import com.jayway.forest.legacy.dto.Value;
import com.jayway.forest.legacy.exceptions.NotFoundException;
import com.jayway.forest.legacy.roles.Resource;

import java.util.List;

import javax.ws.rs.FormParam;
import javax.ws.rs.QueryParam;

import static com.jayway.forest.legacy.core.RoleManager.role;

/**
 */
public class RootResource implements Resource {

    public void command( String input ) {
        StateHolder.set(input);
    }

    public void commandwithnamedparam(@FormParam("theName") String input ) {
        StateHolder.set(input);
    }

    public void commandenum( Value value ) {
        StateHolder.set( value );
    }

    public void commandlist( List<String> list ) {
        StateHolder.set( "Success"+list.get(0) );
    }

    public void addtolist( List<String> list, String append ) {
        list.add( append );
        StateHolder.set( list );
    }

    public IntegerDTO addten( Integer number ) {
        return new IntegerDTO( number + 10);
    }

    public IntegerDTO namedaddten( @QueryParam("paramWithName") Integer number ) {
        return new IntegerDTO( number + 10);
    }

    public Integer add( Integer first, IntegerDTO second ) {
        return first + second.getInteger();
    }

    public void addcommand( Integer first, IntegerDTO second ) {
        StateHolder.set( first + second.getInteger() );
    }

    public String echo( String input ) {
        return input;
    }

    @RolesInContext( String.class )
    public String constraint() {
        return role(String.class);
    }

    public void complex( List<List<List<String>>> list ) {
        list.get(0).get(0).add("NEW");
        StateHolder.set(list);
    }

    public String throwingnotfound() {
        throw new NotFoundException("Bad stuff");
    }

    public StringDTO getstring() {
        Object string = StateHolder.get();
        return (StringDTO) string;
    }

    public RootResource sub() {
        return new RootResource();
    }

    public Resource listresponse() {
        return new ListResponseResource();
    }

    public Resource templates() {
        return new TemplateResource();
    }

    public Resource types() {
        return new TypesResource();
    }

    public Resource exceptions() {
        return new ExceptionsResource();
    }

    public OtherResource other() {
        return new OtherResource();
    }

    public UpdateResource update() {
        return new UpdateResource();
    }
}

