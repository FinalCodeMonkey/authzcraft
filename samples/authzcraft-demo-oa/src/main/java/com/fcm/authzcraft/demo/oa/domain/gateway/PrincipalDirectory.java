package com.fcm.authzcraft.demo.oa.domain.gateway;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface PrincipalDirectory {
    Map<String, Object> findActiveUserProjection(String userKey);
    Set<String> listRoleCodes(String userKey);
    List<Map<String, Object>> listRoles();
    List<Map<String, Object>> listGroups();
    List<Map<String, Object>> listUsers();
    List<Map<String, Object>> listRoleUsers(String roleId);
    List<Map<String, Object>> listGroupUsers(String groupId);
    List<Map<String, Object>> listUserMemberships(String userKey);
    int countRoleUsers(String roleId);
    int countGroupUsers(String groupId);
    String roleCode(String roleId);
    String groupCode(String groupId);
    boolean supportsDataPermissionTemplates();
}