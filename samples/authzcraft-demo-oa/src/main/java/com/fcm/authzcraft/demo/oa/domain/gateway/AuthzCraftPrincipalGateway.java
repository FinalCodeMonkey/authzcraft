package com.fcm.authzcraft.demo.oa.domain.gateway;

import java.util.List;
import java.util.Map;

public interface AuthzCraftPrincipalGateway {
    Map<String, Object> findActiveUserProjection(String userKey);
    List<Map<String, Object>> searchUsers();
    List<Map<String, Object>> searchRoles();
    List<Map<String, Object>> searchGroups();
    List<Map<String, Object>> searchMemberships(String containerKind, String containerCode, String userKey);
    Map<String, Object> upsertRole(String roleCode, String roleName, String description, String lifecycleState);
    Map<String, Object> retireRole(String roleCode);
    Map<String, Object> upsertGroup(String groupCode, String groupName, String description, String lifecycleState);
    Map<String, Object> retireGroup(String groupCode);
    Map<String, Object> upsertMembership(String containerKind, String containerCode, String userKey);
    void closeMembership(String containerKind, String containerCode, String userKey);
    Map<String, Object> syncApplicationRbac(List<Map<String, Object>> roles, List<Map<String, Object>> groups, List<Map<String, Object>> memberships);
    Map<String, Object> searchApplicationRbacStatus(List<Map<String, Object>> roles, List<Map<String, Object>> groups, List<Map<String, Object>> memberships);
}