package com.fcm.authzcraft.demo.oa.domain.model;

public class RbacFieldGrant {
    private String code;
    private String resourceKey;
    private String fieldKey;
    private String accessLevel;
    private String name;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getResourceKey() { return resourceKey; }
    public void setResourceKey(String resourceKey) { this.resourceKey = resourceKey; }
    public String getFieldKey() { return fieldKey; }
    public void setFieldKey(String fieldKey) { this.fieldKey = fieldKey; }
    public String getAccessLevel() { return accessLevel; }
    public void setAccessLevel(String accessLevel) { this.accessLevel = accessLevel; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}