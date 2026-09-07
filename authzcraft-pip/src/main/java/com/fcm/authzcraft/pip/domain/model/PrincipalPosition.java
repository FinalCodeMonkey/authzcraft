package com.fcm.authzcraft.pip.domain.model;

import java.util.Date;

public class PrincipalPosition {

    private final String postCode;
    private final String postName;
    private final String postEnName;
    private final String postTypeCode;
    private final String postType;
    private final String departmentCode;
    private final String departmentName;
    private final String positionGradeCode;
    private final String positionGradeName;
    private final Long isEnable;
    private final Date createTime;
    private final String isenableCode;
    private final PrincipalLifecycleState lifecycleState;

    public PrincipalPosition(String postCode, String postName, String postEnName, String postTypeCode,
                             String postType, String departmentCode, String departmentName, String positionGradeCode,
                             String positionGradeName, Long isEnable, Date createTime, String isenableCode,
                             PrincipalLifecycleState lifecycleState) {
        this.postCode = postCode;
        this.postName = postName;
        this.postEnName = postEnName;
        this.postTypeCode = postTypeCode;
        this.postType = postType;
        this.departmentCode = departmentCode;
        this.departmentName = departmentName;
        this.positionGradeCode = positionGradeCode;
        this.positionGradeName = positionGradeName;
        this.isEnable = isEnable;
        this.createTime = createTime;
        this.isenableCode = isenableCode;
        this.lifecycleState = lifecycleState;
    }

    public String getPostCode() { return postCode; }
    public String getPostName() { return postName; }
    public String getPostEnName() { return postEnName; }
    public String getPostTypeCode() { return postTypeCode; }
    public String getPostType() { return postType; }
    public String getDepartmentCode() { return departmentCode; }
    public String getDepartmentName() { return departmentName; }
    public String getPositionGradeCode() { return positionGradeCode; }
    public String getPositionGradeName() { return positionGradeName; }
    public Long getIsEnable() { return isEnable; }
    public Date getCreateTime() { return createTime; }
    public String getIsenableCode() { return isenableCode; }
    public PrincipalLifecycleState getLifecycleState() { return lifecycleState; }
}
