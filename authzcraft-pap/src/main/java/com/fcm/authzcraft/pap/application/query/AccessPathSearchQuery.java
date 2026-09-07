package com.fcm.authzcraft.pap.application.query;

public class AccessPathSearchQuery {

    private String tenantKey;
    private String appKey;
    private String pathKey;
    private String rootRelationId;
    private String lifecycleState;

    public String getTenantKey() { return tenantKey; }
    public void setTenantKey(String tenantKey) { this.tenantKey = tenantKey; }
    public String getAppKey() { return appKey; }
    public void setAppKey(String appKey) { this.appKey = appKey; }
    public String getPathKey() { return pathKey; }
    public void setPathKey(String pathKey) { this.pathKey = pathKey; }
    public String getRootRelationId() { return rootRelationId; }
    public void setRootRelationId(String rootRelationId) { this.rootRelationId = rootRelationId; }
    public String getLifecycleState() { return lifecycleState; }
    public void setLifecycleState(String lifecycleState) { this.lifecycleState = lifecycleState; }
}