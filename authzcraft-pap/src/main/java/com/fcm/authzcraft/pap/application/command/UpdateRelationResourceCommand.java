package com.fcm.authzcraft.pap.application.command;

public class UpdateRelationResourceCommand {

    private String displayName;
    private String protectionMode;
    private String lifecycleState;
    private String description;

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getProtectionMode() { return protectionMode; }
    public void setProtectionMode(String protectionMode) { this.protectionMode = protectionMode; }
    public String getLifecycleState() { return lifecycleState; }
    public void setLifecycleState(String lifecycleState) { this.lifecycleState = lifecycleState; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}