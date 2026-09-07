package com.fcm.authzcraft.api.attribute;

import com.fcm.authzcraft.api.common.ValueKind;

public class AttributeReference {

    private String attributeKey;
    private ValueKind valueKind;
    private ResolveKind resolveKind;
    private boolean required;

    public AttributeReference() {
    }

    public AttributeReference(String attributeKey, ValueKind valueKind, ResolveKind resolveKind, boolean required) {
        this.attributeKey = attributeKey;
        this.valueKind = valueKind;
        this.resolveKind = resolveKind;
        this.required = required;
    }

    public String getAttributeKey() {
        return attributeKey;
    }

    public void setAttributeKey(String attributeKey) {
        this.attributeKey = attributeKey;
    }

    public ValueKind getValueKind() {
        return valueKind;
    }

    public void setValueKind(ValueKind valueKind) {
        this.valueKind = valueKind;
    }

    public ResolveKind getResolveKind() {
        return resolveKind;
    }

    public void setResolveKind(ResolveKind resolveKind) {
        this.resolveKind = resolveKind;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }
}