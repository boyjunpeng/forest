package com.jayway.forest.samples.bank.grove.resources.accounts;

import com.jayway.forest.samples.bank.grove.RestService;
import com.jayway.restassured.RestAssured;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.mortbay.jetty.testing.ServletTester;

import static com.jayway.restassured.parsing.Parser.JSON;

public class AbstractTestBase {

    protected static ServletTester tester;

    @BeforeClass
    public static void initServletContainer () throws Exception
    {
        tester = new ServletTester();
        tester.setContextPath("/");
        tester.addServlet(RestService.class, "/bank/*");
        RestAssured.baseURI = tester.createSocketConnector(true);
        RestAssured.defaultParser = JSON;
        RestAssured.basePath = "/bank";
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

}
