package com.fcm.authzcraft.pap.application.query;

public class ResourceFieldSearchQuery {

    private Boolean filterableFlag;
    private Boolean joinableFlag;
    private String lifecycleState;

    public Boolean getFilterableFlag() { return filterableFlag; }
    public void setFilterableFlag(Boolean filterableFlag) { this.filterableFlag = filterableFlag; }
    public Boolean getJoinableFlag() { return joinableFlag; }
    public void setJoinableFlag(Boolean joinableFlag) { this.joinableFlag = joinableFlag; }
    public String getLifecycleState() { return lifecycleState; }
    public void setLifecycleState(String lifecycleState) { this.lifecycleState = lifecycleState; }
}