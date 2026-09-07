package com.fcm.authzcraft.pap.domain.model;

public class Principal {
    private Long id;
    private String tenantKey;
    private String principalKind;
    private String principalKey;
    private String displayName;
    private String lifecycleState;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTenantKey() { return tenantKey; }
    public void setTenantKey(String tenantKey) { this.tenantKey = tenantKey; }
    public String getPrincipalKind() { return principalKind; }
    public void setPrincipalKind(String principalKind) { this.principalKind = principalKind; }
    public String getPrincipalKey() { return principalKey; }
    public void setPrincipalKey(String principalKey) { this.principalKey = principalKey; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getLifecycleState() { return lifecycleState; }
    public void setLifecycleState(String lifecycleState) { this.lifecycleState = lifecycleState; }
}