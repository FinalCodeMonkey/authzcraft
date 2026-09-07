package com.fcm.authzcraft.pdp.domain.model;

public class EffectiveGrantSnapshot {
    private Long grantId;
    private Long principalId;
    private Long policyId;

    public Long getGrantId() { return grantId; }
    public void setGrantId(Long grantId) { this.grantId = grantId; }
    public Long getPrincipalId() { return principalId; }
    public void setPrincipalId(Long principalId) { this.principalId = principalId; }
    public Long getPolicyId() { return policyId; }
    public void setPolicyId(Long policyId) { this.policyId = policyId; }
}