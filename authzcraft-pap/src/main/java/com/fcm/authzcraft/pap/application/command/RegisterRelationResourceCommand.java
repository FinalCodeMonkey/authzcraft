package com.fcm.authzcraft.pap.application.command;

public class RegisterRelationResourceCommand {

    private String tenantKey;
    private String appKey;
    private String resourceKey;
    private String displayName;
    private String namespaceName;
    private String physicalName;
    private String relationKind;
    private String protectionMode;
    private String description;

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
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}