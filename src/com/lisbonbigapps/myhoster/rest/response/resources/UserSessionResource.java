package com.lisbonbigapps.myhoster.rest.response.resources;

public class UserSessionResource extends RootResource {
    long userId;

    public long getUserId() {
	return userId;
    }

    public void setUserId(long userId) {
	this.userId = userId;
    }
}
