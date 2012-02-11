package com.jayway.forest.service;

import java.util.List;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.StatusType;

import com.jayway.forest.Body;
import com.jayway.forest.dto.IntegerDTO;
import com.jayway.forest.roles.Resource;

@Path("")
public class RootResource implements Resource {

//    public RootResource sub() {
//        return new RootResource();
//    }
	
	
    public Response qweqwe( ) {
    	return Response.status(405).entity("qwe").build();
    }

    public void command( @Body String input ) {
        StateHolder.set(input);
    }

    public void addcommand( Integer first, IntegerDTO second ) {
        StateHolder.set( first + second.getInteger() );
    }

    public void addcommandprimitive( Integer first, int second ) {
        StateHolder.set( first + second );
    }

    public void commandlist( List<String> list ) {
        StateHolder.set( "Success"+list.get(0) );
    }

    public void addtolist( List<String> list, String append ) {
        list.add( append );
        StateHolder.set( list );
    }

    public void complex( List<List<List<String>>> list ) {
        list.get(0).get(0).add("NEW");
        StateHolder.set(list);
    }

    /*
    public void commandwithnamedparam(@FormParam("theName") String input ) {
        StateHolder.set(input);
    }

    public void commandenum( Value value ) {
        StateHolder.set( value );
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

    public String echo( String input ) {
        return input;
    }

    @RolesInContext( String.class )
    public String constraint() {
        return role(String.class);
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
    */

}

