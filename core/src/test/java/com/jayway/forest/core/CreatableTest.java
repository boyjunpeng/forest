package com.jayway.forest.core;

import com.jayway.forest.service.AbstractRunner;
import com.jayway.forest.service.StateHolder;
import com.jayway.restassured.RestAssured;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.Assert;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.basePath;
import static com.jayway.restassured.RestAssured.baseURI;
import static com.jayway.restassured.RestAssured.given;

/**
 */
public class CreatableTest extends AbstractRunner {

    @Test
    public void testCreate() {
        String result = given().body("{\"string\":\"Hello\", \"integer\": 25}").
        expect().statusCode( 201 ).header("Location", baseURI + basePath + "/other/Hello/" + 25 + "/")
                .when().post("/other/").andReturn().asString();

        JSONObject o = (JSONObject) JSONValue.parse(result);
        Assert.assertEquals( String.format("%s%s/other/Hello/25/", baseURI, basePath), o.get("uri") );
        Assert.assertEquals( "GET", o.get("method") );
        Assert.assertEquals( "Hello", o.get("name") );
        Assert.assertEquals( "child", o.get("rel") );
    }

    @Test
    public void testDelete() {
        given().expect().statusCode(200).delete("/other/");

        Assert.assertEquals( "Delete invoked", StateHolder.get() );
    }

    @Test
    public void testCreateHtml() {
        String result = given().spec( contentTypeFormUrlEncoded() ).expect().statusCode(201)
                .header("Location", baseURI + basePath + "/other/hello/" + 25 + "/")
                .post("/other/?argument1.StringAndIntegerDTO.string=hello&argument1.StringAndIntegerDTO.integer=25").andReturn().asString();

        String expect = String.format("<code>Location:</code> <a href='%s/bank/other/hello/25/' rel='child'>hello</a>", baseURI);
        Assert.assertTrue( result.contains(expect) );
    }
}
