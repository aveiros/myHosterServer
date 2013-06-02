package com.lisbonbigapps.myhoster.rest.api;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import com.lisbonbigapps.myhoster.rest.RestMediaType;
import com.lisbonbigapps.myhoster.rest.RestMessage;
import com.lisbonbigapps.myhoster.rest.exception.BadRequestException;
import com.lisbonbigapps.myhoster.rest.exception.InternalServerException;
import com.lisbonbigapps.myhoster.rest.exception.NotFoundException;
import com.lisbonbigapps.myhoster.rest.exception.UnauthorizedException;
import com.lisbonbigapps.myhoster.rest.response.factories.MessageResponseFactory;
import com.lisbonbigapps.myhoster.rest.response.factories.UserResponseFactory;
import com.lisbonbigapps.myhoster.rest.response.resources.RootResource;
import com.lisbonbigapps.myhoster.rest.response.resources.UserResource;
import com.lisbonbigapps.myhoster.rest.response.resources.UserSessionResource;
import com.lisbonbigapps.myhoster.rest.util.Authentication;
import com.lisbonbigapps.myhoster.xmpp.XmppServerFacade;

@Path("/user")
public class UserFacade {
    @Context
    HttpServletRequest req;

    Authentication auth = new Authentication();

    @GET
    @Produces(RestMediaType.Json)
    public Response getUser() {
	HttpSession session = req.getSession(true);
	UserSessionResource userSession = (UserSessionResource) session.getAttribute("SESSION_OBJECT");

	if (userSession == null) {
	    throw new UnauthorizedException();
	}

	RootResource userResource = new UserResponseFactory().getUser(userSession.getUserId());

	if (userResource == null) {
	    throw new NotFoundException();
	}

	return Response.ok(userResource).build();
    }

    @GET
    @Path("login")
    @Produces(RestMediaType.Json)
    public Response getLogin(@QueryParam("username") String username, @QueryParam("password") String password) throws Exception {
	HttpSession session = req.getSession(true);

	UserResponseFactory userFactory = new UserResponseFactory();
	RootResource userResource = userFactory.getUser(username, password);

	if (userResource == null) {
	    return Response.ok(new MessageResponseFactory().createError(RestMessage.UserOrPasswordInvalid)).build();
	}

	session.setAttribute("SESSION_OBJECT", userFactory.createUserSession((UserResource) userResource));
	return Response.ok(userResource).build();
    }

    @GET
    @Path("logout")
    @Produces(RestMediaType.Json)
    public Response getLogout() throws Exception {
	HttpSession session = req.getSession(true);
	UserSessionResource userSession = (UserSessionResource) session.getAttribute("SESSION_OBJECT");
	session.removeAttribute("SESSION_OBJECT");

	MessageResponseFactory factory = new MessageResponseFactory();
	RootResource logoutResource = factory.createMessage(userSession == null ? "User has to perform a login!" : "User logged out successfully!");
	return Response.ok(logoutResource).build();
    }

    @GET
    @Path("register")
    @Produces(RestMediaType.Json)
    public Response registerUser(@QueryParam("username") String username, @QueryParam("password") String password) throws Exception {
	if (username == null || username.equals("")) {
	    throw new BadRequestException();
	}

	if (password == null || password.equals("")) {
	    throw new BadRequestException();
	}

	XmppServerFacade xmppServer = new XmppServerFacade();
	if (!xmppServer.isOnline()) {
	    throw new InternalServerException();
	}

	UserResponseFactory userFactory = new UserResponseFactory();

	boolean xmppHasUser = xmppServer.isUserRegistered(username);
	boolean hostHasUser = userFactory.getUser(username) == null ? false : true;

	if (xmppHasUser || hostHasUser) {
	    return Response.ok(new MessageResponseFactory().createError("User already exists")).build();
	} else {
	    xmppServer.registerUser(username, password);
	    return Response.ok(userFactory.registerUser(username, password)).build();
	}
    }

    @GET
    @Path("{id}")
    @Produces(RestMediaType.Json)
    public Response getUserById(@PathParam("id") Long id) throws Exception {
	HttpSession session = req.getSession(true);
	UserSessionResource userSession = (UserSessionResource) session.getAttribute("SESSION_OBJECT");

	if (userSession == null) {
	    throw new UnauthorizedException();
	}

	if (id == null) {
	    throw new BadRequestException();
	}

	RootResource userResource = new UserResponseFactory().getUser(id);

	if (userResource == null) {
	    throw new NotFoundException();
	}

	return Response.ok(userResource).build();
    }

    @PUT
    @Path("/hosting")
    @Produces(RestMediaType.Json)
    public Response updateHostingStatus(@QueryParam("status") Boolean status) throws Exception {
	this.auth.setHttpRequest(this.req);
	if (!this.auth.hasUserSession()) {
	    throw new UnauthorizedException();
	}

	if (status == null) {
	    throw new BadRequestException();
	}

	UserResponseFactory factory = new UserResponseFactory();
	factory.updateHostingStatus(this.auth.getUserId(), status);

	return Response.ok().build();
    }

    @GET
    @Path("/available")
    @Produces(RestMediaType.Json)
    public Response userAvailable(@QueryParam("name") String userName) throws Exception {
	if (userName == null || userName.equals("")) {
	    throw new BadRequestException();
	}

	XmppServerFacade xmppServer = new XmppServerFacade();
	if (!xmppServer.isOnline()) {
	    throw new InternalServerException();
	}

	boolean xmppHasUser = xmppServer.isUserRegistered(userName);
	boolean hostHasUser = new UserResponseFactory().getUser(userName) == null ? false : true;

	return Response.ok(new MessageResponseFactory().createMessage(String.valueOf(hostHasUser || xmppHasUser))).build();
    }

    @GET
    @Path("/location")
    @Produces(RestMediaType.Json)
    public Response getLocation() throws Exception {
	this.auth.setHttpRequest(this.req);
	if (!this.auth.hasUserSession()) {
	    throw new UnauthorizedException();
	}

	RootResource geoResource = new UserResponseFactory().getLocation(this.auth.getUserId());
	if (geoResource == null) {
	    throw new NotFoundException();
	}

	return Response.ok(geoResource).build();
    }

    @PUT
    @Path("/location")
    @Produces(RestMediaType.Json)
    public Response updateLocation(@QueryParam("latitude") Double latitude, @QueryParam("longitude") Double longitude) throws Exception {
	this.auth.setHttpRequest(this.req);
	if (!this.auth.hasUserSession()) {
	    throw new UnauthorizedException();
	}

	if (latitude == null || longitude == null) {
	    throw new BadRequestException();
	}

	RootResource resource = new UserResponseFactory().updateLocation(this.auth.getUserId(), latitude, longitude);
	if (resource == null) {
	    throw new NotFoundException();
	}

	return Response.ok(resource).build();
    }

    @GET
    @Path("{id}/feedback")
    @Produces(RestMediaType.Json)
    public Response getUserFeedback(@PathParam("id") Long userId) throws Exception {
	this.auth.setHttpRequest(this.req);
	if (!this.auth.hasUserSession()) {
	    throw new UnauthorizedException();
	}

	if (userId == null) {
	    throw new BadRequestException();
	}

	UserResponseFactory factory = new UserResponseFactory();
	List<RootResource> resource = factory.getUserFeedback(userId);
	if (resource == null) {
	    throw new NotFoundException();
	}

	return Response.ok(resource).build();
    }

    @POST
    @Path("/feedback")
    @Produces(RestMediaType.Json)
    public Response createUserFeedback(@QueryParam("id") Long userId, @QueryParam("text") String text) throws Exception {
	this.auth.setHttpRequest(this.req);
	if (!this.auth.hasUserSession()) {
	    throw new UnauthorizedException();
	}

	if (text == null || text.equals("") || userId == null) {
	    throw new BadRequestException();
	}

	UserResponseFactory factory = new UserResponseFactory();
	RootResource resource = factory.createUserFeedback(this.auth.getUserId(), userId, text);
	if (resource == null) {
	    throw new NotFoundException();
	}

	return Response.ok(resource).build();
    }
}
