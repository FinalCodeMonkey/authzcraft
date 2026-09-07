package com.fcm.authzcraft.demo.oa.infrastructure.persistence;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

public interface DemoOaMapper {

    @Select("SELECT * FROM wb_leave_request WHERE is_deleted = 0 ORDER BY id DESC LIMIT #{limit}")
    List<Map<String, Object>> searchLeaveRequests(@Param("limit") int limit);

    @Select("SELECT * FROM wb_expense_request WHERE is_deleted = 0 ORDER BY id DESC LIMIT #{limit}")
    List<Map<String, Object>> searchExpenseRequests(@Param("limit") int limit);

    @Select("SELECT * FROM wb_leave_request_approval WHERE is_deleted = 0 ORDER BY id DESC LIMIT #{limit}")
    List<Map<String, Object>> searchLeaveApprovals(@Param("limit") int limit);

    @Select("SELECT * FROM wb_meeting WHERE is_deleted = 0 ORDER BY start_time DESC LIMIT #{limit}")
    List<Map<String, Object>> searchMeetings(@Param("limit") int limit);

    @Select("SELECT * FROM wb_meeting_participant WHERE is_deleted = 0 ORDER BY id DESC LIMIT #{limit}")
    List<Map<String, Object>> searchMeetingParticipants(@Param("limit") int limit);

    // 未过滤总数（不配置 PEP 映射，供权限演示展示“共 X 条”不受权限控制的数据总数）
    @Select("SELECT COUNT(*) FROM wb_leave_request WHERE is_deleted = 0")
    long countLeaveRequests();

    @Select("SELECT COUNT(*) FROM wb_expense_request WHERE is_deleted = 0")
    long countExpenseRequests();

    @Select("SELECT COUNT(*) FROM wb_leave_request_approval WHERE is_deleted = 0")
    long countLeaveApprovals();

    @Select("SELECT COUNT(*) FROM wb_leave_request_approval WHERE is_deleted = 0 AND leave_request_id = #{leaveRequestId}")
    long countLeaveApprovalsByLeaveRequestId(@Param("leaveRequestId") String leaveRequestId);

    @Select("SELECT COUNT(*) FROM wb_meeting WHERE is_deleted = 0")
    long countMeetings();

    @Select("SELECT COUNT(*) FROM wb_meeting_participant WHERE is_deleted = 0")
    long countMeetingParticipants();

    @Select("SELECT COUNT(*) FROM wb_meeting_participant WHERE is_deleted = 0 AND meeting_id = #{meetingId}")
    long countMeetingParticipantsByMeetingId(@Param("meetingId") String meetingId);

    // Leave request CRUD
    @Insert("INSERT INTO wb_leave_request (id, applicant_user_id, applicant_name, applicant_dept_code, leave_type, start_date, end_date, leave_days, reason, contact_phone, internal_note, status, created_by, updated_by, created_at, updated_at) "
            + "VALUES (UUID_SHORT(), #{applicantUserId}, #{applicantName}, #{applicantDeptCode}, #{leaveType}, #{startDate}, #{endDate}, #{leaveDays}, #{reason}, #{contactPhone}, #{internalNote}, #{status}, #{createdBy}, #{createdBy}, NOW(), NOW())")
    int insertLeaveRequest(@Param("applicantUserId") String applicantUserId, @Param("applicantName") String applicantName,
                           @Param("applicantDeptCode") String applicantDeptCode, @Param("leaveType") String leaveType,
                           @Param("startDate") String startDate, @Param("endDate") String endDate,
                           @Param("leaveDays") String leaveDays, @Param("reason") String reason,
                           @Param("contactPhone") String contactPhone, @Param("internalNote") String internalNote,
                           @Param("status") String status, @Param("createdBy") String createdBy);

    @Update("UPDATE wb_leave_request SET applicant_user_id=#{applicantUserId}, applicant_name=#{applicantName}, applicant_dept_code=#{applicantDeptCode}, "
            + "leave_type=#{leaveType}, start_date=#{startDate}, end_date=#{endDate}, leave_days=#{leaveDays}, reason=#{reason}, contact_phone=#{contactPhone}, internal_note=#{internalNote}, status=#{status}, updated_by=#{updatedBy}, updated_at=NOW() WHERE id=#{id}")
    int updateLeaveRequest(@Param("id") String id, @Param("applicantUserId") String applicantUserId, @Param("applicantName") String applicantName,
                           @Param("applicantDeptCode") String applicantDeptCode, @Param("leaveType") String leaveType,
                           @Param("startDate") String startDate, @Param("endDate") String endDate,
                           @Param("leaveDays") String leaveDays, @Param("reason") String reason,
                           @Param("contactPhone") String contactPhone, @Param("internalNote") String internalNote,
                           @Param("status") String status, @Param("updatedBy") String updatedBy);

    @Update("UPDATE wb_leave_request SET is_deleted = 1 WHERE id = #{id}")
    int deleteLeaveRequest(@Param("id") String id);

    // Expense request CRUD
    @Insert("INSERT INTO wb_expense_request (id, request_no, title, applicant_user_id, dept_code, expense_type, amount, reason, phone, id_card_no, status, request_date, created_by, updated_by, created_time, updated_time) "
            + "VALUES (UUID_SHORT(), #{requestNo}, #{title}, #{applicantUserId}, #{deptCode}, #{expenseType}, #{amount}, #{reason}, #{phone}, #{idCardNo}, #{status}, CURDATE(), #{createdBy}, #{createdBy}, NOW(), NOW())")
    int insertExpenseRequest(@Param("requestNo") String requestNo, @Param("title") String title,
                             @Param("applicantUserId") String applicantUserId, @Param("deptCode") String deptCode,
                             @Param("expenseType") String expenseType, @Param("amount") String amount,
                             @Param("reason") String reason, @Param("phone") String phone, @Param("idCardNo") String idCardNo,
                             @Param("status") String status, @Param("createdBy") String createdBy);

    @Update("UPDATE wb_expense_request SET request_no=#{requestNo}, title=#{title}, applicant_user_id=#{applicantUserId}, dept_code=#{deptCode}, "
            + "expense_type=#{expenseType}, amount=#{amount}, reason=#{reason}, phone=#{phone}, id_card_no=#{idCardNo}, status=#{status}, updated_by=#{updatedBy}, updated_time=NOW() WHERE id=#{id}")
    int updateExpenseRequest(@Param("id") String id, @Param("requestNo") String requestNo, @Param("title") String title,
                             @Param("applicantUserId") String applicantUserId, @Param("deptCode") String deptCode,
                             @Param("expenseType") String expenseType, @Param("amount") String amount,
                             @Param("reason") String reason, @Param("phone") String phone, @Param("idCardNo") String idCardNo,
                             @Param("status") String status, @Param("updatedBy") String updatedBy);

    @Update("UPDATE wb_expense_request SET is_deleted = 1 WHERE id = #{id}")
    int deleteExpenseRequest(@Param("id") String id);
}