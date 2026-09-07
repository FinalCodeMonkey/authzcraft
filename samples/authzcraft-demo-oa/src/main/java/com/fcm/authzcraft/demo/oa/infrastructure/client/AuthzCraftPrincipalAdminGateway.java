package com.fcm.authzcraft.demo.oa.infrastructure.client;

import com.fcm.authzcraft.demo.oa.domain.gateway.AuthzCraftPrincipalGateway;
import com.fcm.authzcraft.demo.oa.domain.gateway.PrincipalAdminGateway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "authzcraft.demo-oa.rbac", name = "storage-mode", havingValue = "AUTHZCRAFT", matchIfMissing = true)
public class AuthzCraftPrincipalAdminGateway implements PrincipalAdminGateway {

    private final AuthzCraftPrincipalGateway principalGateway;

    public AuthzCraftPrincipalAdminGateway(AuthzCraftPrincipalGateway principalGateway) {
        this.principalGateway = principalGateway;
    }

    @Override
    public Map<String, Object> searchApplicationRbacStatus() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("roles", principalGateway.searchRoles());
        result.put("groups", principalGateway.searchGroups());
        result.put("memberships", allMemberships());
        result.put("roleCount", ((List<?>) result.get("roles")).size());
        result.put("groupCount", ((List<?>) result.get("groups")).size());
        result.put("membershipCount", ((List<?>) result.get("memberships")).size());
        result.put("failedCount", 0);
        result.put("failures", Collections.emptyList());
        return result;
    }

    @Override
    public Map<String, Object> syncApplicationRbac(boolean dryRun) {
        Map<String, Object> result = searchApplicationRbacStatus();
        result.put("dryRun", dryRun);
        return result;
    }

    @Override
    public Map<String, Object> createRole(String code, String name, String description) {
        principalGateway.upsertRole(code, name, description, "ACTIVE");
        return singletonId(code);
    }

    @Override
    public Map<String, Object> updateRole(String roleId, String name, String description) {
        principalGateway.upsertRole(roleId, name, description, "ACTIVE");
        return singletonId(roleId);
    }

    @Override
    public void deleteRole(String roleId) {
        principalGateway.retireRole(roleId);
    }

    @Override
    public Map<String, Object> createGroup(String code, String name, String description) {
        principalGateway.upsertGroup(code, name, description, "ACTIVE");
        return singletonId(code);
    }

    @Override
    public Map<String, Object> updateGroup(String groupId, String name, String description) {
        principalGateway.upsertGroup(groupId, name, description, "ACTIVE");
        return singletonId(groupId);
    }

    @Override
    public void deleteGroup(String groupId) {
        principalGateway.retireGroup(groupId);
    }

    @Override
    public void addRoleUser(String roleId, String userKey, String displayName) {
        requireUserProjection(userKey);
        principalGateway.upsertMembership("ROLE", roleId, userKey);
    }

    @Override
    public void removeRoleUser(String roleId, String userKey) {
        principalGateway.closeMembership("ROLE", roleId, userKey);
    }

    @Override
    public void addGroupUser(String groupId, String userKey, String displayName) {
        requireUserProjection(userKey);
        principalGateway.upsertMembership("GROUP", groupId, userKey);
    }

    @Override
    public void removeGroupUser(String groupId, String userKey) {
        principalGateway.closeMembership("GROUP", groupId, userKey);
    }

    private List<Map<String, Object>> allMemberships() {
        List<Map<String, Object>> memberships = new ArrayList<Map<String, Object>>();
        collectMemberships(memberships, "ROLE", principalGateway.searchRoles(), "roleCode");
        collectMemberships(memberships, "GROUP", principalGateway.searchGroups(), "groupCode");
        return memberships;
    }

    private void collectMemberships(List<Map<String, Object>> result, String containerKind,
                                    List<Map<String, Object>> containers, String codeField) {
        for (Map<String, Object> container : containers) {
            String containerCode = text(container.get(codeField));
            if (StringUtils.hasText(containerCode)) {
                result.addAll(principalGateway.searchMemberships(containerKind, containerCode, null));
            }
        }
    }

    private void requireUserProjection(String userKey) {
        if (principalGateway.findActiveUserProjection(userKey) == null) {
            throw new IllegalArgumentException("user principal was not found in AuthzCraft principal domain: " + userKey);
        }
    }

    private Map<String, Object> singletonId(String id) {
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("id", id);
        return response;
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }
}