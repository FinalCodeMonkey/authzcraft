package com.fcm.authzcraft.pap.domain.model;

import java.time.LocalDateTime;

public class AccessGrant {
    private Long id;
    private String tenantKey;
    private String appKey;
    private String grantKey;
    private Long principalId;
    private Long policyId;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private String lifecycleState;
    private String grantSource;
    private String reason;
    private String revokedBy;
    private LocalDateTime revokedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTenantKey() { return tenantKey; }
    public void setTenantKey(String tenantKey) { this.tenantKey = tenantKey; }
    public String getAppKey() { return appKey; }
    public void setAppKey(String appKey) { this.appKey = appKey; }
    public String getGrantKey() { return grantKey; }
    public void setGrantKey(String grantKey) { this.grantKey = grantKey; }
    public Long getPrincipalId() { return principalId; }
    public void setPrincipalId(Long principalId) { this.principalId = principalId; }
    public Long getPolicyId() { return policyId; }
    public void setPolicyId(Long policyId) { this.policyId = policyId; }
    public LocalDateTime getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDateTime validFrom) { this.validFrom = validFrom; }
    public LocalDateTime getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDateTime validUntil) { this.validUntil = validUntil; }
    public String getLifecycleState() { return lifecycleState; }
    public void setLifecycleState(String lifecycleState) { this.lifecycleState = lifecycleState; }
    public String getGrantSource() { return grantSource; }
    public void setGrantSource(String grantSource) { this.grantSource = grantSource; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getRevokedBy() { return revokedBy; }
    public void setRevokedBy(String revokedBy) { this.revokedBy = revokedBy; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }
}