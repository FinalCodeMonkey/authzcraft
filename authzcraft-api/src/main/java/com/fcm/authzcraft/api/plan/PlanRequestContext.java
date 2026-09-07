package com.fcm.authzcraft.api.plan;

import java.util.LinkedHashMap;
import java.util.Map;

public class PlanRequestContext {

    private String requestKey;
    private String environment;
    private Map<String, Object> trustedAttributes = new LinkedHashMap<String, Object>();

    public PlanRequestContext() {
    }

    public PlanRequestContext(String requestKey, String environment) {
        this.requestKey = requestKey;
        this.environment = environment;
    }

    public String getRequestKey() {
        return requestKey;
    }

    public void setRequestKey(String requestKey) {
        this.requestKey = requestKey;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public Map<String, Object> getTrustedAttributes() {
        return trustedAttributes;
    }

    public void setTrustedAttributes(Map<String, Object> trustedAttributes) {
        this.trustedAttributes = trustedAttributes;
    }
}