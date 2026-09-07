package com.fcm.authzcraft.pip.interfaces.dto;

import java.util.ArrayList;
import java.util.List;

import com.fcm.authzcraft.pip.application.service.ApplicationRbacPrincipalSyncResult;

public class ApplicationRbacPrincipalSyncResponse {
    private String tenantKey;
    private String appKey;
    private int roleCount;
    private int groupCount;
    private int membershipCount;
    private int failedCount;
    private List<ApplicationRbacPrincipalSyncResult.RoleResult> roles = new ArrayList<ApplicationRbacPrincipalSyncResult.RoleResult>();
    private List<ApplicationRbacPrincipalSyncResult.GroupResult> groups = new ArrayList<ApplicationRbacPrincipalSyncResult.GroupResult>();
    private List<ApplicationRbacPrincipalSyncResult.MembershipResult> memberships = new ArrayList<ApplicationRbacPrincipalSyncResult.MembershipResult>();
    private List<ApplicationRbacPrincipalSyncResult.Failure> failures = new ArrayList<ApplicationRbacPrincipalSyncResult.Failure>();

    public static ApplicationRbacPrincipalSyncResponse from(ApplicationRbacPrincipalSyncResult result) {
        ApplicationRbacPrincipalSyncResponse response = new ApplicationRbacPrincipalSyncResponse();
        response.setTenantKey(result.getTenantKey());
        response.setAppKey(result.getAppKey());
        response.setRoleCount(result.getRoleCount());
        response.setGroupCount(result.getGroupCount());
        response.setMembershipCount(result.getMembershipCount());
        response.setFailedCount(result.getFailedCount());
        response.setRoles(result.getRoles());
        response.setGroups(result.getGroups());
        response.setMemberships(result.getMemberships());
        response.setFailures(result.getFailures());
        return response;
    }

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
    public List<ApplicationRbacPrincipalSyncResult.RoleResult> getRoles() { return roles; }
    public void setRoles(List<ApplicationRbacPrincipalSyncResult.RoleResult> roles) { this.roles = roles; }
    public List<ApplicationRbacPrincipalSyncResult.GroupResult> getGroups() { return groups; }
    public void setGroups(List<ApplicationRbacPrincipalSyncResult.GroupResult> groups) { this.groups = groups; }
    public List<ApplicationRbacPrincipalSyncResult.MembershipResult> getMemberships() { return memberships; }
    public void setMemberships(List<ApplicationRbacPrincipalSyncResult.MembershipResult> memberships) { this.memberships = memberships; }
    public List<ApplicationRbacPrincipalSyncResult.Failure> getFailures() { return failures; }
    public void setFailures(List<ApplicationRbacPrincipalSyncResult.Failure> failures) { this.failures = failures; }
}