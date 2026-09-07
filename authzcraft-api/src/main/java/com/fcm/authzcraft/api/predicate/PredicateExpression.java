package com.fcm.authzcraft.api.predicate;

import com.fcm.authzcraft.api.common.ValueKind;

public class PredicateExpression {

    private PredicateExpressionKind kind;
    private String fieldKey;
    private String bindingKey;
    private String attributeKey;
    private String argumentKey;
    private Object literalValue;
    private ValueKind valueKind;

    public PredicateExpression() {
    }

    public PredicateExpression(PredicateExpressionKind kind) {
        this.kind = kind;
    }

    public PredicateExpressionKind getKind() {
        return kind;
    }

    public void setKind(PredicateExpressionKind kind) {
        this.kind = kind;
    }

    public String getFieldKey() {
        return fieldKey;
    }

    public void setFieldKey(String fieldKey) {
        this.fieldKey = fieldKey;
    }

    public String getBindingKey() {
        return bindingKey;
    }

    public void setBindingKey(String bindingKey) {
        this.bindingKey = bindingKey;
    }

    public String getAttributeKey() {
        return attributeKey;
    }

    public void setAttributeKey(String attributeKey) {
        this.attributeKey = attributeKey;
    }

    public String getArgumentKey() {
        return argumentKey;
    }

    public void setArgumentKey(String argumentKey) {
        this.argumentKey = argumentKey;
    }

    public Object getLiteralValue() {
        return literalValue;
    }

    public void setLiteralValue(Object literalValue) {
        this.literalValue = literalValue;
    }

    public ValueKind getValueKind() {
        return valueKind;
    }

    public void setValueKind(ValueKind valueKind) {
        this.valueKind = valueKind;
    }
}