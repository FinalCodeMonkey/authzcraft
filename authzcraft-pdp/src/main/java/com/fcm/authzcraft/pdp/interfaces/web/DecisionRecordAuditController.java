package com.fcm.authzcraft.pdp.interfaces.web;

import com.fcm.authzcraft.common.web.ApiResponse;
import com.fcm.authzcraft.pdp.application.query.DecisionRecordSearchQuery;
import com.fcm.authzcraft.pdp.application.service.DecisionRecordAuditService;
import com.fcm.authzcraft.pdp.domain.model.DecisionRecord;
import com.fcm.authzcraft.pdp.interfaces.dto.DecisionRecordResponse;
import com.fcm.authzcraft.pdp.interfaces.dto.DecisionRecordSearchRequest;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/authzcraft/api/v1/audit")
public class DecisionRecordAuditController {
    private final DecisionRecordAuditService auditService;

    public DecisionRecordAuditController(DecisionRecordAuditService auditService) {
        this.auditService = auditService;
    }

    @PostMapping("/decision-records/search")
    public ApiResponse<List<DecisionRecordResponse>> searchDecisionRecords(@RequestBody(required = false) DecisionRecordSearchRequest request) {
        List<DecisionRecordResponse> responses = new ArrayList<DecisionRecordResponse>();
        for (DecisionRecord record : auditService.search(toQuery(request == null ? new DecisionRecordSearchRequest() : request))) {
            responses.add(DecisionRecordResponse.from(record));
        }
        return ApiResponse.success(responses);
    }

    @PostMapping("/decision-records/{decisionKey}")
    public ApiResponse<DecisionRecordResponse> getDecisionRecord(@PathVariable String decisionKey) {
        DecisionRecord record = auditService.findLatestByDecisionKey(decisionKey);
        if (record == null) {
            return ApiResponse.failure("AUDIT_DECISION_RECORD_NOT_FOUND", "decision record not found: " + decisionKey);
        }
        return ApiResponse.success(DecisionRecordResponse.from(record));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException exception) {
        return ApiResponse.failure("AUDIT_INVALID_REQUEST", exception.getMessage());
    }

    private DecisionRecordSearchQuery toQuery(DecisionRecordSearchRequest request) {
        DecisionRecordSearchQuery query = new DecisionRecordSearchQuery();
        query.setTenantKey(request.getTenantKey());
        query.setAppKey(request.getAppKey());
        query.setDecisionKey(request.getDecisionKey());
        query.setRequestKey(request.getRequestKey());
        query.setRequesterKind(request.getRequesterKind());
        query.setRequesterKey(request.getRequesterKey());
        query.setOperationCode(request.getOperationCode());
        query.setTargetResourceKey(request.getTargetResourceKey());
        query.setPlanDecision(request.getPlanDecision());
        query.setFailureCode(request.getFailureCode());
        query.setLimit(request.getLimit() == null ? 50 : request.getLimit().intValue());
        return query;
    }
}