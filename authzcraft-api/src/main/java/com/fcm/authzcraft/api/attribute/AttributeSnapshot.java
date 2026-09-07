package com.fcm.authzcraft.api.attribute;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class AttributeSnapshot {

    private Map<String, AttributeValue> attributes = new LinkedHashMap<String, AttributeValue>();
    private String attributeDigest;
    private String sourceVersion;
    private String trustLevel;
    private Instant generatedAt;
    private Instant expiresAt;

    public AttributeSnapshot() {
    }

    public Map<String, AttributeValue> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, AttributeValue> attributes) {
        this.attributes = attributes;
    }

    public String getAttributeDigest() {
        return attributeDigest;
    }

    public void setAttributeDigest(String attributeDigest) {
        this.attributeDigest = attributeDigest;
    }

    public String getSourceVersion() {
        return sourceVersion;
    }

    public void setSourceVersion(String sourceVersion) {
        this.sourceVersion = sourceVersion;
    }

    public String getTrustLevel() {
        return trustLevel;
    }

    public void setTrustLevel(String trustLevel) {
        this.trustLevel = trustLevel;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}