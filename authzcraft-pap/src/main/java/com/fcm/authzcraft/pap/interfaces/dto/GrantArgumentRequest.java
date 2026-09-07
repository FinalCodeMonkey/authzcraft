package com.fcm.authzcraft.pap.interfaces.dto;

public class GrantArgumentRequest {
    private String argumentKey;
    private String valueKind;
    private String argumentValue;
    private Boolean sensitiveFlag;

    public String getArgumentKey() { return argumentKey; }
    public void setArgumentKey(String argumentKey) { this.argumentKey = argumentKey; }
    public String getValueKind() { return valueKind; }
    public void setValueKind(String valueKind) { this.valueKind = valueKind; }
    public String getArgumentValue() { return argumentValue; }
    public void setArgumentValue(String argumentValue) { this.argumentValue = argumentValue; }
    public Boolean getSensitiveFlag() { return sensitiveFlag; }
    public void setSensitiveFlag(Boolean sensitiveFlag) { this.sensitiveFlag = sensitiveFlag; }
}