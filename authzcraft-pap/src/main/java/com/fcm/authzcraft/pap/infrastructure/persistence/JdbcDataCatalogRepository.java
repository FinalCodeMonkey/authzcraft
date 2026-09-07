package com.fcm.authzcraft.pap.infrastructure.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.fcm.authzcraft.pap.domain.model.AccessPath;
import com.fcm.authzcraft.pap.domain.model.RelationResource;
import com.fcm.authzcraft.pap.domain.model.ResourceField;
import com.fcm.authzcraft.pap.domain.repository.DataCatalogRepository;
import com.fcm.authzcraft.pap.domain.service.CatalogIdGenerator;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class JdbcDataCatalogRepository implements DataCatalogRepository {

    private static final String ACTOR = "authzcraft-pap";

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final CatalogIdGenerator idGenerator;

    public JdbcDataCatalogRepository(JdbcTemplate jdbcTemplate, DataSource dataSource, CatalogIdGenerator idGenerator) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
        this.idGenerator = idGenerator;
    }

    @Override
    public void saveRelationResource(RelationResource resource) {
        jdbcTemplate.update(
                "INSERT INTO authzcraft_relation_resource "
                        + "(id, tenant_key, app_key, resource_key, display_name, namespace_name, physical_name, relation_kind, "
                        + "protection_mode, structure_digest, lifecycle_state, description, created_by, updated_by) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                resource.getId(), resource.getTenantKey(), resource.getAppKey(), resource.getResourceKey(),
                resource.getDisplayName(), resource.getNamespaceName(), resource.getPhysicalName(), resource.getRelationKind(),
                resource.getProtectionMode(), resource.getStructureDigest(), resource.getLifecycleState(), resource.getDescription(),
                ACTOR, ACTOR);
    }

    @Override
    public RelationResource findRelationResource(Long id) {
        List<RelationResource> resources = jdbcTemplate.query(
                "SELECT * FROM authzcraft_relation_resource WHERE id = ?",
                new Object[]{id}, relationResourceMapper());
        return resources.isEmpty() ? null : resources.get(0);
    }

    @Override
    public List<RelationResource> searchRelationResources(String tenantKey, String appKey, String resourceKey, String keyword,
                                                          String lifecycleState) {
        StringBuilder sql = new StringBuilder("SELECT * FROM authzcraft_relation_resource WHERE 1 = 1");
        List<Object> params = new ArrayList<Object>();
        appendEquals(sql, params, "tenant_key", tenantKey);
        appendEquals(sql, params, "app_key", appKey);
        appendEquals(sql, params, "resource_key", resourceKey);
        appendEquals(sql, params, "lifecycle_state", lifecycleState);
        if (StringUtils.hasText(keyword)) {
            sql.append(" AND (resource_key LIKE ? OR display_name LIKE ? OR physical_name LIKE ?)");
            String like = "%" + keyword + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        sql.append(" ORDER BY updated_at DESC LIMIT 200");
        return jdbcTemplate.query(sql.toString(), params.toArray(), relationResourceMapper());
    }

    @Override
    public void updateRelationResource(RelationResource resource) {
        jdbcTemplate.update(
                "UPDATE authzcraft_relation_resource SET display_name = ?, protection_mode = ?, lifecycle_state = ?, "
                        + "description = ?, updated_by = ?, record_version = record_version + 1 WHERE id = ?",
                resource.getDisplayName(), resource.getProtectionMode(), resource.getLifecycleState(), resource.getDescription(),
                ACTOR, resource.getId());
    }

    @Override
    public List<ResourceField> loadFieldsFromDatabase(RelationResource resource) {
        List<ResourceField> fields = new ArrayList<ResourceField>();
        Connection connection = null;
        ResultSet primaryKeys = null;
        ResultSet columns = null;
        try {
            connection = dataSource.getConnection();
            DatabaseMetaData metaData = connection.getMetaData();
            Map<String, Integer> primaryKeyOrdinals = new HashMap<String, Integer>();
            primaryKeys = metaData.getPrimaryKeys(resource.getNamespaceName(), null, resource.getPhysicalName());
            while (primaryKeys.next()) {
                primaryKeyOrdinals.put(primaryKeys.getString("COLUMN_NAME"), primaryKeys.getInt("KEY_SEQ"));
            }
            close(primaryKeys);
            primaryKeys = null;

            columns = metaData.getColumns(resource.getNamespaceName(), null, resource.getPhysicalName(), null);
            while (columns.next()) {
                ResourceField field = new ResourceField();
                String physicalName = columns.getString("COLUMN_NAME");
                field.setRelationResourceId(resource.getId());
                field.setFieldKey(physicalName);
                field.setDisplayName(physicalName);
                field.setPhysicalName(physicalName);
                field.setOrdinalPosition(columns.getInt("ORDINAL_POSITION"));
                field.setLogicalType(logicalType(columns.getInt("DATA_TYPE")));
                field.setNativeType(nativeType(columns));
                field.setNullableFlag(columns.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls);
                field.setPrimaryKeyOrdinal(primaryKeyOrdinals.get(physicalName));
                field.setFilterableFlag(false);
                field.setJoinableFlag(primaryKeyOrdinals.containsKey(physicalName));
                field.setSensitivityLevel("INTERNAL");
                field.setLifecycleState("ACTIVE");
                fields.add(field);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to read JDBC metadata for " + resource.getPhysicalName(), exception);
        } finally {
            close(columns);
            close(primaryKeys);
            close(connection);
        }
        return fields;
    }

    @Override
    public void replaceImportedFields(Long relationResourceId, List<ResourceField> fields) {
        for (ResourceField field : fields) {
            Long existingId = findActiveFieldId(relationResourceId, field.getPhysicalName());
            if (existingId == null) {
                jdbcTemplate.update(
                        "INSERT INTO authzcraft_resource_field "
                                + "(id, relation_resource_id, field_key, display_name, physical_name, ordinal_position, logical_type, "
                                + "native_type, nullable_flag, primary_key_ordinal, filterable_flag, joinable_flag, principal_ref_kind, "
                                + "sensitivity_level, security_requirement, lifecycle_state, created_by, updated_by) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        idGenerator.nextId(), relationResourceId, field.getFieldKey(), field.getDisplayName(), field.getPhysicalName(),
                        field.getOrdinalPosition(), field.getLogicalType(), field.getNativeType(), toTinyInt(field.getNullableFlag()),
                        field.getPrimaryKeyOrdinal(), toTinyInt(field.getFilterableFlag()), toTinyInt(field.getJoinableFlag()),
                        field.getPrincipalRefKind(), field.getSensitivityLevel(),
                        field.getSecurityRequirement() != null ? field.getSecurityRequirement() : "PLAIN",
                        field.getLifecycleState(), ACTOR, ACTOR);
            } else {
                jdbcTemplate.update(
                        "UPDATE authzcraft_resource_field SET display_name = ?, ordinal_position = ?, logical_type = ?, native_type = ?, "
                                + "nullable_flag = ?, primary_key_ordinal = ?, joinable_flag = ?, updated_by = ? WHERE id = ?",
                        field.getDisplayName(), field.getOrdinalPosition(), field.getLogicalType(), field.getNativeType(),
                        toTinyInt(field.getNullableFlag()), field.getPrimaryKeyOrdinal(), toTinyInt(field.getJoinableFlag()),
                        ACTOR, existingId);
            }
        }
    }

    @Override
    public List<ResourceField> searchResourceFields(Long relationResourceId, Boolean filterableFlag, Boolean joinableFlag,
                                                    String lifecycleState) {
        StringBuilder sql = new StringBuilder("SELECT * FROM authzcraft_resource_field WHERE relation_resource_id = ?");
        List<Object> params = new ArrayList<Object>();
        params.add(relationResourceId);
        if (filterableFlag != null) {
            sql.append(" AND filterable_flag = ?");
            params.add(toTinyInt(filterableFlag));
        }
        if (joinableFlag != null) {
            sql.append(" AND joinable_flag = ?");
            params.add(toTinyInt(joinableFlag));
        }
        appendEquals(sql, params, "lifecycle_state", lifecycleState);
        sql.append(" ORDER BY ordinal_position ASC");
        return jdbcTemplate.query(sql.toString(), params.toArray(), resourceFieldMapper());
    }

    @Override
    public ResourceField findResourceField(Long relationResourceId, Long fieldId) {
        List<ResourceField> fields = jdbcTemplate.query(
                "SELECT * FROM authzcraft_resource_field WHERE relation_resource_id = ? AND id = ?",
                new Object[]{relationResourceId, fieldId}, resourceFieldMapper());
        return fields.isEmpty() ? null : fields.get(0);
    }

    @Override
    public void updateResourceField(ResourceField field) {
        jdbcTemplate.update(
                "UPDATE authzcraft_resource_field SET display_name = ?, filterable_flag = ?, joinable_flag = ?, "
                        + "principal_ref_kind = ?, sensitivity_level = ?, security_requirement = ?, lifecycle_state = ?, updated_by = ? WHERE id = ?",
                field.getDisplayName(), toTinyInt(field.getFilterableFlag()), toTinyInt(field.getJoinableFlag()),
                field.getPrincipalRefKind(), field.getSensitivityLevel(),
                field.getSecurityRequirement() != null ? field.getSecurityRequirement() : "PLAIN",
                field.getLifecycleState(), ACTOR, field.getId());
    }

    @Override
    public void updateStructureDigest(Long relationResourceId, String structureDigest) {
        jdbcTemplate.update(
                "UPDATE authzcraft_relation_resource SET structure_digest = ?, updated_by = ?, record_version = record_version + 1 WHERE id = ?",
                structureDigest, ACTOR, relationResourceId);
    }

    @Override
    public void saveAccessPath(AccessPath accessPath) {
        jdbcTemplate.update(
                "INSERT INTO authzcraft_access_path "
                        + "(id, tenant_key, app_key, path_key, display_name, root_relation_id, destination_relation_id, "
                        + "execution_mode, traversal_steps, lifecycle_state, created_by, updated_by) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                accessPath.getId(), accessPath.getTenantKey(), accessPath.getAppKey(), accessPath.getPathKey(),
                accessPath.getDisplayName(), accessPath.getRootRelationId(), accessPath.getDestinationRelationId(),
                accessPath.getExecutionMode(), accessPath.getTraversalSteps(), accessPath.getLifecycleState(), ACTOR, ACTOR);
    }

    @Override
    public AccessPath findAccessPath(Long id) {
        List<AccessPath> accessPaths = jdbcTemplate.query(
                "SELECT id, tenant_key, app_key, path_key, display_name, root_relation_id, destination_relation_id, "
                        + "execution_mode, CAST(traversal_steps AS CHAR) AS traversal_steps, lifecycle_state "
                        + "FROM authzcraft_access_path WHERE id = ?",
                new Object[]{id}, accessPathMapper());
        return accessPaths.isEmpty() ? null : accessPaths.get(0);
    }

    @Override
    public List<AccessPath> searchAccessPaths(String tenantKey, String appKey, String pathKey, Long rootRelationId,
                                              String lifecycleState) {
        StringBuilder sql = new StringBuilder("SELECT id, tenant_key, app_key, path_key, display_name, root_relation_id, "
                + "destination_relation_id, execution_mode, CAST(traversal_steps AS CHAR) AS traversal_steps, lifecycle_state "
                + "FROM authzcraft_access_path WHERE 1 = 1");
        List<Object> params = new ArrayList<Object>();
        appendEquals(sql, params, "tenant_key", tenantKey);
        appendEquals(sql, params, "app_key", appKey);
        appendEquals(sql, params, "path_key", pathKey);
        if (rootRelationId != null) {
            sql.append(" AND root_relation_id = ?");
            params.add(rootRelationId);
        }
        appendEquals(sql, params, "lifecycle_state", lifecycleState);
        sql.append(" ORDER BY id DESC LIMIT 200");
        return jdbcTemplate.query(sql.toString(), params.toArray(), accessPathMapper());
    }

    @Override
    public void updateAccessPath(AccessPath accessPath) {
        jdbcTemplate.update(
            "UPDATE authzcraft_access_path SET display_name = ?, execution_mode = ?, traversal_steps = ?, "
                        + "lifecycle_state = ?, updated_by = ? WHERE id = ?",
                accessPath.getDisplayName(), accessPath.getExecutionMode(), accessPath.getTraversalSteps(),
                accessPath.getLifecycleState(), ACTOR, accessPath.getId());
    }

    private Long findActiveFieldId(Long relationResourceId, String physicalName) {
        List<Long> ids = jdbcTemplate.query(
                "SELECT id FROM authzcraft_resource_field WHERE relation_resource_id = ? AND physical_name = ? AND lifecycle_state = 'ACTIVE'",
                new Object[]{relationResourceId, physicalName},
                (resultSet, rowNum) -> resultSet.getLong("id"));
        return ids.isEmpty() ? null : ids.get(0);
    }

    private RowMapper<RelationResource> relationResourceMapper() {
        return (resultSet, rowNum) -> {
            RelationResource resource = new RelationResource();
            resource.setId(resultSet.getLong("id"));
            resource.setTenantKey(resultSet.getString("tenant_key"));
            resource.setAppKey(resultSet.getString("app_key"));
            resource.setResourceKey(resultSet.getString("resource_key"));
            resource.setDisplayName(resultSet.getString("display_name"));
            resource.setNamespaceName(resultSet.getString("namespace_name"));
            resource.setPhysicalName(resultSet.getString("physical_name"));
            resource.setRelationKind(resultSet.getString("relation_kind"));
            resource.setProtectionMode(resultSet.getString("protection_mode"));
            resource.setStructureDigest(resultSet.getString("structure_digest"));
            resource.setLifecycleState(resultSet.getString("lifecycle_state"));
            resource.setDescription(resultSet.getString("description"));
            return resource;
        };
    }

    private RowMapper<ResourceField> resourceFieldMapper() {
        return (resultSet, rowNum) -> {
            ResourceField field = new ResourceField();
            field.setId(resultSet.getLong("id"));
            field.setRelationResourceId(resultSet.getLong("relation_resource_id"));
            field.setFieldKey(resultSet.getString("field_key"));
            field.setDisplayName(resultSet.getString("display_name"));
            field.setPhysicalName(resultSet.getString("physical_name"));
            field.setOrdinalPosition(resultSet.getInt("ordinal_position"));
            field.setLogicalType(resultSet.getString("logical_type"));
            field.setNativeType(resultSet.getString("native_type"));
            field.setNullableFlag(resultSet.getInt("nullable_flag") == 1);
            int primaryKeyOrdinal = resultSet.getInt("primary_key_ordinal");
            field.setPrimaryKeyOrdinal(resultSet.wasNull() ? null : primaryKeyOrdinal);
            field.setFilterableFlag(resultSet.getInt("filterable_flag") == 1);
            field.setJoinableFlag(resultSet.getInt("joinable_flag") == 1);
            field.setPrincipalRefKind(resultSet.getString("principal_ref_kind"));
            field.setSensitivityLevel(resultSet.getString("sensitivity_level"));
            field.setSecurityRequirement(resultSet.getString("security_requirement"));
            field.setLifecycleState(resultSet.getString("lifecycle_state"));
            return field;
        };
    }

    private RowMapper<AccessPath> accessPathMapper() {
        return (resultSet, rowNum) -> {
            AccessPath accessPath = new AccessPath();
            accessPath.setId(resultSet.getLong("id"));
            accessPath.setTenantKey(resultSet.getString("tenant_key"));
            accessPath.setAppKey(resultSet.getString("app_key"));
            accessPath.setPathKey(resultSet.getString("path_key"));
            accessPath.setDisplayName(resultSet.getString("display_name"));
            accessPath.setRootRelationId(resultSet.getLong("root_relation_id"));
            accessPath.setDestinationRelationId(resultSet.getLong("destination_relation_id"));
            accessPath.setExecutionMode(resultSet.getString("execution_mode"));
            accessPath.setTraversalSteps(resultSet.getString("traversal_steps"));
            accessPath.setLifecycleState(resultSet.getString("lifecycle_state"));
            return accessPath;
        };
    }

    private void appendEquals(StringBuilder sql, List<Object> params, String columnName, String value) {
        if (StringUtils.hasText(value)) {
            sql.append(" AND ").append(columnName).append(" = ?");
            params.add(value);
        }
    }

    private String logicalType(int sqlType) {
        switch (sqlType) {
            case Types.INTEGER:
            case Types.SMALLINT:
            case Types.TINYINT:
            case Types.BIGINT:
                return "INTEGER";
            case Types.NUMERIC:
            case Types.DECIMAL:
            case Types.FLOAT:
            case Types.DOUBLE:
            case Types.REAL:
                return "DECIMAL";
            case Types.BOOLEAN:
            case Types.BIT:
                return "BOOLEAN";
            case Types.DATE:
                return "DATE";
            case Types.TIMESTAMP:
            case Types.TIMESTAMP_WITH_TIMEZONE:
            case Types.TIME:
                return "DATETIME";
            case Types.JAVA_OBJECT:
            case Types.STRUCT:
            case Types.ARRAY:
                return "JSON";
            default:
                return "STRING";
        }
    }

    private String nativeType(ResultSet columns) throws SQLException {
        String typeName = columns.getString("TYPE_NAME");
        int columnSize = columns.getInt("COLUMN_SIZE");
        return columnSize > 0 ? typeName + "(" + columnSize + ")" : typeName;
    }

    private int toTinyInt(Boolean value) {
        return Boolean.TRUE.equals(value) ? 1 : 0;
    }

    private void close(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignored) {
        }
    }
}