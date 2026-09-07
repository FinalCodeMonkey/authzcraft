package com.fcm.authzcraft.pep.autoconfigure;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Map;

public class AuthzCraftPepPropertiesValidator implements InitializingBean {

    private final AuthzCraftPepProperties properties;

    public AuthzCraftPepPropertiesValidator(AuthzCraftPepProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() {
        requireText(properties.getCenterBaseUrl(), "authzcraft.pep.center-base-url");
        requireText(properties.getTenantKey(), "authzcraft.pep.tenant-key");
        requireText(properties.getAppKey(), "authzcraft.pep.app-key");
        validateRequester();
        validateFailureMode();
        validateMappings();
    }

    private void validateRequester() {
        String source = normalize(properties.getRequesterSource());
        if (!"STATIC".equals(source) && !"HEADER".equals(source)) {
            throw new IllegalStateException("authzcraft.pep.requester-source only supports STATIC or HEADER");
        }
        if ("STATIC".equals(source)) {
            requireText(properties.getRequesterKey(), "authzcraft.pep.requester-key");
        }
        if ("HEADER".equals(source)) {
            requireText(properties.getRequesterKeyHeader(), "authzcraft.pep.requester-key-header");
        }
    }

    private void validateFailureMode() {
        String failureMode = normalize(properties.getFailureMode());
        if (!"FAIL_CLOSE".equals(failureMode) && !"FAIL_OPEN".equals(failureMode)) {
            throw new IllegalStateException("authzcraft.pep.failure-mode only supports FAIL_CLOSE or FAIL_OPEN");
        }
    }

    private void validateMappings() {
        if (properties.getMappings() == null || properties.getMappings().isEmpty()) {
            return;
        }
        for (Map.Entry<String, AuthzCraftPepProperties.ResourceMapping> entry : properties.getMappings().entrySet()) {
            String mappingKey = "authzcraft.pep.mappings[" + entry.getKey() + "]";
            AuthzCraftPepProperties.ResourceMapping mapping = entry.getValue();
            if (mapping == null) {
                throw new IllegalStateException(mappingKey + " must not be null");
            }
            requireText(mapping.getResourceKey(), mappingKey + ".resource-key");
            if (StringUtils.hasText(mapping.getOperationCode())) {
                String operationCode = normalize(mapping.getOperationCode());
                if (!"READ".equals(operationCode) && !"UPDATE".equals(operationCode) && !"DELETE".equals(operationCode)) {
                    throw new IllegalStateException(mappingKey + ".operation-code only supports READ, UPDATE or DELETE");
                }
            }
            validateAccessPaths(mappingKey, mapping);
        }
    }

    private void validateAccessPaths(String mappingKey, AuthzCraftPepProperties.ResourceMapping mapping) {
        if (mapping.getAccessPaths() == null || mapping.getAccessPaths().isEmpty()) {
            return;
        }
        for (Map.Entry<String, AuthzCraftPepProperties.AccessPathMapping> entry : mapping.getAccessPaths().entrySet()) {
            String pathKey = mappingKey + ".access-paths[" + entry.getKey() + "]";
            AuthzCraftPepProperties.AccessPathMapping accessPath = entry.getValue();
            if (accessPath == null) {
                throw new IllegalStateException(pathKey + " must not be null");
            }
            requireText(accessPath.getTargetTable(), pathKey + ".target-table");
            requireText(accessPath.getSourceField(), pathKey + ".source-field");
            requireText(accessPath.getTargetField(), pathKey + ".target-field");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private void requireText(String value, String propertyName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(propertyName + " is required");
        }
    }
}
