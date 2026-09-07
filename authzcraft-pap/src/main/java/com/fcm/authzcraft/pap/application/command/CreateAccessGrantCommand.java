package com.fcm.authzcraft.pap.application.command;

public class CreateAccessGrantCommand {
    private String tenantKey;
    private String appKey;
    private String grantKey;
    private String principalId;
    private String policyId;
    private String validFrom;
    private String validUntil;
    private String grantSource;
    private String reason;

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
    public String getGrantSource() { return grantSource; }
    public void setGrantSource(String grantSource) { this.grantSource = grantSource; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}