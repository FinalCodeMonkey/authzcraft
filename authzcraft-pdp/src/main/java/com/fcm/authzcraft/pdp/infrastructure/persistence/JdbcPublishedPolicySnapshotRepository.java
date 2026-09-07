package com.fcm.authzcraft.pdp.infrastructure.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.fcm.authzcraft.pdp.domain.model.DecisionRecord;
import com.fcm.authzcraft.pdp.domain.model.EffectiveGrantSnapshot;
import com.fcm.authzcraft.pdp.domain.model.GrantArgumentSnapshot;
import com.fcm.authzcraft.pdp.domain.model.PublishedPolicySnapshot;
import com.fcm.authzcraft.pdp.domain.model.RelationResourceSnapshot;
import com.fcm.authzcraft.pdp.domain.repository.PublishedPolicySnapshotRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Repository
public class JdbcPublishedPolicySnapshotRepository implements PublishedPolicySnapshotRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcPublishedPolicySnapshotRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public RelationResourceSnapshot findActiveResource(String tenantKey, String appKey, String resourceKey) {
        List<RelationResourceSnapshot> results = jdbcTemplate.query(
                "SELECT id, resource_key, protection_mode, structure_digest "
                        + "FROM authzcraft_relation_resource "
                        + "WHERE tenant_key = ? AND app_key = ? AND resource_key = ? AND lifecycle_state = 'ACTIVE'",
                new Object[]{tenantKey, appKey, resourceKey},
                (resultSet, rowNum) -> mapResource(resultSet));
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public List<PublishedPolicySnapshot> findPublishedPolicies(String tenantKey, String appKey, String resourceKey, String operationCode) {
        return jdbcTemplate.query(
                "SELECT p.id AS policy_id, p.effect_kind, pr.id AS revision_id, pr.predicate_ast, "
                        + "pr.argument_schema, pr.attribute_references, pr.content_digest "
                        + "FROM authzcraft_relation_resource r "
                        + "JOIN authzcraft_access_policy p ON p.target_relation_id = r.id "
                        + " AND p.tenant_key = r.tenant_key AND p.app_key = r.app_key "
                        + "JOIN authzcraft_policy_revision pr ON pr.policy_id = p.id AND pr.revision_state = 'ACTIVE' "
                        + "WHERE r.tenant_key = ? AND r.app_key = ? AND r.resource_key = ? "
                        + "AND r.lifecycle_state = 'ACTIVE' AND p.lifecycle_state = 'ACTIVE' AND p.operation_code = ?",
                new Object[]{tenantKey, appKey, resourceKey, operationCode},
                (resultSet, rowNum) -> mapPolicy(resultSet));
    }

    @Override
    public Long findActivePrincipalId(String tenantKey, String principalKind, String principalKey) {
        List<Long> results = jdbcTemplate.queryForList(
                "SELECT id FROM authzcraft_principal "
                        + "WHERE tenant_key = ? AND principal_kind = ? AND principal_key = ? AND lifecycle_state = 'ACTIVE'",
                new Object[]{tenantKey, principalKind, principalKey}, Long.class);
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public List<Long> findActivePrincipalIds(String tenantKey, String principalKind, List<String> principalKeys) {
        if (principalKeys == null || principalKeys.isEmpty()) {
            return Collections.emptyList();
        }
        List<Object> params = new ArrayList<Object>();
        params.add(tenantKey);
        params.add(principalKind);
        String sql = "SELECT id FROM authzcraft_principal "
                + "WHERE tenant_key = ? AND principal_kind = ? AND lifecycle_state = 'ACTIVE' "
                + "AND principal_key IN (" + stringPlaceholders(principalKeys, params) + ")";
        return jdbcTemplate.queryForList(sql, params.toArray(), Long.class);
    }

    @Override
    public List<EffectiveGrantSnapshot> findEffectiveGrants(String tenantKey, String appKey, List<Long> principalIds,
                                                            List<Long> policyIds, LocalDateTime decisionTime) {
        if (principalIds == null || principalIds.isEmpty() || policyIds == null || policyIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Object> params = new ArrayList<Object>();
        params.add(tenantKey);
        params.add(appKey);
        params.add(Timestamp.valueOf(decisionTime));
        params.add(Timestamp.valueOf(decisionTime));
        String sql = "SELECT id, principal_id, policy_id FROM authzcraft_access_grant "
                + "WHERE tenant_key = ? AND app_key = ? AND lifecycle_state = 'ACTIVE' "
                + "AND valid_from <= ? AND (valid_until IS NULL OR valid_until > ?) "
                + "AND principal_id IN (" + placeholders(principalIds, params) + ") "
                + "AND policy_id IN (" + placeholders(policyIds, params) + ")";
        return jdbcTemplate.query(sql, params.toArray(), (resultSet, rowNum) -> mapGrant(resultSet));
    }

    @Override
    public List<GrantArgumentSnapshot> findGrantArguments(List<Long> grantIds) {
        if (grantIds == null || grantIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Object> params = new ArrayList<Object>();
        String sql = "SELECT access_grant_id, argument_key, value_kind, argument_value, sensitive_flag "
                + "FROM authzcraft_grant_argument WHERE access_grant_id IN (" + placeholders(grantIds, params) + ")";
        return jdbcTemplate.query(sql, params.toArray(), (resultSet, rowNum) -> mapArgument(resultSet));
    }

    @Override
    public void saveDecisionRecord(DecisionRecord record) {
        jdbcTemplate.update(
                "INSERT INTO authzcraft_decision_record "
                        + "(id, decision_key, request_key, tenant_key, app_key, requester_kind, requester_key, operation_code, "
                        + "target_resource_key, plan_decision, planner_kind, plan_digest, attribute_digest, failure_code, planning_cost_ms) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                record.getId(), record.getDecisionKey(), record.getRequestKey(), record.getTenantKey(), record.getAppKey(),
                record.getRequesterKind(), record.getRequesterKey(), record.getOperationCode(), record.getTargetResourceKey(),
                record.getPlanDecision(), record.getPlannerKind(), record.getPlanDigest(), record.getAttributeDigest(),
                record.getFailureCode(), record.getPlanningCostMs());
    }

    private String placeholders(List<Long> values, List<Object> params) {
        StringBuilder builder = new StringBuilder();
        for (Long value : values) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append("?");
            params.add(value);
        }
        return builder.toString();
    }

    private String stringPlaceholders(List<String> values, List<Object> params) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append("?");
            params.add(value);
        }
        return builder.toString();
    }

    private RelationResourceSnapshot mapResource(ResultSet resultSet) throws SQLException {
        RelationResourceSnapshot resource = new RelationResourceSnapshot();
        resource.setRelationId(resultSet.getLong("id"));
        resource.setResourceKey(resultSet.getString("resource_key"));
        resource.setProtectionMode(resultSet.getString("protection_mode"));
        resource.setStructureDigest(resultSet.getString("structure_digest"));
        return resource;
    }

    private PublishedPolicySnapshot mapPolicy(ResultSet resultSet) throws SQLException {
        PublishedPolicySnapshot policy = new PublishedPolicySnapshot();
        policy.setPolicyId(resultSet.getLong("policy_id"));
        policy.setEffectKind(resultSet.getString("effect_kind"));
        policy.setRevisionId(resultSet.getLong("revision_id"));
        policy.setPredicateAst(resultSet.getString("predicate_ast"));
        policy.setArgumentSchema(resultSet.getString("argument_schema"));
        policy.setAttributeReferences(resultSet.getString("attribute_references"));
        policy.setContentDigest(resultSet.getString("content_digest"));
        return policy;
    }

    private EffectiveGrantSnapshot mapGrant(ResultSet resultSet) throws SQLException {
        EffectiveGrantSnapshot grant = new EffectiveGrantSnapshot();
        grant.setGrantId(resultSet.getLong("id"));
        grant.setPrincipalId(resultSet.getLong("principal_id"));
        grant.setPolicyId(resultSet.getLong("policy_id"));
        return grant;
    }

    private GrantArgumentSnapshot mapArgument(ResultSet resultSet) throws SQLException {
        GrantArgumentSnapshot argument = new GrantArgumentSnapshot();
        argument.setAccessGrantId(resultSet.getLong("access_grant_id"));
        argument.setArgumentKey(resultSet.getString("argument_key"));
        argument.setValueKind(resultSet.getString("value_kind"));
        argument.setArgumentValue(resultSet.getString("argument_value"));
        argument.setSensitive(resultSet.getBoolean("sensitive_flag"));
        return argument;
    }
}