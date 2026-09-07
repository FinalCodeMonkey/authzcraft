package com.fcm.authzcraft.pdp.domain.model;

public class RelationResourceSnapshot {
    private Long relationId;
    private String resourceKey;
    private String protectionMode;
    private String structureDigest;

    public Long getRelationId() { return relationId; }
    public void setRelationId(Long relationId) { this.relationId = relationId; }
    public String getResourceKey() { return resourceKey; }
    public void setResourceKey(String resourceKey) { this.resourceKey = resourceKey; }
    public String getProtectionMode() { return protectionMode; }
    public void setProtectionMode(String protectionMode) { this.protectionMode = protectionMode; }
    public String getStructureDigest() { return structureDigest; }
    public void setStructureDigest(String structureDigest) { this.structureDigest = structureDigest; }
}