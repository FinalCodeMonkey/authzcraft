package com.fcm.authzcraft.pep.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.fcm.authzcraft.api.common.RequesterKind;

import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "authzcraft.pep")
public class AuthzCraftPepProperties {

    private boolean enabled = true;
    private String centerBaseUrl = "http://localhost:8088";
    private String tenantKey = "platform";
    private String appKey = "workbuddy";
    private String requesterSource = "STATIC";
    private RequesterKind requesterKind = RequesterKind.USER;
    private String requesterKey;
    private String requesterKindHeader = "X-AuthzCraft-Requester-Kind";
    private String requesterKeyHeader = "X-AuthzCraft-Requester-Key";
    private String environment = "DEFAULT";
    private String failureMode = "FAIL_CLOSE";
    private Observability observability = new Observability();
    private Map<String, ResourceMapping> mappings = new LinkedHashMap<String, ResourceMapping>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCenterBaseUrl() {
        return centerBaseUrl;
    }

    public void setCenterBaseUrl(String centerBaseUrl) {
        this.centerBaseUrl = centerBaseUrl;
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

    public String getRequesterSource() {
        return requesterSource;
    }

    public void setRequesterSource(String requesterSource) {
        this.requesterSource = requesterSource;
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

    public String getRequesterKindHeader() {
        return requesterKindHeader;
    }

    public void setRequesterKindHeader(String requesterKindHeader) {
        this.requesterKindHeader = requesterKindHeader;
    }

    public String getRequesterKeyHeader() {
        return requesterKeyHeader;
    }

    public void setRequesterKeyHeader(String requesterKeyHeader) {
        this.requesterKeyHeader = requesterKeyHeader;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getFailureMode() {
        return failureMode;
    }

    public void setFailureMode(String failureMode) {
        this.failureMode = failureMode;
    }

    public Observability getObservability() {
        return observability;
    }

    public void setObservability(Observability observability) {
        this.observability = observability;
    }

    public Map<String, ResourceMapping> getMappings() {
        return mappings;
    }

    public void setMappings(Map<String, ResourceMapping> mappings) {
        this.mappings = mappings;
    }

    public static class ResourceMapping {
        private String resourceKey;
        private String operationCode = "READ";
        private Map<String, String> fieldMappings = new LinkedHashMap<String, String>();
        private Map<String, AccessPathMapping> accessPaths = new LinkedHashMap<String, AccessPathMapping>();

        public String getResourceKey() {
            return resourceKey;
        }

        public void setResourceKey(String resourceKey) {
            this.resourceKey = resourceKey;
        }

        public String getOperationCode() {
            return operationCode;
        }

        public void setOperationCode(String operationCode) {
            this.operationCode = operationCode;
        }

        public Map<String, String> getFieldMappings() {
            return fieldMappings;
        }

        public void setFieldMappings(Map<String, String> fieldMappings) {
            this.fieldMappings = fieldMappings;
        }

        public Map<String, AccessPathMapping> getAccessPaths() {
            return accessPaths;
        }

        public void setAccessPaths(Map<String, AccessPathMapping> accessPaths) {
            this.accessPaths = accessPaths;
        }
    }

    public static class AccessPathMapping {
        private String targetTable;
        private String sourceField;
        private String targetField;
        private Map<String, String> targetFieldMappings = new LinkedHashMap<String, String>();

        public String getTargetTable() {
            return targetTable;
        }

        public void setTargetTable(String targetTable) {
            this.targetTable = targetTable;
        }

        public String getSourceField() {
            return sourceField;
        }

        public void setSourceField(String sourceField) {
            this.sourceField = sourceField;
        }

        public String getTargetField() {
            return targetField;
        }

        public void setTargetField(String targetField) {
            this.targetField = targetField;
        }

        public Map<String, String> getTargetFieldMappings() {
            return targetFieldMappings;
        }

        public void setTargetFieldMappings(Map<String, String> targetFieldMappings) {
            this.targetFieldMappings = targetFieldMappings;
        }
    }

    public static class Observability {
        private boolean responseHeadersEnabled = false;
        private boolean executionLogEnabled = true;

        public boolean isResponseHeadersEnabled() {
            return responseHeadersEnabled;
        }

        public void setResponseHeadersEnabled(boolean responseHeadersEnabled) {
            this.responseHeadersEnabled = responseHeadersEnabled;
        }

        public boolean isExecutionLogEnabled() {
            return executionLogEnabled;
        }

        public void setExecutionLogEnabled(boolean executionLogEnabled) {
            this.executionLogEnabled = executionLogEnabled;
        }
    }
}