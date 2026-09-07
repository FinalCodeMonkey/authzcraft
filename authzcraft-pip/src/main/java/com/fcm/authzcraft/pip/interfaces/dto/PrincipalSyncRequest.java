package com.fcm.authzcraft.pip.interfaces.dto;

public class PrincipalSyncRequest {

    private String tenantKey;
    private String appKey;
    private String sourceTenantId;
    private String sourceAppCode;

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

    public String getSourceTenantId() {
        return sourceTenantId;
    }

    public void setSourceTenantId(String sourceTenantId) {
        this.sourceTenantId = sourceTenantId;
    }

    public String getSourceAppCode() {
        return sourceAppCode;
    }

    public void setSourceAppCode(String sourceAppCode) {
        this.sourceAppCode = sourceAppCode;
    }
}
