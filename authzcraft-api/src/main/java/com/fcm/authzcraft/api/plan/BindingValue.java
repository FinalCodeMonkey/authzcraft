package com.fcm.authzcraft.api.plan;

import com.fcm.authzcraft.api.common.ValueKind;

public class BindingValue {

    private ValueKind valueKind;
    private Object value;
    private boolean sensitive;

    public BindingValue() {
    }

    public BindingValue(ValueKind valueKind, Object value, boolean sensitive) {
        this.valueKind = valueKind;
        this.value = value;
        this.sensitive = sensitive;
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

    public boolean isSensitive() {
        return sensitive;
    }

    public void setSensitive(boolean sensitive) {
        this.sensitive = sensitive;
    }
}