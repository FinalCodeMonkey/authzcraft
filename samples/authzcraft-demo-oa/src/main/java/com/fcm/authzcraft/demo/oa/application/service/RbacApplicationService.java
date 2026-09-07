package com.fcm.authzcraft.demo.oa.application.service;

import com.fcm.authzcraft.demo.oa.domain.model.CurrentUser;

import java.util.List;
import java.util.Map;

public interface RbacApplicationService {
    CurrentUser resolveCurrentUser(String userKey, String requestKey);
    Map<String, Object> currentPermissions(CurrentUser currentUser);
    List<Map<String, Object>> projectRows(CurrentUser currentUser, String resourceKey, List<Map<String, Object>> rows);

    List<Map<String, Object>> listRoles();
    List<Map<String, Object>> listGroups();
    List<Map<String, Object>> listPermissions();
    List<Map<String, Object>> listFieldPermissions();
    List<Map<String, Object>> listUsers();
    List<String> listRolePermissionCodes(String roleId);
    List<String> listRoleFieldPermissionCodes(String roleId);
    List<Map<String, Object>> listRoleUsers(String roleId);
    Map<String, Object> syncApplicationRbac(Map<String, Object> request);
    Map<String, Object> searchApplicationRbacStatus();
    Map<String, Object> createRole(Map<String, Object> request);
    Map<String, Object> updateRole(String roleId, Map<String, Object> request);
    Map<String, Object> deleteRoleImpact(String roleId);
    void deleteRole(String roleId, Map<String, Object> request);
    Map<String, Object> createGroup(Map<String, Object> request);
    Map<String, Object> updateGroup(String groupId, Map<String, Object> request);
    Map<String, Object> deleteGroupImpact(String groupId);
    void deleteGroup(String groupId, Map<String, Object> request);
    void replaceRolePermissions(String roleId, Map<String, Object> request);
    void replaceRoleFieldPermissions(String roleId, Map<String, Object> request);
    List<String> listGroupPermissionCodes(String groupId);
    List<String> listGroupFieldPermissionCodes(String groupId);
    void replaceGroupPermissions(String groupId, Map<String, Object> request);
    void replaceGroupFieldPermissions(String groupId, Map<String, Object> request);
    void addRoleUser(String roleId, Map<String, Object> request);
    void removeRoleUser(String roleId, Map<String, Object> request);
    List<Map<String, Object>> listGroupUsers(String groupId);
    List<Map<String, Object>> listUserMemberships(String userKey);
    void addGroupUser(String groupId, Map<String, Object> request);
    void removeGroupUser(String groupId, Map<String, Object> request);
    Map<String, Object> saveDataPermissionTemplate(CurrentUser currentUser, Map<String, Object> request);
    Map<String, Object> createFieldPermission(Map<String, Object> request);
    void deleteFieldPermission(String resourceKey, String fieldKey);
    Map<String, Object> updateFieldPermission(Map<String, Object> request);
}