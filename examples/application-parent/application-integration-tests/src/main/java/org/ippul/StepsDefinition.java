package org.ippul;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.AfterAll;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.Scenario;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


public class StepsDefinition {

    private String username;

    private String result;

    @BeforeAll
    public static void beforeAllScenarioExecution() {
        System.out.println("Before all scenarios");
    }

    @Before
    public void beforeScenarioExecution(Scenario scenario) {
        System.out.println("Before scenario " + scenario.getName());
    }
    
    @AfterAll
    public static void afterAllScenarioExecution(){
        System.out.println("After all scenarios");
    }

    @After
    public void afterScenarioExecution(Scenario scenario){
        System.out.println("After scenario " + scenario.getName());
    }
    
    @Given("a user named {string}")
    public void given(String username) throws Throwable {
        this.username = username;
    }

    @When("the service {string} is invoked")
    public void when(String serviceName) throws Throwable {
        HttpClient client = HttpClient.newBuilder().build();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://application-services-cucumber-operator.apps-crc.testing" + serviceName + "/" + username))
            .header("Content-Type", "text/plain")
            .GET()
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        this.result = response.body();
    }

    @Then("the response is {string}")
    public void then(String expectedResult) throws Throwable {
        org.junit.Assert.assertEquals(expectedResult, this.result);
        
    }
}
