package com.fcm.authzcraft.pap.interfaces.web;

import com.fcm.authzcraft.common.web.ApiResponse;
import com.fcm.authzcraft.pap.application.command.CreateAccessGrantCommand;
import com.fcm.authzcraft.pap.application.command.CreateAccessPolicyCommand;
import com.fcm.authzcraft.pap.application.command.CreatePolicyRevisionCommand;
import com.fcm.authzcraft.pap.application.command.CreateRuleBlueprintCommand;
import com.fcm.authzcraft.pap.application.command.GrantArgumentCommand;
import com.fcm.authzcraft.pap.application.command.UpdateAccessGrantCommand;
import com.fcm.authzcraft.pap.application.command.UpdateAccessPolicyCommand;
import com.fcm.authzcraft.pap.application.query.AccessGrantSearchQuery;
import com.fcm.authzcraft.pap.application.query.AccessPolicySearchQuery;
import com.fcm.authzcraft.pap.application.query.PolicyRevisionSearchQuery;
import com.fcm.authzcraft.pap.application.query.PrincipalProjectionSearchQuery;
import com.fcm.authzcraft.pap.application.query.PrincipalSearchQuery;
import com.fcm.authzcraft.pap.application.query.RuleBlueprintSearchQuery;
import com.fcm.authzcraft.pap.application.service.PolicyGovernanceService;
import com.fcm.authzcraft.pap.domain.model.AccessGrant;
import com.fcm.authzcraft.pap.domain.model.AccessPolicy;
import com.fcm.authzcraft.pap.domain.model.GrantArgument;
import com.fcm.authzcraft.pap.domain.model.PolicyRevision;
import com.fcm.authzcraft.pap.domain.model.Principal;
import com.fcm.authzcraft.pap.domain.model.PrincipalOrganizationProjection;
import com.fcm.authzcraft.pap.domain.model.PrincipalPositionProjection;
import com.fcm.authzcraft.pap.domain.model.PrincipalUserProjection;
import com.fcm.authzcraft.pap.domain.model.RuleBlueprint;
import com.fcm.authzcraft.pap.interfaces.dto.AccessGrantRequest;
import com.fcm.authzcraft.pap.interfaces.dto.AccessGrantResponse;
import com.fcm.authzcraft.pap.interfaces.dto.AccessGrantSearchRequest;
import com.fcm.authzcraft.pap.interfaces.dto.AccessGrantUpdateRequest;
import com.fcm.authzcraft.pap.interfaces.dto.AccessPolicyRequest;
import com.fcm.authzcraft.pap.interfaces.dto.AccessPolicyResponse;
import com.fcm.authzcraft.pap.interfaces.dto.AccessPolicySearchRequest;
import com.fcm.authzcraft.pap.interfaces.dto.AccessPolicyUpdateRequest;
import com.fcm.authzcraft.pap.interfaces.dto.GrantArgumentReplaceRequest;
import com.fcm.authzcraft.pap.interfaces.dto.GrantArgumentRequest;
import com.fcm.authzcraft.pap.interfaces.dto.GrantArgumentResponse;
import com.fcm.authzcraft.pap.interfaces.dto.PolicyRevisionRequest;
import com.fcm.authzcraft.pap.interfaces.dto.PolicyRevisionResponse;
import com.fcm.authzcraft.pap.interfaces.dto.PolicyRevisionSearchRequest;
import com.fcm.authzcraft.pap.interfaces.dto.PrincipalOrganizationProjectionResponse;
import com.fcm.authzcraft.pap.interfaces.dto.PrincipalPositionProjectionResponse;
import com.fcm.authzcraft.pap.interfaces.dto.PrincipalProjectionSearchRequest;
import com.fcm.authzcraft.pap.interfaces.dto.PrincipalResponse;
import com.fcm.authzcraft.pap.interfaces.dto.PrincipalSearchRequest;
import com.fcm.authzcraft.pap.interfaces.dto.PrincipalUserProjectionResponse;
import com.fcm.authzcraft.pap.interfaces.dto.RuleBlueprintRequest;
import com.fcm.authzcraft.pap.interfaces.dto.RuleBlueprintResponse;
import com.fcm.authzcraft.pap.interfaces.dto.RuleBlueprintSearchRequest;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/authzcraft/api/v1")
public class PolicyGovernanceController {
    private final PolicyGovernanceService policyGovernanceService;

    public PolicyGovernanceController(PolicyGovernanceService policyGovernanceService) {
        this.policyGovernanceService = policyGovernanceService;
    }

    @PostMapping("/policies/rule-blueprints")
    public ApiResponse<RuleBlueprintResponse> createRuleBlueprint(@RequestBody RuleBlueprintRequest request) {
        return ApiResponse.success(RuleBlueprintResponse.from(policyGovernanceService.createRuleBlueprint(toCommand(request))));
    }

    @PostMapping("/policies/rule-blueprints/search")
    public ApiResponse<List<RuleBlueprintResponse>> searchRuleBlueprints(@RequestBody(required = false) RuleBlueprintSearchRequest request) {
        List<RuleBlueprintResponse> responses = new ArrayList<RuleBlueprintResponse>();
        for (RuleBlueprint blueprint : policyGovernanceService.searchRuleBlueprints(toQuery(request == null ? new RuleBlueprintSearchRequest() : request))) {
            responses.add(RuleBlueprintResponse.from(blueprint));
        }
        return ApiResponse.success(responses);
    }

    @PostMapping("/policies/rule-blueprints/{id}/update")
    public ApiResponse<RuleBlueprintResponse> updateRuleBlueprint(@PathVariable String id,
                                                                  @RequestBody RuleBlueprintRequest request) {
        return ApiResponse.success(RuleBlueprintResponse.from(policyGovernanceService.updateRuleBlueprint(Long.valueOf(id), toCommand(request))));
    }

    @PostMapping("/policies/rule-blueprints/{id}/publish")
    public ApiResponse<RuleBlueprintResponse> publishRuleBlueprint(@PathVariable String id) {
        return ApiResponse.success(RuleBlueprintResponse.from(policyGovernanceService.publishRuleBlueprint(Long.valueOf(id))));
    }

    @PostMapping("/policies/rule-blueprints/{id}/retire")
    public ApiResponse<RuleBlueprintResponse> retireRuleBlueprint(@PathVariable String id) {
        return ApiResponse.success(RuleBlueprintResponse.from(policyGovernanceService.retireRuleBlueprint(Long.valueOf(id))));
    }

    @PostMapping("/policies/access-policies")
    public ApiResponse<AccessPolicyResponse> createAccessPolicy(@RequestBody AccessPolicyRequest request) {
        return ApiResponse.success(AccessPolicyResponse.from(policyGovernanceService.createAccessPolicy(toCommand(request))));
    }

    @PostMapping("/policies/access-policies/search")
    public ApiResponse<List<AccessPolicyResponse>> searchAccessPolicies(@RequestBody(required = false) AccessPolicySearchRequest request) {
        List<AccessPolicyResponse> responses = new ArrayList<AccessPolicyResponse>();
        for (AccessPolicy policy : policyGovernanceService.searchAccessPolicies(toQuery(request == null ? new AccessPolicySearchRequest() : request))) {
            responses.add(AccessPolicyResponse.from(policy));
        }
        return ApiResponse.success(responses);
    }

    @PostMapping("/policies/access-policies/{id}/update")
    public ApiResponse<AccessPolicyResponse> updateAccessPolicy(@PathVariable String id,
                                                                @RequestBody AccessPolicyUpdateRequest request) {
        return ApiResponse.success(AccessPolicyResponse.from(policyGovernanceService.updateAccessPolicy(Long.valueOf(id), toCommand(request))));
    }

    @PostMapping("/policies/access-policies/{id}/activate")
    public ApiResponse<AccessPolicyResponse> activateAccessPolicy(@PathVariable String id) {
        return ApiResponse.success(AccessPolicyResponse.from(policyGovernanceService.activateAccessPolicy(Long.valueOf(id))));
    }

    @PostMapping("/policies/access-policies/{id}/revisions")
    public ApiResponse<PolicyRevisionResponse> createPolicyRevision(@PathVariable String id,
                                                                    @RequestBody PolicyRevisionRequest request) {
        return ApiResponse.success(PolicyRevisionResponse.from(policyGovernanceService.createPolicyRevision(Long.valueOf(id), toCommand(request))));
    }

    @PostMapping("/policies/access-policies/{id}/revisions/search")
    public ApiResponse<List<PolicyRevisionResponse>> searchPolicyRevisions(@PathVariable String id,
                                                                           @RequestBody(required = false) PolicyRevisionSearchRequest request) {
        List<PolicyRevisionResponse> responses = new ArrayList<PolicyRevisionResponse>();
        for (PolicyRevision revision : policyGovernanceService.searchPolicyRevisions(Long.valueOf(id), toQuery(request == null ? new PolicyRevisionSearchRequest() : request))) {
            responses.add(PolicyRevisionResponse.from(revision));
        }
        return ApiResponse.success(responses);
    }

    @PostMapping("/policies/revisions/{id}/activate")
    public ApiResponse<PolicyRevisionResponse> activatePolicyRevision(@PathVariable String id) {
        return ApiResponse.success(PolicyRevisionResponse.from(policyGovernanceService.activatePolicyRevision(Long.valueOf(id))));
    }

    @PostMapping("/policies/revisions/{id}/retire")
    public ApiResponse<PolicyRevisionResponse> retirePolicyRevision(@PathVariable String id) {
        return ApiResponse.success(PolicyRevisionResponse.from(policyGovernanceService.retirePolicyRevision(Long.valueOf(id))));
    }

    @PostMapping("/grants/access-grants")
    public ApiResponse<AccessGrantResponse> createAccessGrant(@RequestBody AccessGrantRequest request) {
        return ApiResponse.success(AccessGrantResponse.from(policyGovernanceService.createAccessGrant(toCommand(request))));
    }

    @PostMapping("/grants/access-grants/search")
    public ApiResponse<List<AccessGrantResponse>> searchAccessGrants(@RequestBody(required = false) AccessGrantSearchRequest request) {
        List<AccessGrantResponse> responses = new ArrayList<AccessGrantResponse>();
        for (AccessGrant grant : policyGovernanceService.searchAccessGrants(toQuery(request == null ? new AccessGrantSearchRequest() : request))) {
            responses.add(AccessGrantResponse.from(grant));
        }
        return ApiResponse.success(responses);
    }

    @PostMapping("/principals/search")
    public ApiResponse<List<PrincipalResponse>> searchPrincipals(@RequestBody(required = false) PrincipalSearchRequest request) {
        List<PrincipalResponse> responses = new ArrayList<PrincipalResponse>();
        for (Principal principal : policyGovernanceService.searchPrincipals(toQuery(request == null ? new PrincipalSearchRequest() : request))) {
            responses.add(PrincipalResponse.from(principal));
        }
        return ApiResponse.success(responses);
    }

    @PostMapping("/principals/users/search")
    public ApiResponse<List<PrincipalUserProjectionResponse>> searchPrincipalUsers(@RequestBody(required = false) PrincipalProjectionSearchRequest request) {
        List<PrincipalUserProjectionResponse> responses = new ArrayList<PrincipalUserProjectionResponse>();
        for (PrincipalUserProjection projection : policyGovernanceService.searchPrincipalUsers(toQuery(request == null ? new PrincipalProjectionSearchRequest() : request))) {
            responses.add(PrincipalUserProjectionResponse.from(projection));
        }
        return ApiResponse.success(responses);
    }

    @PostMapping("/principals/organizations/search")
    public ApiResponse<List<PrincipalOrganizationProjectionResponse>> searchPrincipalOrganizations(@RequestBody(required = false) PrincipalProjectionSearchRequest request) {
        List<PrincipalOrganizationProjectionResponse> responses = new ArrayList<PrincipalOrganizationProjectionResponse>();
        for (PrincipalOrganizationProjection projection : policyGovernanceService.searchPrincipalOrganizations(toQuery(request == null ? new PrincipalProjectionSearchRequest() : request))) {
            responses.add(PrincipalOrganizationProjectionResponse.from(projection));
        }
        return ApiResponse.success(responses);
    }

    @PostMapping("/principals/positions/search")
    public ApiResponse<List<PrincipalPositionProjectionResponse>> searchPrincipalPositions(@RequestBody(required = false) PrincipalProjectionSearchRequest request) {
        List<PrincipalPositionProjectionResponse> responses = new ArrayList<PrincipalPositionProjectionResponse>();
        for (PrincipalPositionProjection projection : policyGovernanceService.searchPrincipalPositions(toQuery(request == null ? new PrincipalProjectionSearchRequest() : request))) {
            responses.add(PrincipalPositionProjectionResponse.from(projection));
        }
        return ApiResponse.success(responses);
    }

    @PostMapping("/grants/access-grants/{id}/update")
    public ApiResponse<AccessGrantResponse> updateAccessGrant(@PathVariable String id,
                                                              @RequestBody AccessGrantUpdateRequest request) {
        return ApiResponse.success(AccessGrantResponse.from(policyGovernanceService.updateAccessGrant(Long.valueOf(id), toCommand(request))));
    }

    @PostMapping("/grants/access-grants/{id}/suspend")
    public ApiResponse<AccessGrantResponse> suspendAccessGrant(@PathVariable String id) {
        return ApiResponse.success(AccessGrantResponse.from(policyGovernanceService.suspendAccessGrant(Long.valueOf(id))));
    }

    @PostMapping("/grants/access-grants/{id}/resume")
    public ApiResponse<AccessGrantResponse> resumeAccessGrant(@PathVariable String id) {
        return ApiResponse.success(AccessGrantResponse.from(policyGovernanceService.resumeAccessGrant(Long.valueOf(id))));
    }

    @PostMapping("/grants/access-grants/{id}/revoke")
    public ApiResponse<AccessGrantResponse> revokeAccessGrant(@PathVariable String id) {
        return ApiResponse.success(AccessGrantResponse.from(policyGovernanceService.revokeAccessGrant(Long.valueOf(id))));
    }

    @PostMapping("/grants/access-grants/{id}/arguments/replace")
    public ApiResponse<List<GrantArgumentResponse>> replaceGrantArguments(@PathVariable String id,
                                                                          @RequestBody(required = false) GrantArgumentReplaceRequest request) {
        List<GrantArgumentRequest> argumentRequests = request == null || request.getArguments() == null
                ? Collections.<GrantArgumentRequest>emptyList() : request.getArguments();
        List<GrantArgumentResponse> responses = new ArrayList<GrantArgumentResponse>();
        for (GrantArgument argument : policyGovernanceService.replaceGrantArguments(Long.valueOf(id), toCommands(argumentRequests))) {
            responses.add(GrantArgumentResponse.from(argument));
        }
        return ApiResponse.success(responses);
    }

    @PostMapping("/grants/access-grants/{id}/arguments/search")
    public ApiResponse<List<GrantArgumentResponse>> searchGrantArguments(@PathVariable String id) {
        List<GrantArgumentResponse> responses = new ArrayList<GrantArgumentResponse>();
        for (GrantArgument argument : policyGovernanceService.findGrantArguments(Long.valueOf(id))) {
            responses.add(GrantArgumentResponse.from(argument));
        }
        return ApiResponse.success(responses);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException exception) {
        return ApiResponse.failure("POLICY_INVALID_REQUEST", exception.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResponse<Void> handleUnreadableMessage(HttpMessageNotReadableException exception) {
        Throwable cause = exception;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        String message = cause.getMessage() == null ? "Request body is invalid" : cause.getMessage();
        return ApiResponse.failure("POLICY_INVALID_REQUEST", message);
    }

    private CreateRuleBlueprintCommand toCommand(RuleBlueprintRequest request) {
        CreateRuleBlueprintCommand command = new CreateRuleBlueprintCommand();
        command.setTenantKey(request.getTenantKey());
        command.setBlueprintKey(request.getBlueprintKey());
        command.setBlueprintKind(request.getBlueprintKind());
        command.setBlueprintVersion(request.getBlueprintVersion());
        command.setDisplayName(request.getDisplayName());
        command.setDescription(request.getDescription());
        command.setPredicateTemplate(request.getPredicateTemplate());
        command.setInputSchema(request.getInputSchema());
        return command;
    }

    private RuleBlueprintSearchQuery toQuery(RuleBlueprintSearchRequest request) {
        RuleBlueprintSearchQuery query = new RuleBlueprintSearchQuery();
        query.setTenantKey(request.getTenantKey());
        query.setBlueprintKey(request.getBlueprintKey());
        query.setLifecycleState(request.getLifecycleState());
        return query;
    }

    private CreateAccessPolicyCommand toCommand(AccessPolicyRequest request) {
        CreateAccessPolicyCommand command = new CreateAccessPolicyCommand();
        command.setTenantKey(request.getTenantKey());
        command.setAppKey(request.getAppKey());
        command.setPolicyKey(request.getPolicyKey());
        command.setDisplayName(request.getDisplayName());
        command.setDescription(request.getDescription());
        command.setTargetRelationId(request.getTargetRelationId());
        command.setOperationCode(request.getOperationCode());
        command.setEffectKind(request.getEffectKind());
        return command;
    }

    private UpdateAccessPolicyCommand toCommand(AccessPolicyUpdateRequest request) {
        UpdateAccessPolicyCommand command = new UpdateAccessPolicyCommand();
        command.setDisplayName(request.getDisplayName());
        command.setDescription(request.getDescription());
        command.setOperationCode(request.getOperationCode());
        command.setEffectKind(request.getEffectKind());
        command.setLifecycleState(request.getLifecycleState());
        return command;
    }

    private AccessPolicySearchQuery toQuery(AccessPolicySearchRequest request) {
        AccessPolicySearchQuery query = new AccessPolicySearchQuery();
        query.setTenantKey(request.getTenantKey());
        query.setAppKey(request.getAppKey());
        query.setPolicyKey(request.getPolicyKey());
        query.setTargetRelationId(request.getTargetRelationId());
        query.setOperationCode(request.getOperationCode());
        query.setLifecycleState(request.getLifecycleState());
        return query;
    }

    private CreatePolicyRevisionCommand toCommand(PolicyRevisionRequest request) {
        CreatePolicyRevisionCommand command = new CreatePolicyRevisionCommand();
        command.setBlueprintId(request.getBlueprintId());
        command.setTemplateBinding(request.getTemplateBinding());
        command.setCustomAst(request.getCustomAst());
        command.setArgumentSchema(request.getArgumentSchema());
        command.setAttributeReferences(request.getAttributeReferences());
        command.setChangeSummary(request.getChangeSummary());
        return command;
    }

    private PolicyRevisionSearchQuery toQuery(PolicyRevisionSearchRequest request) {
        PolicyRevisionSearchQuery query = new PolicyRevisionSearchQuery();
        query.setRevisionState(request.getRevisionState());
        return query;
    }

    private CreateAccessGrantCommand toCommand(AccessGrantRequest request) {
        CreateAccessGrantCommand command = new CreateAccessGrantCommand();
        command.setTenantKey(request.getTenantKey());
        command.setAppKey(request.getAppKey());
        command.setGrantKey(request.getGrantKey());
        command.setPrincipalId(request.getPrincipalId());
        command.setPolicyId(request.getPolicyId());
        command.setValidFrom(request.getValidFrom());
        command.setValidUntil(request.getValidUntil());
        command.setGrantSource(request.getGrantSource());
        command.setReason(request.getReason());
        return command;
    }

    private AccessGrantSearchQuery toQuery(AccessGrantSearchRequest request) {
        AccessGrantSearchQuery query = new AccessGrantSearchQuery();
        query.setTenantKey(request.getTenantKey());
        query.setAppKey(request.getAppKey());
        query.setPrincipalId(request.getPrincipalId());
        query.setPolicyId(request.getPolicyId());
        query.setLifecycleState(request.getLifecycleState());
        return query;
    }

    private PrincipalSearchQuery toQuery(PrincipalSearchRequest request) {
        PrincipalSearchQuery query = new PrincipalSearchQuery();
        query.setTenantKey(request.getTenantKey());
        query.setPrincipalKind(request.getPrincipalKind());
        query.setPrincipalKey(request.getPrincipalKey());
        query.setKeyword(request.getKeyword());
        query.setLifecycleState(request.getLifecycleState());
        return query;
    }

    private PrincipalProjectionSearchQuery toQuery(PrincipalProjectionSearchRequest request) {
        PrincipalProjectionSearchQuery query = new PrincipalProjectionSearchQuery();
        query.setTenantKey(request.getTenantKey());
        query.setPrincipalKey(request.getPrincipalKey());
        query.setKeyword(request.getKeyword());
        query.setLifecycleState(request.getLifecycleState());
        query.setUserId(request.getUserId());
        query.setStaffNo(request.getStaffNo());
        query.setDepartmentCode(request.getDepartmentCode());
        query.setPostCode(request.getPostCode());
        query.setParentDepartmentCode(request.getParentDepartmentCode());
        query.setLimit(request.getLimit());
        query.setOffset(request.getOffset());
        return query;
    }

    private UpdateAccessGrantCommand toCommand(AccessGrantUpdateRequest request) {
        AccessGrantUpdateRequest actualRequest = request == null ? new AccessGrantUpdateRequest() : request;
        UpdateAccessGrantCommand command = new UpdateAccessGrantCommand();
        command.setValidFrom(actualRequest.getValidFrom());
        command.setValidUntil(actualRequest.getValidUntil());
        command.setGrantSource(actualRequest.getGrantSource());
        command.setReason(actualRequest.getReason());
        return command;
    }

    private List<GrantArgumentCommand> toCommands(List<GrantArgumentRequest> requests) {
        List<GrantArgumentCommand> commands = new ArrayList<GrantArgumentCommand>();
        for (GrantArgumentRequest request : requests) {
            GrantArgumentCommand command = new GrantArgumentCommand();
            command.setArgumentKey(request.getArgumentKey());
            command.setValueKind(request.getValueKind());
            command.setArgumentValue(request.getArgumentValue());
            command.setSensitiveFlag(request.getSensitiveFlag());
            commands.add(command);
        }
        return commands;
    }
}