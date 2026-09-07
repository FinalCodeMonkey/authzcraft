package com.fcm.authzcraft.pap.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fcm.authzcraft.pap.application.command.RegisterAccessPathCommand;
import com.fcm.authzcraft.pap.application.command.RegisterRelationResourceCommand;
import com.fcm.authzcraft.pap.application.command.UpdateAccessPathCommand;
import com.fcm.authzcraft.pap.application.command.UpdateRelationResourceCommand;
import com.fcm.authzcraft.pap.application.command.UpdateResourceFieldCommand;
import com.fcm.authzcraft.pap.application.query.AccessPathSearchQuery;
import com.fcm.authzcraft.pap.application.query.RelationResourceSearchQuery;
import com.fcm.authzcraft.pap.application.query.ResourceFieldSearchQuery;
import com.fcm.authzcraft.pap.domain.model.AccessPath;
import com.fcm.authzcraft.pap.domain.model.RelationResource;
import com.fcm.authzcraft.pap.domain.model.ResourceField;
import com.fcm.authzcraft.pap.domain.repository.DataCatalogRepository;
import com.fcm.authzcraft.pap.domain.service.CatalogIdGenerator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@Service
public class DataCatalogService {

    private static final String DEFAULT_ACTOR = "authzcraft-pap";

    private final DataCatalogRepository repository;
    private final CatalogIdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    public DataCatalogService(DataCatalogRepository repository, CatalogIdGenerator idGenerator, ObjectMapper objectMapper) {
        this.repository = repository;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public RelationResource registerRelationResource(RegisterRelationResourceCommand command) {
        requireText(command.getTenantKey(), "tenantKey");
        requireText(command.getAppKey(), "appKey");
        requireText(command.getResourceKey(), "resourceKey");
        requireText(command.getDisplayName(), "displayName");
        requireText(command.getNamespaceName(), "namespaceName");
        requireText(command.getPhysicalName(), "physicalName");
        assertIn(command.getRelationKind(), "relationKind", "TABLE", "VIEW");
        String protectionMode = defaultText(command.getProtectionMode(), "SHADOW");
        assertIn(protectionMode, "protectionMode", "ENFORCE", "SHADOW", "DISABLED");

        RelationResource resource = new RelationResource();
        resource.setId(idGenerator.nextId());
        resource.setTenantKey(command.getTenantKey());
        resource.setAppKey(command.getAppKey());
        resource.setResourceKey(command.getResourceKey());
        resource.setDisplayName(command.getDisplayName());
        resource.setNamespaceName(command.getNamespaceName());
        resource.setPhysicalName(command.getPhysicalName());
        resource.setRelationKind(command.getRelationKind());
        resource.setProtectionMode(protectionMode);
        resource.setLifecycleState("ACTIVE");
        resource.setDescription(command.getDescription());
        resource.setStructureDigest(sha256(command.getTenantKey() + "|" + command.getAppKey() + "|" + command.getNamespaceName()
                + "|" + command.getPhysicalName() + "|" + command.getRelationKind()));
        repository.saveRelationResource(resource);
        return repository.findRelationResource(resource.getId());
    }

    public List<RelationResource> searchRelationResources(RelationResourceSearchQuery query) {
        return repository.searchRelationResources(query.getTenantKey(), query.getAppKey(), query.getResourceKey(),
                query.getKeyword(), query.getLifecycleState());
    }

    @Transactional
    public RelationResource updateRelationResource(Long id, UpdateRelationResourceCommand command) {
        RelationResource resource = requireRelationResource(id);
        if (StringUtils.hasText(command.getDisplayName())) {
            resource.setDisplayName(command.getDisplayName());
        }
        if (StringUtils.hasText(command.getProtectionMode())) {
            assertIn(command.getProtectionMode(), "protectionMode", "ENFORCE", "SHADOW", "DISABLED");
            resource.setProtectionMode(command.getProtectionMode());
        }
        if (StringUtils.hasText(command.getLifecycleState())) {
            assertIn(command.getLifecycleState(), "lifecycleState", "ACTIVE", "SUSPENDED", "ARCHIVED");
            resource.setLifecycleState(command.getLifecycleState());
        }
        if (command.getDescription() != null) {
            resource.setDescription(command.getDescription());
        }
        repository.updateRelationResource(resource);
        return repository.findRelationResource(id);
    }

    @Transactional
    public List<ResourceField> importResourceFields(Long relationResourceId) {
        RelationResource resource = requireRelationResource(relationResourceId);
        List<ResourceField> fields = repository.loadFieldsFromDatabase(resource);
        if (fields.isEmpty()) {
            throw new IllegalArgumentException("No columns found for relation resource: " + resource.getResourceKey());
        }
        repository.replaceImportedFields(relationResourceId, fields);
        repository.updateStructureDigest(relationResourceId, structureDigest(repository.searchResourceFields(
                relationResourceId, null, null, null)));
        return repository.searchResourceFields(relationResourceId, null, null, null);
    }

    public List<ResourceField> searchResourceFields(Long relationResourceId, ResourceFieldSearchQuery query) {
        requireRelationResource(relationResourceId);
        return repository.searchResourceFields(relationResourceId, query.getFilterableFlag(), query.getJoinableFlag(),
                query.getLifecycleState());
    }

    @Transactional
    public ResourceField updateResourceField(Long relationResourceId, Long fieldId, UpdateResourceFieldCommand command) {
        requireRelationResource(relationResourceId);
        ResourceField field = repository.findResourceField(relationResourceId, fieldId);
        if (field == null) {
            throw new IllegalArgumentException("Resource field not found: " + fieldId);
        }
        if (StringUtils.hasText(command.getDisplayName())) {
            field.setDisplayName(command.getDisplayName());
        }
        if (command.getFilterableFlag() != null) {
            field.setFilterableFlag(command.getFilterableFlag());
        }
        if (command.getJoinableFlag() != null) {
            field.setJoinableFlag(command.getJoinableFlag());
        }
        if (command.getPrincipalRefKind() != null) {
            if (StringUtils.hasText(command.getPrincipalRefKind())) {
                assertIn(command.getPrincipalRefKind(), "principalRefKind", "USER", "ORGANIZATION", "POSITION");
                field.setPrincipalRefKind(command.getPrincipalRefKind());
            } else {
                field.setPrincipalRefKind(null);
            }
        }
        if (StringUtils.hasText(command.getSensitivityLevel())) {
            assertIn(command.getSensitivityLevel(), "sensitivityLevel", "PUBLIC", "INTERNAL", "CONFIDENTIAL", "RESTRICTED");
            field.setSensitivityLevel(command.getSensitivityLevel());
        }
        if (StringUtils.hasText(command.getSecurityRequirement())) {
            assertIn(command.getSecurityRequirement(), "securityRequirement", "PLAIN", "MASKED", "HIDDEN");
            field.setSecurityRequirement(command.getSecurityRequirement());
        }
        if (StringUtils.hasText(command.getLifecycleState())) {
            assertIn(command.getLifecycleState(), "lifecycleState", "ACTIVE", "RETIRED");
            field.setLifecycleState(command.getLifecycleState());
        }
        repository.updateResourceField(field);
        repository.updateStructureDigest(relationResourceId, structureDigest(repository.searchResourceFields(
                relationResourceId, null, null, null)));
        return repository.findResourceField(relationResourceId, fieldId);
    }

    @Transactional
    public AccessPath registerAccessPath(RegisterAccessPathCommand command) {
        requireText(command.getTenantKey(), "tenantKey");
        requireText(command.getAppKey(), "appKey");
        requireText(command.getPathKey(), "pathKey");
        requireText(command.getDisplayName(), "displayName");
        requireText(command.getTraversalSteps(), "traversalSteps");
        assertIn(command.getExecutionMode(), "executionMode", "EXISTS", "JOIN");
        validateTraversalSteps(command.getTraversalSteps());
        RelationResource root = requireRelationResource(command.getRootRelationId());
        RelationResource destination = requireRelationResource(command.getDestinationRelationId());
        assertSameScope(command.getTenantKey(), command.getAppKey(), root, destination);

        AccessPath accessPath = new AccessPath();
        accessPath.setId(idGenerator.nextId());
        accessPath.setTenantKey(command.getTenantKey());
        accessPath.setAppKey(command.getAppKey());
        accessPath.setPathKey(command.getPathKey());
        accessPath.setDisplayName(command.getDisplayName());
        accessPath.setRootRelationId(command.getRootRelationId());
        accessPath.setDestinationRelationId(command.getDestinationRelationId());
        accessPath.setExecutionMode(command.getExecutionMode());
        accessPath.setTraversalSteps(command.getTraversalSteps());
        accessPath.setLifecycleState("ACTIVE");
        repository.saveAccessPath(accessPath);
        return repository.findAccessPath(accessPath.getId());
    }

    public List<AccessPath> searchAccessPaths(AccessPathSearchQuery query) {
        return repository.searchAccessPaths(query.getTenantKey(), query.getAppKey(), query.getPathKey(),
                parseLong(query.getRootRelationId()), query.getLifecycleState());
    }

    @Transactional
    public AccessPath updateAccessPath(Long id, UpdateAccessPathCommand command) {
        AccessPath accessPath = requireAccessPath(id);
        if (StringUtils.hasText(command.getDisplayName())) {
            accessPath.setDisplayName(command.getDisplayName());
        }
        if (StringUtils.hasText(command.getExecutionMode())) {
            assertIn(command.getExecutionMode(), "executionMode", "EXISTS", "JOIN");
            accessPath.setExecutionMode(command.getExecutionMode());
        }
        if (StringUtils.hasText(command.getTraversalSteps())) {
            validateTraversalSteps(command.getTraversalSteps());
            accessPath.setTraversalSteps(command.getTraversalSteps());
        }
        if (StringUtils.hasText(command.getLifecycleState())) {
            assertIn(command.getLifecycleState(), "lifecycleState", "ACTIVE", "SUSPENDED", "ARCHIVED");
            accessPath.setLifecycleState(command.getLifecycleState());
        }
        repository.updateAccessPath(accessPath);
        return repository.findAccessPath(id);
    }

    private RelationResource requireRelationResource(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("relationResourceId must not be null");
        }
        RelationResource resource = repository.findRelationResource(id);
        if (resource == null) {
            throw new IllegalArgumentException("Relation resource not found: " + id);
        }
        return resource;
    }

    private AccessPath requireAccessPath(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("accessPathId must not be null");
        }
        AccessPath accessPath = repository.findAccessPath(id);
        if (accessPath == null) {
            throw new IllegalArgumentException("Access path not found: " + id);
        }
        return accessPath;
    }

    private void assertSameScope(String tenantKey, String appKey, RelationResource root, RelationResource destination) {
        if (!tenantKey.equals(root.getTenantKey()) || !appKey.equals(root.getAppKey())
                || !tenantKey.equals(destination.getTenantKey()) || !appKey.equals(destination.getAppKey())) {
            throw new IllegalArgumentException("Access path root and destination resources must belong to the same tenant and app");
        }
    }

    private void validateTraversalSteps(String traversalSteps) {
        try {
            JsonNode node = objectMapper.readTree(traversalSteps);
            if (!node.isArray() || node.size() < 1 || node.size() > 5) {
                throw new IllegalArgumentException("traversalSteps must be an array with 1 to 5 steps");
            }
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("traversalSteps must be valid JSON array", exception);
        }
    }

    private String structureDigest(List<ResourceField> fields) {
        StringBuilder builder = new StringBuilder();
        for (ResourceField field : fields) {
            builder.append(field.getFieldKey()).append('|')
                    .append(field.getPhysicalName()).append('|')
                    .append(field.getOrdinalPosition()).append('|')
                    .append(field.getLogicalType()).append('|')
                    .append(field.getNativeType()).append('|')
                    .append(field.getFilterableFlag()).append('|')
                    .append(field.getJoinableFlag()).append(';');
        }
        return sha256(builder.toString());
    }

    private void requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private void assertIn(String value, String fieldName, String... candidates) {
        requireText(value, fieldName);
        for (String candidate : candidates) {
            if (candidate.equals(value)) {
                return;
            }
        }
        throw new IllegalArgumentException(fieldName + " is invalid: " + value);
    }

    private Long parseLong(String value) {
        return StringUtils.hasText(value) ? Long.valueOf(value) : null;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte item : bytes) {
                builder.append(String.format("%02x", item & 0xff));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}