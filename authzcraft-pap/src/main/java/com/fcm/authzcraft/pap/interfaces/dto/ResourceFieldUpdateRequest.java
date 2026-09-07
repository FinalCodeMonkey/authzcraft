package com.fcm.authzcraft.pap.interfaces.dto;

public class ResourceFieldUpdateRequest {

    private String displayName;
    private Boolean filterableFlag;
    private Boolean joinableFlag;
    private String principalRefKind;
    private String sensitivityLevel;
    private String securityRequirement;
    private String lifecycleState;

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public Boolean getFilterableFlag() { return filterableFlag; }
    public void setFilterableFlag(Boolean filterableFlag) { this.filterableFlag = filterableFlag; }
    public Boolean getJoinableFlag() { return joinableFlag; }
    public void setJoinableFlag(Boolean joinableFlag) { this.joinableFlag = joinableFlag; }
    public String getPrincipalRefKind() { return principalRefKind; }
    public void setPrincipalRefKind(String principalRefKind) { this.principalRefKind = principalRefKind; }
    public String getSensitivityLevel() { return sensitivityLevel; }
    public void setSensitivityLevel(String sensitivityLevel) { this.sensitivityLevel = sensitivityLevel; }
    public String getSecurityRequirement() { return securityRequirement; }
    public void setSecurityRequirement(String securityRequirement) { this.securityRequirement = securityRequirement; }
    public String getLifecycleState() { return lifecycleState; }
    public void setLifecycleState(String lifecycleState) { this.lifecycleState = lifecycleState; }
}