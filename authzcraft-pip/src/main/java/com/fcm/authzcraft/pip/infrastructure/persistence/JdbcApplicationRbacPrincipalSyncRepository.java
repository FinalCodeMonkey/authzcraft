package com.fcm.authzcraft.pip.infrastructure.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.fcm.authzcraft.pip.application.command.ApplicationRbacPrincipalSyncCommand;
import com.fcm.authzcraft.pip.application.service.ApplicationRbacPrincipalSyncResult;
import com.fcm.authzcraft.pip.domain.model.PrincipalKind;
import com.fcm.authzcraft.pip.domain.repository.ApplicationRbacPrincipalSyncRepository;
import com.fcm.authzcraft.pip.domain.service.PrincipalIdGenerator;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Repository
public class JdbcApplicationRbacPrincipalSyncRepository implements ApplicationRbacPrincipalSyncRepository {
    private static final String ACTOR = "application-rbac-sync";

    private final JdbcTemplate jdbcTemplate;
    private final PrincipalIdGenerator idGenerator;

    public JdbcApplicationRbacPrincipalSyncRepository(JdbcTemplate jdbcTemplate, PrincipalIdGenerator idGenerator) {
        this.jdbcTemplate = jdbcTemplate;
        this.idGenerator = idGenerator;
    }

    @Override
    public ApplicationRbacPrincipalSyncResult sync(ApplicationRbacPrincipalSyncCommand command) {
        ApplicationRbacPrincipalSyncResult result = new ApplicationRbacPrincipalSyncResult();
        result.setTenantKey(command.getTenantKey());
        result.setAppKey(command.getAppKey());

        Map<String, Long> activeRoleIds = new LinkedHashMap<String, Long>();
        for (ApplicationRbacPrincipalSyncCommand.RoleItem role : command.getRoles()) {
            if (!StringUtils.hasText(role.getRoleCode())) {
                addFailure(result, "ROLE", "", "roleCode is required");
                continue;
            }
            String roleCode = role.getRoleCode().trim();
            String lifecycleState = lifecycleState(role.getLifecycleState());
            Long principalId = upsertRole(command.getTenantKey(), command.getAppKey(), roleCode,
                    firstText(role.getRoleName(), roleCode), role.getDescription(), lifecycleState);
            ApplicationRbacPrincipalSyncResult.RoleResult roleResult = new ApplicationRbacPrincipalSyncResult.RoleResult();
            roleResult.setRoleCode(roleCode);
            roleResult.setRoleName(firstText(role.getRoleName(), roleCode));
            roleResult.setDescription(role.getDescription());
            roleResult.setPrincipalId(String.valueOf(principalId));
            roleResult.setLifecycleState(lifecycleState);
            result.getRoles().add(roleResult);
            if ("ACTIVE".equals(lifecycleState)) {
                activeRoleIds.put(roleCode, principalId);
            }
        }

        Map<String, Long> activeGroupIds = new LinkedHashMap<String, Long>();
        for (ApplicationRbacPrincipalSyncCommand.GroupItem group : command.getGroups()) {
            if (!StringUtils.hasText(group.getGroupCode())) {
                addFailure(result, "GROUP", "", "groupCode is required");
                continue;
            }
            String groupCode = group.getGroupCode().trim();
            String lifecycleState = lifecycleState(group.getLifecycleState());
            Long principalId = upsertGroup(command.getTenantKey(), command.getAppKey(), groupCode,
                    firstText(group.getGroupName(), groupCode), group.getDescription(), lifecycleState);
            ApplicationRbacPrincipalSyncResult.GroupResult groupResult = new ApplicationRbacPrincipalSyncResult.GroupResult();
            groupResult.setGroupCode(groupCode);
            groupResult.setGroupName(firstText(group.getGroupName(), groupCode));
            groupResult.setDescription(group.getDescription());
            groupResult.setPrincipalId(String.valueOf(principalId));
            groupResult.setLifecycleState(lifecycleState);
            result.getGroups().add(groupResult);
            if ("ACTIVE".equals(lifecycleState)) {
                activeGroupIds.put(groupCode, principalId);
            }
        }

        if (command.isDeactivateMissing()) {
            retireMissingRoles(command.getTenantKey(), command.getAppKey(), activeRoleIds.keySet());
            retireMissingGroups(command.getTenantKey(), command.getAppKey(), activeGroupIds.keySet());
        }

        Set<String> requestedMembershipKeys = new LinkedHashSet<String>();
        for (ApplicationRbacPrincipalSyncCommand.MembershipItem membership : command.getMemberships()) {
            ContainerRef container = resolveContainer(membership);
            String userKey = membership.getUserKey() == null ? null : membership.getUserKey().trim();
            if (container == null || !StringUtils.hasText(userKey)) {
                addFailure(result, "MEMBERSHIP", membershipKey(membership),
                        "container (roleCode/groupCode or containerKind+containerCode) and userKey are required");
                continue;
            }
            Long containerPrincipalId = "ROLE".equals(container.kind)
                    ? firstNonNull(activeRoleIds.get(container.code), findPrincipalId(command.getTenantKey(), PrincipalKind.ROLE.name(), container.code))
                    : firstNonNull(activeGroupIds.get(container.code), findPrincipalId(command.getTenantKey(), PrincipalKind.GROUP.name(), container.code));
            if (containerPrincipalId == null) {
                addFailure(result, "MEMBERSHIP", membershipKey(membership), container.kind + " container was not found in AuthzCraft principal domain");
                continue;
            }
            Long userPrincipalId = findPrincipalId(command.getTenantKey(), PrincipalKind.USER.name(), userKey);
            if (userPrincipalId == null) {
                addFailure(result, "MEMBERSHIP", membershipKey(membership), "user principal was not found in AuthzCraft principal domain");
                continue;
            }
            result.getMemberships().add(upsertMembership(command.getTenantKey(), container.kind, container.code,
                    containerPrincipalId, userKey, userPrincipalId, membership.getValidFrom(), membership.getValidUntil()));
            requestedMembershipKeys.add(containerPrincipalId + ":" + userPrincipalId);
        }
        if (result.getFailures().isEmpty()) {
            result.getMemberships().addAll(closeMissingMemberships(command.getTenantKey(), command.getAppKey(), requestedMembershipKeys));
        }

        result.setRoleCount(result.getRoles().size());
        result.setGroupCount(result.getGroups().size());
        result.setMembershipCount(result.getMemberships().size());
        result.setFailedCount(result.getFailures().size());
        return result;
    }

    @Override
    public ApplicationRbacPrincipalSyncResult status(ApplicationRbacPrincipalSyncCommand command) {
        ApplicationRbacPrincipalSyncResult result = new ApplicationRbacPrincipalSyncResult();
        result.setTenantKey(command.getTenantKey());
        result.setAppKey(command.getAppKey());

        for (ApplicationRbacPrincipalSyncCommand.RoleItem role : command.getRoles()) {
            String roleCode = role.getRoleCode() == null ? null : role.getRoleCode().trim();
            if (!StringUtils.hasText(roleCode)) {
                addFailure(result, "ROLE", "", "roleCode is required");
                continue;
            }
            ApplicationRbacPrincipalSyncResult.RoleResult roleResult = findRoleResult(command.getTenantKey(), command.getAppKey(), roleCode);
            if (roleResult == null) {
                addFailure(result, "ROLE", roleCode, "role principal was not found in AuthzCraft principal domain");
                continue;
            }
            result.getRoles().add(roleResult);
        }

        for (ApplicationRbacPrincipalSyncCommand.GroupItem group : command.getGroups()) {
            String groupCode = group.getGroupCode() == null ? null : group.getGroupCode().trim();
            if (!StringUtils.hasText(groupCode)) {
                addFailure(result, "GROUP", "", "groupCode is required");
                continue;
            }
            ApplicationRbacPrincipalSyncResult.GroupResult groupResult = findGroupResult(command.getTenantKey(), command.getAppKey(), groupCode);
            if (groupResult == null) {
                addFailure(result, "GROUP", groupCode, "group principal was not found in AuthzCraft principal domain");
                continue;
            }
            result.getGroups().add(groupResult);
        }

        for (ApplicationRbacPrincipalSyncCommand.MembershipItem membership : command.getMemberships()) {
            ContainerRef container = resolveContainer(membership);
            String userKey = membership.getUserKey() == null ? null : membership.getUserKey().trim();
            if (container == null || !StringUtils.hasText(userKey)) {
                addFailure(result, "MEMBERSHIP", membershipKey(membership),
                        "container (roleCode/groupCode or containerKind+containerCode) and userKey are required");
                continue;
            }
            Long containerPrincipalId = findPrincipalId(command.getTenantKey(), container.kind, container.code);
            Long userPrincipalId = findPrincipalId(command.getTenantKey(), PrincipalKind.USER.name(), userKey);
            if (containerPrincipalId == null || userPrincipalId == null) {
                addFailure(result, "MEMBERSHIP", membershipKey(membership), "container or user principal was not found in AuthzCraft principal domain");
                continue;
            }
            ApplicationRbacPrincipalSyncResult.MembershipResult membershipResult = findMembershipResultOrNull(
                    command.getTenantKey(), container.kind, container.code, containerPrincipalId, userKey, userPrincipalId);
            if (membershipResult == null) {
                addFailure(result, "MEMBERSHIP", membershipKey(membership), "membership projection was not found in AuthzCraft principal domain");
                continue;
            }
            result.getMemberships().add(membershipResult);
        }

        result.setRoleCount(result.getRoles().size());
        result.setGroupCount(result.getGroups().size());
        result.setMembershipCount(result.getMemberships().size());
        result.setFailedCount(result.getFailures().size());
        return result;
    }

    public Long upsertRole(String tenantKey, String appKey, String roleCode, String roleName, String description, String lifecycleState) {
        Long principalId = findPrincipalId(tenantKey, PrincipalKind.ROLE.name(), roleCode);
        if (principalId == null) {
            principalId = idGenerator.nextId();
        }
        jdbcTemplate.update(
                "INSERT INTO authzcraft_principal "
                        + "(id, tenant_key, principal_kind, principal_key, display_name, lifecycle_state, created_by, updated_by) "
                        + "VALUES (?, ?, 'ROLE', ?, ?, ?, ?, ?) "
                        + "ON DUPLICATE KEY UPDATE display_name = VALUES(display_name), lifecycle_state = VALUES(lifecycle_state), "
                        + "updated_by = VALUES(updated_by), record_version = record_version + 1",
                principalId, tenantKey, roleCode, roleName, lifecycleState, ACTOR, ACTOR);
        jdbcTemplate.update(
                "INSERT INTO authzcraft_principal_role "
                        + "(principal_id, principal_kind, tenant_key, role_code, role_name, app_key, description, created_by, updated_by) "
                        + "VALUES (?, 'ROLE', ?, ?, ?, ?, ?, ?, ?) "
                        + "ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), app_key = VALUES(app_key), "
                        + "description = VALUES(description), updated_by = VALUES(updated_by)",
                principalId, tenantKey, roleCode, roleName, appKey, description, ACTOR, ACTOR);
        return principalId;
    }

    public ApplicationRbacPrincipalSyncResult.RoleResult findRoleResult(String tenantKey, String appKey, String roleCode) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT p.id, p.lifecycle_state, r.role_name, r.description "
                        + "FROM authzcraft_principal p "
                        + "JOIN authzcraft_principal_role r ON r.principal_id = p.id AND r.tenant_key = p.tenant_key "
                        + "WHERE p.tenant_key = ? AND p.principal_kind = 'ROLE' AND p.principal_key = ? AND r.app_key = ?",
                tenantKey, roleCode, appKey);
        if (rows.isEmpty()) {
            return null;
        }
        Map<String, Object> row = rows.get(0);
        ApplicationRbacPrincipalSyncResult.RoleResult result = new ApplicationRbacPrincipalSyncResult.RoleResult();
        result.setRoleCode(roleCode);
        result.setRoleName(text(row.get("role_name")));
        result.setDescription(text(row.get("description")));
        result.setPrincipalId(String.valueOf(row.get("id")));
        result.setLifecycleState(String.valueOf(row.get("lifecycle_state")));
        return result;
    }

    @SuppressWarnings("null")
    public List<Map<String, Object>> searchRoleMaps(String tenantKey, String appKey, String roleCode, String lifecycleState) {
        StringBuilder sql = new StringBuilder("SELECT CAST(p.id AS CHAR) AS principalId, p.principal_key AS roleCode, r.role_name AS roleName, r.description, p.lifecycle_state AS lifecycleState FROM authzcraft_principal p JOIN authzcraft_principal_role r ON r.principal_id = p.id AND r.tenant_key = p.tenant_key WHERE p.tenant_key = ? AND p.principal_kind = 'ROLE' AND r.app_key = ?");
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add(tenantKey);
        args.add(appKey);
        if (StringUtils.hasText(roleCode)) {
            sql.append(" AND p.principal_key = ?");
            args.add(roleCode.trim());
        }
        if (StringUtils.hasText(lifecycleState)) {
            sql.append(" AND p.lifecycle_state = ?");
            args.add(lifecycleState.trim().toUpperCase(Locale.ROOT));
        }
        sql.append(" ORDER BY p.principal_key");
        return jdbcTemplate.queryForList(sql.toString(), args.toArray(new Object[0]));
    }

    public void retireMissingRoles(String tenantKey, String appKey, Set<String> activeRoleCodes) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT p.principal_key FROM authzcraft_principal p "
                        + "JOIN authzcraft_principal_role r ON r.principal_id = p.id AND r.tenant_key = p.tenant_key "
                        + "WHERE p.tenant_key = ? AND p.principal_kind = 'ROLE' AND r.app_key = ? AND p.lifecycle_state = 'ACTIVE'",
                tenantKey, appKey);
        for (Map<String, Object> row : rows) {
            String roleCode = String.valueOf(row.get("principal_key"));
            if (!activeRoleCodes.contains(roleCode)) {
                jdbcTemplate.update("UPDATE authzcraft_principal SET lifecycle_state = 'INACTIVE', updated_by = ?, record_version = record_version + 1 "
                        + "WHERE tenant_key = ? AND principal_kind = 'ROLE' AND principal_key = ?", ACTOR, tenantKey, roleCode);
            }
        }
    }

    public void retireRoleByCode(String tenantKey, String roleCode) {
        jdbcTemplate.update("UPDATE authzcraft_principal SET lifecycle_state = 'INACTIVE', updated_by = ?, record_version = record_version + 1 "
                + "WHERE tenant_key = ? AND principal_kind = 'ROLE' AND principal_key = ?", ACTOR, tenantKey, roleCode);
    }

    public Long upsertGroup(String tenantKey, String appKey, String groupCode, String groupName, String description, String lifecycleState) {
        Long principalId = findPrincipalId(tenantKey, PrincipalKind.GROUP.name(), groupCode);
        if (principalId == null) {
            principalId = idGenerator.nextId();
        }
        jdbcTemplate.update(
                "INSERT INTO authzcraft_principal "
                        + "(id, tenant_key, principal_kind, principal_key, display_name, lifecycle_state, created_by, updated_by) "
                        + "VALUES (?, ?, 'GROUP', ?, ?, ?, ?, ?) "
                        + "ON DUPLICATE KEY UPDATE display_name = VALUES(display_name), lifecycle_state = VALUES(lifecycle_state), "
                        + "updated_by = VALUES(updated_by), record_version = record_version + 1",
                principalId, tenantKey, groupCode, groupName, lifecycleState, ACTOR, ACTOR);
        jdbcTemplate.update(
                "INSERT INTO authzcraft_principal_group "
                        + "(principal_id, principal_kind, tenant_key, group_code, group_name, app_key, description, created_by, updated_by) "
                        + "VALUES (?, 'GROUP', ?, ?, ?, ?, ?, ?, ?) "
                        + "ON DUPLICATE KEY UPDATE group_name = VALUES(group_name), app_key = VALUES(app_key), "
                        + "description = VALUES(description), updated_by = VALUES(updated_by)",
                principalId, tenantKey, groupCode, groupName, appKey, description, ACTOR, ACTOR);
        return principalId;
    }

    public ApplicationRbacPrincipalSyncResult.GroupResult findGroupResult(String tenantKey, String appKey, String groupCode) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT p.id, p.lifecycle_state, g.group_name, g.description "
                        + "FROM authzcraft_principal p "
                        + "JOIN authzcraft_principal_group g ON g.principal_id = p.id AND g.tenant_key = p.tenant_key "
                        + "WHERE p.tenant_key = ? AND p.principal_kind = 'GROUP' AND p.principal_key = ? AND g.app_key = ?",
                tenantKey, groupCode, appKey);
        if (rows.isEmpty()) {
            return null;
        }
        Map<String, Object> row = rows.get(0);
        ApplicationRbacPrincipalSyncResult.GroupResult result = new ApplicationRbacPrincipalSyncResult.GroupResult();
        result.setGroupCode(groupCode);
        result.setGroupName(text(row.get("group_name")));
        result.setDescription(text(row.get("description")));
        result.setPrincipalId(String.valueOf(row.get("id")));
        result.setLifecycleState(String.valueOf(row.get("lifecycle_state")));
        return result;
    }

    @SuppressWarnings("null")
    public List<Map<String, Object>> searchGroupMaps(String tenantKey, String appKey, String groupCode, String lifecycleState) {
        StringBuilder sql = new StringBuilder("SELECT CAST(p.id AS CHAR) AS principalId, p.principal_key AS groupCode, g.group_name AS groupName, g.description, p.lifecycle_state AS lifecycleState FROM authzcraft_principal p JOIN authzcraft_principal_group g ON g.principal_id = p.id AND g.tenant_key = p.tenant_key WHERE p.tenant_key = ? AND p.principal_kind = 'GROUP' AND g.app_key = ?");
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add(tenantKey);
        args.add(appKey);
        if (StringUtils.hasText(groupCode)) {
            sql.append(" AND p.principal_key = ?");
            args.add(groupCode.trim());
        }
        if (StringUtils.hasText(lifecycleState)) {
            sql.append(" AND p.lifecycle_state = ?");
            args.add(lifecycleState.trim().toUpperCase(Locale.ROOT));
        }
        sql.append(" ORDER BY p.principal_key");
        return jdbcTemplate.queryForList(sql.toString(), args.toArray(new Object[0]));
    }

    public void retireMissingGroups(String tenantKey, String appKey, Set<String> activeGroupCodes) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT p.principal_key FROM authzcraft_principal p "
                        + "JOIN authzcraft_principal_group g ON g.principal_id = p.id AND g.tenant_key = p.tenant_key "
                        + "WHERE p.tenant_key = ? AND p.principal_kind = 'GROUP' AND g.app_key = ? AND p.lifecycle_state = 'ACTIVE'",
                tenantKey, appKey);
        for (Map<String, Object> row : rows) {
            String groupCode = String.valueOf(row.get("principal_key"));
            if (!activeGroupCodes.contains(groupCode)) {
                jdbcTemplate.update("UPDATE authzcraft_principal SET lifecycle_state = 'INACTIVE', updated_by = ?, record_version = record_version + 1 "
                        + "WHERE tenant_key = ? AND principal_kind = 'GROUP' AND principal_key = ?", ACTOR, tenantKey, groupCode);
            }
        }
    }

    public void retireGroupByCode(String tenantKey, String groupCode) {
        jdbcTemplate.update("UPDATE authzcraft_principal SET lifecycle_state = 'INACTIVE', updated_by = ?, record_version = record_version + 1 "
                + "WHERE tenant_key = ? AND principal_kind = 'GROUP' AND principal_key = ?", ACTOR, tenantKey, groupCode);
    }

    public ApplicationRbacPrincipalSyncResult.MembershipResult upsertMembership(
            String tenantKey, String containerKind, String containerCode,
            Long containerPrincipalId, String userKey, Long userPrincipalId,
            String validFromStr, String validUntilStr) {
        Timestamp validFrom = toTimestamp(validFromStr);
        Timestamp validUntil = toTimestamp(validUntilStr);
        if (validFrom == null) {
            validFrom = Timestamp.valueOf(LocalDateTime.now());
        }
        if (validUntil != null && validUntil.before(validFrom)) {
            throw new IllegalArgumentException("validUntil must be after validFrom for membership " + containerCode + ":" + userKey);
        }
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM authzcraft_principal_membership "
                        + "WHERE tenant_key = ? AND container_principal_id = ? AND member_principal_id = ? AND valid_until IS NULL",
                new Object[]{tenantKey, containerPrincipalId, userPrincipalId}, Integer.class);
        if (count != null && count > 0 && validUntil == null) {
            return findMembershipResult(tenantKey, containerKind, containerCode, containerPrincipalId, userKey, userPrincipalId);
        }
        if (count != null && count > 0 && validUntil != null) {
            jdbcTemplate.update("UPDATE authzcraft_principal_membership SET valid_until = ?, updated_by = ? "
                    + "WHERE tenant_key = ? AND container_principal_id = ? AND member_principal_id = ? AND valid_until IS NULL",
                    validUntil, ACTOR, tenantKey, containerPrincipalId, userPrincipalId);
            return findMembershipResult(tenantKey, containerKind, containerCode, containerPrincipalId, userKey, userPrincipalId);
        }
        jdbcTemplate.update(
                "INSERT INTO authzcraft_principal_membership "
                        + "(id, tenant_key, container_principal_id, container_kind, member_principal_id, member_kind, valid_from, valid_until, created_by, updated_by) "
                        + "VALUES (?, ?, ?, ?, ?, 'USER', ?, ?, ?, ?)",
                idGenerator.nextId(), tenantKey, containerPrincipalId, containerKind, userPrincipalId, validFrom, validUntil, ACTOR, ACTOR);
        return findMembershipResult(tenantKey, containerKind, containerCode, containerPrincipalId, userKey, userPrincipalId);
    }

    private List<ApplicationRbacPrincipalSyncResult.MembershipResult> closeMissingMemberships(String tenantKey, String appKey, Set<String> requestedMembershipKeys) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT m.id, m.container_principal_id, m.container_kind, m.member_principal_id, "
                + "cp.principal_key AS container_code, mp.principal_key AS user_key "
                + "FROM authzcraft_principal_membership m "
                + "JOIN authzcraft_principal cp ON cp.id = m.container_principal_id AND cp.tenant_key = m.tenant_key "
                + "JOIN authzcraft_principal mp ON mp.id = m.member_principal_id AND mp.tenant_key = m.tenant_key "
                + "LEFT JOIN authzcraft_principal_role r ON r.principal_id = m.container_principal_id AND r.tenant_key = m.tenant_key "
                + "LEFT JOIN authzcraft_principal_group g ON g.principal_id = m.container_principal_id AND g.tenant_key = m.tenant_key "
                + "WHERE m.tenant_key = ? AND m.member_kind = 'USER' AND m.valid_until IS NULL "
                + "AND (r.app_key = ? OR g.app_key = ?)",
                tenantKey, appKey, appKey);
        List<ApplicationRbacPrincipalSyncResult.MembershipResult> closed = new java.util.ArrayList<ApplicationRbacPrincipalSyncResult.MembershipResult>();
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        for (Map<String, Object> row : rows) {
            String key = String.valueOf(row.get("container_principal_id")) + ":" + String.valueOf(row.get("member_principal_id"));
            if (!requestedMembershipKeys.contains(key)) {
                jdbcTemplate.update("UPDATE authzcraft_principal_membership SET valid_until = ?, updated_by = ? WHERE id = ?",
                        now, ACTOR, row.get("id"));
                closed.add(findMembershipResult(tenantKey,
                        String.valueOf(row.get("container_kind")),
                        String.valueOf(row.get("container_code")),
                        toLong(row.get("container_principal_id")),
                        String.valueOf(row.get("user_key")),
                        toLong(row.get("member_principal_id"))));
            }
        }
        return closed;
    }

    private ApplicationRbacPrincipalSyncResult.MembershipResult findMembershipResult(
            String tenantKey, String containerKind, String containerCode,
            Long containerPrincipalId, String userKey, Long userPrincipalId) {
        ApplicationRbacPrincipalSyncResult.MembershipResult result = findMembershipResultOrNull(
                tenantKey, containerKind, containerCode, containerPrincipalId, userKey, userPrincipalId);
        if (result == null) {
            throw new IllegalStateException("membership projection was not found after sync");
        }
        return result;
    }

    public ApplicationRbacPrincipalSyncResult.MembershipResult findMembershipResultOrNull(
            String tenantKey, String containerKind, String containerCode,
            Long containerPrincipalId, String userKey, Long userPrincipalId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, container_principal_id, member_principal_id, valid_from, valid_until, updated_at "
                        + "FROM authzcraft_principal_membership "
                        + "WHERE tenant_key = ? AND container_principal_id = ? AND member_principal_id = ? "
                        + "ORDER BY valid_until IS NULL DESC, updated_at DESC LIMIT 1",
                tenantKey, containerPrincipalId, userPrincipalId);
        if (rows.isEmpty()) {
            return null;
        }
        Map<String, Object> row = rows.get(0);
        ApplicationRbacPrincipalSyncResult.MembershipResult result = new ApplicationRbacPrincipalSyncResult.MembershipResult();
        result.setContainerKind(containerKind);
        result.setContainerCode(containerCode);
        if ("ROLE".equals(containerKind)) {
            result.setRoleCode(containerCode);
            result.setRolePrincipalId(String.valueOf(row.get("container_principal_id")));
        } else {
            result.setGroupCode(containerCode);
            result.setGroupPrincipalId(String.valueOf(row.get("container_principal_id")));
        }
        result.setContainerPrincipalId(String.valueOf(row.get("container_principal_id")));
        result.setUserKey(userKey);
        result.setMembershipId(String.valueOf(row.get("id")));
        result.setUserPrincipalId(String.valueOf(row.get("member_principal_id")));
        result.setValidFrom(text(row.get("valid_from")));
        result.setValidUntil(text(row.get("valid_until")));
        result.setSyncVersion(text(row.get("updated_at")));
        result.setLifecycleState(row.get("valid_until") == null ? "ACTIVE" : "INACTIVE");
        return result;
    }

    @SuppressWarnings("null")
    public List<Map<String, Object>> searchMembershipMaps(String tenantKey, String appKey, String containerKind, String containerCode, String userKey) {
        StringBuilder sql = new StringBuilder("SELECT CAST(m.id AS CHAR) AS membershipId, m.container_kind AS containerKind, cp.principal_key AS containerCode, mp.principal_key AS userKey, CAST(m.container_principal_id AS CHAR) AS containerPrincipalId, CAST(m.member_principal_id AS CHAR) AS userPrincipalId, m.valid_from AS validFrom, m.valid_until AS validUntil, m.updated_at AS syncVersion, CASE WHEN m.valid_until IS NULL THEN 'ACTIVE' ELSE 'INACTIVE' END AS lifecycleState FROM authzcraft_principal_membership m JOIN authzcraft_principal cp ON cp.id = m.container_principal_id AND cp.tenant_key = m.tenant_key JOIN authzcraft_principal mp ON mp.id = m.member_principal_id AND mp.tenant_key = m.tenant_key LEFT JOIN authzcraft_principal_role r ON r.principal_id = m.container_principal_id AND r.tenant_key = m.tenant_key LEFT JOIN authzcraft_principal_group g ON g.principal_id = m.container_principal_id AND g.tenant_key = m.tenant_key WHERE m.tenant_key = ? AND (r.app_key = ? OR g.app_key = ?)");
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add(tenantKey);
        args.add(appKey);
        args.add(appKey);
        if (StringUtils.hasText(containerKind)) {
            sql.append(" AND m.container_kind = ?");
            args.add(containerKind.trim().toUpperCase(Locale.ROOT));
        }
        if (StringUtils.hasText(containerCode)) {
            sql.append(" AND cp.principal_key = ?");
            args.add(containerCode.trim());
        }
        if (StringUtils.hasText(userKey)) {
            sql.append(" AND mp.principal_key = ?");
            args.add(userKey.trim());
        }
        sql.append(" ORDER BY m.valid_until IS NULL DESC, cp.principal_key, mp.principal_key");
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), args.toArray(new Object[0]));
        for (Map<String, Object> row : rows) {
            if ("ROLE".equals(row.get("containerKind"))) {
                row.put("roleCode", row.get("containerCode"));
                row.put("rolePrincipalId", row.get("containerPrincipalId"));
            } else if ("GROUP".equals(row.get("containerKind"))) {
                row.put("groupCode", row.get("containerCode"));
                row.put("groupPrincipalId", row.get("containerPrincipalId"));
            }
        }
        return rows;
    }

    private ContainerRef resolveContainer(ApplicationRbacPrincipalSyncCommand.MembershipItem membership) {
        if (StringUtils.hasText(membership.getContainerKind()) && StringUtils.hasText(membership.getContainerCode())) {
            String kind = membership.getContainerKind().trim().toUpperCase(Locale.ROOT);
            if (!"ROLE".equals(kind) && !"GROUP".equals(kind)) {
                return null;
            }
            return new ContainerRef(kind, membership.getContainerCode().trim());
        }
        if (StringUtils.hasText(membership.getRoleCode())) {
            return new ContainerRef("ROLE", membership.getRoleCode().trim());
        }
        if (StringUtils.hasText(membership.getGroupCode())) {
            return new ContainerRef("GROUP", membership.getGroupCode().trim());
        }
        return null;
    }

    private String membershipKey(ApplicationRbacPrincipalSyncCommand.MembershipItem membership) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(membership.getRoleCode())) {
            sb.append("ROLE:").append(membership.getRoleCode().trim());
        } else if (StringUtils.hasText(membership.getGroupCode())) {
            sb.append("GROUP:").append(membership.getGroupCode().trim());
        } else if (StringUtils.hasText(membership.getContainerKind()) && StringUtils.hasText(membership.getContainerCode())) {
            sb.append(membership.getContainerKind().trim().toUpperCase(Locale.ROOT)).append(":").append(membership.getContainerCode().trim());
        } else {
            sb.append("UNKNOWN");
        }
        sb.append(":").append(membership.getUserKey() == null ? "" : membership.getUserKey().trim());
        return sb.toString();
    }

    private Long firstNonNull(Long first, Long second) {
        return first != null ? first : second;
    }

    private static class ContainerRef {
        final String kind;
        final String code;
        ContainerRef(String kind, String code) {
            this.kind = kind;
            this.code = code;
        }
    }

    public Long findPrincipalId(String tenantKey, String principalKind, String principalKey) {
        List<Long> ids = jdbcTemplate.queryForList(
                "SELECT id FROM authzcraft_principal WHERE tenant_key = ? AND principal_kind = ? AND principal_key = ?",
                new Object[]{tenantKey, principalKind, principalKey}, Long.class);
        return ids.isEmpty() ? null : ids.get(0);
    }

    private void addFailure(ApplicationRbacPrincipalSyncResult result, String itemType, String itemKey, String reason) {
        ApplicationRbacPrincipalSyncResult.Failure failure = new ApplicationRbacPrincipalSyncResult.Failure();
        failure.setItemType(itemType);
        failure.setItemKey(itemKey);
        failure.setReason(reason);
        result.getFailures().add(failure);
    }

    public String lifecycleState(String lifecycleState) {
        if (!StringUtils.hasText(lifecycleState)) {
            return "ACTIVE";
        }
        String normalized = lifecycleState.trim().toUpperCase(Locale.ROOT);
        if (!"ACTIVE".equals(normalized) && !"INACTIVE".equals(normalized) && !"RETIRED".equals(normalized)) {
            throw new IllegalArgumentException("Unsupported lifecycleState: " + lifecycleState);
        }
        return normalized;
    }

    public String firstText(String first, String fallback) {
        return StringUtils.hasText(first) ? first.trim() : fallback;
    }

    private Long toLong(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : Long.valueOf(String.valueOf(value));
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Timestamp toTimestamp(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        try {
            return Timestamp.valueOf(trimmed);
        } catch (IllegalArgumentException e) {
            // 兼容 MySQL Connector/J 8.x 将 DATETIME 映射为 LocalDateTime 后
            // String.valueOf 输出的 ISO-8601 格式（如 2026-08-26T14:36:38.588）
            return Timestamp.valueOf(LocalDateTime.parse(trimmed));
        }
    }
}