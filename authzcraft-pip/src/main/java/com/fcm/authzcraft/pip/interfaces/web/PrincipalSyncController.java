package com.fcm.authzcraft.pip.interfaces.web;

import com.fcm.authzcraft.common.web.ApiResponse;
import com.fcm.authzcraft.pip.application.command.ApplicationRbacPrincipalSyncCommand;
import com.fcm.authzcraft.pip.application.command.PrincipalSyncCommand;
import com.fcm.authzcraft.pip.application.service.ApplicationRbacPrincipalSyncService;
import com.fcm.authzcraft.pip.application.service.AsyncPrincipalSyncService;
import com.fcm.authzcraft.pip.application.service.PrincipalSyncResult;
import com.fcm.authzcraft.pip.interfaces.dto.ApplicationRbacPrincipalSyncRequest;
import com.fcm.authzcraft.pip.interfaces.dto.ApplicationRbacPrincipalSyncResponse;
import com.fcm.authzcraft.pip.interfaces.dto.PrincipalSyncRequest;
import com.fcm.authzcraft.pip.interfaces.dto.PrincipalSyncResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/authzcraft/api/v1/principals")
public class PrincipalSyncController {

    private static final String UNIFY_ENGINE_SOURCE = "unify_engine";

    private final AsyncPrincipalSyncService asyncPrincipalSyncService;
    private final ApplicationRbacPrincipalSyncService applicationRbacPrincipalSyncService;

    @Value("${authzcraft.context.tenant-key:platform}")
    private String defaultTenantKey;

    @Value("${authzcraft.context.app-key:authzcraft}")
    private String defaultAppKey;

    public PrincipalSyncController(AsyncPrincipalSyncService asyncPrincipalSyncService,
                                   ApplicationRbacPrincipalSyncService applicationRbacPrincipalSyncService) {
        this.asyncPrincipalSyncService = asyncPrincipalSyncService;
        this.applicationRbacPrincipalSyncService = applicationRbacPrincipalSyncService;
    }

    @PostMapping("/sync/unify-engine")
    public ApiResponse<PrincipalSyncResponse> submitSyncUnifyEngine(@RequestBody(required = false) PrincipalSyncRequest request) {
        PrincipalSyncRequest actualRequest = request == null ? new PrincipalSyncRequest() : request;
        String taskId = asyncPrincipalSyncService.submit(new PrincipalSyncCommand(
                firstText(actualRequest.getTenantKey(), defaultTenantKey),
                firstText(actualRequest.getAppKey(), defaultAppKey),
                UNIFY_ENGINE_SOURCE,
                actualRequest.getSourceTenantId(),
                actualRequest.getSourceAppCode()));
        return ApiResponse.success(new PrincipalSyncResponse(
                taskId,
                "PENDING",
            "PENDING",
            0,
            0,
            null,
            null,
                0,
                0,
                0,
                null));
    }

    @GetMapping("/sync/tasks/{taskId}")
    public ApiResponse<PrincipalSyncResponse> getSyncTask(@PathVariable String taskId) {
        PrincipalSyncResult result = asyncPrincipalSyncService.getResult(taskId);
        return ApiResponse.success(new PrincipalSyncResponse(
                result.getTaskId(),
                result.getState(),
            result.getCurrentStage(),
            result.getProcessedCount(),
            result.getTotalCount(),
            result.getStartedAt(),
            result.getFinishedAt(),
                result.getOrganizationCount(),
                result.getPositionCount(),
                result.getUserCount(),
                result.getErrorMessage()));
    }

    @PostMapping("/sync/application-rbac")
    public ApiResponse<ApplicationRbacPrincipalSyncResponse> syncApplicationRbac(@RequestBody ApplicationRbacPrincipalSyncRequest request) {
        ApplicationRbacPrincipalSyncRequest actualRequest = request == null ? new ApplicationRbacPrincipalSyncRequest() : request;
        return ApiResponse.success(ApplicationRbacPrincipalSyncResponse.from(applicationRbacPrincipalSyncService.sync(toCommand(actualRequest))));
    }

    @PostMapping("/sync/application-rbac/status/search")
    public ApiResponse<ApplicationRbacPrincipalSyncResponse> searchApplicationRbacStatus(@RequestBody ApplicationRbacPrincipalSyncRequest request) {
        ApplicationRbacPrincipalSyncRequest actualRequest = request == null ? new ApplicationRbacPrincipalSyncRequest() : request;
        return ApiResponse.success(ApplicationRbacPrincipalSyncResponse.from(applicationRbacPrincipalSyncService.status(toCommand(actualRequest))));
    }

    private ApplicationRbacPrincipalSyncCommand toCommand(ApplicationRbacPrincipalSyncRequest request) {
        List<ApplicationRbacPrincipalSyncCommand.RoleItem> roles = new ArrayList<ApplicationRbacPrincipalSyncCommand.RoleItem>();
        if (request.getRoles() != null) {
            for (ApplicationRbacPrincipalSyncRequest.RoleItem role : request.getRoles()) {
                roles.add(new ApplicationRbacPrincipalSyncCommand.RoleItem(role.getRoleCode(), role.getRoleName(),
                        role.getDescription(), role.getLifecycleState()));
            }
        }
        List<ApplicationRbacPrincipalSyncCommand.GroupItem> groups = new ArrayList<ApplicationRbacPrincipalSyncCommand.GroupItem>();
        if (request.getGroups() != null) {
            for (ApplicationRbacPrincipalSyncRequest.GroupItem group : request.getGroups()) {
                groups.add(new ApplicationRbacPrincipalSyncCommand.GroupItem(group.getGroupCode(), group.getGroupName(),
                        group.getDescription(), group.getLifecycleState()));
            }
        }
        List<ApplicationRbacPrincipalSyncCommand.MembershipItem> memberships = new ArrayList<ApplicationRbacPrincipalSyncCommand.MembershipItem>();
        if (request.getMemberships() != null) {
            for (ApplicationRbacPrincipalSyncRequest.MembershipItem membership : request.getMemberships()) {
                memberships.add(new ApplicationRbacPrincipalSyncCommand.MembershipItem(membership.getRoleCode(),
                        membership.getGroupCode(), membership.getContainerKind(), membership.getContainerCode(),
                        membership.getUserKey(), membership.getValidFrom(), membership.getValidUntil()));
            }
        }
        return new ApplicationRbacPrincipalSyncCommand(
                firstText(request.getTenantKey(), defaultTenantKey),
                firstText(request.getAppKey(), defaultAppKey),
                !Boolean.FALSE.equals(request.getDeactivateMissing()),
                roles,
                groups,
                memberships);
    }

    private String firstText(String first, String fallback) {
        return StringUtils.hasText(first) ? first : fallback;
    }
}
