package com.fcm.authzcraft.pap.interfaces.dto;

public class PrincipalProjectionSearchRequest {
    private String tenantKey;
    private String principalKey;
    private String keyword;
    private String lifecycleState;
    private String userId;
    private String staffNo;
    private String departmentCode;
    private String postCode;
    private String parentDepartmentCode;
    private Integer limit;
    private Integer offset;

    public String getTenantKey() { return tenantKey; }
    public void setTenantKey(String tenantKey) { this.tenantKey = tenantKey; }
    public String getPrincipalKey() { return principalKey; }
    public void setPrincipalKey(String principalKey) { this.principalKey = principalKey; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getLifecycleState() { return lifecycleState; }
    public void setLifecycleState(String lifecycleState) { this.lifecycleState = lifecycleState; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getStaffNo() { return staffNo; }
    public void setStaffNo(String staffNo) { this.staffNo = staffNo; }
    public String getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }
    public String getPostCode() { return postCode; }
    public void setPostCode(String postCode) { this.postCode = postCode; }
    public String getParentDepartmentCode() { return parentDepartmentCode; }
    public void setParentDepartmentCode(String parentDepartmentCode) { this.parentDepartmentCode = parentDepartmentCode; }
    public Integer getLimit() { return limit; }
    public void setLimit(Integer limit) { this.limit = limit; }
    public Integer getOffset() { return offset; }
    public void setOffset(Integer offset) { this.offset = offset; }
}