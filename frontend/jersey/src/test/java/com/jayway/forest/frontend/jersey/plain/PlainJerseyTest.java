package com.jayway.forest.frontend.jersey.plain;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.parsing.Parser.JSON;
import static org.hamcrest.CoreMatchers.is;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.testing.ServletTester;

import com.jayway.restassured.RestAssured;

public class PlainJerseyTest {
    private static ServletTester tester;

	@BeforeClass
    public static void initServletContainer () throws Exception
    {
        tester = new ServletTester();
        tester.setContextPath("/app");
        ServletHolder servlet = tester.addServlet(com.sun.jersey.spi.container.servlet.ServletContainer.class, "/*");
        servlet.setInitParameter("com.sun.jersey.config.property.packages", "com.jayway.forest.frontend.jersey.plain");
        servlet.setInitParameter("com.sun.jersey.api.json.POJOMappingFeature", "true");
        RestAssured.baseURI = tester.createSocketConnector(true);
        RestAssured.defaultParser = JSON;
        RestAssured.basePath = "/app";
        RestAssured.requestContentType("application/json");
        tester.start();
    }

    /**
     * Stops the Jetty container.
     */
    @AfterClass
    public static void cleanupServletContainer () throws Exception
    {
        tester.stop();
        RestAssured.reset();
    }

    @Test
    public void doTest() {
        given().
        	queryParam("param", "hello").
        expect().
        	body(is("hello")).
        when().
        	get("/simpleEcho");
    }

    @Test
    public void doTest2() {
        given().
        	queryParam("param", "hello").
        expect().
        	body("string", is("hello")).
        when().
        	get("/objectEcho");
    }

    @Test
    public void doTest3() {
        given().
        	queryParam("param", "hello").
        expect().
        	body(is("subben:hello")).
        when().
        	get("/sub/subben/echo");
    }
}
