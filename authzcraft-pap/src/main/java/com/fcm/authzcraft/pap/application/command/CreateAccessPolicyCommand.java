package com.fcm.authzcraft.pap.application.command;

public class CreateAccessPolicyCommand {
    private String tenantKey;
    private String appKey;
    private String policyKey;
    private String displayName;
    private String description;
    private String targetRelationId;
    private String operationCode;
    private String effectKind;

    public String getTenantKey() { return tenantKey; }
    public void setTenantKey(String tenantKey) { this.tenantKey = tenantKey; }
    public String getAppKey() { return appKey; }
    public void setAppKey(String appKey) { this.appKey = appKey; }
    public String getPolicyKey() { return policyKey; }
    public void setPolicyKey(String policyKey) { this.policyKey = policyKey; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getTargetRelationId() { return targetRelationId; }
    public void setTargetRelationId(String targetRelationId) { this.targetRelationId = targetRelationId; }
    public String getOperationCode() { return operationCode; }
    public void setOperationCode(String operationCode) { this.operationCode = operationCode; }
    public String getEffectKind() { return effectKind; }
    public void setEffectKind(String effectKind) { this.effectKind = effectKind; }
}