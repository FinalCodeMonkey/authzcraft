package com.fcm.authzcraft.pip.application.command;

public class PrincipalSyncCommand {

    private final String tenantKey;
    private final String appKey;
    private final String sourceCode;
    private final String sourceTenantId;
    private final String sourceAppCode;

    public PrincipalSyncCommand(String tenantKey, String appKey, String sourceCode, String sourceTenantId, String sourceAppCode) {
        this.tenantKey = tenantKey;
        this.appKey = appKey;
        this.sourceCode = sourceCode;
        this.sourceTenantId = sourceTenantId;
        this.sourceAppCode = sourceAppCode;
    }

    public String getTenantKey() {
        return tenantKey;
    }

    public String getAppKey() {
        return appKey;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public String getSourceTenantId() {
        return sourceTenantId;
    }

    public String getSourceAppCode() {
        return sourceAppCode;
    }
}
