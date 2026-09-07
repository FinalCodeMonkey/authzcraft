package com.fcm.authzcraft.pap.interfaces.dto;

public class AccessPathUpdateRequest {

    private String displayName;
    private String executionMode;
    private String traversalSteps;
    private String lifecycleState;

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getExecutionMode() { return executionMode; }
    public void setExecutionMode(String executionMode) { this.executionMode = executionMode; }
    public String getTraversalSteps() { return traversalSteps; }
    public void setTraversalSteps(String traversalSteps) { this.traversalSteps = traversalSteps; }
    public String getLifecycleState() { return lifecycleState; }
    public void setLifecycleState(String lifecycleState) { this.lifecycleState = lifecycleState; }
}