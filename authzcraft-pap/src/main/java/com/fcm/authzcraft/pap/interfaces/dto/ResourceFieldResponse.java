package com.fcm.authzcraft.pap.interfaces.dto;

import com.fcm.authzcraft.pap.domain.model.ResourceField;

public class ResourceFieldResponse {

    private String id;
    private String relationResourceId;
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

    public static ResourceFieldResponse from(ResourceField field) {
        ResourceFieldResponse response = new ResourceFieldResponse();
        response.id = String.valueOf(field.getId());
        response.relationResourceId = String.valueOf(field.getRelationResourceId());
        response.fieldKey = field.getFieldKey();
        response.displayName = field.getDisplayName();
        response.physicalName = field.getPhysicalName();
        response.ordinalPosition = field.getOrdinalPosition();
        response.logicalType = field.getLogicalType();
        response.nativeType = field.getNativeType();
        response.nullableFlag = field.getNullableFlag();
        response.primaryKeyOrdinal = field.getPrimaryKeyOrdinal();
        response.filterableFlag = field.getFilterableFlag();
        response.joinableFlag = field.getJoinableFlag();
        response.principalRefKind = field.getPrincipalRefKind();
        response.sensitivityLevel = field.getSensitivityLevel();
        response.securityRequirement = field.getSecurityRequirement();
        response.lifecycleState = field.getLifecycleState();
        return response;
    }

    public String getId() { return id; }
    public String getRelationResourceId() { return relationResourceId; }
    public String getFieldKey() { return fieldKey; }
    public String getDisplayName() { return displayName; }
    public String getPhysicalName() { return physicalName; }
    public Integer getOrdinalPosition() { return ordinalPosition; }
    public String getLogicalType() { return logicalType; }
    public String getNativeType() { return nativeType; }
    public Boolean getNullableFlag() { return nullableFlag; }
    public Integer getPrimaryKeyOrdinal() { return primaryKeyOrdinal; }
    public Boolean getFilterableFlag() { return filterableFlag; }
    public Boolean getJoinableFlag() { return joinableFlag; }
    public String getPrincipalRefKind() { return principalRefKind; }
    public String getSensitivityLevel() { return sensitivityLevel; }
    public String getSecurityRequirement() { return securityRequirement; }
    public String getLifecycleState() { return lifecycleState; }
}