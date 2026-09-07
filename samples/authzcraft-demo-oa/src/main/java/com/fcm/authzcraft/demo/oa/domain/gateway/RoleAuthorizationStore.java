package com.fcm.authzcraft.demo.oa.domain.gateway;

import com.fcm.authzcraft.demo.oa.domain.model.RbacFieldGrant;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface RoleAuthorizationStore {
    List<Map<String, Object>> listPermissions();
    List<Map<String, Object>> listFieldPermissions();
    List<String> listRolePermissionCodesByRoleCode(String roleCode);
    List<String> listRoleFieldPermissionCodesByRoleCode(String roleCode);
    List<RbacFieldGrant> listFieldGrantsByRoleCodes(Set<String> roleCodes);
    Set<String> listConfiguredFieldKeys(String resourceKey);
    Map<String, String> listConfiguredFieldSecurityRequirements(String resourceKey);
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
    void createFieldPermission(String code, String resourceKey, String fieldKey, String accessLevel, String name, String description);
    void deleteFieldPermission(String resourceKey, String fieldKey);
}