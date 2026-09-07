package com.fcm.authzcraft.demo.oa.interfaces.web;

import com.fcm.authzcraft.common.web.ApiResponse;
import com.fcm.authzcraft.demo.oa.application.service.RbacApplicationService;
import com.fcm.authzcraft.demo.oa.interfaces.security.CurrentUserContext;
import com.fcm.authzcraft.demo.oa.interfaces.security.RequirePermission;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/authzcraft-demo-oa/api/v1")
public class RbacController {

    private final RbacApplicationService rbacApplicationService;

    public RbacController(RbacApplicationService rbacApplicationService) {
        this.rbacApplicationService = rbacApplicationService;
    }

    @PostMapping("/auth/me/permissions")
    public ApiResponse<Map<String, Object>> currentPermissions() {
        return ApiResponse.success(rbacApplicationService.currentPermissions(CurrentUserContext.get()));
    }

    @RequirePermission("demo-oa:rbac:read")
    @PostMapping("/rbac/roles/search")
    public ApiResponse<List<Map<String, Object>>> listRoles() {
        return ApiResponse.success(rbacApplicationService.listRoles());
    }

    @RequirePermission("demo-oa:rbac:read")
    @PostMapping("/rbac/groups/search")
    public ApiResponse<List<Map<String, Object>>> listGroups() {
        return ApiResponse.success(rbacApplicationService.listGroups());
    }

    @RequirePermission("demo-oa:rbac:manage")
    @PostMapping("/rbac/groups")
    public ApiResponse<Map<String, Object>> createGroup(@RequestBody Map<String, Object> request) {
        return ApiResponse.success(rbacApplicationService.createGroup(request));
    }

    @RequirePermission("demo-oa:rbac:manage")
    @PostMapping("/rbac/groups/{id}/update")
    public ApiResponse<Map<String, Object>> updateGroup(@PathVariable String id, @RequestBody Map<String, Object> request) {
        return ApiResponse.success(rbacApplicationService.updateGroup(id, request));
    }

    @RequirePermission("demo-oa:rbac:manage")
    @PostMapping("/rbac/groups/{id}/delete-impact")
    public ApiResponse<Map<String, Object>> deleteGroupImpact(@PathVariable String id) {
        return ApiResponse.success(rbacApplicationService.deleteGroupImpact(id));
    }

    @RequirePermission("demo-oa:rbac:manage")
    @PostMapping("/rbac/groups/{id}/delete")
    public ApiResponse<Void> deleteGroup(@PathVariable String id, @RequestBody(required = false) Map<String, Object> request) {
        rbacApplicationService.deleteGroup(id, request);
        return ApiResponse.success();
    }

    @RequirePermission("demo-oa:rbac:manage")
    @PostMapping("/rbac/roles")
    public ApiResponse<Map<String, Object>> createRole(@RequestBody Map<String, Object> request) {
        return ApiResponse.success(rbacApplicationService.createRole(request));
    }

    @RequirePermission("demo-oa:rbac:manage")
    @PostMapping("/rbac/roles/{id}/update")
    public ApiResponse<Map<String, Object>> updateRole(@PathVariable String id, @RequestBody Map<String, Object> request) {
        return ApiResponse.success(rbacApplicationService.updateRole(id, request));
    }

    @RequirePermission("demo-oa:rbac:manage")
    @PostMapping("/rbac/roles/{id}/delete-impact")
    public ApiResponse<Map<String, Object>> deleteRoleImpact(@PathVariable String id) {
        return ApiResponse.success(rbacApplicationService.deleteRoleImpact(id));
    }

    @RequirePermission("demo-oa:rbac:manage")
    @PostMapping("/rbac/roles/{id}/delete")
    public ApiResponse<Void> deleteRole(@PathVariable String id, @RequestBody(required = false) Map<String, Object> request) {
        rbacApplicationService.deleteRole(id, request);
        return ApiResponse.success();
    }

    @RequirePermission("demo-oa:rbac:read")
    @PostMapping("/rbac/permissions/search")
    public ApiResponse<List<Map<String, Object>>> listPermissions() {
        return ApiResponse.success(rbacApplicationService.listPermissions());
    }

    @RequirePermission("demo-oa:rbac:read")
    @PostMapping("/rbac/field-permissions/search")
    public ApiResponse<List<Map<String, Object>>> listFieldPermissions() {
        return ApiResponse.success(rbacApplicationService.listFieldPermissions());
    }

    @RequirePermission("demo-oa:rbac:manage")
    @PostMapping("/rbac/field-permissions/create")
    public ApiResponse<Map<String, Object>> createFieldPermission(@RequestBody Map<String, Object> request) {
        return ApiResponse.success(rbacApplicationService.createFieldPermission(request));
    }

    @RequirePermission("demo-oa:rbac:manage")
    @PostMapping("/rbac/field-permissions/delete")
    public ApiResponse<Void> deleteFieldPermission(@RequestBody Map<String, Object> request) {
        rbacApplicationService.deleteFieldPermission(
                String.valueOf(request.get("resourceKey")),
                String.valueOf(request.get("fieldKey"))
        );
        return ApiResponse.success();
    }

    @RequirePermission("demo-oa:rbac:manage")
    @PostMapping("/rbac/field-permissions/update")
    public ApiResponse<Map<String, Object>> updateFieldPermission(@RequestBody Map<String, Object> request) {
        return ApiResponse.success(rbacApplicationService.updateFieldPermission(request));
    }

    @PostMapping("/rbac/users/search")
    public ApiResponse<List<Map<String, Object>>> listUsers() {
        return ApiResponse.success(rbacApplicationService.listUsers());
    }

    @RequirePermission("demo-oa:rbac:read")
    @PostMapping("/rbac/users/{userKey}/memberships/search")
    public ApiResponse<List<Map<String, Object>>> listUserMemberships(@PathVariable String userKey) {
        return ApiResponse.success(rbacApplicationService.listUserMemberships(userKey));
    }

    @RequirePermission("demo-oa:rbac:read")
    @PostMapping("/rbac/roles/{id}/permissions/search")
    public ApiResponse<List<String>> listRolePermissionCodes(@PathVariable String id) {
        return ApiResponse.success(rbacApplicationService.listRolePermissionCodes(id));
    }

    @RequirePermission("demo-oa:rbac:manage")
    @PostMapping("/rbac/roles/{id}/permissions/replace")
    public ApiResponse<Void> replaceRolePermissions(@PathVariable String id, @RequestBody Map<String, Object> request) {
        rbacApplicationService.replaceRolePermissions(id, request);
        return ApiResponse.success();
    }

    @RequirePermission("demo-oa:rbac:read")
    @PostMapping("/rbac/roles/{id}/field-permissions/search")
    public ApiResponse<List<String>> listRoleFieldPermissionCodes(@PathVariable String id) {
        return ApiResponse.success(rbacApplicationService.listRoleFieldPermissionCodes(id));
    }

    @RequirePermission("demo-oa:rbac:manage")
    @PostMapping("/rbac/roles/{id}/field-permissions/replace")
    public ApiResponse<Void> replaceRoleFieldPermissions(@PathVariable String id, @RequestBody Map<String, Object> request) {
        rbacApplicationService.replaceRoleFieldPermissions(id, request);
        return ApiResponse.success();
    }
    @RequirePermission("demo-oa:rbac:read")
    @PostMapping("/rbac/groups/{id}/permissions/search")
    public ApiResponse<List<String>> listGroupPermissionCodes(@PathVariable String id) {
        return ApiResponse.success(rbacApplicationService.listGroupPermissionCodes(id));
    }

    @RequirePermission("demo-oa:rbac:manage")
    @PostMapping("/rbac/groups/{id}/permissions/replace")
    public ApiResponse<Void> replaceGroupPermissions(@PathVariable String id, @RequestBody Map<String, Object> request) {
        rbacApplicationService.replaceGroupPermissions(id, request);
        return ApiResponse.success();
    }

    @RequirePermission("demo-oa:rbac:read")
    @PostMapping("/rbac/groups/{id}/field-permissions/search")
    public ApiResponse<List<String>> listGroupFieldPermissionCodes(@PathVariable String id) {
        return ApiResponse.success(rbacApplicationService.listGroupFieldPermissionCodes(id));
    }

    @RequirePermission("demo-oa:rbac:manage")
    @PostMapping("/rbac/groups/{id}/field-permissions/replace")
    public ApiResponse<Void> replaceGroupFieldPermissions(@PathVariable String id, @RequestBody Map<String, Object> request) {
        rbacApplicationService.replaceGroupFieldPermissions(id, request);
        return ApiResponse.success();
    }
    @RequirePermission("demo-oa:rbac:read")
    @PostMapping("/rbac/roles/{id}/users/search")
    public ApiResponse<List<Map<String, Object>>> listRoleUsers(@PathVariable String id) {
        return ApiResponse.success(rbacApplicationService.listRoleUsers(id));
    }

    @RequirePermission("demo-oa:rbac:read")
    @PostMapping("/rbac/sync/application-rbac/status/search")
    public ApiResponse<Map<String, Object>> searchApplicationRbacStatus() {
        return ApiResponse.success(rbacApplicationService.searchApplicationRbacStatus());
    }

    @RequirePermission("demo-oa:rbac:manage")
    @PostMapping("/rbac/sync/application-rbac")
    public ApiResponse<Map<String, Object>> syncApplicationRbac(@RequestBody(required = false) Map<String, Object> request) {
        return ApiResponse.success(rbacApplicationService.syncApplicationRbac(request));
    }

    @RequirePermission("demo-oa:rbac:manage")
    @PostMapping("/rbac/roles/{id}/users/add")
    public ApiResponse<Void> addRoleUser(@PathVariable String id, @RequestBody Map<String, Object> request) {
        rbacApplicationService.addRoleUser(id, request);
        return ApiResponse.success();
    }

    @RequirePermission("demo-oa:rbac:manage")
    @PostMapping("/rbac/roles/{id}/users/remove")
    public ApiResponse<Void> removeRoleUser(@PathVariable String id, @RequestBody Map<String, Object> request) {
        rbacApplicationService.removeRoleUser(id, request);
        return ApiResponse.success();
    }

    @RequirePermission("demo-oa:rbac:read")
    @PostMapping("/rbac/groups/{id}/users/search")
    public ApiResponse<List<Map<String, Object>>> listGroupUsers(@PathVariable String id) {
        return ApiResponse.success(rbacApplicationService.listGroupUsers(id));
    }

    @RequirePermission("demo-oa:rbac:manage")
    @PostMapping("/rbac/groups/{id}/users/add")
    public ApiResponse<Void> addGroupUser(@PathVariable String id, @RequestBody Map<String, Object> request) {
        rbacApplicationService.addGroupUser(id, request);
        return ApiResponse.success();
    }

    @RequirePermission("demo-oa:rbac:manage")
    @PostMapping("/rbac/groups/{id}/users/remove")
    public ApiResponse<Void> removeGroupUser(@PathVariable String id, @RequestBody Map<String, Object> request) {
        rbacApplicationService.removeGroupUser(id, request);
        return ApiResponse.success();
    }

    @RequirePermission("demo-oa:rbac:manage")
    @PostMapping("/rbac/data-permissions/templates/save")
    public ApiResponse<Map<String, Object>> saveDataPermissionTemplate(@RequestBody Map<String, Object> request) {
        return ApiResponse.success(rbacApplicationService.saveDataPermissionTemplate(CurrentUserContext.get(), request));
    }
}