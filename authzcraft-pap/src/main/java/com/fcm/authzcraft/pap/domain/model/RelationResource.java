package com.fcm.authzcraft.pap.domain.model;

public class RelationResource {

    private Long id;
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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTenantKey() { return tenantKey; }
    public void setTenantKey(String tenantKey) { this.tenantKey = tenantKey; }
    public String getAppKey() { return appKey; }
    public void setAppKey(String appKey) { this.appKey = appKey; }
    public String getResourceKey() { return resourceKey; }
    public void setResourceKey(String resourceKey) { this.resourceKey = resourceKey; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getNamespaceName() { return namespaceName; }
    public void setNamespaceName(String namespaceName) { this.namespaceName = namespaceName; }
    public String getPhysicalName() { return physicalName; }
    public void setPhysicalName(String physicalName) { this.physicalName = physicalName; }
    public String getRelationKind() { return relationKind; }
    public void setRelationKind(String relationKind) { this.relationKind = relationKind; }
    public String getProtectionMode() { return protectionMode; }
    public void setProtectionMode(String protectionMode) { this.protectionMode = protectionMode; }
    public String getStructureDigest() { return structureDigest; }
    public void setStructureDigest(String structureDigest) { this.structureDigest = structureDigest; }
    public String getLifecycleState() { return lifecycleState; }
    public void setLifecycleState(String lifecycleState) { this.lifecycleState = lifecycleState; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}