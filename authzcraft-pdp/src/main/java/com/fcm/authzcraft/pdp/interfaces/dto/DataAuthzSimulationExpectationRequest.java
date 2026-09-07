package com.fcm.authzcraft.pdp.interfaces.dto;

import java.util.List;

public class DataAuthzSimulationExpectationRequest {
    private String targetResourceKey;
    private String expectedPlanDecision;
    private String expectedFailureCode;
    private List<String> expectedBindingKeys;
    private List<String> expectedAccessPathKeys;

    public String getTargetResourceKey() {
        return targetResourceKey;
    }

    public void setTargetResourceKey(String targetResourceKey) {
        this.targetResourceKey = targetResourceKey;
    }

    public String getExpectedPlanDecision() {
        return expectedPlanDecision;
    }

    public void setExpectedPlanDecision(String expectedPlanDecision) {
        this.expectedPlanDecision = expectedPlanDecision;
    }

    public String getExpectedFailureCode() {
        return expectedFailureCode;
    }

    public void setExpectedFailureCode(String expectedFailureCode) {
        this.expectedFailureCode = expectedFailureCode;
    }

    public List<String> getExpectedBindingKeys() {
        return expectedBindingKeys;
    }

    public void setExpectedBindingKeys(List<String> expectedBindingKeys) {
        this.expectedBindingKeys = expectedBindingKeys;
    }

    public List<String> getExpectedAccessPathKeys() {
        return expectedAccessPathKeys;
    }

    public void setExpectedAccessPathKeys(List<String> expectedAccessPathKeys) {
        this.expectedAccessPathKeys = expectedAccessPathKeys;
    }
}