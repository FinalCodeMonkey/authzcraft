package com.fcm.authzcraft.pdp.application.query;

public class DecisionRecordSearchQuery {
    private String tenantKey;
    private String appKey;
    private String decisionKey;
    private String requestKey;
    private String requesterKind;
    private String requesterKey;
    private String operationCode;
    private String targetResourceKey;
    private String planDecision;
    private String failureCode;
    private int limit = 50;

    public String getTenantKey() { return tenantKey; }
    public void setTenantKey(String tenantKey) { this.tenantKey = tenantKey; }
    public String getAppKey() { return appKey; }
    public void setAppKey(String appKey) { this.appKey = appKey; }
    public String getDecisionKey() { return decisionKey; }
    public void setDecisionKey(String decisionKey) { this.decisionKey = decisionKey; }
    public String getRequestKey() { return requestKey; }
    public void setRequestKey(String requestKey) { this.requestKey = requestKey; }
    public String getRequesterKind() { return requesterKind; }
    public void setRequesterKind(String requesterKind) { this.requesterKind = requesterKind; }
    public String getRequesterKey() { return requesterKey; }
    public void setRequesterKey(String requesterKey) { this.requesterKey = requesterKey; }
    public String getOperationCode() { return operationCode; }
    public void setOperationCode(String operationCode) { this.operationCode = operationCode; }
    public String getTargetResourceKey() { return targetResourceKey; }
    public void setTargetResourceKey(String targetResourceKey) { this.targetResourceKey = targetResourceKey; }
    public String getPlanDecision() { return planDecision; }
    public void setPlanDecision(String planDecision) { this.planDecision = planDecision; }
    public String getFailureCode() { return failureCode; }
    public void setFailureCode(String failureCode) { this.failureCode = failureCode; }
    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }
}