package com.fcm.authzcraft.demo.oa.domain.repository;

import com.fcm.authzcraft.demo.oa.domain.model.RbacFieldGrant;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface RbacRepository {
    Map<String, Object> findActiveUser(String userKey);
    void upsertUserProjection(String userKey, String displayName, int status);
    Set<String> listRoleCodes(String userKey);
    Set<String> listPermissionCodes(String userKey);
    List<RbacFieldGrant> listFieldGrants(String userKey);
    Set<String> listConfiguredFieldKeys(String resourceKey);
    Map<String, String> listConfiguredFieldSecurityRequirements(String resourceKey);

    List<Map<String, Object>> listRoles();
    List<Map<String, Object>> listGroups();
    List<Map<String, Object>> listPermissions();
    List<Map<String, Object>> listFieldPermissions();
    List<Map<String, Object>> listUsers();
    List<Map<String, Object>> listUsersWithAssociations();
    String findRoleCodeById(Long roleId);
    List<String> listRolePermissionCodesByRoleCode(String roleCode);
    List<String> listRoleFieldPermissionCodesByRoleCode(String roleCode);
    List<RbacFieldGrant> listFieldGrantsByRoleCodes(Set<String> roleCodes);
    List<Map<String, Object>> listRoleUsers(Long roleId);
    List<Map<String, Object>> listRoleSyncItems();
    List<Map<String, Object>> listRoleMembershipSyncItems();
    List<Map<String, Object>> listGroupSyncItems();
    List<Map<String, Object>> listGroupMembershipSyncItems();
    List<Map<String, Object>> listGroupUsers(Long groupId);
    List<Map<String, Object>> listUserMemberships(String userKey);

    Long createRole(String code, String name, String description);
    int updateRole(Long roleId, String name, String description);
    int deleteRole(Long roleId);
    Long createGroup(String code, String name, String description);
    int updateGroup(Long groupId, String name, String description);
    int deleteGroup(Long groupId);
    Long findGroupIdByCode(String code);
    int countRoleUsers(Long roleId);
    int countGroupUsers(Long groupId);
    int countRolePermissionsByRoleCode(String roleCode);
    int countRoleFieldPermissionsByRoleCode(String roleCode);
    int countPermissionsByCodes(List<String> permissionCodes);
    int countFieldPermissionsByCodes(List<String> fieldPermissionCodes);
    void replaceRolePermissionsByRoleCode(String roleCode, List<String> permissionCodes);
    void replaceRoleFieldPermissionsByRoleCode(String roleCode, List<String> fieldPermissionCodes);
    List<String> listGroupPermissionCodesByGroupCode(String groupCode);
    List<String> listGroupFieldPermissionCodesByGroupCode(String groupCode);
    int countGroupPermissionsByGroupCode(String groupCode);
    int countGroupFieldPermissionsByGroupCode(String groupCode);
    void replaceGroupPermissionsByGroupCode(String groupCode, List<String> permissionCodes);
    void replaceGroupFieldPermissionsByGroupCode(String groupCode, List<String> fieldPermissionCodes);
    void addRoleUser(Long roleId, String userKey, String displayName);
    void removeRoleUser(Long roleId, String userKey);
    void addGroupUser(Long groupId, String userKey, String displayName);
    void removeGroupUser(Long groupId, String userKey);
    void createFieldPermission(String code, String resourceKey, String fieldKey, String accessLevel, String name, String description);
    void deleteFieldPermission(String resourceKey, String fieldKey);
}