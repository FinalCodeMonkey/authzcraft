package com.fcm.authzcraft.api.attribute;

import com.fcm.authzcraft.api.plan.FailureCode;

public class AttributeFailure {

    private FailureCode failureCode;
    private String attributeKey;
    private ResolveKind resolveKind;
    private boolean retryable;
    private String sourceVersion;
    private String message;

    public AttributeFailure() {
    }

    public FailureCode getFailureCode() {
        return failureCode;
    }

    public void setFailureCode(FailureCode failureCode) {
        this.failureCode = failureCode;
    }

    public String getAttributeKey() {
        return attributeKey;
    }

    public void setAttributeKey(String attributeKey) {
        this.attributeKey = attributeKey;
    }

    public ResolveKind getResolveKind() {
        return resolveKind;
    }

    public void setResolveKind(ResolveKind resolveKind) {
        this.resolveKind = resolveKind;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public void setRetryable(boolean retryable) {
        this.retryable = retryable;
    }

    public String getSourceVersion() {
        return sourceVersion;
    }

    public void setSourceVersion(String sourceVersion) {
        this.sourceVersion = sourceVersion;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}