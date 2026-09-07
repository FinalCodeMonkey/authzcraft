package com.fcm.authzcraft.api.plan;

import java.util.ArrayList;
import java.util.List;

import com.fcm.authzcraft.api.common.RequesterKind;

public class RowFilterPlanRequest {

    private String tenantKey;
    private String appKey;
    private RequesterKind requesterKind;
    private String requesterKey;
    private String operationCode;
    private List<PlanResource> resources = new ArrayList<PlanResource>();
    private PlanRequestContext context;

    public RowFilterPlanRequest() {
    }

    public String getTenantKey() {
        return tenantKey;
    }

    public void setTenantKey(String tenantKey) {
        this.tenantKey = tenantKey;
    }

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public RequesterKind getRequesterKind() {
        return requesterKind;
    }

    public void setRequesterKind(RequesterKind requesterKind) {
        this.requesterKind = requesterKind;
    }

    public String getRequesterKey() {
        return requesterKey;
    }

    public void setRequesterKey(String requesterKey) {
        this.requesterKey = requesterKey;
    }

    public String getOperationCode() {
        return operationCode;
    }

    public void setOperationCode(String operationCode) {
        this.operationCode = operationCode;
    }

    public List<PlanResource> getResources() {
        return resources;
    }

    public void setResources(List<PlanResource> resources) {
        this.resources = resources;
    }

    public PlanRequestContext getContext() {
        return context;
    }

    public void setContext(PlanRequestContext context) {
        this.context = context;
    }
}