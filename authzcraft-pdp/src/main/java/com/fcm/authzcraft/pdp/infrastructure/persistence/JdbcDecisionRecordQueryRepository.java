package com.fcm.authzcraft.pdp.infrastructure.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.fcm.authzcraft.pdp.application.query.DecisionRecordSearchQuery;
import com.fcm.authzcraft.pdp.domain.model.DecisionRecord;
import com.fcm.authzcraft.pdp.domain.repository.DecisionRecordQueryRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class JdbcDecisionRecordQueryRepository implements DecisionRecordQueryRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcDecisionRecordQueryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<DecisionRecord> search(DecisionRecordSearchQuery query) {
        List<Object> params = new ArrayList<Object>();
        StringBuilder sql = new StringBuilder(baseSelect());
        sql.append(" WHERE 1 = 1");
        appendEquals(sql, params, "tenant_key", query.getTenantKey());
        appendEquals(sql, params, "app_key", query.getAppKey());
        appendEquals(sql, params, "decision_key", query.getDecisionKey());
        appendEquals(sql, params, "request_key", query.getRequestKey());
        appendEquals(sql, params, "requester_kind", query.getRequesterKind());
        appendEquals(sql, params, "requester_key", query.getRequesterKey());
        appendEquals(sql, params, "operation_code", query.getOperationCode());
        appendEquals(sql, params, "target_resource_key", query.getTargetResourceKey());
        appendEquals(sql, params, "plan_decision", query.getPlanDecision());
        appendEquals(sql, params, "failure_code", query.getFailureCode());
        sql.append(" ORDER BY decided_at DESC LIMIT ?");
        params.add(safeLimit(query.getLimit()));
        return jdbcTemplate.query(sql.toString(), params.toArray(), (resultSet, rowNum) -> mapRecord(resultSet));
    }

    @Override
    public DecisionRecord findLatestByDecisionKey(String decisionKey) {
        DecisionRecordSearchQuery query = new DecisionRecordSearchQuery();
        query.setDecisionKey(decisionKey);
        query.setLimit(1);
        List<DecisionRecord> records = search(query);
        return records.isEmpty() ? null : records.get(0);
    }

    private void appendEquals(StringBuilder sql, List<Object> params, String column, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        sql.append(" AND ").append(column).append(" = ?");
        params.add(value);
    }

    private int safeLimit(int limit) {
        if (limit <= 0) {
            return 50;
        }
        return Math.min(limit, 200);
    }

    private String baseSelect() {
        return "SELECT id, decided_at, decision_key, request_key, tenant_key, app_key, requester_kind, requester_key, "
                + "operation_code, target_resource_key, plan_decision, planner_kind, plan_digest, attribute_digest, "
                + "failure_code, planning_cost_ms FROM authzcraft_decision_record";
    }

    private DecisionRecord mapRecord(ResultSet resultSet) throws SQLException {
        DecisionRecord record = new DecisionRecord();
        record.setId(resultSet.getLong("id"));
        record.setDecidedAt(resultSet.getTimestamp("decided_at").toLocalDateTime());
        record.setDecisionKey(resultSet.getString("decision_key"));
        record.setRequestKey(resultSet.getString("request_key"));
        record.setTenantKey(resultSet.getString("tenant_key"));
        record.setAppKey(resultSet.getString("app_key"));
        record.setRequesterKind(resultSet.getString("requester_kind"));
        record.setRequesterKey(resultSet.getString("requester_key"));
        record.setOperationCode(resultSet.getString("operation_code"));
        record.setTargetResourceKey(resultSet.getString("target_resource_key"));
        record.setPlanDecision(resultSet.getString("plan_decision"));
        record.setPlannerKind(resultSet.getString("planner_kind"));
        record.setPlanDigest(resultSet.getString("plan_digest"));
        record.setAttributeDigest(resultSet.getString("attribute_digest"));
        record.setFailureCode(resultSet.getString("failure_code"));
        record.setPlanningCostMs(resultSet.getInt("planning_cost_ms"));
        return record;
    }
}