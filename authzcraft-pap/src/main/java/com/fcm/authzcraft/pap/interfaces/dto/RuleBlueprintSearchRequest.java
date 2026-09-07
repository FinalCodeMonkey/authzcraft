package com.fcm.authzcraft.pap.interfaces.dto;

public class RuleBlueprintSearchRequest {
    private String tenantKey;
    private String blueprintKey;
    private String lifecycleState;

    public String getTenantKey() { return tenantKey; }
    public void setTenantKey(String tenantKey) { this.tenantKey = tenantKey; }
    public String getBlueprintKey() { return blueprintKey; }
    public void setBlueprintKey(String blueprintKey) { this.blueprintKey = blueprintKey; }
    public String getLifecycleState() { return lifecycleState; }
    public void setLifecycleState(String lifecycleState) { this.lifecycleState = lifecycleState; }
}