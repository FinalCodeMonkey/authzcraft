package com.fcm.authzcraft.pap.infrastructure.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.fcm.authzcraft.pap.domain.model.AccessGrant;
import com.fcm.authzcraft.pap.domain.model.AccessPolicy;
import com.fcm.authzcraft.pap.domain.model.GrantArgument;
import com.fcm.authzcraft.pap.domain.model.PolicyRevision;
import com.fcm.authzcraft.pap.domain.model.Principal;
import com.fcm.authzcraft.pap.domain.model.PrincipalOrganizationProjection;
import com.fcm.authzcraft.pap.domain.model.PrincipalPositionProjection;
import com.fcm.authzcraft.pap.domain.model.PrincipalUserProjection;
import com.fcm.authzcraft.pap.domain.model.RuleBlueprint;
import com.fcm.authzcraft.pap.domain.repository.PolicyGovernanceRepository;
import com.fcm.authzcraft.pap.domain.service.CatalogIdGenerator;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Repository
public class JdbcPolicyGovernanceRepository implements PolicyGovernanceRepository {
    private static final String ACTOR = "authzcraft-pap";

    private final JdbcTemplate jdbcTemplate;
    private final CatalogIdGenerator idGenerator;

    public JdbcPolicyGovernanceRepository(JdbcTemplate jdbcTemplate, CatalogIdGenerator idGenerator) {
        this.jdbcTemplate = jdbcTemplate;
        this.idGenerator = idGenerator;
    }

    @Override
    public void saveRuleBlueprint(RuleBlueprint blueprint) {
        jdbcTemplate.update(
                "INSERT INTO authzcraft_rule_blueprint "
                + "(id, tenant_key, blueprint_key, blueprint_kind, blueprint_version, display_name, description, predicate_template, "
                + "input_schema, lifecycle_state, content_digest, created_by, updated_by) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            blueprint.getId(), blueprint.getTenantKey(), blueprint.getBlueprintKey(), blueprint.getBlueprintKind(),
            blueprint.getBlueprintVersion(), blueprint.getDisplayName(), blueprint.getDescription(), blueprint.getPredicateTemplate(),
            blueprint.getInputSchema(), blueprint.getLifecycleState(), blueprint.getContentDigest(), ACTOR, ACTOR);
    }

    @Override
    public RuleBlueprint findRuleBlueprint(Long id) {
        List<RuleBlueprint> results = jdbcTemplate.query(
            "SELECT id, tenant_key, blueprint_key, blueprint_kind, blueprint_version, display_name, description, "
                        + "CAST(predicate_template AS CHAR) AS predicate_template, CAST(input_schema AS CHAR) AS input_schema, "
                        + "lifecycle_state, content_digest FROM authzcraft_rule_blueprint WHERE id = ?",
                new Object[]{id}, ruleBlueprintMapper());
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public List<RuleBlueprint> searchRuleBlueprints(String tenantKey, String blueprintKey, String lifecycleState) {
        StringBuilder sql = new StringBuilder("SELECT id, tenant_key, blueprint_key, blueprint_kind, blueprint_version, display_name, description, "
                + "CAST(predicate_template AS CHAR) AS predicate_template, CAST(input_schema AS CHAR) AS input_schema, "
                + "lifecycle_state, content_digest FROM authzcraft_rule_blueprint WHERE 1 = 1");
        List<Object> params = new ArrayList<Object>();
        appendEquals(sql, params, "tenant_key", tenantKey);
        appendEquals(sql, params, "blueprint_key", blueprintKey);
        appendEquals(sql, params, "lifecycle_state", lifecycleState);
        sql.append(" ORDER BY blueprint_key ASC, blueprint_version DESC LIMIT 200");
        return jdbcTemplate.query(sql.toString(), params.toArray(), ruleBlueprintMapper());
    }

    @Override
    public void retirePublishedRuleBlueprints(String tenantKey, String blueprintKey) {
        jdbcTemplate.update("UPDATE authzcraft_rule_blueprint SET lifecycle_state = 'RETIRED', updated_by = ? "
                        + "WHERE tenant_key = ? AND blueprint_key = ? AND lifecycle_state = 'PUBLISHED'",
                ACTOR, tenantKey, blueprintKey);
    }

    @Override
    public void publishRuleBlueprint(Long id, String actor) {
        jdbcTemplate.update("UPDATE authzcraft_rule_blueprint SET lifecycle_state = 'PUBLISHED', published_by = ?, "
                        + "published_at = CURRENT_TIMESTAMP(3), updated_by = ? WHERE id = ?",
                actor, ACTOR, id);
    }

    @Override
    public void updateRuleBlueprint(RuleBlueprint blueprint) {
        jdbcTemplate.update("UPDATE authzcraft_rule_blueprint SET display_name = ?, description = ?, "
                        + "predicate_template = ?, input_schema = ?, content_digest = ?, updated_by = ?, "
                        + "record_version = record_version + 1 WHERE id = ?",
                blueprint.getDisplayName(), blueprint.getDescription(), blueprint.getPredicateTemplate(),
                blueprint.getInputSchema(), blueprint.getContentDigest(), ACTOR, blueprint.getId());
    }

    @Override
    public void retireRuleBlueprint(Long id, String actor) {
        jdbcTemplate.update("UPDATE authzcraft_rule_blueprint SET lifecycle_state = 'RETIRED', updated_by = ? WHERE id = ?",
                ACTOR, id);
    }

    @Override
    public void saveAccessPolicy(AccessPolicy policy) {
        jdbcTemplate.update(
                "INSERT INTO authzcraft_access_policy "
                        + "(id, tenant_key, app_key, policy_key, display_name, description, target_relation_id, operation_code, "
                        + "effect_kind, lifecycle_state, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                policy.getId(), policy.getTenantKey(), policy.getAppKey(), policy.getPolicyKey(), policy.getDisplayName(),
                policy.getDescription(), policy.getTargetRelationId(), policy.getOperationCode(), policy.getEffectKind(),
                policy.getLifecycleState(), ACTOR, ACTOR);
    }

    @Override
    public AccessPolicy findAccessPolicy(Long id) {
        List<AccessPolicy> results = jdbcTemplate.query(accessPolicySelect() + " WHERE p.id = ?",
                new Object[]{id}, accessPolicyMapper());
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public List<AccessPolicy> searchAccessPolicies(String tenantKey, String appKey, String policyKey, Long targetRelationId,
                                                   String operationCode, String lifecycleState) {
        StringBuilder sql = new StringBuilder(accessPolicySelect() + " WHERE 1 = 1");
        List<Object> params = new ArrayList<Object>();
        appendEquals(sql, params, "p.tenant_key", tenantKey);
        appendEquals(sql, params, "p.app_key", appKey);
        appendEquals(sql, params, "p.policy_key", policyKey);
        appendEquals(sql, params, "p.operation_code", operationCode);
        appendEquals(sql, params, "p.lifecycle_state", lifecycleState);
        if (targetRelationId != null) {
            sql.append(" AND p.target_relation_id = ?");
            params.add(targetRelationId);
        }
        sql.append(" ORDER BY p.updated_at DESC LIMIT 200");
        return jdbcTemplate.query(sql.toString(), params.toArray(), accessPolicyMapper());
    }

    private String accessPolicySelect() {
        // 关联 ACTIVE 修订的规则蓝图，输出 ruleBlueprintDisplayName（数据规则模板取自 authzcraft_rule_blueprint.display_name）
        return "SELECT p.*, rb.display_name AS rule_blueprint_display_name "
                + "FROM authzcraft_access_policy p "
                + "LEFT JOIN authzcraft_policy_revision pr ON pr.policy_id = p.id AND pr.revision_state = 'ACTIVE' "
                + "LEFT JOIN authzcraft_rule_blueprint rb ON rb.id = pr.blueprint_id";
    }

    @Override
    public void updateAccessPolicy(AccessPolicy policy) {
        jdbcTemplate.update("UPDATE authzcraft_access_policy SET display_name = ?, description = ?, operation_code = ?, "
                        + "effect_kind = ?, lifecycle_state = ?, updated_by = ?, record_version = record_version + 1 WHERE id = ?",
                policy.getDisplayName(), policy.getDescription(), policy.getOperationCode(), policy.getEffectKind(),
                policy.getLifecycleState(), ACTOR, policy.getId());
    }

    @Override
    public void savePolicyRevision(PolicyRevision revision) {
        jdbcTemplate.update(
                "INSERT INTO authzcraft_policy_revision "
                        + "(id, policy_id, revision_no, blueprint_id, predicate_ast, argument_schema, attribute_references, "
                        + "content_digest, revision_state, change_summary, created_by, updated_by) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                revision.getId(), revision.getPolicyId(), revision.getRevisionNo(), revision.getBlueprintId(),
                revision.getPredicateAst(), revision.getArgumentSchema(), revision.getAttributeReferences(),
                revision.getContentDigest(), revision.getRevisionState(), revision.getChangeSummary(), ACTOR, ACTOR);
    }

    @Override
    public PolicyRevision findPolicyRevision(Long id) {
        List<PolicyRevision> results = jdbcTemplate.query(policyRevisionSelect() + " WHERE id = ?",
                new Object[]{id}, policyRevisionMapper());
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public List<PolicyRevision> searchPolicyRevisions(Long policyId, String revisionState) {
        StringBuilder sql = new StringBuilder(policyRevisionSelect() + " WHERE policy_id = ?");
        List<Object> params = new ArrayList<Object>();
        params.add(policyId);
        appendEquals(sql, params, "revision_state", revisionState);
        sql.append(" ORDER BY revision_no DESC");
        return jdbcTemplate.query(sql.toString(), params.toArray(), policyRevisionMapper());
    }

    @Override
    public int nextRevisionNo(Long policyId) {
        Integer maxRevisionNo = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(revision_no), 0) FROM authzcraft_policy_revision WHERE policy_id = ?",
                new Object[]{policyId}, Integer.class);
        return maxRevisionNo == null ? 1 : maxRevisionNo + 1;
    }

    @Override
    public void retireActivePolicyRevisions(Long policyId) {
        jdbcTemplate.update("UPDATE authzcraft_policy_revision SET revision_state = 'RETIRED', updated_by = ? "
                + "WHERE policy_id = ? AND revision_state = 'ACTIVE'", ACTOR, policyId);
    }

    @Override
    public void activatePolicyRevision(Long id, String actor) {
        jdbcTemplate.update("UPDATE authzcraft_policy_revision SET revision_state = 'ACTIVE', published_by = ?, "
                        + "published_at = CURRENT_TIMESTAMP(3), updated_by = ? WHERE id = ?",
                actor, ACTOR, id);
    }

    @Override
    public void retirePolicyRevision(Long id, String actor) {
        jdbcTemplate.update("UPDATE authzcraft_policy_revision SET revision_state = 'RETIRED', updated_by = ? WHERE id = ?",
                ACTOR, id);
    }

    @Override
    public void saveAccessGrant(AccessGrant grant) {
        jdbcTemplate.update(
                "INSERT INTO authzcraft_access_grant "
                        + "(id, tenant_key, app_key, grant_key, principal_id, policy_id, valid_from, valid_until, "
                        + "lifecycle_state, grant_source, reason, created_by, updated_by) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                grant.getId(), grant.getTenantKey(), grant.getAppKey(), grant.getGrantKey(), grant.getPrincipalId(),
                grant.getPolicyId(), Timestamp.valueOf(grant.getValidFrom()),
                grant.getValidUntil() == null ? null : Timestamp.valueOf(grant.getValidUntil()), grant.getLifecycleState(),
                grant.getGrantSource(), grant.getReason(), ACTOR, ACTOR);
    }

    @Override
    public AccessGrant findAccessGrant(Long id) {
        List<AccessGrant> results = jdbcTemplate.query("SELECT * FROM authzcraft_access_grant WHERE id = ?",
                new Object[]{id}, accessGrantMapper());
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public List<AccessGrant> searchAccessGrants(String tenantKey, String appKey, Long principalId, Long policyId,
                                                String lifecycleState) {
        StringBuilder sql = new StringBuilder("SELECT * FROM authzcraft_access_grant WHERE 1 = 1");
        List<Object> params = new ArrayList<Object>();
        appendEquals(sql, params, "tenant_key", tenantKey);
        appendEquals(sql, params, "app_key", appKey);
        appendEquals(sql, params, "lifecycle_state", lifecycleState);
        if (principalId != null) {
            sql.append(" AND principal_id = ?");
            params.add(principalId);
        }
        if (policyId != null) {
            sql.append(" AND policy_id = ?");
            params.add(policyId);
        }
        sql.append(" ORDER BY updated_at DESC LIMIT 200");
        return jdbcTemplate.query(sql.toString(), params.toArray(), accessGrantMapper());
    }

    @Override
    public void updateAccessGrant(AccessGrant grant) {
        jdbcTemplate.update("UPDATE authzcraft_access_grant SET valid_from = ?, valid_until = ?, lifecycle_state = ?, "
                        + "grant_source = ?, reason = ?, revoked_by = ?, revoked_at = ?, updated_by = ?, "
                        + "record_version = record_version + 1 WHERE id = ?",
                Timestamp.valueOf(grant.getValidFrom()),
                grant.getValidUntil() == null ? null : Timestamp.valueOf(grant.getValidUntil()),
                grant.getLifecycleState(), grant.getGrantSource(), grant.getReason(), grant.getRevokedBy(),
                grant.getRevokedAt() == null ? null : Timestamp.valueOf(grant.getRevokedAt()), ACTOR, grant.getId());
    }

    @Override
    public boolean principalExists(String tenantKey, Long principalId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM authzcraft_principal WHERE tenant_key = ? AND id = ?",
                new Object[]{tenantKey, principalId}, Integer.class);
        return count != null && count > 0;
    }

    @Override
    public List<Principal> searchPrincipals(String tenantKey, String principalKind, String principalKey, String keyword,
                                           String lifecycleState) {
        StringBuilder sql = new StringBuilder("SELECT id, tenant_key, principal_kind, principal_key, display_name, lifecycle_state "
                + "FROM authzcraft_principal WHERE tenant_key = ?");
        List<Object> params = new ArrayList<Object>();
        params.add(tenantKey);
        appendEquals(sql, params, "principal_kind", principalKind);
        appendEquals(sql, params, "principal_key", principalKey);
        appendEquals(sql, params, "lifecycle_state", lifecycleState);
        if (StringUtils.hasText(keyword)) {
            sql.append(" AND (principal_key LIKE ? OR display_name LIKE ?)");
            String likeKeyword = "%" + keyword.trim() + "%";
            params.add(likeKeyword);
            params.add(likeKeyword);
        }
        sql.append(" ORDER BY principal_kind ASC, display_name ASC, principal_key ASC LIMIT 200");
        return jdbcTemplate.query(sql.toString(), params.toArray(), principalMapper());
    }

    @Override
    public List<PrincipalUserProjection> searchPrincipalUsers(String tenantKey, String userId, String staffNo,
                                                              String departmentCode, String postCode, String keyword,
                                                              String lifecycleState, int limit, int offset) {
        StringBuilder sql = new StringBuilder("SELECT p.id, p.tenant_key, p.principal_key, p.display_name, p.lifecycle_state, "
                + "u.user_id, u.staff_no, u.staff_name, u.department_code, u.department_name, u.post_code, u.post_name, "
                + "u.manage_user_id, u.manage_staff_no, u.manage_staff_name, u.staff_status, u.staff_status_code, "
                + "u.is_dept_manage, u.is_dept_portion_manage "
                + "FROM authzcraft_principal p JOIN authzcraft_principal_user u ON u.principal_id = p.id "
                + "WHERE p.tenant_key = ? AND p.principal_kind = 'USER'");
        List<Object> params = new ArrayList<Object>();
        params.add(tenantKey);
        appendEquals(sql, params, "u.user_id", userId);
        appendEquals(sql, params, "u.staff_no", staffNo);
        appendEquals(sql, params, "u.department_code", departmentCode);
        appendEquals(sql, params, "u.post_code", postCode);
        appendEquals(sql, params, "p.lifecycle_state", lifecycleState);
        if (StringUtils.hasText(keyword)) {
            sql.append(" AND (u.user_id LIKE ? OR u.staff_no LIKE ? OR u.staff_name LIKE ? OR p.display_name LIKE ?)");
            String likeKeyword = "%" + keyword.trim() + "%";
            params.add(likeKeyword);
            params.add(likeKeyword);
            params.add(likeKeyword);
            params.add(likeKeyword);
        }
        sql.append(" ORDER BY u.user_id ASC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);
        return jdbcTemplate.query(sql.toString(), params.toArray(), principalUserProjectionMapper());
    }

    @Override
    public List<PrincipalOrganizationProjection> searchPrincipalOrganizations(String tenantKey, String departmentCode,
                                                                              String parentDepartmentCode, String keyword,
                                                                              String lifecycleState, int limit, int offset) {
        StringBuilder sql = new StringBuilder("SELECT p.id, p.tenant_key, p.principal_key, p.display_name, p.lifecycle_state, "
                + "o.department_code, o.department_name, o.department_level, o.department_type_code, o.department_category, "
                + "o.parent_department_code, o.parent_department_name, o.manage_user_id, o.manage_staff_no, o.manage_name, "
                + "o.portion_manage_user_id, o.portion_manage_staff_no, o.portion_manage_name, o.is_enable, o.isenable_code "
                + "FROM authzcraft_principal p JOIN authzcraft_principal_organization o ON o.principal_id = p.id "
                + "WHERE p.tenant_key = ? AND p.principal_kind = 'ORGANIZATION'");
        List<Object> params = new ArrayList<Object>();
        params.add(tenantKey);
        appendEquals(sql, params, "o.department_code", departmentCode);
        appendEquals(sql, params, "o.parent_department_code", parentDepartmentCode);
        appendEquals(sql, params, "p.lifecycle_state", lifecycleState);
        if (StringUtils.hasText(keyword)) {
            sql.append(" AND (o.department_code LIKE ? OR o.department_name LIKE ? OR p.display_name LIKE ?)");
            String likeKeyword = "%" + keyword.trim() + "%";
            params.add(likeKeyword);
            params.add(likeKeyword);
            params.add(likeKeyword);
        }
        sql.append(" ORDER BY o.department_code ASC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);
        return jdbcTemplate.query(sql.toString(), params.toArray(), principalOrganizationProjectionMapper());
    }

    @Override
    public List<PrincipalPositionProjection> searchPrincipalPositions(String tenantKey, String postCode, String departmentCode,
                                                                      String keyword, String lifecycleState, int limit, int offset) {
        StringBuilder sql = new StringBuilder("SELECT p.id, p.tenant_key, p.principal_key, p.display_name, p.lifecycle_state, "
                + "pos.post_code, pos.post_name, pos.post_en_name, pos.post_type_code, pos.post_type, "
                + "pos.department_code, pos.department_name, pos.position_grade_code, pos.position_grade_name, pos.is_enable, pos.isenable_code "
                + "FROM authzcraft_principal p JOIN authzcraft_principal_position pos ON pos.principal_id = p.id "
                + "WHERE p.tenant_key = ? AND p.principal_kind = 'POSITION'");
        List<Object> params = new ArrayList<Object>();
        params.add(tenantKey);
        appendEquals(sql, params, "pos.post_code", postCode);
        appendEquals(sql, params, "pos.department_code", departmentCode);
        appendEquals(sql, params, "p.lifecycle_state", lifecycleState);
        if (StringUtils.hasText(keyword)) {
            sql.append(" AND (pos.post_code LIKE ? OR pos.post_name LIKE ? OR p.display_name LIKE ?)");
            String likeKeyword = "%" + keyword.trim() + "%";
            params.add(likeKeyword);
            params.add(likeKeyword);
            params.add(likeKeyword);
        }
        sql.append(" ORDER BY pos.post_code ASC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);
        return jdbcTemplate.query(sql.toString(), params.toArray(), principalPositionProjectionMapper());
    }

    @Override
    public void replaceGrantArguments(Long accessGrantId, List<GrantArgument> arguments) {
        jdbcTemplate.update("DELETE FROM authzcraft_grant_argument WHERE access_grant_id = ?", accessGrantId);
        for (GrantArgument argument : arguments) {
            jdbcTemplate.update(
                    "INSERT INTO authzcraft_grant_argument "
                            + "(id, access_grant_id, argument_key, value_kind, argument_value, sensitive_flag, created_by, updated_by) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    idGenerator.nextId(), accessGrantId, argument.getArgumentKey(), argument.getValueKind(),
                    argument.getArgumentValue(), Boolean.TRUE.equals(argument.getSensitiveFlag()) ? 1 : 0, ACTOR, ACTOR);
        }
    }

    @Override
    public List<GrantArgument> findGrantArguments(Long accessGrantId) {
        return jdbcTemplate.query("SELECT id, access_grant_id, argument_key, value_kind, CAST(argument_value AS CHAR) AS argument_value, "
                        + "sensitive_flag FROM authzcraft_grant_argument WHERE access_grant_id = ? ORDER BY argument_key ASC",
                new Object[]{accessGrantId}, grantArgumentMapper());
    }

    private String policyRevisionSelect() {
        return "SELECT id, policy_id, revision_no, blueprint_id, CAST(predicate_ast AS CHAR) AS predicate_ast, "
                + "CAST(argument_schema AS CHAR) AS argument_schema, CAST(attribute_references AS CHAR) AS attribute_references, "
                + "content_digest, revision_state, change_summary FROM authzcraft_policy_revision";
    }

    private RowMapper<RuleBlueprint> ruleBlueprintMapper() {
        return (resultSet, rowNum) -> {
            RuleBlueprint blueprint = new RuleBlueprint();
            blueprint.setId(resultSet.getLong("id"));
            blueprint.setTenantKey(resultSet.getString("tenant_key"));
            blueprint.setBlueprintKey(resultSet.getString("blueprint_key"));
            blueprint.setBlueprintKind(resultSet.getString("blueprint_kind"));
            blueprint.setBlueprintVersion(resultSet.getInt("blueprint_version"));
            blueprint.setDisplayName(resultSet.getString("display_name"));
            blueprint.setDescription(resultSet.getString("description"));
            blueprint.setPredicateTemplate(resultSet.getString("predicate_template"));
            blueprint.setInputSchema(resultSet.getString("input_schema"));
            blueprint.setLifecycleState(resultSet.getString("lifecycle_state"));
            blueprint.setContentDigest(resultSet.getString("content_digest"));
            return blueprint;
        };
    }

    private RowMapper<AccessPolicy> accessPolicyMapper() {
        return (resultSet, rowNum) -> {
            AccessPolicy policy = new AccessPolicy();
            policy.setId(resultSet.getLong("id"));
            policy.setTenantKey(resultSet.getString("tenant_key"));
            policy.setAppKey(resultSet.getString("app_key"));
            policy.setPolicyKey(resultSet.getString("policy_key"));
            policy.setDisplayName(resultSet.getString("display_name"));
            policy.setDescription(resultSet.getString("description"));
            policy.setTargetRelationId(resultSet.getLong("target_relation_id"));
            policy.setOperationCode(resultSet.getString("operation_code"));
            policy.setEffectKind(resultSet.getString("effect_kind"));
            policy.setLifecycleState(resultSet.getString("lifecycle_state"));
            policy.setRuleBlueprintDisplayName(resultSet.getString("rule_blueprint_display_name"));
            return policy;
        };
    }

    private RowMapper<PolicyRevision> policyRevisionMapper() {
        return (resultSet, rowNum) -> {
            PolicyRevision revision = new PolicyRevision();
            revision.setId(resultSet.getLong("id"));
            revision.setPolicyId(resultSet.getLong("policy_id"));
            revision.setRevisionNo(resultSet.getInt("revision_no"));
            long blueprintId = resultSet.getLong("blueprint_id");
            revision.setBlueprintId(resultSet.wasNull() ? null : blueprintId);
            revision.setPredicateAst(resultSet.getString("predicate_ast"));
            revision.setArgumentSchema(resultSet.getString("argument_schema"));
            revision.setAttributeReferences(resultSet.getString("attribute_references"));
            revision.setContentDigest(resultSet.getString("content_digest"));
            revision.setRevisionState(resultSet.getString("revision_state"));
            revision.setChangeSummary(resultSet.getString("change_summary"));
            return revision;
        };
    }

    private RowMapper<AccessGrant> accessGrantMapper() {
        return (resultSet, rowNum) -> {
            AccessGrant grant = new AccessGrant();
            grant.setId(resultSet.getLong("id"));
            grant.setTenantKey(resultSet.getString("tenant_key"));
            grant.setAppKey(resultSet.getString("app_key"));
            grant.setGrantKey(resultSet.getString("grant_key"));
            grant.setPrincipalId(resultSet.getLong("principal_id"));
            grant.setPolicyId(resultSet.getLong("policy_id"));
            grant.setValidFrom(resultSet.getTimestamp("valid_from").toLocalDateTime());
            Timestamp validUntil = resultSet.getTimestamp("valid_until");
            grant.setValidUntil(validUntil == null ? null : validUntil.toLocalDateTime());
            grant.setLifecycleState(resultSet.getString("lifecycle_state"));
            grant.setGrantSource(resultSet.getString("grant_source"));
            grant.setReason(resultSet.getString("reason"));
            grant.setRevokedBy(resultSet.getString("revoked_by"));
            Timestamp revokedAt = resultSet.getTimestamp("revoked_at");
            grant.setRevokedAt(revokedAt == null ? null : revokedAt.toLocalDateTime());
            return grant;
        };
    }

    private RowMapper<GrantArgument> grantArgumentMapper() {
        return (resultSet, rowNum) -> {
            GrantArgument argument = new GrantArgument();
            argument.setId(resultSet.getLong("id"));
            argument.setAccessGrantId(resultSet.getLong("access_grant_id"));
            argument.setArgumentKey(resultSet.getString("argument_key"));
            argument.setValueKind(resultSet.getString("value_kind"));
            argument.setArgumentValue(resultSet.getString("argument_value"));
            argument.setSensitiveFlag(resultSet.getInt("sensitive_flag") == 1);
            return argument;
        };
    }

    private RowMapper<Principal> principalMapper() {
        return (resultSet, rowNum) -> {
            Principal principal = new Principal();
            principal.setId(resultSet.getLong("id"));
            principal.setTenantKey(resultSet.getString("tenant_key"));
            principal.setPrincipalKind(resultSet.getString("principal_kind"));
            principal.setPrincipalKey(resultSet.getString("principal_key"));
            principal.setDisplayName(resultSet.getString("display_name"));
            principal.setLifecycleState(resultSet.getString("lifecycle_state"));
            return principal;
        };
    }

    private RowMapper<PrincipalUserProjection> principalUserProjectionMapper() {
        return (resultSet, rowNum) -> {
            PrincipalUserProjection projection = new PrincipalUserProjection();
            projection.setId(resultSet.getLong("id"));
            projection.setTenantKey(resultSet.getString("tenant_key"));
            projection.setPrincipalKey(resultSet.getString("principal_key"));
            projection.setDisplayName(resultSet.getString("display_name"));
            projection.setLifecycleState(resultSet.getString("lifecycle_state"));
            projection.setUserId(resultSet.getString("user_id"));
            projection.setStaffNo(resultSet.getString("staff_no"));
            projection.setStaffName(resultSet.getString("staff_name"));
            projection.setDepartmentCode(resultSet.getString("department_code"));
            projection.setDepartmentName(resultSet.getString("department_name"));
            projection.setPostCode(resultSet.getString("post_code"));
            projection.setPostName(resultSet.getString("post_name"));
            projection.setManageUserId(resultSet.getString("manage_user_id"));
            projection.setManageStaffNo(resultSet.getString("manage_staff_no"));
            projection.setManageStaffName(resultSet.getString("manage_staff_name"));
            projection.setStaffStatus(resultSet.getString("staff_status"));
            projection.setStaffStatusCode(resultSet.getString("staff_status_code"));
            projection.setDeptManage(toBoolean(resultSet.getObject("is_dept_manage")));
            projection.setDeptPortionManage(toBoolean(resultSet.getObject("is_dept_portion_manage")));
            return projection;
        };
    }

    private RowMapper<PrincipalOrganizationProjection> principalOrganizationProjectionMapper() {
        return (resultSet, rowNum) -> {
            PrincipalOrganizationProjection projection = new PrincipalOrganizationProjection();
            projection.setId(resultSet.getLong("id"));
            projection.setTenantKey(resultSet.getString("tenant_key"));
            projection.setPrincipalKey(resultSet.getString("principal_key"));
            projection.setDisplayName(resultSet.getString("display_name"));
            projection.setLifecycleState(resultSet.getString("lifecycle_state"));
            projection.setDepartmentCode(resultSet.getString("department_code"));
            projection.setDepartmentName(resultSet.getString("department_name"));
            projection.setDepartmentLevel(readNullableLong(resultSet.getObject("department_level")));
            projection.setDepartmentTypeCode(resultSet.getString("department_type_code"));
            projection.setDepartmentCategory(resultSet.getString("department_category"));
            projection.setParentDepartmentCode(resultSet.getString("parent_department_code"));
            projection.setParentDepartmentName(resultSet.getString("parent_department_name"));
            projection.setManageUserId(resultSet.getString("manage_user_id"));
            projection.setManageStaffNo(resultSet.getString("manage_staff_no"));
            projection.setManageName(resultSet.getString("manage_name"));
            projection.setPortionManageUserId(resultSet.getString("portion_manage_user_id"));
            projection.setPortionManageStaffNo(resultSet.getString("portion_manage_staff_no"));
            projection.setPortionManageName(resultSet.getString("portion_manage_name"));
            projection.setEnableFlag(readNullableLong(resultSet.getObject("is_enable")));
            projection.setEnableCode(resultSet.getString("isenable_code"));
            return projection;
        };
    }

    private RowMapper<PrincipalPositionProjection> principalPositionProjectionMapper() {
        return (resultSet, rowNum) -> {
            PrincipalPositionProjection projection = new PrincipalPositionProjection();
            projection.setId(resultSet.getLong("id"));
            projection.setTenantKey(resultSet.getString("tenant_key"));
            projection.setPrincipalKey(resultSet.getString("principal_key"));
            projection.setDisplayName(resultSet.getString("display_name"));
            projection.setLifecycleState(resultSet.getString("lifecycle_state"));
            projection.setPostCode(resultSet.getString("post_code"));
            projection.setPostName(resultSet.getString("post_name"));
            projection.setPostEnName(resultSet.getString("post_en_name"));
            projection.setPostTypeCode(resultSet.getString("post_type_code"));
            projection.setPostType(resultSet.getString("post_type"));
            projection.setDepartmentCode(resultSet.getString("department_code"));
            projection.setDepartmentName(resultSet.getString("department_name"));
            projection.setPositionGradeCode(resultSet.getString("position_grade_code"));
            projection.setPositionGradeName(resultSet.getString("position_grade_name"));
            projection.setEnableFlag(readNullableLong(resultSet.getObject("is_enable")));
            projection.setEnableCode(resultSet.getString("isenable_code"));
            return projection;
        };
    }

    private Boolean toBoolean(Object value) {
        if (value == null) {
            return null;
        }
        return Integer.valueOf(1).equals(Integer.valueOf(String.valueOf(value)));
    }

    private Long readNullableLong(Object value) {
        return value == null ? null : Long.valueOf(String.valueOf(value));
    }

    private void appendEquals(StringBuilder sql, List<Object> params, String columnName, String value) {
        if (StringUtils.hasText(value)) {
            sql.append(" AND ").append(columnName).append(" = ?");
            params.add(value);
        }
    }
}