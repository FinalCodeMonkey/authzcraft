package com.fcm.authzcraft.pap.interfaces.dto;

import com.fcm.authzcraft.pap.domain.model.AccessPath;

public class AccessPathResponse {

    private String id;
    private String tenantKey;
    private String appKey;
    private String pathKey;
    private String displayName;
    private String rootRelationId;
    private String destinationRelationId;
    private String executionMode;
    private String traversalSteps;
    private String lifecycleState;

    public static AccessPathResponse from(AccessPath accessPath) {
        AccessPathResponse response = new AccessPathResponse();
        response.id = String.valueOf(accessPath.getId());
        response.tenantKey = accessPath.getTenantKey();
        response.appKey = accessPath.getAppKey();
        response.pathKey = accessPath.getPathKey();
        response.displayName = accessPath.getDisplayName();
        response.rootRelationId = String.valueOf(accessPath.getRootRelationId());
        response.destinationRelationId = String.valueOf(accessPath.getDestinationRelationId());
        response.executionMode = accessPath.getExecutionMode();
        response.traversalSteps = accessPath.getTraversalSteps();
        response.lifecycleState = accessPath.getLifecycleState();
        return response;
    }

    public String getId() { return id; }
    public String getTenantKey() { return tenantKey; }
    public String getAppKey() { return appKey; }
    public String getPathKey() { return pathKey; }
    public String getDisplayName() { return displayName; }
    public String getRootRelationId() { return rootRelationId; }
    public String getDestinationRelationId() { return destinationRelationId; }
    public String getExecutionMode() { return executionMode; }
    public String getTraversalSteps() { return traversalSteps; }
    public String getLifecycleState() { return lifecycleState; }
}