package com.fcm.authzcraft.pap.application.query;

public class PrincipalSearchQuery {
    private String tenantKey;
    private String principalKind;
    private String principalKey;
    private String keyword;
    private String lifecycleState;

    public String getTenantKey() { return tenantKey; }
    public void setTenantKey(String tenantKey) { this.tenantKey = tenantKey; }
    public String getPrincipalKind() { return principalKind; }
    public void setPrincipalKind(String principalKind) { this.principalKind = principalKind; }
    public String getPrincipalKey() { return principalKey; }
    public void setPrincipalKey(String principalKey) { this.principalKey = principalKey; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getLifecycleState() { return lifecycleState; }
    public void setLifecycleState(String lifecycleState) { this.lifecycleState = lifecycleState; }
}