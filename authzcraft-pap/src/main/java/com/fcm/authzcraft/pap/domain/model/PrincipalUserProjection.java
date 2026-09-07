package com.fcm.authzcraft.pap.domain.model;

public class PrincipalUserProjection {
    private Long id;
    private String tenantKey;
    private String principalKey;
    private String displayName;
    private String lifecycleState;
    private String userId;
    private String staffNo;
    private String staffName;
    private String departmentCode;
    private String departmentName;
    private String postCode;
    private String postName;
    private String manageUserId;
    private String manageStaffNo;
    private String manageStaffName;
    private String staffStatus;
    private String staffStatusCode;
    private Boolean deptManage;
    private Boolean deptPortionManage;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTenantKey() { return tenantKey; }
    public void setTenantKey(String tenantKey) { this.tenantKey = tenantKey; }
    public String getPrincipalKey() { return principalKey; }
    public void setPrincipalKey(String principalKey) { this.principalKey = principalKey; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getLifecycleState() { return lifecycleState; }
    public void setLifecycleState(String lifecycleState) { this.lifecycleState = lifecycleState; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getStaffNo() { return staffNo; }
    public void setStaffNo(String staffNo) { this.staffNo = staffNo; }
    public String getStaffName() { return staffName; }
    public void setStaffName(String staffName) { this.staffName = staffName; }
    public String getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    public String getPostCode() { return postCode; }
    public void setPostCode(String postCode) { this.postCode = postCode; }
    public String getPostName() { return postName; }
    public void setPostName(String postName) { this.postName = postName; }
    public String getManageUserId() { return manageUserId; }
    public void setManageUserId(String manageUserId) { this.manageUserId = manageUserId; }
    public String getManageStaffNo() { return manageStaffNo; }
    public void setManageStaffNo(String manageStaffNo) { this.manageStaffNo = manageStaffNo; }
    public String getManageStaffName() { return manageStaffName; }
    public void setManageStaffName(String manageStaffName) { this.manageStaffName = manageStaffName; }
    public String getStaffStatus() { return staffStatus; }
    public void setStaffStatus(String staffStatus) { this.staffStatus = staffStatus; }
    public String getStaffStatusCode() { return staffStatusCode; }
    public void setStaffStatusCode(String staffStatusCode) { this.staffStatusCode = staffStatusCode; }
    public Boolean getDeptManage() { return deptManage; }
    public void setDeptManage(Boolean deptManage) { this.deptManage = deptManage; }
    public Boolean getDeptPortionManage() { return deptPortionManage; }
    public void setDeptPortionManage(Boolean deptPortionManage) { this.deptPortionManage = deptPortionManage; }
}