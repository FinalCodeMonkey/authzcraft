package com.fcm.authzcraft.pip.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fcm.authzcraft.pip.application.service.ApplicationRbacPrincipalSyncResult.GroupResult;
import com.fcm.authzcraft.pip.application.service.ApplicationRbacPrincipalSyncResult.MembershipResult;
import com.fcm.authzcraft.pip.application.service.ApplicationRbacPrincipalSyncResult.RoleResult;
import com.fcm.authzcraft.pip.domain.model.PrincipalKind;
import com.fcm.authzcraft.pip.infrastructure.persistence.JdbcApplicationRbacPrincipalSyncRepository;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PrincipalRbacCrudService {
    private static final String DEFAULT_ACTOR = "principal-rbac-crud";

    private final JdbcApplicationRbacPrincipalSyncRepository repository;

    public PrincipalRbacCrudService(JdbcApplicationRbacPrincipalSyncRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public RoleResult upsertRole(String tenantKey, String appKey, String roleCode, String roleName,
                                 String description, String lifecycleState) {
        requireText(tenantKey, "tenantKey");
        requireText(appKey, "appKey");
        requireText(roleCode, "roleCode");
        String state = repository.lifecycleState(lifecycleState);
        Long principalId = repository.upsertRole(tenantKey, appKey, roleCode.trim(),
                repository.firstText(roleName, roleCode.trim()), description, state);
        RoleResult result = new RoleResult();
        result.setRoleCode(roleCode.trim());
        result.setPrincipalId(String.valueOf(principalId));
        result.setLifecycleState(state);
        return result;
    }

    @Transactional(readOnly = true)
    public RoleResult findRole(String tenantKey, String appKey, String roleCode) {
        requireText(tenantKey, "tenantKey");
        requireText(appKey, "appKey");
        requireText(roleCode, "roleCode");
        return repository.findRoleResult(tenantKey, appKey, roleCode.trim());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> searchRoles(String tenantKey, String appKey, String roleCode, String lifecycleState) {
        requireText(tenantKey, "tenantKey");
        requireText(appKey, "appKey");
        return repository.searchRoleMaps(tenantKey, appKey, roleCode, lifecycleState);
    }

    @Transactional
    public RoleResult retireRole(String tenantKey, String appKey, String roleCode) {
        requireText(tenantKey, "tenantKey");
        requireText(appKey, "appKey");
        requireText(roleCode, "roleCode");
        RoleResult existing = repository.findRoleResult(tenantKey, appKey, roleCode.trim());
        if (existing == null) {
            throw new IllegalArgumentException("role not found: " + roleCode);
        }
        repository.retireRoleByCode(tenantKey, roleCode.trim());
        return repository.findRoleResult(tenantKey, appKey, roleCode.trim());
    }

    @Transactional
    public GroupResult upsertGroup(String tenantKey, String appKey, String groupCode, String groupName,
                                   String description, String lifecycleState) {
        requireText(tenantKey, "tenantKey");
        requireText(appKey, "appKey");
        requireText(groupCode, "groupCode");
        String state = repository.lifecycleState(lifecycleState);
        Long principalId = repository.upsertGroup(tenantKey, appKey, groupCode.trim(),
                repository.firstText(groupName, groupCode.trim()), description, state);
        GroupResult result = new GroupResult();
        result.setGroupCode(groupCode.trim());
        result.setPrincipalId(String.valueOf(principalId));
        result.setLifecycleState(state);
        return result;
    }

    @Transactional(readOnly = true)
    public GroupResult findGroup(String tenantKey, String appKey, String groupCode) {
        requireText(tenantKey, "tenantKey");
        requireText(appKey, "appKey");
        requireText(groupCode, "groupCode");
        return repository.findGroupResult(tenantKey, appKey, groupCode.trim());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> searchGroups(String tenantKey, String appKey, String groupCode, String lifecycleState) {
        requireText(tenantKey, "tenantKey");
        requireText(appKey, "appKey");
        return repository.searchGroupMaps(tenantKey, appKey, groupCode, lifecycleState);
    }

    @Transactional
    public GroupResult retireGroup(String tenantKey, String appKey, String groupCode) {
        requireText(tenantKey, "tenantKey");
        requireText(appKey, "appKey");
        requireText(groupCode, "groupCode");
        GroupResult existing = repository.findGroupResult(tenantKey, appKey, groupCode.trim());
        if (existing == null) {
            throw new IllegalArgumentException("group not found: " + groupCode);
        }
        repository.retireGroupByCode(tenantKey, groupCode.trim());
        return repository.findGroupResult(tenantKey, appKey, groupCode.trim());
    }

    @Transactional
    public MembershipResult upsertMembership(String tenantKey, String appKey, String containerKind,
                                             String containerCode, String userKey, String validFrom, String validUntil) {
        requireText(tenantKey, "tenantKey");
        requireText(containerKind, "containerKind");
        requireText(containerCode, "containerCode");
        requireText(userKey, "userKey");
        String kind = containerKind.trim().toUpperCase();
        if (!"ROLE".equals(kind) && !"GROUP".equals(kind)) {
            throw new IllegalArgumentException("containerKind must be ROLE or GROUP");
        }
        Long containerPrincipalId = repository.findPrincipalId(tenantKey, kind, containerCode.trim());
        if (containerPrincipalId == null) {
            throw new IllegalArgumentException(kind + " principal not found: " + containerCode);
        }
        Long userPrincipalId = repository.findPrincipalId(tenantKey, PrincipalKind.USER.name(), userKey.trim());
        if (userPrincipalId == null) {
            throw new IllegalArgumentException("user principal not found: " + userKey);
        }
        return repository.upsertMembership(tenantKey, kind, containerCode.trim(), containerPrincipalId,
                userKey.trim(), userPrincipalId, validFrom, validUntil);
    }

    @Transactional(readOnly = true)
    public MembershipResult findMembership(String tenantKey, String appKey, String containerKind,
                                           String containerCode, String userKey) {
        requireText(tenantKey, "tenantKey");
        requireText(containerKind, "containerKind");
        requireText(containerCode, "containerCode");
        requireText(userKey, "userKey");
        String kind = containerKind.trim().toUpperCase();
        Long containerPrincipalId = repository.findPrincipalId(tenantKey, kind, containerCode.trim());
        Long userPrincipalId = repository.findPrincipalId(tenantKey, PrincipalKind.USER.name(), userKey.trim());
        if (containerPrincipalId == null || userPrincipalId == null) {
            return null;
        }
        return repository.findMembershipResultOrNull(tenantKey, kind, containerCode.trim(),
                containerPrincipalId, userKey.trim(), userPrincipalId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> searchMemberships(String tenantKey, String appKey, String containerKind,
                                                        String containerCode, String userKey) {
        requireText(tenantKey, "tenantKey");
        requireText(appKey, "appKey");
        return repository.searchMembershipMaps(tenantKey, appKey, containerKind, containerCode, userKey);
    }

    @Transactional
    public void closeMembership(String tenantKey, String appKey, String containerKind,
                               String containerCode, String userKey) {
        requireText(tenantKey, "tenantKey");
        requireText(containerKind, "containerKind");
        requireText(containerCode, "containerCode");
        requireText(userKey, "userKey");
        String kind = containerKind.trim().toUpperCase();
        Long containerPrincipalId = repository.findPrincipalId(tenantKey, kind, containerCode.trim());
        Long userPrincipalId = repository.findPrincipalId(tenantKey, PrincipalKind.USER.name(), userKey.trim());
        if (containerPrincipalId == null || userPrincipalId == null) {
            throw new IllegalArgumentException("container or user principal not found");
        }
        MembershipResult existing = repository.findMembershipResultOrNull(tenantKey, kind, containerCode.trim(),
                containerPrincipalId, userKey.trim(), userPrincipalId);
        if (existing == null) {
            throw new IllegalArgumentException("membership not found");
        }
        if (!"ACTIVE".equals(existing.getLifecycleState())) {
            return;
        }
        repository.upsertMembership(tenantKey, kind, containerCode.trim(), containerPrincipalId,
                userKey.trim(), userPrincipalId, existing.getValidFrom(),
                java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()).toString());
    }

    private void requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
