package com.fcm.authzcraft.demo.oa.application.service.impl;

import com.fcm.authzcraft.demo.oa.application.service.DemoOaQueryService;
import com.fcm.authzcraft.demo.oa.application.service.RbacApplicationService;
import com.fcm.authzcraft.demo.oa.infrastructure.persistence.DemoOaMapper;
import com.fcm.authzcraft.demo.oa.interfaces.dto.DemoOaSearchRequest;
import com.fcm.authzcraft.demo.oa.interfaces.security.CurrentUserContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DemoOaQueryServiceImpl implements DemoOaQueryService {
    private final DemoOaMapper mapper;
    private final RbacApplicationService rbacApplicationService;

    public DemoOaQueryServiceImpl(DemoOaMapper mapper, RbacApplicationService rbacApplicationService) {
        this.mapper = mapper;
        this.rbacApplicationService = rbacApplicationService;
    }

    @Override
    public List<Map<String, Object>> searchLeaveRequests(DemoOaSearchRequest request) {
        return rbacApplicationService.projectRows(CurrentUserContext.get(), "WB_LEAVE_REQUEST", mapper.searchLeaveRequests(resolveLimit(request)));
    }

    @Override
    public List<Map<String, Object>> searchExpenseRequests(DemoOaSearchRequest request) {
        return rbacApplicationService.projectRows(CurrentUserContext.get(), "WB_EXPENSE_REQUEST", mapper.searchExpenseRequests(resolveLimit(request)));
    }

    @Override
    public List<Map<String, Object>> searchLeaveApprovals(DemoOaSearchRequest request) {
        return rbacApplicationService.projectRows(CurrentUserContext.get(), "WB_LEAVE_REQUEST_APPROVAL", mapper.searchLeaveApprovals(resolveLimit(request)));
    }

    @Override
    public List<Map<String, Object>> searchMeetings(DemoOaSearchRequest request) {
        return rbacApplicationService.projectRows(CurrentUserContext.get(), "WB_MEETING", mapper.searchMeetings(resolveLimit(request)));
    }

    @Override
    public List<Map<String, Object>> searchMeetingParticipants(DemoOaSearchRequest request) {
        return rbacApplicationService.projectRows(CurrentUserContext.get(), "WB_MEETING_PARTICIPANT", mapper.searchMeetingParticipants(resolveLimit(request)));
    }

    @Override
    public long countLeaveRequests() {
        return mapper.countLeaveRequests();
    }

    @Override
    public long countExpenseRequests() {
        return mapper.countExpenseRequests();
    }

    @Override
    public long countLeaveApprovals() {
        return mapper.countLeaveApprovals();
    }

    @Override
    public long countLeaveApprovalsByLeaveRequestId(String leaveRequestId) {
        return mapper.countLeaveApprovalsByLeaveRequestId(leaveRequestId);
    }

    @Override
    public long countMeetings() {
        return mapper.countMeetings();
    }

    @Override
    public long countMeetingParticipants() {
        return mapper.countMeetingParticipants();
    }

    @Override
    public long countMeetingParticipantsByMeetingId(String meetingId) {
        return mapper.countMeetingParticipantsByMeetingId(meetingId);
    }

    private int resolveLimit(DemoOaSearchRequest request) {
        if (request == null || request.getLimit() == null) {
            return 100;
        }
        return Math.max(1, Math.min(request.getLimit().intValue(), 500));
    }

    private String text(Map<String, Object> request, String key) {
        Object val = request == null ? null : request.get(key);
        return val == null ? "" : val.toString();
    }

    @Override
    public void createLeaveRequest(Map<String, Object> request) {
        String currentUser = CurrentUserContext.get() != null ? CurrentUserContext.get().getUserKey() : "system";
        mapper.insertLeaveRequest(text(request, "applicant_user_id"), text(request, "applicant_name"),
                text(request, "applicant_dept_code"), text(request, "leave_type"), text(request, "start_date"),
                text(request, "end_date"), text(request, "leave_days"), text(request, "reason"),
                text(request, "contact_phone"), text(request, "internal_note"), text(request, "status"),
                currentUser);
    }

    @Override
    public void updateLeaveRequest(Map<String, Object> request) {
        String currentUser = CurrentUserContext.get() != null ? CurrentUserContext.get().getUserKey() : "system";
        mapper.updateLeaveRequest(text(request, "id"), text(request, "applicant_user_id"), text(request, "applicant_name"),
                text(request, "applicant_dept_code"), text(request, "leave_type"), text(request, "start_date"),
                text(request, "end_date"), text(request, "leave_days"), text(request, "reason"),
                text(request, "contact_phone"), text(request, "internal_note"), text(request, "status"),
                currentUser);
    }

    @Override
    public void deleteLeaveRequest(String id) {
        mapper.deleteLeaveRequest(id);
    }

    @Override
    public void createExpenseRequest(Map<String, Object> request) {
        String currentUser = CurrentUserContext.get() != null ? CurrentUserContext.get().getUserKey() : "system";
        mapper.insertExpenseRequest(text(request, "request_no"), text(request, "title"), text(request, "applicant_user_id"),
                text(request, "dept_code"), text(request, "expense_type"), text(request, "amount"),
                text(request, "reason"), text(request, "phone"), text(request, "id_card_no"),
                text(request, "status"), currentUser);
    }

    @Override
    public void updateExpenseRequest(Map<String, Object> request) {
        String currentUser = CurrentUserContext.get() != null ? CurrentUserContext.get().getUserKey() : "system";
        mapper.updateExpenseRequest(text(request, "id"), text(request, "request_no"), text(request, "title"),
                text(request, "applicant_user_id"), text(request, "dept_code"), text(request, "expense_type"),
                text(request, "amount"), text(request, "reason"), text(request, "phone"), text(request, "id_card_no"),
                text(request, "status"), currentUser);
    }

    @Override
    public void deleteExpenseRequest(String id) {
        mapper.deleteExpenseRequest(id);
    }
}