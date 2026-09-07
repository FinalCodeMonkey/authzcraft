package com.fcm.authzcraft.pip.domain.model;

public class PrincipalUser {

    private final String userId;
    private final String staffNo;
    private final String staffName;
    private final String departmentCode;
    private final String departmentName;
    private final String postCode;
    private final String postName;
    private final String manageUserId;
    private final String manageStaffNo;
    private final String manageStaffName;
    private final String staffStatus;
    private final String staffStatusCode;
    private final Integer isDeptManage;
    private final Integer isDeptPortionManage;
    private final PrincipalLifecycleState lifecycleState;

    public PrincipalUser(String userId, String staffNo, String staffName, String departmentCode, String departmentName,
                         String postCode, String postName, String manageUserId, String manageStaffNo,
                         String manageStaffName, String staffStatus, String staffStatusCode, Integer isDeptManage,
                         Integer isDeptPortionManage, PrincipalLifecycleState lifecycleState) {
        this.userId = userId;
        this.staffNo = staffNo;
        this.staffName = staffName;
        this.departmentCode = departmentCode;
        this.departmentName = departmentName;
        this.postCode = postCode;
        this.postName = postName;
        this.manageUserId = manageUserId;
        this.manageStaffNo = manageStaffNo;
        this.manageStaffName = manageStaffName;
        this.staffStatus = staffStatus;
        this.staffStatusCode = staffStatusCode;
        this.isDeptManage = isDeptManage;
        this.isDeptPortionManage = isDeptPortionManage;
        this.lifecycleState = lifecycleState;
    }

    public String getUserId() { return userId; }
    public String getStaffNo() { return staffNo; }
    public String getStaffName() { return staffName; }
    public String getDepartmentCode() { return departmentCode; }
    public String getDepartmentName() { return departmentName; }
    public String getPostCode() { return postCode; }
    public String getPostName() { return postName; }
    public String getManageUserId() { return manageUserId; }
    public String getManageStaffNo() { return manageStaffNo; }
    public String getManageStaffName() { return manageStaffName; }
    public String getStaffStatus() { return staffStatus; }
    public String getStaffStatusCode() { return staffStatusCode; }
    public Integer getIsDeptManage() { return isDeptManage; }
    public Integer getIsDeptPortionManage() { return isDeptPortionManage; }
    public PrincipalLifecycleState getLifecycleState() { return lifecycleState; }
}
