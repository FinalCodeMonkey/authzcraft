package com.fcm.authzcraft.pap.interfaces.dto;

import com.fcm.authzcraft.pap.domain.model.Principal;

public class PrincipalResponse {
    private String id;
    private String tenantKey;
    private String principalKind;
    private String principalKey;
    private String displayName;
    private String lifecycleState;

    public static PrincipalResponse from(Principal principal) {
        PrincipalResponse response = new PrincipalResponse();
        response.setId(String.valueOf(principal.getId()));
        response.setTenantKey(principal.getTenantKey());
        response.setPrincipalKind(principal.getPrincipalKind());
        response.setPrincipalKey(principal.getPrincipalKey());
        response.setDisplayName(principal.getDisplayName());
        response.setLifecycleState(principal.getLifecycleState());
        return response;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
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