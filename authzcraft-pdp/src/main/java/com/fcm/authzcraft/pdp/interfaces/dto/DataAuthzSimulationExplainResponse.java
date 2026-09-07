package com.fcm.authzcraft.pdp.interfaces.dto;

import java.util.List;

public class DataAuthzSimulationExplainResponse {
    private String targetResourceKey;
    private String planDecision;
    private String protectionMode;
    private String predicateOperator;
    private List<String> bindingKeys;
    private List<String> requiredAccessPathKeys;
    private String failureCode;
    private String summary;

    public String getTargetResourceKey() {
        return targetResourceKey;
    }

    public void setTargetResourceKey(String targetResourceKey) {
        this.targetResourceKey = targetResourceKey;
    }

    public String getPlanDecision() {
        return planDecision;
    }

    public void setPlanDecision(String planDecision) {
        this.planDecision = planDecision;
    }

    public String getProtectionMode() {
        return protectionMode;
    }

    public void setProtectionMode(String protectionMode) {
        this.protectionMode = protectionMode;
    }

    public String getPredicateOperator() {
        return predicateOperator;
    }

    public void setPredicateOperator(String predicateOperator) {
        this.predicateOperator = predicateOperator;
    }

    public List<String> getBindingKeys() {
        return bindingKeys;
    }

    public void setBindingKeys(List<String> bindingKeys) {
        this.bindingKeys = bindingKeys;
    }

    public List<String> getRequiredAccessPathKeys() {
        return requiredAccessPathKeys;
    }

    public void setRequiredAccessPathKeys(List<String> requiredAccessPathKeys) {
        this.requiredAccessPathKeys = requiredAccessPathKeys;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public void setFailureCode(String failureCode) {
        this.failureCode = failureCode;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}