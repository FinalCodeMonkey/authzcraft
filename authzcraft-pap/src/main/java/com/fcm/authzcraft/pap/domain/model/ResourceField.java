package com.fcm.authzcraft.pap.domain.model;

public class ResourceField {

    private Long id;
    private Long relationResourceId;
    private String fieldKey;
    private String displayName;
    private String physicalName;
    private Integer ordinalPosition;
    private String logicalType;
    private String nativeType;
    private Boolean nullableFlag;
    private Integer primaryKeyOrdinal;
    private Boolean filterableFlag;
    private Boolean joinableFlag;
    private String principalRefKind;
    private String sensitivityLevel;
    private String securityRequirement;
    private String lifecycleState;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRelationResourceId() { return relationResourceId; }
    public void setRelationResourceId(Long relationResourceId) { this.relationResourceId = relationResourceId; }
    public String getFieldKey() { return fieldKey; }
    public void setFieldKey(String fieldKey) { this.fieldKey = fieldKey; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getPhysicalName() { return physicalName; }
    public void setPhysicalName(String physicalName) { this.physicalName = physicalName; }
    public Integer getOrdinalPosition() { return ordinalPosition; }
    public void setOrdinalPosition(Integer ordinalPosition) { this.ordinalPosition = ordinalPosition; }
    public String getLogicalType() { return logicalType; }
    public void setLogicalType(String logicalType) { this.logicalType = logicalType; }
    public String getNativeType() { return nativeType; }
    public void setNativeType(String nativeType) { this.nativeType = nativeType; }
    public Boolean getNullableFlag() { return nullableFlag; }
    public void setNullableFlag(Boolean nullableFlag) { this.nullableFlag = nullableFlag; }
    public Integer getPrimaryKeyOrdinal() { return primaryKeyOrdinal; }
    public void setPrimaryKeyOrdinal(Integer primaryKeyOrdinal) { this.primaryKeyOrdinal = primaryKeyOrdinal; }
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