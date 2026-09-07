package com.fcm.authzcraft.demo.oa.infrastructure.persistence;

import com.fcm.authzcraft.demo.oa.domain.model.RbacFieldGrant;
import com.fcm.authzcraft.demo.oa.domain.gateway.RoleAuthorizationStore;
import com.fcm.authzcraft.demo.oa.domain.repository.RbacRepository;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
public class RbacRepositoryImpl implements RbacRepository, RoleAuthorizationStore {

    private final RbacMapper mapper;

    public RbacRepositoryImpl(RbacMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Map<String, Object> findActiveUser(String userKey) { return mapper.findActiveUser(userKey); }

    @Override
    public void upsertUserProjection(String userKey, String displayName, int status) { mapper.upsertUserProjection(userKey, displayName, status); }

    @Override
    public Set<String> listRoleCodes(String userKey) { return new LinkedHashSet<String>(mapper.listRoleCodes(userKey)); }

    @Override
    public Set<String> listPermissionCodes(String userKey) { return new LinkedHashSet<String>(mapper.listPermissionCodes(userKey)); }

    @Override
    public List<RbacFieldGrant> listFieldGrants(String userKey) { return mapper.listFieldGrants(userKey); }

    @Override
    public Set<String> listConfiguredFieldKeys(String resourceKey) { return new LinkedHashSet<String>(mapper.listConfiguredFieldKeys(resourceKey)); }

    @Override
    public Map<String, String> listConfiguredFieldSecurityRequirements(String resourceKey) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        for (Map<String, String> row : mapper.listConfiguredFieldSecurityRequirements(resourceKey)) {
            result.put(row.get("fieldKey"), row.get("securityRequirement"));
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> listRoles() { return mapper.listRoles(); }

    @Override
    public List<Map<String, Object>> listGroups() { return mapper.listGroups(); }

    @Override
    public List<Map<String, Object>> listPermissions() { return mapper.listPermissions(); }

    @Override
    public List<Map<String, Object>> listFieldPermissions() { return mapper.listFieldPermissions(); }

    @Override
    public List<Map<String, Object>> listUsers() { return mapper.listUsers(); }

    @Override
    public List<Map<String, Object>> listUsersWithAssociations() { return mapper.listUsersWithAssociations(); }

    @Override
    public String findRoleCodeById(Long roleId) { return mapper.findRoleCodeById(roleId); }

    @Override
    public List<String> listRolePermissionCodesByRoleCode(String roleCode) { return mapper.listRolePermissionCodesByRoleCode(roleCode); }

    @Override
    public List<String> listRoleFieldPermissionCodesByRoleCode(String roleCode) { return mapper.listRoleFieldPermissionCodesByRoleCode(roleCode); }

    @Override
    public List<RbacFieldGrant> listFieldGrantsByRoleCodes(Set<String> roleCodes) {
        return roleCodes == null || roleCodes.isEmpty() ? Collections.<RbacFieldGrant>emptyList() : mapper.listFieldGrantsByRoleCodes(roleCodes);
    }

    @Override
    public List<Map<String, Object>> listRoleUsers(Long roleId) { return mapper.listRoleUsers(roleId); }

    @Override
    public List<Map<String, Object>> listRoleSyncItems() { return mapper.listRoleSyncItems(); }

    @Override
    public List<Map<String, Object>> listRoleMembershipSyncItems() { return mapper.listRoleMembershipSyncItems(); }

    @Override
    public List<Map<String, Object>> listGroupSyncItems() { return mapper.listGroupSyncItems(); }

    @Override
    public List<Map<String, Object>> listGroupMembershipSyncItems() { return mapper.listGroupMembershipSyncItems(); }

    @Override
    public Long createRole(String code, String name, String description) {
        mapper.insertRole(code, name, description);
        return mapper.findRoleIdByCode(code);
    }

    @Override
    public int updateRole(Long roleId, String name, String description) { return mapper.updateRole(roleId, name, description); }

    @Override
    public int deleteRole(Long roleId) { return mapper.deleteRole(roleId); }

    @Override
    public Long createGroup(String code, String name, String description) {
        mapper.insertGroup(code, name, description);
        return mapper.findGroupIdByCode(code);
    }

    @Override
    public int updateGroup(Long groupId, String name, String description) { return mapper.updateGroup(groupId, name, description); }

    @Override
    public int deleteGroup(Long groupId) { return mapper.deleteGroup(groupId); }

    @Override
    public Long findGroupIdByCode(String code) { return mapper.findGroupIdByCode(code); }

    @Override
    public int countRoleUsers(Long roleId) { return mapper.countRoleUsers(roleId); }

    @Override
    public int countGroupUsers(Long groupId) { return mapper.countGroupUsers(groupId); }

    @Override
    public int countRolePermissionsByRoleCode(String roleCode) { return mapper.countRolePermissionsByRoleCode(roleCode); }

    @Override
    public int countRoleFieldPermissionsByRoleCode(String roleCode) { return mapper.countRoleFieldPermissionsByRoleCode(roleCode); }

    @Override
    public int countPermissionsByCodes(List<String> permissionCodes) {
        return permissionCodes == null || permissionCodes.isEmpty() ? 0 : mapper.countPermissionsByCodes(permissionCodes);
    }

    @Override
    public int countFieldPermissionsByCodes(List<String> fieldPermissionCodes) {
        return fieldPermissionCodes == null || fieldPermissionCodes.isEmpty() ? 0 : mapper.countFieldPermissionsByCodes(fieldPermissionCodes);
    }

    @Override
    public void replaceRolePermissionsByRoleCode(String roleCode, List<String> permissionCodes) {
        mapper.deleteRolePermissionsByRoleCode(roleCode);
        for (String code : safeList(permissionCodes)) {
            mapper.insertRolePermissionByRoleCode(roleCode, code);
        }
    }

    @Override
    public void replaceRoleFieldPermissionsByRoleCode(String roleCode, List<String> fieldPermissionCodes) {
        mapper.deleteRoleFieldPermissionsByRoleCode(roleCode);
        for (String code : safeList(fieldPermissionCodes)) {
            mapper.insertRoleFieldPermissionByRoleCode(roleCode, code);
        }
    }

    @Override
    public List<String> listGroupPermissionCodesByGroupCode(String groupCode) { return mapper.listGroupPermissionCodesByGroupCode(groupCode); }
    @Override
    public List<String> listGroupFieldPermissionCodesByGroupCode(String groupCode) { return mapper.listGroupFieldPermissionCodesByGroupCode(groupCode); }
    @Override
    public int countGroupPermissionsByGroupCode(String groupCode) { return mapper.countGroupPermissionsByGroupCode(groupCode); }
    @Override
    public int countGroupFieldPermissionsByGroupCode(String groupCode) { return mapper.countGroupFieldPermissionsByGroupCode(groupCode); }
    @Override
    public void replaceGroupPermissionsByGroupCode(String groupCode, List<String> permissionCodes) {
        mapper.deleteGroupPermissionsByGroupCode(groupCode);
        for (String code : safeList(permissionCodes)) {
            mapper.insertGroupPermissionByGroupCode(groupCode, code);
        }
    }
    @Override
    public void replaceGroupFieldPermissionsByGroupCode(String groupCode, List<String> fieldPermissionCodes) {
        mapper.deleteGroupFieldPermissionsByGroupCode(groupCode);
        for (String code : safeList(fieldPermissionCodes)) {
            mapper.insertGroupFieldPermissionByGroupCode(groupCode, code);
        }
    }

    @Override
    public void addRoleUser(Long roleId, String userKey, String displayName) {
        mapper.upsertUser(userKey, displayName);
        Long userId = mapper.findUserIdByKey(userKey);
        mapper.insertUserRole(userId, roleId);
    }

    @Override
    public void removeRoleUser(Long roleId, String userKey) { mapper.deleteUserRole(roleId, userKey); }

    @Override
    public List<Map<String, Object>> listGroupUsers(Long groupId) { return mapper.listGroupUsers(groupId); }

    @Override
    public List<Map<String, Object>> listUserMemberships(String userKey) { return mapper.listUserMemberships(userKey); }

    @Override
    public void addGroupUser(Long groupId, String userKey, String displayName) {
        mapper.upsertUser(userKey, displayName);
        Long userId = mapper.findUserIdByKey(userKey);
        mapper.insertUserGroup(userId, groupId);
    }

    @Override
    public void removeGroupUser(Long groupId, String userKey) { mapper.deleteUserGroup(groupId, userKey); }

    @Override
    public void createFieldPermission(String code, String resourceKey, String fieldKey, String accessLevel, String name, String description) {
        mapper.insertFieldPermission(code, resourceKey, fieldKey, accessLevel, name, description);
    }

    @Override
    public void deleteFieldPermission(String resourceKey, String fieldKey) {
        mapper.deleteFieldPermissionBindings(resourceKey, fieldKey);
        mapper.deleteGroupFieldPermissionBindings(resourceKey, fieldKey);
        mapper.deleteFieldPermissions(resourceKey, fieldKey);
    }

    private List<String> safeList(List<String> values) {
        return values == null ? Collections.<String>emptyList() : values;
    }
}