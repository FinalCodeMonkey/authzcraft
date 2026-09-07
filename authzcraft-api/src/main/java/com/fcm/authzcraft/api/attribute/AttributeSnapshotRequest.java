package com.fcm.authzcraft.api.attribute;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fcm.authzcraft.api.common.RequesterKind;

public class AttributeSnapshotRequest {

    private String tenantKey;
    private String appKey;
    private RequesterKind requesterKind;
    private String requesterKey;
    private String requestKey;
    private List<AttributeReference> attributeReferences = new ArrayList<AttributeReference>();
    private Map<String, Object> trustedEnvironment = new LinkedHashMap<String, Object>();
    private Map<String, Object> grantArguments = new LinkedHashMap<String, Object>();

    public AttributeSnapshotRequest() {
    }

    public String getTenantKey() {
        return tenantKey;
    }

    public void setTenantKey(String tenantKey) {
        this.tenantKey = tenantKey;
    }

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public RequesterKind getRequesterKind() {
        return requesterKind;
    }

    public void setRequesterKind(RequesterKind requesterKind) {
        this.requesterKind = requesterKind;
    }

    public String getRequesterKey() {
        return requesterKey;
    }

    public void setRequesterKey(String requesterKey) {
        this.requesterKey = requesterKey;
    }

    public String getRequestKey() {
        return requestKey;
    }

    public void setRequestKey(String requestKey) {
        this.requestKey = requestKey;
    }

    public List<AttributeReference> getAttributeReferences() {
        return attributeReferences;
    }

    public void setAttributeReferences(List<AttributeReference> attributeReferences) {
        this.attributeReferences = attributeReferences;
    }

    public Map<String, Object> getTrustedEnvironment() {
        return trustedEnvironment;
    }

    public void setTrustedEnvironment(Map<String, Object> trustedEnvironment) {
        this.trustedEnvironment = trustedEnvironment;
    }

    public Map<String, Object> getGrantArguments() {
        return grantArguments;
    }

    public void setGrantArguments(Map<String, Object> grantArguments) {
        this.grantArguments = grantArguments;
    }
}