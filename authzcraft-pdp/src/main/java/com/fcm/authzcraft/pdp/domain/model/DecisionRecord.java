package com.fcm.authzcraft.pdp.domain.model;

import java.time.LocalDateTime;

public class DecisionRecord {
    private Long id;
    private LocalDateTime decidedAt;
    private String decisionKey;
    private String requestKey;
    private String tenantKey;
    private String appKey;
    private String requesterKind;
    private String requesterKey;
    private String operationCode;
    private String targetResourceKey;
    private String planDecision;
    private String plannerKind;
    private String planDigest;
    private String attributeDigest;
    private String failureCode;
    private int planningCostMs;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getDecidedAt() { return decidedAt; }
    public void setDecidedAt(LocalDateTime decidedAt) { this.decidedAt = decidedAt; }
    public String getDecisionKey() { return decisionKey; }
    public void setDecisionKey(String decisionKey) { this.decisionKey = decisionKey; }
    public String getRequestKey() { return requestKey; }
    public void setRequestKey(String requestKey) { this.requestKey = requestKey; }
    public String getTenantKey() { return tenantKey; }
    public void setTenantKey(String tenantKey) { this.tenantKey = tenantKey; }
    public String getAppKey() { return appKey; }
    public void setAppKey(String appKey) { this.appKey = appKey; }
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
    public String getPlannerKind() { return plannerKind; }
    public void setPlannerKind(String plannerKind) { this.plannerKind = plannerKind; }
    public String getPlanDigest() { return planDigest; }
    public void setPlanDigest(String planDigest) { this.planDigest = planDigest; }
    public String getAttributeDigest() { return attributeDigest; }
    public void setAttributeDigest(String attributeDigest) { this.attributeDigest = attributeDigest; }
    public String getFailureCode() { return failureCode; }
    public void setFailureCode(String failureCode) { this.failureCode = failureCode; }
    public int getPlanningCostMs() { return planningCostMs; }
    public void setPlanningCostMs(int planningCostMs) { this.planningCostMs = planningCostMs; }
}