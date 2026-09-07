package com.fcm.authzcraft.pap.domain.model;

public class PrincipalPositionProjection {
    private Long id;
    private String tenantKey;
    private String principalKey;
    private String displayName;
    private String lifecycleState;
    private String postCode;
    private String postName;
    private String postEnName;
    private String postTypeCode;
    private String postType;
    private String departmentCode;
    private String departmentName;
    private String positionGradeCode;
    private String positionGradeName;
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
    public String getPostCode() { return postCode; }
    public void setPostCode(String postCode) { this.postCode = postCode; }
    public String getPostName() { return postName; }
    public void setPostName(String postName) { this.postName = postName; }
    public String getPostEnName() { return postEnName; }
    public void setPostEnName(String postEnName) { this.postEnName = postEnName; }
    public String getPostTypeCode() { return postTypeCode; }
    public void setPostTypeCode(String postTypeCode) { this.postTypeCode = postTypeCode; }
    public String getPostType() { return postType; }
    public void setPostType(String postType) { this.postType = postType; }
    public String getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    public String getPositionGradeCode() { return positionGradeCode; }
    public void setPositionGradeCode(String positionGradeCode) { this.positionGradeCode = positionGradeCode; }
    public String getPositionGradeName() { return positionGradeName; }
    public void setPositionGradeName(String positionGradeName) { this.positionGradeName = positionGradeName; }
    public Long getEnableFlag() { return enableFlag; }
    public void setEnableFlag(Long enableFlag) { this.enableFlag = enableFlag; }
    public String getEnableCode() { return enableCode; }
    public void setEnableCode(String enableCode) { this.enableCode = enableCode; }
}