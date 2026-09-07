package com.fcm.authzcraft.pap.interfaces.dto;

public class AccessPolicySearchRequest {
    private String tenantKey;
    private String appKey;
    private String policyKey;
    private String targetRelationId;
    private String operationCode;
    private String lifecycleState;

    public String getTenantKey() { return tenantKey; }
    public void setTenantKey(String tenantKey) { this.tenantKey = tenantKey; }
    public String getAppKey() { return appKey; }
    public void setAppKey(String appKey) { this.appKey = appKey; }
    public String getPolicyKey() { return policyKey; }
    public void setPolicyKey(String policyKey) { this.policyKey = policyKey; }
    public String getTargetRelationId() { return targetRelationId; }
    public void setTargetRelationId(String targetRelationId) { this.targetRelationId = targetRelationId; }
    public String getOperationCode() { return operationCode; }
    public void setOperationCode(String operationCode) { this.operationCode = operationCode; }
    public String getLifecycleState() { return lifecycleState; }
    public void setLifecycleState(String lifecycleState) { this.lifecycleState = lifecycleState; }
}