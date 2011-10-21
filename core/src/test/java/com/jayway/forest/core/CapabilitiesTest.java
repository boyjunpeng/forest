package com.jayway.forest.core;

import com.jayway.forest.service.AbstractRunner;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

import static com.jayway.restassured.RestAssured.expect;
import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;

/**
 */
public class CapabilitiesTest extends AbstractRunner {
    
    @Test
    public void testRoot() throws IOException {
        System.out.println( get("/").asString() );
    }


    @Test
    public void discover() {
        String s = expect().get("/").asString();
        Object parse = JSONValue.parse(s);
        Assert.assertTrue( parse instanceof JSONArray);

        JSONArray array = (JSONArray) parse;
        Assert.assertTrue("Must have more than 10 elements", array.size() > 10 );
        for (Object elm : array) {
            assertElement((JSONObject) elm);
        }
    }

    @Test
    public void discoverHtml() {
        String s = given().spec(acceptTextHtml()).get("/").asString();
        Assert.assertTrue("Must have element 'addwithtemplates'", s.contains("addwithtemplates"));
        Assert.assertTrue("Must have element 'addwithwrongtemplates'", s.contains("addwithwrongtemplates"));
    }

    private void assertElement( JSONObject obj ) {
        Assert.assertTrue("Must have element 'name'", obj.get("name") != null);
        Assert.assertTrue("Must have element 'href'", obj.get("href") != null);
        Assert.assertTrue("Must have element 'method'", obj.get("method") != null);
    }

    @Test
    public void testPathEvaluationWrong() {
        expect().statusCode(404).get("/sub/sub2/");
    }

}
