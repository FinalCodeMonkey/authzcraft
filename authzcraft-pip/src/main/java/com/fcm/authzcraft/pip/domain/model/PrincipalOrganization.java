package com.fcm.authzcraft.pip.domain.model;

import java.util.Date;

public class PrincipalOrganization {

    private final String departmentCode;
    private final String departmentName;
    private final Long departmentLevel;
    private final String departmentTypeCode;
    private final String departmentCategory;
    private final String parentDepartmentCode;
    private final String parentDepartmentName;
    private final String manageUserId;
    private final String manageStaffNo;
    private final String manageName;
    private final String portionManageUserId;
    private final String portionManageStaffNo;
    private final String portionManageName;
    private final Long isEnable;
    private final Date createTime;
    private final String departmentHrbpList;
    private final String isenableCode;
    private final PrincipalLifecycleState lifecycleState;

    public PrincipalOrganization(String departmentCode, String departmentName, Long departmentLevel,
                                 String departmentTypeCode, String departmentCategory, String parentDepartmentCode,
                                 String parentDepartmentName, String manageUserId, String manageStaffNo,
                                 String manageName, String portionManageUserId, String portionManageStaffNo,
                                 String portionManageName, Long isEnable, Date createTime, String departmentHrbpList,
                                 String isenableCode, PrincipalLifecycleState lifecycleState) {
        this.departmentCode = departmentCode;
        this.departmentName = departmentName;
        this.departmentLevel = departmentLevel;
        this.departmentTypeCode = departmentTypeCode;
        this.departmentCategory = departmentCategory;
        this.parentDepartmentCode = parentDepartmentCode;
        this.parentDepartmentName = parentDepartmentName;
        this.manageUserId = manageUserId;
        this.manageStaffNo = manageStaffNo;
        this.manageName = manageName;
        this.portionManageUserId = portionManageUserId;
        this.portionManageStaffNo = portionManageStaffNo;
        this.portionManageName = portionManageName;
        this.isEnable = isEnable;
        this.createTime = createTime;
        this.departmentHrbpList = departmentHrbpList;
        this.isenableCode = isenableCode;
        this.lifecycleState = lifecycleState;
    }

    public String getDepartmentCode() { return departmentCode; }
    public String getDepartmentName() { return departmentName; }
    public Long getDepartmentLevel() { return departmentLevel; }
    public String getDepartmentTypeCode() { return departmentTypeCode; }
    public String getDepartmentCategory() { return departmentCategory; }
    public String getParentDepartmentCode() { return parentDepartmentCode; }
    public String getParentDepartmentName() { return parentDepartmentName; }
    public String getManageUserId() { return manageUserId; }
    public String getManageStaffNo() { return manageStaffNo; }
    public String getManageName() { return manageName; }
    public String getPortionManageUserId() { return portionManageUserId; }
    public String getPortionManageStaffNo() { return portionManageStaffNo; }
    public String getPortionManageName() { return portionManageName; }
    public Long getIsEnable() { return isEnable; }
    public Date getCreateTime() { return createTime; }
    public String getDepartmentHrbpList() { return departmentHrbpList; }
    public String getIsenableCode() { return isenableCode; }
    public PrincipalLifecycleState getLifecycleState() { return lifecycleState; }
}
