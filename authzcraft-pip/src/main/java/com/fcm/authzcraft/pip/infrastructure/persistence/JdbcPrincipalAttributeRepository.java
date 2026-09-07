package com.fcm.authzcraft.pip.infrastructure.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.fcm.authzcraft.pip.domain.model.PrincipalAttributeProjection;
import com.fcm.authzcraft.pip.domain.repository.PrincipalAttributeRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Repository
public class JdbcPrincipalAttributeRepository implements PrincipalAttributeRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcPrincipalAttributeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public PrincipalAttributeProjection findActiveUser(String tenantKey, String requesterKey) {
        List<PrincipalAttributeProjection> results = jdbcTemplate.query(
                "SELECT p.id AS principal_id, u.user_id, u.department_code, u.post_code "
                        + "FROM authzcraft_principal p "
                        + "JOIN authzcraft_principal_user u ON u.principal_id = p.id AND u.tenant_key = p.tenant_key "
                        + "WHERE p.tenant_key = ? AND p.principal_kind = 'USER' AND p.principal_key = ? AND p.lifecycle_state = 'ACTIVE'",
                new Object[]{tenantKey, requesterKey},
                (resultSet, rowNum) -> mapUser(resultSet));
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    @SuppressWarnings("null")
    public List<String> findManagedDepartmentCodes(String tenantKey, String requesterKey, List<String> departmentScopes,
                                                   String subDepartmentDepth) {
        if (departmentScopes == null || departmentScopes.isEmpty() || departmentScopes.contains("ALL_MANAGED")) {
            return jdbcTemplate.queryForList(
                    "SELECT DISTINCT c.descendant_department_code "
                            + "FROM authzcraft_principal_organization o "
                            + "JOIN authzcraft_principal p ON p.id = o.principal_id AND p.tenant_key = o.tenant_key "
                            + "JOIN authzcraft_organization_closure c ON c.tenant_key = o.tenant_key "
                            + " AND c.ancestor_department_code = o.department_code "
                            + "WHERE o.tenant_key = ? AND p.lifecycle_state = 'ACTIVE' AND o.manage_user_id = ? "
                            + "ORDER BY c.descendant_department_code",
                    new Object[]{tenantKey, requesterKey}, String.class);
        }
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<Object>();
        sql.append("SELECT DISTINCT c.descendant_department_code ")
                .append("FROM authzcraft_principal_organization o ")
                .append("JOIN authzcraft_principal p ON p.id = o.principal_id AND p.tenant_key = o.tenant_key ")
                .append("JOIN authzcraft_organization_closure c ON c.tenant_key = o.tenant_key ")
                .append(" AND c.ancestor_department_code = o.department_code ")
                .append("WHERE o.tenant_key = ? AND p.lifecycle_state = 'ACTIVE' AND (");
        params.add(tenantKey);
        List<String> clauses = new ArrayList<String>();
        if (departmentScopes.contains("DIRECT_MANAGED")) {
            clauses.add("(o.manage_user_id = ? AND c.depth = 0)");
            params.add(requesterKey);
        }
        if (departmentScopes.contains("SUB_MANAGED")) {
            Integer maxDepth = maxDepth(subDepartmentDepth);
            if (maxDepth == null) {
                clauses.add("(o.manage_user_id = ? AND c.depth > 0)");
                params.add(requesterKey);
            } else {
                clauses.add("(o.manage_user_id = ? AND c.depth BETWEEN 1 AND ?)");
                params.add(requesterKey);
                params.add(maxDepth);
            }
        }
        if (departmentScopes.contains("PORTION_MANAGED")) {
            clauses.add("(o.portion_manage_user_id = ? AND c.depth = 0)");
            params.add(requesterKey);
        }
        if (clauses.isEmpty()) {
            return Collections.emptyList();
        }
        for (int index = 0; index < clauses.size(); index++) {
            if (index > 0) {
                sql.append(" OR ");
            }
            sql.append(clauses.get(index));
        }
        sql.append(") ORDER BY c.descendant_department_code");
        List<String> departmentCodes = new ArrayList<String>();
        jdbcTemplate.query(sql.toString(), params.toArray(), resultSet -> {
            String departmentCode = resultSet.getString("descendant_department_code");
            if (departmentCode != null) {
                departmentCodes.add(departmentCode);
            }
        });
        return departmentCodes;
    }

    private Integer maxDepth(String subDepartmentDepth) {
        if (subDepartmentDepth == null || subDepartmentDepth.trim().isEmpty()
                || "ALL".equalsIgnoreCase(subDepartmentDepth.trim())) {
            return null;
        }
        return Integer.valueOf(subDepartmentDepth.trim());
    }

    @Override
    public List<String> findRoleKeys(String tenantKey, String appKey, Long requesterPrincipalId, LocalDateTime decisionTime) {
        return findContainerKeys(tenantKey, appKey, requesterPrincipalId, decisionTime,
                "ROLE", "authzcraft_principal_role", "role_code");
    }

    @Override
    public List<String> findGroupKeys(String tenantKey, String appKey, Long requesterPrincipalId, LocalDateTime decisionTime) {
        return findContainerKeys(tenantKey, appKey, requesterPrincipalId, decisionTime,
                "GROUP", "authzcraft_principal_group", "group_code");
    }

    private List<String> findContainerKeys(String tenantKey, String appKey, Long requesterPrincipalId,
                                           LocalDateTime decisionTime, String containerKind,
                                           String subtypeTable, String codeColumn) {
        return jdbcTemplate.queryForList(
                "SELECT DISTINCT s." + codeColumn + " "
                        + "FROM authzcraft_principal_membership m "
                        + "JOIN authzcraft_principal cp ON cp.id = m.container_principal_id AND cp.tenant_key = m.tenant_key "
                        + "JOIN authzcraft_principal mp ON mp.id = m.member_principal_id AND mp.tenant_key = m.tenant_key "
                        + "JOIN " + subtypeTable + " s ON s.principal_id = cp.id AND s.tenant_key = cp.tenant_key "
                        + "WHERE m.tenant_key = ? AND m.member_principal_id = ? AND m.container_kind = ? AND m.member_kind = 'USER' "
                        + "AND m.valid_from <= ? AND (m.valid_until IS NULL OR m.valid_until > ?) "
                        + "AND cp.lifecycle_state = 'ACTIVE' AND mp.lifecycle_state = 'ACTIVE' "
                        + "AND (s.app_key IS NULL OR s.app_key = ?) "
                        + "ORDER BY s." + codeColumn,
                new Object[]{tenantKey, requesterPrincipalId, containerKind, Timestamp.valueOf(decisionTime),
                        Timestamp.valueOf(decisionTime), appKey}, String.class);
    }

    private PrincipalAttributeProjection mapUser(ResultSet resultSet) throws SQLException {
        PrincipalAttributeProjection projection = new PrincipalAttributeProjection();
        projection.setPrincipalId(resultSet.getLong("principal_id"));
        projection.setUserId(resultSet.getString("user_id"));
        projection.setDepartmentCode(resultSet.getString("department_code"));
        projection.setPostCode(resultSet.getString("post_code"));
        return projection;
    }
}