package com.lisbonbigapps.myhoster.rest.api;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import com.lisbonbigapps.myhoster.rest.RestMediaType;
import com.lisbonbigapps.myhoster.rest.exception.NotFoundException;
import com.lisbonbigapps.myhoster.rest.exception.UnauthorizedException;
import com.lisbonbigapps.myhoster.rest.response.factories.HosterResponseFactory;
import com.lisbonbigapps.myhoster.rest.response.factories.UserResponseFactory;
import com.lisbonbigapps.myhoster.rest.response.resources.RootResource;
import com.lisbonbigapps.myhoster.rest.util.Authentication;

@Path("/hosters")
public class HosterFacade {
    @Context
    HttpServletRequest request;
    Authentication auth = new Authentication();

    @GET
    @Produces(RestMediaType.Json)
    public Response getHosters() throws Exception {
	this.auth.setHttpRequest(this.request);	
	if (!this.auth.hasUserSession()) {
	    throw new UnauthorizedException();
	}

	UserResponseFactory factory = new UserResponseFactory();
	return Response.ok(factory.getHosters()).build();
    }

    @GET
    @Path("{id}")
    @Produces(RestMediaType.Json)
    public Response getHosterById(@PathParam("id") long id) throws Exception {
	this.auth.setHttpRequest(this.request);	
	if (!this.auth.hasUserSession()) {
	    throw new UnauthorizedException();
	}

	RootResource resource = new HosterResponseFactory().getHoster(id);
	if (resource == null) {
	    throw new NotFoundException();
	}

	return Response.ok(resource).build();
    }
}