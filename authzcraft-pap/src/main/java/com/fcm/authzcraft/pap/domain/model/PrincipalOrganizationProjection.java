package com.fcm.authzcraft.pap.domain.model;

public class PrincipalOrganizationProjection {
    private Long id;
    private String tenantKey;
    private String principalKey;
    private String displayName;
    private String lifecycleState;
    private String departmentCode;
    private String departmentName;
    private Long departmentLevel;
    private String departmentTypeCode;
    private String departmentCategory;
    private String parentDepartmentCode;
    private String parentDepartmentName;
    private String manageUserId;
    private String manageStaffNo;
    private String manageName;
    private String portionManageUserId;
    private String portionManageStaffNo;
    private String portionManageName;
    private Long enableFlag;
    private String enableCode;

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
    public String getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    public Long getDepartmentLevel() { return departmentLevel; }
    public void setDepartmentLevel(Long departmentLevel) { this.departmentLevel = departmentLevel; }
    public String getDepartmentTypeCode() { return departmentTypeCode; }
    public void setDepartmentTypeCode(String departmentTypeCode) { this.departmentTypeCode = departmentTypeCode; }
    public String getDepartmentCategory() { return departmentCategory; }
    public void setDepartmentCategory(String departmentCategory) { this.departmentCategory = departmentCategory; }
    public String getParentDepartmentCode() { return parentDepartmentCode; }
    public void setParentDepartmentCode(String parentDepartmentCode) { this.parentDepartmentCode = parentDepartmentCode; }
    public String getParentDepartmentName() { return parentDepartmentName; }
    public void setParentDepartmentName(String parentDepartmentName) { this.parentDepartmentName = parentDepartmentName; }
    public String getManageUserId() { return manageUserId; }
    public void setManageUserId(String manageUserId) { this.manageUserId = manageUserId; }
    public String getManageStaffNo() { return manageStaffNo; }
    public void setManageStaffNo(String manageStaffNo) { this.manageStaffNo = manageStaffNo; }
    public String getManageName() { return manageName; }
    public void setManageName(String manageName) { this.manageName = manageName; }
    public String getPortionManageUserId() { return portionManageUserId; }
    public void setPortionManageUserId(String portionManageUserId) { this.portionManageUserId = portionManageUserId; }
    public String getPortionManageStaffNo() { return portionManageStaffNo; }
    public void setPortionManageStaffNo(String portionManageStaffNo) { this.portionManageStaffNo = portionManageStaffNo; }
    public String getPortionManageName() { return portionManageName; }
    public void setPortionManageName(String portionManageName) { this.portionManageName = portionManageName; }
    public Long getEnableFlag() { return enableFlag; }
    public void setEnableFlag(Long enableFlag) { this.enableFlag = enableFlag; }
    public String getEnableCode() { return enableCode; }
    public void setEnableCode(String enableCode) { this.enableCode = enableCode; }
}