package com.fcm.authzcraft.api.plan;

public class PlanResource {

    private String resourceKey;
    private ResourceQueryRole queryRole;

    public PlanResource() {
    }

    public PlanResource(String resourceKey, ResourceQueryRole queryRole) {
        this.resourceKey = resourceKey;
        this.queryRole = queryRole;
    }

    public String getResourceKey() {
        return resourceKey;
    }

    public void setResourceKey(String resourceKey) {
        this.resourceKey = resourceKey;
    }

    public ResourceQueryRole getQueryRole() {
        return queryRole;
    }

    public void setQueryRole(ResourceQueryRole queryRole) {
        this.queryRole = queryRole;
    }
}