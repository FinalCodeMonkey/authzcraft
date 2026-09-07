package com.fcm.authzcraft.demo.oa.infrastructure.client;

import com.fcm.authzcraft.demo.oa.domain.gateway.AuthzCraftPrincipalGateway;
import com.fcm.authzcraft.demo.oa.domain.gateway.PrincipalDirectory;
import com.fcm.authzcraft.demo.oa.domain.gateway.RoleAuthorizationStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@ConditionalOnProperty(prefix = "authzcraft.demo-oa.rbac", name = "storage-mode", havingValue = "AUTHZCRAFT", matchIfMissing = true)
public class AuthzCraftPrincipalDirectory implements PrincipalDirectory {

    private final AuthzCraftPrincipalGateway principalGateway;
    private final RoleAuthorizationStore roleAuthorizationStore;

    public AuthzCraftPrincipalDirectory(AuthzCraftPrincipalGateway principalGateway,
                                        RoleAuthorizationStore roleAuthorizationStore) {
        this.principalGateway = principalGateway;
        this.roleAuthorizationStore = roleAuthorizationStore;
    }

    @Override
    public Map<String, Object> findActiveUserProjection(String userKey) {
        return principalGateway.findActiveUserProjection(userKey);
    }

    @Override
    public Set<String> listRoleCodes(String userKey) {
        Set<String> result = new LinkedHashSet<String>();
        for (Map<String, Object> role : principalGateway.searchRoles()) {
            String roleCode = text(role.get("roleCode"));
            if (!StringUtils.hasText(roleCode)) {
                continue;
            }
            for (Map<String, Object> membership : principalGateway.searchMemberships("ROLE", roleCode, null)) {
                if (userKey.equals(text(membership.get("userKey"))) && "ACTIVE".equals(text(membership.get("lifecycleState")))) {
                    result.add(roleCode);
                }
            }
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> listRoles() {
        List<Map<String, Object>> roles = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> item : principalGateway.searchRoles()) {
            // 只返回 ACTIVE 角色：删除（retire）后角色变为 INACTIVE，应从管理列表消失
            if (!"ACTIVE".equals(text(item.get("lifecycleState")))) {
                continue;
            }
            Map<String, Object> role = new LinkedHashMap<String, Object>();
            role.put("id", text(item.get("roleCode")));
            role.put("code", text(item.get("roleCode")));
            role.put("name", defaultText(text(item.get("roleName")), text(item.get("roleCode"))));
            role.put("description", text(item.get("description")));
            role.put("principalId", text(item.get("principalId")));
            role.put("lifecycleState", text(item.get("lifecycleState")));
            roles.add(role);
        }
        return roles;
    }

    @Override
    public List<Map<String, Object>> listGroups() {
        List<Map<String, Object>> groups = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> item : principalGateway.searchGroups()) {
            // 只返回 ACTIVE 用户组：停用（retire）后用户组变为 INACTIVE，应从管理列表消失
            if (!"ACTIVE".equals(text(item.get("lifecycleState")))) {
                continue;
            }
            Map<String, Object> group = new LinkedHashMap<String, Object>();
            group.put("id", text(item.get("groupCode")));
            group.put("code", text(item.get("groupCode")));
            group.put("name", defaultText(text(item.get("groupName")), text(item.get("groupCode"))));
            group.put("description", text(item.get("description")));
            group.put("principalId", text(item.get("principalId")));
            group.put("lifecycleState", text(item.get("lifecycleState")));
            groups.add(group);
        }
        return groups;
    }

    @Override
    public List<Map<String, Object>> listUsers() {
        Map<String, Map<String, Object>> usersByKey = new LinkedHashMap<String, Map<String, Object>>();
        Map<String, Map<String, Object>> userProjections = userProjectionsByKey();
        for (Map<String, Object> role : principalGateway.searchRoles()) {
            String roleCode = text(role.get("roleCode"));
            if (!StringUtils.hasText(roleCode) || !"ACTIVE".equals(text(role.get("lifecycleState")))) {
                continue;
            }
            List<String> permissionCodes = roleAuthorizationStore.listRolePermissionCodesByRoleCode(roleCode);
            for (Map<String, Object> membership : principalGateway.searchMemberships("ROLE", roleCode, null)) {
                if (!"ACTIVE".equals(text(membership.get("lifecycleState")))) {
                    continue;
                }
                Map<String, Object> user = ensureUser(usersByKey, userProjections, text(membership.get("userKey")));
                if (user == null) {
                    continue;
                }
                ((Set<String>) user.get("roles")).add(roleCode);
                if (permissionCodes != null) {
                    ((Set<String>) user.get("permissions")).addAll(permissionCodes);
                }
            }
        }
        for (Map<String, Object> group : principalGateway.searchGroups()) {
            String groupCode = text(group.get("groupCode"));
            if (!StringUtils.hasText(groupCode) || !"ACTIVE".equals(text(group.get("lifecycleState")))) {
                continue;
            }
            for (Map<String, Object> membership : principalGateway.searchMemberships("GROUP", groupCode, null)) {
                if ("ACTIVE".equals(text(membership.get("lifecycleState")))) {
                    ensureUser(usersByKey, userProjections, text(membership.get("userKey")));
                }
            }
        }
        return materializeUsers(usersByKey);
    }

    @Override
    public List<Map<String, Object>> listRoleUsers(String roleId) {
        return members("ROLE", roleId);
    }

    @Override
    public List<Map<String, Object>> listGroupUsers(String groupId) {
        return members("GROUP", groupId);
    }

    @Override
    public List<Map<String, Object>> listUserMemberships(String userKey) {
        List<Map<String, Object>> memberships = new ArrayList<Map<String, Object>>();
        collectUserMemberships(memberships, "ROLE", principalGateway.searchRoles(), "roleCode", userKey);
        collectUserMemberships(memberships, "GROUP", principalGateway.searchGroups(), "groupCode", userKey);
        return memberships;
    }

    @Override
    public int countRoleUsers(String roleId) {
        return listRoleUsers(roleId).size();
    }

    @Override
    public int countGroupUsers(String groupId) {
        return listGroupUsers(groupId).size();
    }

    @Override
    public String roleCode(String roleId) {
        return roleId;
    }

    @Override
    public String groupCode(String groupId) {
        return groupId;
    }

    @Override
    public boolean supportsDataPermissionTemplates() {
        return true;
    }

    private List<Map<String, Object>> members(String containerKind, String containerCode) {
        Map<String, Map<String, Object>> userProjections = userProjectionsByKey();
        List<Map<String, Object>> users = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> membership : principalGateway.searchMemberships(containerKind, containerCode, null)) {
            if (!"ACTIVE".equals(text(membership.get("lifecycleState")))) {
                continue;
            }
            String userKey = text(membership.get("userKey"));
            Map<String, Object> projection = projectionFor(userProjections, userKey);
            Map<String, Object> user = new LinkedHashMap<String, Object>();
            user.put("id", userKey);
            user.put("userKey", userKey);
            user.put("displayName", displayName(projection, userKey));
            user.put("principalId", projection == null ? "" : defaultText(text(projection.get("principalId")), text(projection.get("id"))));
            user.put("membershipId", text(membership.get("membershipId")));
            user.put("projectionMissing", projection == null);
            users.add(user);
        }
        return users;
    }

    private void collectUserMemberships(List<Map<String, Object>> result, String containerKind,
                                        List<Map<String, Object>> containers, String codeField, String userKey) {
        for (Map<String, Object> container : containers) {
            String containerCode = text(container.get(codeField));
            // 只收集 ACTIVE 角色/用户组的成员关系，INACTIVE（已删除/停用）容器不参与
            if (!StringUtils.hasText(containerCode) || !"ACTIVE".equals(text(container.get("lifecycleState")))) {
                continue;
            }
            for (Map<String, Object> membership : principalGateway.searchMemberships(containerKind, containerCode, null)) {
                if (userKey.equals(text(membership.get("userKey"))) && "ACTIVE".equals(text(membership.get("lifecycleState")))) {
                    result.add(membership);
                }
            }
        }
    }

    private Map<String, Object> ensureUser(Map<String, Map<String, Object>> usersByKey,
                                           Map<String, Map<String, Object>> userProjections,
                                           String userKey) {
        if (!StringUtils.hasText(userKey)) {
            return null;
        }
        Map<String, Object> user = usersByKey.get(userKey);
        if (user != null) {
            return user;
        }
        Map<String, Object> projection = projectionFor(userProjections, userKey);
        user = new LinkedHashMap<String, Object>();
        user.put("id", userKey);
        user.put("userKey", userKey);
        user.put("displayName", displayName(projection, userKey));
        user.put("principalId", projection == null ? "" : defaultText(text(projection.get("principalId")), text(projection.get("id"))));
        user.put("lifecycleState", projection == null ? "ACTIVE" : defaultText(text(projection.get("lifecycleState")), text(projection.get("status"))));
        user.put("projectionMissing", projection == null);
        user.put("roles", new LinkedHashSet<String>());
        user.put("permissions", new LinkedHashSet<String>());
        usersByKey.put(userKey, user);
        return user;
    }

    private List<Map<String, Object>> materializeUsers(Map<String, Map<String, Object>> usersByKey) {
        List<Map<String, Object>> users = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> user : usersByKey.values()) {
            user.put("roles", new ArrayList<String>((Set<String>) user.get("roles")));
            user.put("permissions", new ArrayList<String>((Set<String>) user.get("permissions")));
            users.add(user);
        }
        return users;
    }

    private Map<String, Map<String, Object>> userProjectionsByKey() {
        Map<String, Map<String, Object>> projections = new LinkedHashMap<String, Map<String, Object>>();
        for (Map<String, Object> item : principalGateway.searchUsers()) {
            String userKey = defaultText(text(item.get("userKey")), text(item.get("principalKey")));
            if (StringUtils.hasText(userKey)) {
                projections.put(userKey, item);
            }
        }
        return projections;
    }

    private Map<String, Object> projectionFor(Map<String, Map<String, Object>> userProjections, String userKey) {
        if (!StringUtils.hasText(userKey)) {
            return null;
        }
        Map<String, Object> projection = userProjections.get(userKey);
        if (projection != null) {
            return projection;
        }
        projection = principalGateway.findActiveUserProjection(userKey);
        if (projection != null) {
            userProjections.put(userKey, projection);
        }
        return projection;
    }

    private String displayName(Map<String, Object> projection, String userKey) {
        if (projection == null) {
            return userKey;
        }
        return defaultText(text(projection.get("displayName")), defaultText(text(projection.get("staffName")), userKey));
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }
}