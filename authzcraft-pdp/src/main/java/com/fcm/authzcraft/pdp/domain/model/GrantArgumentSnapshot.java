package com.fcm.authzcraft.pdp.domain.model;

public class GrantArgumentSnapshot {
    private Long accessGrantId;
    private String argumentKey;
    private String valueKind;
    private String argumentValue;
    private boolean sensitive;

    public Long getAccessGrantId() { return accessGrantId; }
    public void setAccessGrantId(Long accessGrantId) { this.accessGrantId = accessGrantId; }
    public String getArgumentKey() { return argumentKey; }
    public void setArgumentKey(String argumentKey) { this.argumentKey = argumentKey; }
    public String getValueKind() { return valueKind; }
    public void setValueKind(String valueKind) { this.valueKind = valueKind; }
    public String getArgumentValue() { return argumentValue; }
    public void setArgumentValue(String argumentValue) { this.argumentValue = argumentValue; }
    public boolean isSensitive() { return sensitive; }
    public void setSensitive(boolean sensitive) { this.sensitive = sensitive; }
}