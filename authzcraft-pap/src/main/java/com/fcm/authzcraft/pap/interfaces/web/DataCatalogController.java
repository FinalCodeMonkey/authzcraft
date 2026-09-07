package com.fcm.authzcraft.pap.interfaces.web;

import com.fcm.authzcraft.common.web.ApiResponse;
import com.fcm.authzcraft.pap.application.command.RegisterAccessPathCommand;
import com.fcm.authzcraft.pap.application.command.RegisterRelationResourceCommand;
import com.fcm.authzcraft.pap.application.command.UpdateAccessPathCommand;
import com.fcm.authzcraft.pap.application.command.UpdateRelationResourceCommand;
import com.fcm.authzcraft.pap.application.command.UpdateResourceFieldCommand;
import com.fcm.authzcraft.pap.application.query.AccessPathSearchQuery;
import com.fcm.authzcraft.pap.application.query.RelationResourceSearchQuery;
import com.fcm.authzcraft.pap.application.query.ResourceFieldSearchQuery;
import com.fcm.authzcraft.pap.application.service.DataCatalogService;
import com.fcm.authzcraft.pap.domain.model.AccessPath;
import com.fcm.authzcraft.pap.domain.model.RelationResource;
import com.fcm.authzcraft.pap.domain.model.ResourceField;
import com.fcm.authzcraft.pap.interfaces.dto.AccessPathRequest;
import com.fcm.authzcraft.pap.interfaces.dto.AccessPathResponse;
import com.fcm.authzcraft.pap.interfaces.dto.AccessPathSearchRequest;
import com.fcm.authzcraft.pap.interfaces.dto.AccessPathUpdateRequest;
import com.fcm.authzcraft.pap.interfaces.dto.RelationResourceRequest;
import com.fcm.authzcraft.pap.interfaces.dto.RelationResourceResponse;
import com.fcm.authzcraft.pap.interfaces.dto.RelationResourceSearchRequest;
import com.fcm.authzcraft.pap.interfaces.dto.RelationResourceUpdateRequest;
import com.fcm.authzcraft.pap.interfaces.dto.ResourceFieldResponse;
import com.fcm.authzcraft.pap.interfaces.dto.ResourceFieldSearchRequest;
import com.fcm.authzcraft.pap.interfaces.dto.ResourceFieldUpdateRequest;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/authzcraft/api/v1/catalog")
public class DataCatalogController {

    private final DataCatalogService dataCatalogService;

    public DataCatalogController(DataCatalogService dataCatalogService) {
        this.dataCatalogService = dataCatalogService;
    }

    @PostMapping("/relation-resources")
    public ApiResponse<RelationResourceResponse> registerRelationResource(@RequestBody RelationResourceRequest request) {
        return ApiResponse.success(RelationResourceResponse.from(dataCatalogService.registerRelationResource(toCommand(request))));
    }

    @PostMapping("/relation-resources/search")
    public ApiResponse<List<RelationResourceResponse>> searchRelationResources(@RequestBody(required = false) RelationResourceSearchRequest request) {
        RelationResourceSearchQuery query = toQuery(request == null ? new RelationResourceSearchRequest() : request);
        List<RelationResourceResponse> responses = new ArrayList<RelationResourceResponse>();
        for (RelationResource resource : dataCatalogService.searchRelationResources(query)) {
            responses.add(RelationResourceResponse.from(resource));
        }
        return ApiResponse.success(responses);
    }

    @PostMapping("/relation-resources/{id}/update")
    public ApiResponse<RelationResourceResponse> updateRelationResource(@PathVariable String id,
                                                                        @RequestBody RelationResourceUpdateRequest request) {
        return ApiResponse.success(RelationResourceResponse.from(dataCatalogService.updateRelationResource(
                Long.valueOf(id), toCommand(request))));
    }

    @PostMapping("/relation-resources/{id}/fields/import")
    public ApiResponse<List<ResourceFieldResponse>> importResourceFields(@PathVariable String id) {
        List<ResourceFieldResponse> responses = new ArrayList<ResourceFieldResponse>();
        for (ResourceField field : dataCatalogService.importResourceFields(Long.valueOf(id))) {
            responses.add(ResourceFieldResponse.from(field));
        }
        return ApiResponse.success(responses);
    }

    @PostMapping("/relation-resources/{id}/fields/search")
    public ApiResponse<List<ResourceFieldResponse>> searchResourceFields(@PathVariable String id,
                                                                         @RequestBody(required = false) ResourceFieldSearchRequest request) {
        ResourceFieldSearchQuery query = toQuery(request == null ? new ResourceFieldSearchRequest() : request);
        List<ResourceFieldResponse> responses = new ArrayList<ResourceFieldResponse>();
        for (ResourceField field : dataCatalogService.searchResourceFields(Long.valueOf(id), query)) {
            responses.add(ResourceFieldResponse.from(field));
        }
        return ApiResponse.success(responses);
    }

    @PostMapping("/relation-resources/{id}/fields/{fieldId}/update")
    public ApiResponse<ResourceFieldResponse> updateResourceField(@PathVariable String id, @PathVariable String fieldId,
                                                                  @RequestBody ResourceFieldUpdateRequest request) {
        return ApiResponse.success(ResourceFieldResponse.from(dataCatalogService.updateResourceField(
                Long.valueOf(id), Long.valueOf(fieldId), toCommand(request))));
    }

    @PostMapping("/access-paths")
    public ApiResponse<AccessPathResponse> registerAccessPath(@RequestBody AccessPathRequest request) {
        return ApiResponse.success(AccessPathResponse.from(dataCatalogService.registerAccessPath(toCommand(request))));
    }

    @PostMapping("/access-paths/search")
    public ApiResponse<List<AccessPathResponse>> searchAccessPaths(@RequestBody(required = false) AccessPathSearchRequest request) {
        AccessPathSearchQuery query = toQuery(request == null ? new AccessPathSearchRequest() : request);
        List<AccessPathResponse> responses = new ArrayList<AccessPathResponse>();
        for (AccessPath accessPath : dataCatalogService.searchAccessPaths(query)) {
            responses.add(AccessPathResponse.from(accessPath));
        }
        return ApiResponse.success(responses);
    }

    @PostMapping("/access-paths/{id}/update")
    public ApiResponse<AccessPathResponse> updateAccessPath(@PathVariable String id,
                                                            @RequestBody AccessPathUpdateRequest request) {
        return ApiResponse.success(AccessPathResponse.from(dataCatalogService.updateAccessPath(Long.valueOf(id), toCommand(request))));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException exception) {
        return ApiResponse.failure("CATALOG_INVALID_REQUEST", exception.getMessage());
    }

    private RegisterRelationResourceCommand toCommand(RelationResourceRequest request) {
        RegisterRelationResourceCommand command = new RegisterRelationResourceCommand();
        command.setTenantKey(request.getTenantKey());
        command.setAppKey(request.getAppKey());
        command.setResourceKey(request.getResourceKey());
        command.setDisplayName(request.getDisplayName());
        command.setNamespaceName(request.getNamespaceName());
        command.setPhysicalName(request.getPhysicalName());
        command.setRelationKind(request.getRelationKind());
        command.setProtectionMode(request.getProtectionMode());
        command.setDescription(request.getDescription());
        return command;
    }

    private UpdateRelationResourceCommand toCommand(RelationResourceUpdateRequest request) {
        UpdateRelationResourceCommand command = new UpdateRelationResourceCommand();
        command.setDisplayName(request.getDisplayName());
        command.setProtectionMode(request.getProtectionMode());
        command.setLifecycleState(request.getLifecycleState());
        command.setDescription(request.getDescription());
        return command;
    }

    private UpdateResourceFieldCommand toCommand(ResourceFieldUpdateRequest request) {
        UpdateResourceFieldCommand command = new UpdateResourceFieldCommand();
        command.setDisplayName(request.getDisplayName());
        command.setFilterableFlag(request.getFilterableFlag());
        command.setJoinableFlag(request.getJoinableFlag());
        command.setPrincipalRefKind(request.getPrincipalRefKind());
        command.setSensitivityLevel(request.getSensitivityLevel());
        command.setSecurityRequirement(request.getSecurityRequirement());
        command.setLifecycleState(request.getLifecycleState());
        return command;
    }

    private RegisterAccessPathCommand toCommand(AccessPathRequest request) {
        RegisterAccessPathCommand command = new RegisterAccessPathCommand();
        command.setTenantKey(request.getTenantKey());
        command.setAppKey(request.getAppKey());
        command.setPathKey(request.getPathKey());
        command.setDisplayName(request.getDisplayName());
        command.setRootRelationId(Long.valueOf(request.getRootRelationId()));
        command.setDestinationRelationId(Long.valueOf(request.getDestinationRelationId()));
        command.setExecutionMode(request.getExecutionMode());
        command.setTraversalSteps(request.getTraversalSteps());
        return command;
    }

    private UpdateAccessPathCommand toCommand(AccessPathUpdateRequest request) {
        UpdateAccessPathCommand command = new UpdateAccessPathCommand();
        command.setDisplayName(request.getDisplayName());
        command.setExecutionMode(request.getExecutionMode());
        command.setTraversalSteps(request.getTraversalSteps());
        command.setLifecycleState(request.getLifecycleState());
        return command;
    }

    private RelationResourceSearchQuery toQuery(RelationResourceSearchRequest request) {
        RelationResourceSearchQuery query = new RelationResourceSearchQuery();
        query.setTenantKey(request.getTenantKey());
        query.setAppKey(request.getAppKey());
        query.setResourceKey(request.getResourceKey());
        query.setKeyword(request.getKeyword());
        query.setLifecycleState(request.getLifecycleState());
        return query;
    }

    private ResourceFieldSearchQuery toQuery(ResourceFieldSearchRequest request) {
        ResourceFieldSearchQuery query = new ResourceFieldSearchQuery();
        query.setFilterableFlag(request.getFilterableFlag());
        query.setJoinableFlag(request.getJoinableFlag());
        query.setLifecycleState(request.getLifecycleState());
        return query;
    }

    private AccessPathSearchQuery toQuery(AccessPathSearchRequest request) {
        AccessPathSearchQuery query = new AccessPathSearchQuery();
        query.setTenantKey(request.getTenantKey());
        query.setAppKey(request.getAppKey());
        query.setPathKey(request.getPathKey());
        query.setRootRelationId(request.getRootRelationId());
        query.setLifecycleState(request.getLifecycleState());
        return query;
    }
}