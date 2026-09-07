package com.fcm.authzcraft.pip.application.service;

import java.util.ArrayList;
import java.util.List;

public class ApplicationRbacPrincipalSyncResult {
    private String tenantKey;
    private String appKey;
    private int roleCount;
    private int groupCount;
    private int membershipCount;
    private int failedCount;
    private final List<RoleResult> roles = new ArrayList<RoleResult>();
    private final List<GroupResult> groups = new ArrayList<GroupResult>();
    private final List<MembershipResult> memberships = new ArrayList<MembershipResult>();
    private final List<Failure> failures = new ArrayList<Failure>();

    public String getTenantKey() { return tenantKey; }
    public void setTenantKey(String tenantKey) { this.tenantKey = tenantKey; }
    public String getAppKey() { return appKey; }
    public void setAppKey(String appKey) { this.appKey = appKey; }
    public int getRoleCount() { return roleCount; }
    public void setRoleCount(int roleCount) { this.roleCount = roleCount; }
    public int getGroupCount() { return groupCount; }
    public void setGroupCount(int groupCount) { this.groupCount = groupCount; }
    public int getMembershipCount() { return membershipCount; }
    public void setMembershipCount(int membershipCount) { this.membershipCount = membershipCount; }
    public int getFailedCount() { return failedCount; }
    public void setFailedCount(int failedCount) { this.failedCount = failedCount; }
    public List<RoleResult> getRoles() { return roles; }
    public List<GroupResult> getGroups() { return groups; }
    public List<MembershipResult> getMemberships() { return memberships; }
    public List<Failure> getFailures() { return failures; }

    public static class RoleResult {
        private String roleCode;
        private String roleName;
        private String description;
        private String principalId;
        private String lifecycleState;

        public String getRoleCode() { return roleCode; }
        public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
        public String getRoleName() { return roleName; }
        public void setRoleName(String roleName) { this.roleName = roleName; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getPrincipalId() { return principalId; }
        public void setPrincipalId(String principalId) { this.principalId = principalId; }
        public String getLifecycleState() { return lifecycleState; }
        public void setLifecycleState(String lifecycleState) { this.lifecycleState = lifecycleState; }
    }

    public static class GroupResult {
        private String groupCode;
        private String groupName;
        private String description;
        private String principalId;
        private String lifecycleState;

        public String getGroupCode() { return groupCode; }
        public void setGroupCode(String groupCode) { this.groupCode = groupCode; }
        public String getGroupName() { return groupName; }
        public void setGroupName(String groupName) { this.groupName = groupName; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getPrincipalId() { return principalId; }
        public void setPrincipalId(String principalId) { this.principalId = principalId; }
        public String getLifecycleState() { return lifecycleState; }
        public void setLifecycleState(String lifecycleState) { this.lifecycleState = lifecycleState; }
    }

    public static class MembershipResult {
        private String containerKind;
        private String containerCode;
        private String roleCode;
        private String groupCode;
        private String userKey;
        private String membershipId;
        private String containerPrincipalId;
        private String rolePrincipalId;
        private String groupPrincipalId;
        private String userPrincipalId;
        private String lifecycleState;
        private String validFrom;
        private String validUntil;
        private String syncVersion;

        public String getContainerKind() { return containerKind; }
        public void setContainerKind(String containerKind) { this.containerKind = containerKind; }
        public String getContainerCode() { return containerCode; }
        public void setContainerCode(String containerCode) { this.containerCode = containerCode; }
        public String getRoleCode() { return roleCode; }
        public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
        public String getGroupCode() { return groupCode; }
        public void setGroupCode(String groupCode) { this.groupCode = groupCode; }
        public String getUserKey() { return userKey; }
        public void setUserKey(String userKey) { this.userKey = userKey; }
        public String getMembershipId() { return membershipId; }
        public void setMembershipId(String membershipId) { this.membershipId = membershipId; }
        public String getContainerPrincipalId() { return containerPrincipalId; }
        public void setContainerPrincipalId(String containerPrincipalId) { this.containerPrincipalId = containerPrincipalId; }
        public String getRolePrincipalId() { return rolePrincipalId; }
        public void setRolePrincipalId(String rolePrincipalId) { this.rolePrincipalId = rolePrincipalId; }
        public String getGroupPrincipalId() { return groupPrincipalId; }
        public void setGroupPrincipalId(String groupPrincipalId) { this.groupPrincipalId = groupPrincipalId; }
        public String getUserPrincipalId() { return userPrincipalId; }
        public void setUserPrincipalId(String userPrincipalId) { this.userPrincipalId = userPrincipalId; }
        public String getLifecycleState() { return lifecycleState; }
        public void setLifecycleState(String lifecycleState) { this.lifecycleState = lifecycleState; }
        public String getValidFrom() { return validFrom; }
        public void setValidFrom(String validFrom) { this.validFrom = validFrom; }
        public String getValidUntil() { return validUntil; }
        public void setValidUntil(String validUntil) { this.validUntil = validUntil; }
        public String getSyncVersion() { return syncVersion; }
        public void setSyncVersion(String syncVersion) { this.syncVersion = syncVersion; }
    }

    public static class Failure {
        private String itemType;
        private String itemKey;
        private String reason;

        public String getItemType() { return itemType; }
        public void setItemType(String itemType) { this.itemType = itemType; }
        public String getItemKey() { return itemKey; }
        public void setItemKey(String itemKey) { this.itemKey = itemKey; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}