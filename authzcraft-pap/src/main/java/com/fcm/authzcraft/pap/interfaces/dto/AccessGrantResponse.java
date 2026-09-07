package com.fcm.authzcraft.pap.interfaces.dto;

import com.fcm.authzcraft.pap.domain.model.AccessGrant;

public class AccessGrantResponse {
    private String id;
    private String tenantKey;
    private String appKey;
    private String grantKey;
    private String principalId;
    private String policyId;
    private String validFrom;
    private String validUntil;
    private String lifecycleState;
    private String grantSource;
    private String reason;
    private String revokedBy;
    private String revokedAt;

    public static AccessGrantResponse from(AccessGrant grant) {
        AccessGrantResponse response = new AccessGrantResponse();
        response.setId(String.valueOf(grant.getId()));
        response.setTenantKey(grant.getTenantKey());
        response.setAppKey(grant.getAppKey());
        response.setGrantKey(grant.getGrantKey());
        response.setPrincipalId(String.valueOf(grant.getPrincipalId()));
        response.setPolicyId(String.valueOf(grant.getPolicyId()));
        response.setValidFrom(grant.getValidFrom() == null ? null : grant.getValidFrom().toString());
        response.setValidUntil(grant.getValidUntil() == null ? null : grant.getValidUntil().toString());
        response.setLifecycleState(grant.getLifecycleState());
        response.setGrantSource(grant.getGrantSource());
        response.setReason(grant.getReason());
        response.setRevokedBy(grant.getRevokedBy());
        response.setRevokedAt(grant.getRevokedAt() == null ? null : grant.getRevokedAt().toString());
        return response;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTenantKey() { return tenantKey; }
    public void setTenantKey(String tenantKey) { this.tenantKey = tenantKey; }
    public String getAppKey() { return appKey; }
    public void setAppKey(String appKey) { this.appKey = appKey; }
    public String getGrantKey() { return grantKey; }
    public void setGrantKey(String grantKey) { this.grantKey = grantKey; }
    public String getPrincipalId() { return principalId; }
    public void setPrincipalId(String principalId) { this.principalId = principalId; }
    public String getPolicyId() { return policyId; }
    public void setPolicyId(String policyId) { this.policyId = policyId; }
    public String getValidFrom() { return validFrom; }
    public void setValidFrom(String validFrom) { this.validFrom = validFrom; }
    public String getValidUntil() { return validUntil; }
    public void setValidUntil(String validUntil) { this.validUntil = validUntil; }
    public String getLifecycleState() { return lifecycleState; }
    public void setLifecycleState(String lifecycleState) { this.lifecycleState = lifecycleState; }
    public String getGrantSource() { return grantSource; }
    public void setGrantSource(String grantSource) { this.grantSource = grantSource; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getRevokedBy() { return revokedBy; }
    public void setRevokedBy(String revokedBy) { this.revokedBy = revokedBy; }
    public String getRevokedAt() { return revokedAt; }
    public void setRevokedAt(String revokedAt) { this.revokedAt = revokedAt; }
}