package com.fcm.authzcraft.pip.interfaces.web;

import com.fcm.authzcraft.common.web.ApiResponse;
import com.fcm.authzcraft.pip.application.service.PrincipalRbacCrudService;
import com.fcm.authzcraft.pip.interfaces.dto.PrincipalGroupRequest;
import com.fcm.authzcraft.pip.interfaces.dto.PrincipalGroupSearchRequest;
import com.fcm.authzcraft.pip.interfaces.dto.PrincipalMembershipRequest;
import com.fcm.authzcraft.pip.interfaces.dto.PrincipalMembershipSearchRequest;
import com.fcm.authzcraft.pip.interfaces.dto.PrincipalRoleRequest;
import com.fcm.authzcraft.pip.interfaces.dto.PrincipalRoleSearchRequest;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/authzcraft/api/v1/principals")
public class PrincipalRbacCrudController {
    private final PrincipalRbacCrudService service;

    public PrincipalRbacCrudController(PrincipalRbacCrudService service) {
        this.service = service;
    }

    @PostMapping("/roles")
    public ApiResponse<Map<String, Object>> upsertRole(@RequestBody PrincipalRoleRequest request) {
        return ApiResponse.success(toRoleMap(service.upsertRole(request.getTenantKey(), request.getAppKey(),
                request.getRoleCode(), request.getRoleName(), request.getDescription(), request.getLifecycleState())));
    }

    @PostMapping("/roles/search")
    public ApiResponse<List<Map<String, Object>>> searchRoles(@RequestBody(required = false) PrincipalRoleSearchRequest request) {
        PrincipalRoleSearchRequest actual = request == null ? new PrincipalRoleSearchRequest() : request;
        return ApiResponse.success(service.searchRoles(actual.getTenantKey(), actual.getAppKey(),
                actual.getRoleCode(), actual.getLifecycleState()));
    }

    @PostMapping("/roles/{roleCode}/retire")
    public ApiResponse<Map<String, Object>> retireRole(@PathVariable String roleCode,
                                                        @RequestBody PrincipalRoleRequest request) {
        return ApiResponse.success(toRoleMap(service.retireRole(request.getTenantKey(), request.getAppKey(), roleCode)));
    }

    @PostMapping("/groups")
    public ApiResponse<Map<String, Object>> upsertGroup(@RequestBody PrincipalGroupRequest request) {
        return ApiResponse.success(toGroupMap(service.upsertGroup(request.getTenantKey(), request.getAppKey(),
                request.getGroupCode(), request.getGroupName(), request.getDescription(), request.getLifecycleState())));
    }

    @PostMapping("/groups/search")
    public ApiResponse<List<Map<String, Object>>> searchGroups(@RequestBody(required = false) PrincipalGroupSearchRequest request) {
        PrincipalGroupSearchRequest actual = request == null ? new PrincipalGroupSearchRequest() : request;
        return ApiResponse.success(service.searchGroups(actual.getTenantKey(), actual.getAppKey(),
                actual.getGroupCode(), actual.getLifecycleState()));
    }

    @PostMapping("/groups/{groupCode}/retire")
    public ApiResponse<Map<String, Object>> retireGroup(@PathVariable String groupCode,
                                                         @RequestBody PrincipalGroupRequest request) {
        return ApiResponse.success(toGroupMap(service.retireGroup(request.getTenantKey(), request.getAppKey(), groupCode)));
    }

    @PostMapping("/memberships")
    public ApiResponse<Map<String, Object>> upsertMembership(@RequestBody PrincipalMembershipRequest request) {
        String containerKind = resolveContainerKind(request);
        String containerCode = resolveContainerCode(request, containerKind);
        return ApiResponse.success(toMembershipMap(service.upsertMembership(request.getTenantKey(), request.getAppKey(),
                containerKind, containerCode, request.getUserKey(), request.getValidFrom(), request.getValidUntil())));
    }

    @PostMapping("/memberships/search")
    public ApiResponse<List<Map<String, Object>>> searchMemberships(@RequestBody(required = false) PrincipalMembershipSearchRequest request) {
        PrincipalMembershipSearchRequest actual = request == null ? new PrincipalMembershipSearchRequest() : request;
        String containerKind = optionalMembershipSearchContainerKind(actual);
        String containerCode = containerKind == null ? null : resolveMembershipSearchContainerCode(actual, containerKind);
        return ApiResponse.success(service.searchMemberships(actual.getTenantKey(), actual.getAppKey(),
                containerKind, containerCode, actual.getUserKey()));
    }

    @PostMapping("/memberships/close")
    public ApiResponse<Void> closeMembership(@RequestBody PrincipalMembershipRequest request) {
        String containerKind = resolveContainerKind(request);
        String containerCode = resolveContainerCode(request, containerKind);
        service.closeMembership(request.getTenantKey(), request.getAppKey(), containerKind, containerCode, request.getUserKey());
        return ApiResponse.success();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException exception) {
        return ApiResponse.failure("PRINCIPAL_INVALID_REQUEST", exception.getMessage());
    }

    private String resolveContainerKind(PrincipalMembershipRequest request) {
        if (request.getContainerKind() != null && !request.getContainerKind().trim().isEmpty()) {
            return request.getContainerKind().trim().toUpperCase();
        }
        if (request.getRoleCode() != null && !request.getRoleCode().trim().isEmpty()) {
            return "ROLE";
        }
        if (request.getGroupCode() != null && !request.getGroupCode().trim().isEmpty()) {
            return "GROUP";
        }
        throw new IllegalArgumentException("containerKind or roleCode or groupCode is required");
    }

    private String resolveContainerCode(PrincipalMembershipRequest request, String containerKind) {
        if (request.getContainerCode() != null && !request.getContainerCode().trim().isEmpty()) {
            return request.getContainerCode().trim();
        }
        if ("ROLE".equals(containerKind) && request.getRoleCode() != null) {
            return request.getRoleCode().trim();
        }
        if ("GROUP".equals(containerKind) && request.getGroupCode() != null) {
            return request.getGroupCode().trim();
        }
        throw new IllegalArgumentException("containerCode or roleCode/groupCode is required");
    }

    private String optionalMembershipSearchContainerKind(PrincipalMembershipSearchRequest request) {
        if (request.getContainerKind() != null && !request.getContainerKind().trim().isEmpty()) {
            return request.getContainerKind().trim().toUpperCase();
        }
        if (request.getRoleCode() != null && !request.getRoleCode().trim().isEmpty()) {
            return "ROLE";
        }
        if (request.getGroupCode() != null && !request.getGroupCode().trim().isEmpty()) {
            return "GROUP";
        }
        return null;
    }

    private String resolveMembershipSearchContainerCode(PrincipalMembershipSearchRequest request, String containerKind) {
        if (request.getContainerCode() != null && !request.getContainerCode().trim().isEmpty()) {
            return request.getContainerCode().trim();
        }
        if ("ROLE".equals(containerKind) && request.getRoleCode() != null) {
            return request.getRoleCode().trim();
        }
        if ("GROUP".equals(containerKind) && request.getGroupCode() != null) {
            return request.getGroupCode().trim();
        }
        throw new IllegalArgumentException("containerCode or roleCode/groupCode is required");
    }

    private Map<String, Object> toRoleMap(com.fcm.authzcraft.pip.application.service.ApplicationRbacPrincipalSyncResult.RoleResult result) {
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<String, Object>();
        map.put("roleCode", result.getRoleCode());
        map.put("roleName", result.getRoleName());
        map.put("description", result.getDescription());
        map.put("principalId", result.getPrincipalId());
        map.put("lifecycleState", result.getLifecycleState());
        return map;
    }

    private Map<String, Object> toGroupMap(com.fcm.authzcraft.pip.application.service.ApplicationRbacPrincipalSyncResult.GroupResult result) {
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<String, Object>();
        map.put("groupCode", result.getGroupCode());
        map.put("groupName", result.getGroupName());
        map.put("description", result.getDescription());
        map.put("principalId", result.getPrincipalId());
        map.put("lifecycleState", result.getLifecycleState());
        return map;
    }

    private Map<String, Object> toMembershipMap(com.fcm.authzcraft.pip.application.service.ApplicationRbacPrincipalSyncResult.MembershipResult result) {
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<String, Object>();
        map.put("containerKind", result.getContainerKind());
        map.put("containerCode", result.getContainerCode());
        map.put("userKey", result.getUserKey());
        map.put("membershipId", result.getMembershipId());
        map.put("containerPrincipalId", result.getContainerPrincipalId());
        map.put("userPrincipalId", result.getUserPrincipalId());
        map.put("validFrom", result.getValidFrom());
        map.put("validUntil", result.getValidUntil());
        map.put("lifecycleState", result.getLifecycleState());
        return map;
    }
}
