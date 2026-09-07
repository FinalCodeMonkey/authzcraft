package com.fcm.authzcraft.pip.domain.model;

public class PrincipalSyncScope {

    private final String tenantKey;
    private final String appKey;
    private final String sourceTenantId;
    private final String sourceAppCode;

    public PrincipalSyncScope(String tenantKey, String appKey, String sourceTenantId, String sourceAppCode) {
        this.tenantKey = tenantKey;
        this.appKey = appKey;
        this.sourceTenantId = sourceTenantId;
        this.sourceAppCode = sourceAppCode;
    }

    public String getTenantKey() {
        return tenantKey;
    }

    public String getAppKey() {
        return appKey;
    }

    public String getSourceTenantId() {
        return sourceTenantId;
    }

    public String getSourceAppCode() {
        return sourceAppCode;
    }
}
