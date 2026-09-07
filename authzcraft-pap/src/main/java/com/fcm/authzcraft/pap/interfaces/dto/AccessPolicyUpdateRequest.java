package com.fcm.authzcraft.pap.interfaces.dto;

public class AccessPolicyUpdateRequest {
    private String displayName;
    private String description;
    private String operationCode;
    private String effectKind;
    private String lifecycleState;

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getOperationCode() { return operationCode; }
    public void setOperationCode(String operationCode) { this.operationCode = operationCode; }
    public String getEffectKind() { return effectKind; }
    public void setEffectKind(String effectKind) { this.effectKind = effectKind; }
    public String getLifecycleState() { return lifecycleState; }
    public void setLifecycleState(String lifecycleState) { this.lifecycleState = lifecycleState; }
}