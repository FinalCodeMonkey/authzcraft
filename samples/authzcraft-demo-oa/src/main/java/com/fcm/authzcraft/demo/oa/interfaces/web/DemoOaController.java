package com.fcm.authzcraft.demo.oa.interfaces.web;

import com.fcm.authzcraft.common.web.ApiResponse;
import com.fcm.authzcraft.demo.oa.application.service.DemoOaQueryService;
import com.fcm.authzcraft.demo.oa.interfaces.dto.DemoOaSearchRequest;
import com.fcm.authzcraft.demo.oa.interfaces.security.RequirePermission;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/authzcraft-demo-oa/api/v1")
public class DemoOaController {
    private final DemoOaQueryService queryService;

    public DemoOaController(DemoOaQueryService queryService) {
        this.queryService = queryService;
    }

    @RequirePermission("demo-oa:leave-requests:read")
    @PostMapping("/leave-requests/search")
    public ApiResponse<List<Map<String, Object>>> searchLeaveRequests(@RequestBody(required = false) DemoOaSearchRequest request) {
        return ApiResponse.success(queryService.searchLeaveRequests(request));
    }

    @RequirePermission("demo-oa:expense-requests:read")
    @PostMapping("/expense-requests/search")
    public ApiResponse<List<Map<String, Object>>> searchExpenseRequests(@RequestBody(required = false) DemoOaSearchRequest request) {
        return ApiResponse.success(queryService.searchExpenseRequests(request));
    }

    @RequirePermission("demo-oa:leave-approvals:read")
    @PostMapping("/leave-approvals/search")
    public ApiResponse<List<Map<String, Object>>> searchLeaveApprovals(@RequestBody(required = false) DemoOaSearchRequest request) {
        return ApiResponse.success(queryService.searchLeaveApprovals(request));
    }

    @RequirePermission("demo-oa:meetings:read")
    @PostMapping("/meetings/search")
    public ApiResponse<List<Map<String, Object>>> searchMeetings(@RequestBody(required = false) DemoOaSearchRequest request) {
        return ApiResponse.success(queryService.searchMeetings(request));
    }

    @RequirePermission("demo-oa:meeting-participants:read")
    @PostMapping("/meeting-participants/search")
    public ApiResponse<List<Map<String, Object>>> searchMeetingParticipants(@RequestBody(required = false) DemoOaSearchRequest request) {
        return ApiResponse.success(queryService.searchMeetingParticipants(request));
    }

    @RequirePermission("demo-oa:meetings:read")
    @PostMapping("/meetings/count")
    public ApiResponse<Long> countMeetings() {
        return ApiResponse.success(queryService.countMeetings());
    }

    @RequirePermission("demo-oa:meeting-participants:read")
    @PostMapping("/meeting-participants/count")
    public ApiResponse<Long> countMeetingParticipants(@RequestBody(required = false) Map<String, Object> request) {
        Object meetingId = request == null ? null : request.get("meetingId");
        return ApiResponse.success(meetingId == null || !StringUtils.hasText(meetingId.toString())
                ? queryService.countMeetingParticipants()
                : queryService.countMeetingParticipantsByMeetingId(meetingId.toString()));
    }

    @RequirePermission("demo-oa:leave-requests:read")
    @PostMapping("/leave-requests/count")
    public ApiResponse<Long> countLeaveRequests() {
        return ApiResponse.success(queryService.countLeaveRequests());
    }

    @RequirePermission("demo-oa:leave-approvals:read")
    @PostMapping("/leave-approvals/count")
    public ApiResponse<Long> countLeaveApprovals(@RequestBody(required = false) Map<String, Object> request) {
        Object leaveRequestId = request == null ? null : request.get("leaveRequestId");
        return ApiResponse.success(leaveRequestId == null || !StringUtils.hasText(leaveRequestId.toString())
                ? queryService.countLeaveApprovals()
                : queryService.countLeaveApprovalsByLeaveRequestId(leaveRequestId.toString()));
    }

    @RequirePermission("demo-oa:expense-requests:read")
    @PostMapping("/expense-requests/count")
    public ApiResponse<Long> countExpenseRequests() {
        return ApiResponse.success(queryService.countExpenseRequests());
    }

    // Leave request CRUD
    @RequirePermission("demo-oa:leave-requests:create")
    @PostMapping("/leave-requests/create")
    public ApiResponse<Void> createLeaveRequest(@RequestBody Map<String, Object> request) {
        queryService.createLeaveRequest(request);
        return ApiResponse.success();
    }

    @RequirePermission("demo-oa:leave-requests:update")
    @PostMapping("/leave-requests/{id}/update")
    public ApiResponse<Void> updateLeaveRequest(@org.springframework.web.bind.annotation.PathVariable String id, @RequestBody Map<String, Object> request) {
        request.put("id", id);
        queryService.updateLeaveRequest(request);
        return ApiResponse.success();
    }

    @RequirePermission("demo-oa:leave-requests:delete")
    @PostMapping("/leave-requests/{id}/delete")
    public ApiResponse<Void> deleteLeaveRequest(@org.springframework.web.bind.annotation.PathVariable String id) {
        queryService.deleteLeaveRequest(id);
        return ApiResponse.success();
    }

    // Expense request CRUD
    @RequirePermission("demo-oa:expense-requests:create")
    @PostMapping("/expense-requests/create")
    public ApiResponse<Void> createExpenseRequest(@RequestBody Map<String, Object> request) {
        queryService.createExpenseRequest(request);
        return ApiResponse.success();
    }

    @RequirePermission("demo-oa:expense-requests:update")
    @PostMapping("/expense-requests/{id}/update")
    public ApiResponse<Void> updateExpenseRequest(@org.springframework.web.bind.annotation.PathVariable String id, @RequestBody Map<String, Object> request) {
        request.put("id", id);
        queryService.updateExpenseRequest(request);
        return ApiResponse.success();
    }

    @RequirePermission("demo-oa:expense-requests:delete")
    @PostMapping("/expense-requests/{id}/delete")
    public ApiResponse<Void> deleteExpenseRequest(@org.springframework.web.bind.annotation.PathVariable String id) {
        queryService.deleteExpenseRequest(id);
        return ApiResponse.success();
    }
}