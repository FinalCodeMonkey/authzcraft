package com.fcm.authzcraft.demo.oa.application.service;

import com.fcm.authzcraft.demo.oa.interfaces.dto.DemoOaSearchRequest;

import java.util.List;
import java.util.Map;

public interface DemoOaQueryService {
    List<Map<String, Object>> searchLeaveRequests(DemoOaSearchRequest request);

    List<Map<String, Object>> searchExpenseRequests(DemoOaSearchRequest request);

    List<Map<String, Object>> searchLeaveApprovals(DemoOaSearchRequest request);

    List<Map<String, Object>> searchMeetings(DemoOaSearchRequest request);

    List<Map<String, Object>> searchMeetingParticipants(DemoOaSearchRequest request);

    long countLeaveRequests();

    long countExpenseRequests();

    long countLeaveApprovals();

    long countLeaveApprovalsByLeaveRequestId(String leaveRequestId);

    long countMeetings();

    long countMeetingParticipants();

    long countMeetingParticipantsByMeetingId(String meetingId);

    void createLeaveRequest(Map<String, Object> request);
    void updateLeaveRequest(Map<String, Object> request);
    void deleteLeaveRequest(String id);

    void createExpenseRequest(Map<String, Object> request);
    void updateExpenseRequest(Map<String, Object> request);
    void deleteExpenseRequest(String id);
}