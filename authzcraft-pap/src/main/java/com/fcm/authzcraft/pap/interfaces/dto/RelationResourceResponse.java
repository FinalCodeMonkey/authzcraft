package com.fcm.authzcraft.pap.interfaces.dto;

import com.fcm.authzcraft.pap.domain.model.RelationResource;

public class RelationResourceResponse {

    private String id;
    private String tenantKey;
    private String appKey;
    private String resourceKey;
    private String displayName;
    private String namespaceName;
    private String physicalName;
    private String relationKind;
    private String protectionMode;
    private String structureDigest;
    private String lifecycleState;
    private String description;

    public static RelationResourceResponse from(RelationResource resource) {
        RelationResourceResponse response = new RelationResourceResponse();
        response.id = String.valueOf(resource.getId());
        response.tenantKey = resource.getTenantKey();
        response.appKey = resource.getAppKey();
        response.resourceKey = resource.getResourceKey();
        response.displayName = resource.getDisplayName();
        response.namespaceName = resource.getNamespaceName();
        response.physicalName = resource.getPhysicalName();
        response.relationKind = resource.getRelationKind();
        response.protectionMode = resource.getProtectionMode();
        response.structureDigest = resource.getStructureDigest();
        response.lifecycleState = resource.getLifecycleState();
        response.description = resource.getDescription();
        return response;
    }

    public String getId() { return id; }
    public String getTenantKey() { return tenantKey; }
    public String getAppKey() { return appKey; }
    public String getResourceKey() { return resourceKey; }
    public String getDisplayName() { return displayName; }
    public String getNamespaceName() { return namespaceName; }
    public String getPhysicalName() { return physicalName; }
    public String getRelationKind() { return relationKind; }
    public String getProtectionMode() { return protectionMode; }
    public String getStructureDigest() { return structureDigest; }
    public String getLifecycleState() { return lifecycleState; }
    public String getDescription() { return description; }
}