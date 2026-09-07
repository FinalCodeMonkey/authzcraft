package com.fcm.authzcraft.pap.interfaces.dto;

import com.fcm.authzcraft.pap.domain.model.GrantArgument;

public class GrantArgumentResponse {
    private String id;
    private String accessGrantId;
    private String argumentKey;
    private String valueKind;
    private String argumentValue;
    private Boolean sensitiveFlag;

    public static GrantArgumentResponse from(GrantArgument argument) {
        GrantArgumentResponse response = new GrantArgumentResponse();
        response.setId(String.valueOf(argument.getId()));
        response.setAccessGrantId(String.valueOf(argument.getAccessGrantId()));
        response.setArgumentKey(argument.getArgumentKey());
        response.setValueKind(argument.getValueKind());
        response.setArgumentValue(argument.getArgumentValue());
        response.setSensitiveFlag(argument.getSensitiveFlag());
        return response;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getAccessGrantId() { return accessGrantId; }
    public void setAccessGrantId(String accessGrantId) { this.accessGrantId = accessGrantId; }
    public String getArgumentKey() { return argumentKey; }
    public void setArgumentKey(String argumentKey) { this.argumentKey = argumentKey; }
    public String getValueKind() { return valueKind; }
    public void setValueKind(String valueKind) { this.valueKind = valueKind; }
    public String getArgumentValue() { return argumentValue; }
    public void setArgumentValue(String argumentValue) { this.argumentValue = argumentValue; }
    public Boolean getSensitiveFlag() { return sensitiveFlag; }
    public void setSensitiveFlag(Boolean sensitiveFlag) { this.sensitiveFlag = sensitiveFlag; }
}