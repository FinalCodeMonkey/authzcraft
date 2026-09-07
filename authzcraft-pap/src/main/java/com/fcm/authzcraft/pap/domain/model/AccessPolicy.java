package com.fcm.authzcraft.pap.domain.model;

public class AccessPolicy {
    private Long id;
    private String tenantKey;
    private String appKey;
    private String policyKey;
    private String displayName;
    private String description;
    private Long targetRelationId;
    private String operationCode;
    private String effectKind;
    private String lifecycleState;
    private String ruleBlueprintDisplayName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public Long getTargetRelationId() { return targetRelationId; }
    public void setTargetRelationId(Long targetRelationId) { this.targetRelationId = targetRelationId; }
    public String getOperationCode() { return operationCode; }
    public void setOperationCode(String operationCode) { this.operationCode = operationCode; }
    public String getEffectKind() { return effectKind; }
    public void setEffectKind(String effectKind) { this.effectKind = effectKind; }
    public String getLifecycleState() { return lifecycleState; }
    public void setLifecycleState(String lifecycleState) { this.lifecycleState = lifecycleState; }
    public String getRuleBlueprintDisplayName() { return ruleBlueprintDisplayName; }
    public void setRuleBlueprintDisplayName(String ruleBlueprintDisplayName) { this.ruleBlueprintDisplayName = ruleBlueprintDisplayName; }
}