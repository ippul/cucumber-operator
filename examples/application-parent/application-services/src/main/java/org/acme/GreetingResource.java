package org.acme;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/greetings")
public class GreetingResource {

    @GET
    @Path("{username}")
    @Produces(MediaType.TEXT_PLAIN)
    public String hello(String username) {
        return "Hello " + username;
    }
}
