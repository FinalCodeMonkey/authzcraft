package com.fcm.authzcraft.demo.oa.domain.gateway;

import java.util.Map;

public interface PrincipalAdminGateway {
    Map<String, Object> searchApplicationRbacStatus();
    Map<String, Object> syncApplicationRbac(boolean dryRun);
    Map<String, Object> createRole(String code, String name, String description);
    Map<String, Object> updateRole(String roleId, String name, String description);
    void deleteRole(String roleId);
    Map<String, Object> createGroup(String code, String name, String description);
    Map<String, Object> updateGroup(String groupId, String name, String description);
    void deleteGroup(String groupId);
    void addRoleUser(String roleId, String userKey, String displayName);
    void removeRoleUser(String roleId, String userKey);
    void addGroupUser(String groupId, String userKey, String displayName);
    void removeGroupUser(String groupId, String userKey);
}