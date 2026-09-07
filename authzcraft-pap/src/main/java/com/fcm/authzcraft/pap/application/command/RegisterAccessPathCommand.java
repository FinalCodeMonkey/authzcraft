package com.fcm.authzcraft.pap.application.command;

public class RegisterAccessPathCommand {

    private String tenantKey;
    private String appKey;
    private String pathKey;
    private String displayName;
    private Long rootRelationId;
    private Long destinationRelationId;
    private String executionMode;
    private String traversalSteps;

    public String getTenantKey() { return tenantKey; }
    public void setTenantKey(String tenantKey) { this.tenantKey = tenantKey; }
    public String getAppKey() { return appKey; }
    public void setAppKey(String appKey) { this.appKey = appKey; }
    public String getPathKey() { return pathKey; }
    public void setPathKey(String pathKey) { this.pathKey = pathKey; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public Long getRootRelationId() { return rootRelationId; }
    public void setRootRelationId(Long rootRelationId) { this.rootRelationId = rootRelationId; }
    public Long getDestinationRelationId() { return destinationRelationId; }
    public void setDestinationRelationId(Long destinationRelationId) { this.destinationRelationId = destinationRelationId; }
    public String getExecutionMode() { return executionMode; }
    public void setExecutionMode(String executionMode) { this.executionMode = executionMode; }
    public String getTraversalSteps() { return traversalSteps; }
    public void setTraversalSteps(String traversalSteps) { this.traversalSteps = traversalSteps; }
}