package com.fcm.authzcraft.api.attribute;

import com.fcm.authzcraft.api.common.ValueKind;

public class AttributeValue {

    private String attributeKey;
    private ValueKind valueKind;
    private Object value;
    private boolean available = true;
    private AttributeFailure failure;

    public AttributeValue() {
    }

    public AttributeValue(String attributeKey, ValueKind valueKind, Object value) {
        this.attributeKey = attributeKey;
        this.valueKind = valueKind;
        this.value = value;
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

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public AttributeFailure getFailure() {
        return failure;
    }

    public void setFailure(AttributeFailure failure) {
        this.failure = failure;
    }
}