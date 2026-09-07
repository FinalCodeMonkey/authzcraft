package com.fcm.authzcraft.demo.oa.infrastructure.persistence;

import com.fcm.authzcraft.demo.oa.domain.gateway.AuthzCraftPrincipalGateway;
import com.fcm.authzcraft.demo.oa.domain.gateway.PrincipalAdminGateway;
import com.fcm.authzcraft.demo.oa.domain.repository.RbacRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "authzcraft.demo-oa.rbac", name = "storage-mode", havingValue = "LOCAL")
public class LocalPrincipalAdminGateway implements PrincipalAdminGateway {

    private final RbacRepository repository;
    private final AuthzCraftPrincipalGateway principalGateway;

    public LocalPrincipalAdminGateway(RbacRepository repository, AuthzCraftPrincipalGateway principalGateway) {
        this.repository = repository;
        this.principalGateway = principalGateway;
    }

    @Override
    public Map<String, Object> searchApplicationRbacStatus() {
        return principalGateway.searchApplicationRbacStatus(repository.listRoleSyncItems(), repository.listGroupSyncItems(), memberships());
    }

    @Override
    public Map<String, Object> syncApplicationRbac(boolean dryRun) {
        List<Map<String, Object>> roles = repository.listRoleSyncItems();
        List<Map<String, Object>> groups = repository.listGroupSyncItems();
        List<Map<String, Object>> memberships = memberships();
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("roles", roles);
        result.put("groups", groups);
        result.put("memberships", memberships);
        result.put("dryRun", dryRun);
        if (!dryRun) {
            result.put("syncResult", principalGateway.syncApplicationRbac(roles, groups, memberships));
        }
        return result;
    }

    @Override
    public Map<String, Object> createRole(String code, String name, String description) {
        return singletonId(repository.createRole(code, name, description));
    }

    @Override
    public Map<String, Object> updateRole(String roleId, String name, String description) {
        Long localRoleId = localRoleId(roleId);
        repository.updateRole(localRoleId, name, description);
        return singletonId(localRoleId);
    }

    @Override
    public void deleteRole(String roleId) {
        repository.deleteRole(localRoleId(roleId));
    }

    @Override
    public Map<String, Object> createGroup(String code, String name, String description) {
        return singletonId(repository.createGroup(code, name, description));
    }

    @Override
    public Map<String, Object> updateGroup(String groupId, String name, String description) {
        Long localGroupId = localGroupId(groupId);
        repository.updateGroup(localGroupId, name, description);
        return singletonId(localGroupId);
    }

    @Override
    public void deleteGroup(String groupId) {
        repository.deleteGroup(localGroupId(groupId));
    }

    @Override
    public void addRoleUser(String roleId, String userKey, String displayName) {
        repository.addRoleUser(localRoleId(roleId), userKey, displayName);
    }

    @Override
    public void removeRoleUser(String roleId, String userKey) {
        repository.removeRoleUser(localRoleId(roleId), userKey);
    }

    @Override
    public void addGroupUser(String groupId, String userKey, String displayName) {
        repository.addGroupUser(localGroupId(groupId), userKey, displayName);
    }

    @Override
    public void removeGroupUser(String groupId, String userKey) {
        repository.removeGroupUser(localGroupId(groupId), userKey);
    }

    private List<Map<String, Object>> memberships() {
        List<Map<String, Object>> memberships = repository.listRoleMembershipSyncItems();
        memberships.addAll(repository.listGroupMembershipSyncItems());
        return memberships;
    }

    private Long localRoleId(String roleId) {
        return Long.valueOf(roleId);
    }

    private Long localGroupId(String groupId) {
        return Long.valueOf(groupId);
    }

    private Map<String, Object> singletonId(Long id) {
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("id", id == null ? null : String.valueOf(id));
        return response;
    }
}