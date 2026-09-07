package com.fcm.authzcraft.pip.interfaces.dto;

import java.util.ArrayList;
import java.util.List;

public class ApplicationRbacPrincipalSyncRequest {
    private String tenantKey;
    private String appKey;
    private Boolean deactivateMissing;
    private List<RoleItem> roles = new ArrayList<RoleItem>();
    private List<GroupItem> groups = new ArrayList<GroupItem>();
    private List<MembershipItem> memberships = new ArrayList<MembershipItem>();

    public String getTenantKey() { return tenantKey; }
    public void setTenantKey(String tenantKey) { this.tenantKey = tenantKey; }
    public String getAppKey() { return appKey; }
    public void setAppKey(String appKey) { this.appKey = appKey; }
    public Boolean getDeactivateMissing() { return deactivateMissing; }
    public void setDeactivateMissing(Boolean deactivateMissing) { this.deactivateMissing = deactivateMissing; }
    public List<RoleItem> getRoles() { return roles; }
    public void setRoles(List<RoleItem> roles) { this.roles = roles; }
    public List<GroupItem> getGroups() { return groups; }
    public void setGroups(List<GroupItem> groups) { this.groups = groups; }
    public List<MembershipItem> getMemberships() { return memberships; }
    public void setMemberships(List<MembershipItem> memberships) { this.memberships = memberships; }

    public static class RoleItem {
        private String roleCode;
        private String roleName;
        private String description;
        private String lifecycleState;

        public String getRoleCode() { return roleCode; }
        public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
        public String getRoleName() { return roleName; }
        public void setRoleName(String roleName) { this.roleName = roleName; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getLifecycleState() { return lifecycleState; }
        public void setLifecycleState(String lifecycleState) { this.lifecycleState = lifecycleState; }
    }

    public static class GroupItem {
        private String groupCode;
        private String groupName;
        private String description;
        private String lifecycleState;

        public String getGroupCode() { return groupCode; }
        public void setGroupCode(String groupCode) { this.groupCode = groupCode; }
        public String getGroupName() { return groupName; }
        public void setGroupName(String groupName) { this.groupName = groupName; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getLifecycleState() { return lifecycleState; }
        public void setLifecycleState(String lifecycleState) { this.lifecycleState = lifecycleState; }
    }

    public static class MembershipItem {
        private String roleCode;
        private String groupCode;
        private String containerKind;
        private String containerCode;
        private String userKey;
        private String validFrom;
        private String validUntil;

        public String getRoleCode() { return roleCode; }
        public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
        public String getGroupCode() { return groupCode; }
        public void setGroupCode(String groupCode) { this.groupCode = groupCode; }
        public String getContainerKind() { return containerKind; }
        public void setContainerKind(String containerKind) { this.containerKind = containerKind; }
        public String getContainerCode() { return containerCode; }
        public void setContainerCode(String containerCode) { this.containerCode = containerCode; }
        public String getUserKey() { return userKey; }
        public void setUserKey(String userKey) { this.userKey = userKey; }
        public String getValidFrom() { return validFrom; }
        public void setValidFrom(String validFrom) { this.validFrom = validFrom; }
        public String getValidUntil() { return validUntil; }
        public void setValidUntil(String validUntil) { this.validUntil = validUntil; }
    }
}