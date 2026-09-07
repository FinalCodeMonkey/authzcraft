package com.fcm.authzcraft.pap.domain.model;

public class GrantArgument {
    private Long id;
    private Long accessGrantId;
    private String argumentKey;
    private String valueKind;
    private String argumentValue;
    private Boolean sensitiveFlag;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAccessGrantId() { return accessGrantId; }
    public void setAccessGrantId(Long accessGrantId) { this.accessGrantId = accessGrantId; }
    public String getArgumentKey() { return argumentKey; }
    public void setArgumentKey(String argumentKey) { this.argumentKey = argumentKey; }
    public String getValueKind() { return valueKind; }
    public void setValueKind(String valueKind) { this.valueKind = valueKind; }
    public String getArgumentValue() { return argumentValue; }
    public void setArgumentValue(String argumentValue) { this.argumentValue = argumentValue; }
    public Boolean getSensitiveFlag() { return sensitiveFlag; }
    public void setSensitiveFlag(Boolean sensitiveFlag) { this.sensitiveFlag = sensitiveFlag; }
}