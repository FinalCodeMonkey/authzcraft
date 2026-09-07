package com.fcm.authzcraft.pap.interfaces.dto;

import com.fcm.authzcraft.pap.domain.model.PrincipalOrganizationProjection;

public class PrincipalOrganizationProjectionResponse {
    private String id;
    private String tenantKey;
    private String principalKey;
    private String displayName;
    private String lifecycleState;
    private String departmentCode;
    private String departmentName;
    private String departmentLevel;
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
    private String enableFlag;
    private String enableCode;

    public static PrincipalOrganizationProjectionResponse from(PrincipalOrganizationProjection projection) {
        PrincipalOrganizationProjectionResponse response = new PrincipalOrganizationProjectionResponse();
        response.setId(String.valueOf(projection.getId()));
        response.setTenantKey(projection.getTenantKey());
        response.setPrincipalKey(projection.getPrincipalKey());
        response.setDisplayName(projection.getDisplayName());
        response.setLifecycleState(projection.getLifecycleState());
        response.setDepartmentCode(projection.getDepartmentCode());
        response.setDepartmentName(projection.getDepartmentName());
        response.setDepartmentLevel(projection.getDepartmentLevel() == null ? null : String.valueOf(projection.getDepartmentLevel()));
        response.setDepartmentTypeCode(projection.getDepartmentTypeCode());
        response.setDepartmentCategory(projection.getDepartmentCategory());
        response.setParentDepartmentCode(projection.getParentDepartmentCode());
        response.setParentDepartmentName(projection.getParentDepartmentName());
        response.setManageUserId(projection.getManageUserId());
        response.setManageStaffNo(projection.getManageStaffNo());
        response.setManageName(projection.getManageName());
        response.setPortionManageUserId(projection.getPortionManageUserId());
        response.setPortionManageStaffNo(projection.getPortionManageStaffNo());
        response.setPortionManageName(projection.getPortionManageName());
        response.setEnableFlag(projection.getEnableFlag() == null ? null : String.valueOf(projection.getEnableFlag()));
        response.setEnableCode(projection.getEnableCode());
        return response;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
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
    public String getDepartmentLevel() { return departmentLevel; }
    public void setDepartmentLevel(String departmentLevel) { this.departmentLevel = departmentLevel; }
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
    public String getEnableFlag() { return enableFlag; }
    public void setEnableFlag(String enableFlag) { this.enableFlag = enableFlag; }
    public String getEnableCode() { return enableCode; }
    public void setEnableCode(String enableCode) { this.enableCode = enableCode; }
}