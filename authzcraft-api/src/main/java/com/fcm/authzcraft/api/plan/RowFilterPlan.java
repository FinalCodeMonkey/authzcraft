package com.fcm.authzcraft.api.plan;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fcm.authzcraft.api.predicate.PredicateNode;

public class RowFilterPlan {

    private String decisionKey;
    private PlanDecision planDecision;
    private String targetResourceKey;
    private ProtectionMode protectionMode;
    private PlannerKind plannerKind;
    private PredicateNode predicate;
    private Map<String, BindingValue> bindings = new LinkedHashMap<String, BindingValue>();
    private List<String> requiredAccessPathKeys = new ArrayList<String>();
    private String planDigest;
    private String attributeDigest;
    private FailureCode failureCode;
    private String failureMessage;
    private Instant expiresAt;

    public RowFilterPlan() {
    }

    public String getDecisionKey() {
        return decisionKey;
    }

    public void setDecisionKey(String decisionKey) {
        this.decisionKey = decisionKey;
    }

    public PlanDecision getPlanDecision() {
        return planDecision;
    }

    public void setPlanDecision(PlanDecision planDecision) {
        this.planDecision = planDecision;
    }

    public String getTargetResourceKey() {
        return targetResourceKey;
    }

    public void setTargetResourceKey(String targetResourceKey) {
        this.targetResourceKey = targetResourceKey;
    }

    public ProtectionMode getProtectionMode() {
        return protectionMode;
    }

    public void setProtectionMode(ProtectionMode protectionMode) {
        this.protectionMode = protectionMode;
    }

    public PlannerKind getPlannerKind() {
        return plannerKind;
    }

    public void setPlannerKind(PlannerKind plannerKind) {
        this.plannerKind = plannerKind;
    }

    public PredicateNode getPredicate() {
        return predicate;
    }

    public void setPredicate(PredicateNode predicate) {
        this.predicate = predicate;
    }

    public Map<String, BindingValue> getBindings() {
        return bindings;
    }

    public void setBindings(Map<String, BindingValue> bindings) {
        this.bindings = bindings;
    }

    public List<String> getRequiredAccessPathKeys() {
        return requiredAccessPathKeys;
    }

    public void setRequiredAccessPathKeys(List<String> requiredAccessPathKeys) {
        this.requiredAccessPathKeys = requiredAccessPathKeys;
    }

    public String getPlanDigest() {
        return planDigest;
    }

    public void setPlanDigest(String planDigest) {
        this.planDigest = planDigest;
    }

    public String getAttributeDigest() {
        return attributeDigest;
    }

    public void setAttributeDigest(String attributeDigest) {
        this.attributeDigest = attributeDigest;
    }

    public FailureCode getFailureCode() {
        return failureCode;
    }

    public void setFailureCode(FailureCode failureCode) {
        this.failureCode = failureCode;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public void setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}