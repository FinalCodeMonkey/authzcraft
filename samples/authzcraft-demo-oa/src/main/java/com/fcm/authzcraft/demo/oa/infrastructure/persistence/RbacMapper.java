package com.fcm.authzcraft.demo.oa.infrastructure.persistence;

import com.fcm.authzcraft.demo.oa.domain.model.RbacFieldGrant;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

public interface RbacMapper {

    @Select("SELECT CAST(id AS CHAR) AS id, user_key AS userKey, display_name AS displayName FROM demo_oa_users WHERE user_key = #{userKey} AND status = 1")
    Map<String, Object> findActiveUser(@Param("userKey") String userKey);

    @Insert("INSERT INTO demo_oa_users (user_key, display_name, status) VALUES (#{userKey}, #{displayName}, #{status}) ON DUPLICATE KEY UPDATE display_name = VALUES(display_name), status = VALUES(status)")
    int upsertUserProjection(@Param("userKey") String userKey, @Param("displayName") String displayName, @Param("status") int status);

    @Select("SELECT r.code FROM demo_oa_roles r JOIN demo_oa_user_roles ur ON ur.role_id = r.id JOIN demo_oa_users u ON u.id = ur.user_id WHERE u.user_key = #{userKey} AND u.status = 1 ORDER BY r.code")
    List<String> listRoleCodes(@Param("userKey") String userKey);

    @Select("SELECT DISTINCT p.code FROM demo_oa_permissions p JOIN demo_oa_role_permission_bindings rp ON rp.permission_id = p.id JOIN demo_oa_roles r ON r.code = rp.role_code JOIN demo_oa_user_roles ur ON ur.role_id = r.id JOIN demo_oa_users u ON u.id = ur.user_id WHERE u.user_key = #{userKey} AND u.status = 1 ORDER BY p.code")
    List<String> listPermissionCodes(@Param("userKey") String userKey);

    @Select("SELECT DISTINCT fp.code, fp.resource_key AS resourceKey, fp.field_key AS fieldKey, fp.access_level AS accessLevel, fp.name FROM demo_oa_field_permissions fp JOIN demo_oa_role_field_permission_bindings rfp ON rfp.field_permission_id = fp.id JOIN demo_oa_roles r ON r.code = rfp.role_code JOIN demo_oa_user_roles ur ON ur.role_id = r.id JOIN demo_oa_users u ON u.id = ur.user_id WHERE u.user_key = #{userKey} AND u.status = 1 ORDER BY fp.resource_key, fp.field_key, fp.access_level")
    List<RbacFieldGrant> listFieldGrants(@Param("userKey") String userKey);

    @Select("SELECT DISTINCT field_key FROM demo_oa_field_permissions WHERE resource_key = #{resourceKey}")
    List<String> listConfiguredFieldKeys(@Param("resourceKey") String resourceKey);

    @Select("SELECT DISTINCT field_key AS fieldKey, security_requirement AS securityRequirement FROM demo_oa_field_permissions WHERE resource_key = #{resourceKey}")
    List<Map<String, String>> listConfiguredFieldSecurityRequirements(@Param("resourceKey") String resourceKey);

    @Select("SELECT CAST(id AS CHAR) AS id, code, name, description FROM demo_oa_roles ORDER BY id")
    List<Map<String, Object>> listRoles();

    @Select("SELECT CAST(id AS CHAR) AS id, code, name, description FROM demo_oa_permissions ORDER BY id")
    List<Map<String, Object>> listPermissions();

    @Select("SELECT CAST(id AS CHAR) AS id, code, resource_key AS resourceKey, field_key AS fieldKey, access_level AS accessLevel, name, description FROM demo_oa_field_permissions ORDER BY resource_key, field_key, access_level")
    List<Map<String, Object>> listFieldPermissions();

    @Select("SELECT CAST(id AS CHAR) AS id, user_key AS userKey, display_name AS displayName, status FROM demo_oa_users ORDER BY user_key")
    List<Map<String, Object>> listUsers();

    @Select("SELECT CAST(u.id AS CHAR) AS id, u.user_key AS userKey, u.display_name AS displayName, " +
            "COALESCE(role_codes.roleCodes, '') AS roleCodes, " +
            "COALESCE(perm_codes.permissionCodes, '') AS permissionCodes " +
            "FROM demo_oa_users u " +
            "LEFT JOIN (SELECT ur.user_id AS userId, GROUP_CONCAT(r.code ORDER BY r.code SEPARATOR ',') AS roleCodes " +
            "            FROM demo_oa_user_roles ur JOIN demo_oa_roles r ON r.id = ur.role_id GROUP BY ur.user_id) role_codes ON role_codes.userId = u.id " +
            "LEFT JOIN (SELECT ur.user_id AS userId, GROUP_CONCAT(DISTINCT p.code ORDER BY p.code SEPARATOR ',') AS permissionCodes " +
            "            FROM demo_oa_user_roles ur " +
            "            JOIN demo_oa_roles r ON r.id = ur.role_id " +
            "            JOIN demo_oa_role_permission_bindings rp ON rp.role_code = r.code " +
            "            JOIN demo_oa_permissions p ON p.id = rp.permission_id GROUP BY ur.user_id) perm_codes ON perm_codes.userId = u.id " +
            "WHERE u.status = 1 AND (role_codes.roleCodes IS NOT NULL OR perm_codes.permissionCodes IS NOT NULL) " +
            "ORDER BY u.user_key")
    List<Map<String, Object>> listUsersWithAssociations();

    @Select("SELECT code FROM demo_oa_roles WHERE id = #{roleId}")
    String findRoleCodeById(@Param("roleId") Long roleId);

    @Select("SELECT p.code FROM demo_oa_permissions p JOIN demo_oa_role_permission_bindings rp ON rp.permission_id = p.id WHERE rp.role_code = #{roleCode} ORDER BY p.code")
    List<String> listRolePermissionCodesByRoleCode(@Param("roleCode") String roleCode);

    @Select("SELECT fp.code FROM demo_oa_field_permissions fp JOIN demo_oa_role_field_permission_bindings rfp ON rfp.field_permission_id = fp.id WHERE rfp.role_code = #{roleCode} ORDER BY fp.code")
    List<String> listRoleFieldPermissionCodesByRoleCode(@Param("roleCode") String roleCode);

    @Select({"<script>", "SELECT DISTINCT fp.code, fp.resource_key AS resourceKey, fp.field_key AS fieldKey, fp.access_level AS accessLevel, fp.name FROM demo_oa_field_permissions fp JOIN demo_oa_role_field_permission_bindings rfp ON rfp.field_permission_id = fp.id WHERE rfp.role_code IN", "<foreach collection='roleCodes' item='roleCode' open='(' separator=',' close=')'>#{roleCode}</foreach>", "ORDER BY fp.resource_key, fp.field_key, fp.access_level", "</script>"})
    List<RbacFieldGrant> listFieldGrantsByRoleCodes(@Param("roleCodes") java.util.Collection<String> roleCodes);

    @Select("SELECT CAST(u.id AS CHAR) AS id, u.user_key AS userKey, u.display_name AS displayName FROM demo_oa_users u JOIN demo_oa_user_roles ur ON ur.user_id = u.id WHERE ur.role_id = #{roleId} ORDER BY u.user_key")
    List<Map<String, Object>> listRoleUsers(@Param("roleId") Long roleId);

    @Select("SELECT code AS roleCode, name AS roleName, description, 'ACTIVE' AS lifecycleState FROM demo_oa_roles ORDER BY code")
    List<Map<String, Object>> listRoleSyncItems();

    @Select("SELECT r.code AS roleCode, u.user_key AS userKey FROM demo_oa_roles r JOIN demo_oa_user_roles ur ON ur.role_id = r.id JOIN demo_oa_users u ON u.id = ur.user_id WHERE u.status = 1 ORDER BY r.code, u.user_key")
    List<Map<String, Object>> listRoleMembershipSyncItems();

    @Insert("INSERT INTO demo_oa_roles (code, name, description) VALUES (#{code}, #{name}, #{description})")
    int insertRole(@Param("code") String code, @Param("name") String name, @Param("description") String description);

    @Select("SELECT id FROM demo_oa_roles WHERE code = #{code}")
    Long findRoleIdByCode(@Param("code") String code);

    @Update("UPDATE demo_oa_roles SET name = #{name}, description = #{description} WHERE id = #{roleId}")
    int updateRole(@Param("roleId") Long roleId, @Param("name") String name, @Param("description") String description);

    @Delete("DELETE FROM demo_oa_roles WHERE id = #{roleId}")
    int deleteRole(@Param("roleId") Long roleId);

    @Select("SELECT COUNT(*) FROM demo_oa_user_roles WHERE role_id = #{roleId}")
    int countRoleUsers(@Param("roleId") Long roleId);

    @Select("SELECT COUNT(*) FROM demo_oa_role_permission_bindings WHERE role_code = #{roleCode}")
    int countRolePermissionsByRoleCode(@Param("roleCode") String roleCode);

    @Select("SELECT COUNT(*) FROM demo_oa_role_field_permission_bindings WHERE role_code = #{roleCode}")
    int countRoleFieldPermissionsByRoleCode(@Param("roleCode") String roleCode);

    @Select({"<script>", "SELECT COUNT(*) FROM demo_oa_permissions WHERE code IN", "<foreach collection='codes' item='code' open='(' separator=',' close=')'>#{code}</foreach>", "</script>"})
    int countPermissionsByCodes(@Param("codes") List<String> codes);

    @Select({"<script>", "SELECT COUNT(*) FROM demo_oa_field_permissions WHERE code IN", "<foreach collection='codes' item='code' open='(' separator=',' close=')'>#{code}</foreach>", "</script>"})
    int countFieldPermissionsByCodes(@Param("codes") List<String> codes);

    @Delete("DELETE FROM demo_oa_role_permission_bindings WHERE role_code = #{roleCode}")
    int deleteRolePermissionsByRoleCode(@Param("roleCode") String roleCode);

    @Insert("INSERT IGNORE INTO demo_oa_role_permission_bindings (role_code, permission_id) SELECT #{roleCode}, p.id FROM demo_oa_permissions p WHERE p.code = #{permissionCode}")
    int insertRolePermissionByRoleCode(@Param("roleCode") String roleCode, @Param("permissionCode") String permissionCode);

    @Delete("DELETE FROM demo_oa_role_field_permission_bindings WHERE role_code = #{roleCode}")
    int deleteRoleFieldPermissionsByRoleCode(@Param("roleCode") String roleCode);

    @Insert("INSERT IGNORE INTO demo_oa_role_field_permission_bindings (role_code, field_permission_id) SELECT #{roleCode}, fp.id FROM demo_oa_field_permissions fp WHERE fp.code = #{fieldPermissionCode}")
    int insertRoleFieldPermissionByRoleCode(@Param("roleCode") String roleCode, @Param("fieldPermissionCode") String fieldPermissionCode);

    // Group permission bindings
    @Select("SELECT p.code FROM demo_oa_permissions p JOIN demo_oa_group_permission_bindings gpb ON gpb.permission_id = p.id WHERE gpb.group_code = #{groupCode} ORDER BY p.code")
    List<String> listGroupPermissionCodesByGroupCode(@Param("groupCode") String groupCode);

    @Select("SELECT fp.code FROM demo_oa_field_permissions fp JOIN demo_oa_group_field_permission_bindings gfpb ON gfpb.field_permission_id = fp.id WHERE gfpb.group_code = #{groupCode} ORDER BY fp.code")
    List<String> listGroupFieldPermissionCodesByGroupCode(@Param("groupCode") String groupCode);

    @Select("SELECT COUNT(*) FROM demo_oa_group_permission_bindings WHERE group_code = #{groupCode}")
    int countGroupPermissionsByGroupCode(@Param("groupCode") String groupCode);

    @Select("SELECT COUNT(*) FROM demo_oa_group_field_permission_bindings WHERE group_code = #{groupCode}")
    int countGroupFieldPermissionsByGroupCode(@Param("groupCode") String groupCode);

    @Delete("DELETE FROM demo_oa_group_permission_bindings WHERE group_code = #{groupCode}")
    int deleteGroupPermissionsByGroupCode(@Param("groupCode") String groupCode);

    @Insert("INSERT IGNORE INTO demo_oa_group_permission_bindings (group_code, permission_id) SELECT #{groupCode}, p.id FROM demo_oa_permissions p WHERE p.code = #{permissionCode}")
    int insertGroupPermissionByGroupCode(@Param("groupCode") String groupCode, @Param("permissionCode") String permissionCode);

    @Delete("DELETE FROM demo_oa_group_field_permission_bindings WHERE group_code = #{groupCode}")
    int deleteGroupFieldPermissionsByGroupCode(@Param("groupCode") String groupCode);

    @Insert("INSERT IGNORE INTO demo_oa_group_field_permission_bindings (group_code, field_permission_id) SELECT #{groupCode}, fp.id FROM demo_oa_field_permissions fp WHERE fp.code = #{fieldPermissionCode}")
    int insertGroupFieldPermissionByGroupCode(@Param("groupCode") String groupCode, @Param("fieldPermissionCode") String fieldPermissionCode);

    @Insert("INSERT INTO demo_oa_users (user_key, display_name, status) VALUES (#{userKey}, #{displayName}, 1) ON DUPLICATE KEY UPDATE display_name = COALESCE(VALUES(display_name), display_name), status = 1")
    int upsertUser(@Param("userKey") String userKey, @Param("displayName") String displayName);

    @Select("SELECT id FROM demo_oa_users WHERE user_key = #{userKey}")
    Long findUserIdByKey(@Param("userKey") String userKey);

    @Insert("INSERT IGNORE INTO demo_oa_user_roles (user_id, role_id) VALUES (#{userId}, #{roleId})")
    int insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    @Delete("DELETE ur FROM demo_oa_user_roles ur JOIN demo_oa_users u ON u.id = ur.user_id WHERE ur.role_id = #{roleId} AND u.user_key = #{userKey}")
    int deleteUserRole(@Param("roleId") Long roleId, @Param("userKey") String userKey);

    @Select("SELECT CAST(id AS CHAR) AS id, code, name, description FROM demo_oa_groups ORDER BY id")
    List<Map<String, Object>> listGroups();

    @Insert("INSERT INTO demo_oa_groups (code, name, description) VALUES (#{code}, #{name}, #{description})")
    int insertGroup(@Param("code") String code, @Param("name") String name, @Param("description") String description);

    @Select("SELECT id FROM demo_oa_groups WHERE code = #{code}")
    Long findGroupIdByCode(@Param("code") String code);

    @Update("UPDATE demo_oa_groups SET name = #{name}, description = #{description} WHERE id = #{groupId}")
    int updateGroup(@Param("groupId") Long groupId, @Param("name") String name, @Param("description") String description);

    @Delete("DELETE FROM demo_oa_groups WHERE id = #{groupId}")
    int deleteGroup(@Param("groupId") Long groupId);

    @Select("SELECT COUNT(*) FROM demo_oa_user_groups WHERE group_id = #{groupId}")
    int countGroupUsers(@Param("groupId") Long groupId);

    @Select("SELECT CAST(u.id AS CHAR) AS id, u.user_key AS userKey, u.display_name AS displayName FROM demo_oa_users u JOIN demo_oa_user_groups ug ON ug.user_id = u.id WHERE ug.group_id = #{groupId} ORDER BY u.user_key")
    List<Map<String, Object>> listGroupUsers(@Param("groupId") Long groupId);

    @Insert("INSERT IGNORE INTO demo_oa_user_groups (user_id, group_id) VALUES (#{userId}, #{groupId})")
    int insertUserGroup(@Param("userId") Long userId, @Param("groupId") Long groupId);

    @Delete("DELETE ug FROM demo_oa_user_groups ug JOIN demo_oa_users u ON u.id = ug.user_id WHERE ug.group_id = #{groupId} AND u.user_key = #{userKey}")
    int deleteUserGroup(@Param("groupId") Long groupId, @Param("userKey") String userKey);

    @Select("SELECT 'ROLE' AS containerKind, r.code AS containerCode, u.user_key AS userKey, CAST(ur.role_id AS CHAR) AS membershipId, 'ACTIVE' AS lifecycleState FROM demo_oa_users u JOIN demo_oa_user_roles ur ON ur.user_id = u.id JOIN demo_oa_roles r ON r.id = ur.role_id WHERE u.user_key = #{userKey} UNION ALL SELECT 'GROUP' AS containerKind, g.code AS containerCode, u.user_key AS userKey, CAST(ug.group_id AS CHAR) AS membershipId, 'ACTIVE' AS lifecycleState FROM demo_oa_users u JOIN demo_oa_user_groups ug ON ug.user_id = u.id JOIN demo_oa_groups g ON g.id = ug.group_id WHERE u.user_key = #{userKey} ORDER BY containerKind, containerCode")
    List<Map<String, Object>> listUserMemberships(@Param("userKey") String userKey);

    @Select("SELECT code AS groupCode, name AS groupName, description, 'ACTIVE' AS lifecycleState FROM demo_oa_groups ORDER BY code")
    List<Map<String, Object>> listGroupSyncItems();

    @Select("SELECT g.code AS groupCode, u.user_key AS userKey FROM demo_oa_groups g JOIN demo_oa_user_groups ug ON ug.group_id = g.id JOIN demo_oa_users u ON u.id = ug.user_id WHERE u.status = 1 ORDER BY g.code, u.user_key")
    List<Map<String, Object>> listGroupMembershipSyncItems();

    @Insert("INSERT INTO demo_oa_field_permissions (code, resource_key, field_key, access_level, name, description) VALUES (#{code}, #{resourceKey}, #{fieldKey}, #{accessLevel}, #{name}, #{description})")
    int insertFieldPermission(@Param("code") String code, @Param("resourceKey") String resourceKey, @Param("fieldKey") String fieldKey, @Param("accessLevel") String accessLevel, @Param("name") String name, @Param("description") String description);

    @Delete("DELETE FROM demo_oa_role_field_permission_bindings WHERE field_permission_id IN (SELECT id FROM demo_oa_field_permissions WHERE resource_key = #{resourceKey} AND field_key = #{fieldKey})")
    int deleteFieldPermissionBindings(@Param("resourceKey") String resourceKey, @Param("fieldKey") String fieldKey);

    @Delete("DELETE FROM demo_oa_group_field_permission_bindings WHERE field_permission_id IN (SELECT id FROM demo_oa_field_permissions WHERE resource_key = #{resourceKey} AND field_key = #{fieldKey})")
    int deleteGroupFieldPermissionBindings(@Param("resourceKey") String resourceKey, @Param("fieldKey") String fieldKey);

    @Delete("DELETE FROM demo_oa_field_permissions WHERE resource_key = #{resourceKey} AND field_key = #{fieldKey}")
    int deleteFieldPermissions(@Param("resourceKey") String resourceKey, @Param("fieldKey") String fieldKey);
}