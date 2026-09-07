package com.fcm.authzcraft.pap.interfaces.dto;

import java.util.List;

public class GrantArgumentReplaceRequest {
    private List<GrantArgumentRequest> arguments;

    public List<GrantArgumentRequest> getArguments() { return arguments; }
    public void setArguments(List<GrantArgumentRequest> arguments) { this.arguments = arguments; }
}