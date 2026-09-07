package com.fcm.authzcraft.pip.application.command;

import java.util.Collections;
import java.util.List;

public class ApplicationRbacPrincipalSyncCommand {
    private final String tenantKey;
    private final String appKey;
    private final boolean deactivateMissing;
    private final List<RoleItem> roles;
    private final List<GroupItem> groups;
    private final List<MembershipItem> memberships;

    public ApplicationRbacPrincipalSyncCommand(String tenantKey,
                                               String appKey,
                                               boolean deactivateMissing,
                                               List<RoleItem> roles,
                                               List<GroupItem> groups,
                                               List<MembershipItem> memberships) {
        this.tenantKey = tenantKey;
        this.appKey = appKey;
        this.deactivateMissing = deactivateMissing;
        this.roles = roles == null ? Collections.<RoleItem>emptyList() : roles;
        this.groups = groups == null ? Collections.<GroupItem>emptyList() : groups;
        this.memberships = memberships == null ? Collections.<MembershipItem>emptyList() : memberships;
    }

    public String getTenantKey() { return tenantKey; }
    public String getAppKey() { return appKey; }
    public boolean isDeactivateMissing() { return deactivateMissing; }
    public List<RoleItem> getRoles() { return roles; }
    public List<GroupItem> getGroups() { return groups; }
    public List<MembershipItem> getMemberships() { return memberships; }

    public static class RoleItem {
        private final String roleCode;
        private final String roleName;
        private final String description;
        private final String lifecycleState;

        public RoleItem(String roleCode, String roleName, String description, String lifecycleState) {
            this.roleCode = roleCode;
            this.roleName = roleName;
            this.description = description;
            this.lifecycleState = lifecycleState;
        }

        public String getRoleCode() { return roleCode; }
        public String getRoleName() { return roleName; }
        public String getDescription() { return description; }
        public String getLifecycleState() { return lifecycleState; }
    }

    public static class GroupItem {
        private final String groupCode;
        private final String groupName;
        private final String description;
        private final String lifecycleState;

        public GroupItem(String groupCode, String groupName, String description, String lifecycleState) {
            this.groupCode = groupCode;
            this.groupName = groupName;
            this.description = description;
            this.lifecycleState = lifecycleState;
        }

        public String getGroupCode() { return groupCode; }
        public String getGroupName() { return groupName; }
        public String getDescription() { return description; }
        public String getLifecycleState() { return lifecycleState; }
    }

    public static class MembershipItem {
        private final String roleCode;
        private final String groupCode;
        private final String containerKind;
        private final String containerCode;
        private final String userKey;
        private final String validFrom;
        private final String validUntil;

        public MembershipItem(String roleCode, String groupCode, String containerKind, String containerCode,
                              String userKey, String validFrom, String validUntil) {
            this.roleCode = roleCode;
            this.groupCode = groupCode;
            this.containerKind = containerKind;
            this.containerCode = containerCode;
            this.userKey = userKey;
            this.validFrom = validFrom;
            this.validUntil = validUntil;
        }

        public String getRoleCode() { return roleCode; }
        public String getGroupCode() { return groupCode; }
        public String getContainerKind() { return containerKind; }
        public String getContainerCode() { return containerCode; }
        public String getUserKey() { return userKey; }
        public String getValidFrom() { return validFrom; }
        public String getValidUntil() { return validUntil; }
    }
}