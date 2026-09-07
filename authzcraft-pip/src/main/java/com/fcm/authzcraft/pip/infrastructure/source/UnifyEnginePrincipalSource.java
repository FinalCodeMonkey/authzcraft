package com.fcm.authzcraft.pip.infrastructure.source;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fcm.authzcraft.pip.domain.gateway.PrincipalSyncSource;
import com.fcm.authzcraft.pip.domain.model.PrincipalLifecycleState;
import com.fcm.authzcraft.pip.domain.model.PrincipalOrganization;
import com.fcm.authzcraft.pip.domain.model.PrincipalPosition;
import com.fcm.authzcraft.pip.domain.model.PrincipalSyncScope;
import com.fcm.authzcraft.pip.domain.model.PrincipalUser;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class UnifyEnginePrincipalSource implements PrincipalSyncSource {

    private final JdbcTemplate jdbcTemplate;
    private final UnifyEnginePrincipalSourceProperties properties;

    public UnifyEnginePrincipalSource(JdbcTemplate jdbcTemplate, UnifyEnginePrincipalSourceProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    @Override
    public String sourceCode() {
        return properties.getSourceCode();
    }

    @Override
    public List<PrincipalOrganization> loadOrganizations(PrincipalSyncScope scope) {
        String sql = "SELECT "
                + "COALESCE(NULLIF(department_code, ''), NULLIF(code, '')) AS department_code, "
                + "COALESCE(NULLIF(department_name, ''), NULLIF(name, '')) AS department_name, "
                + "department_level, department_type_code, department_category, "
                + "NULLIF(parent_department_code, '') AS parent_department_code, parent_department_name, "
                + "manage_user_id, manage_staff_no, manage_name, "
                + "portion_manage_user_id, portion_manage_staff_no, portion_manage_name, "
                + "is_enable, create_time, department_hrbp_list, isenable_code "
                + "FROM " + table("dap_sys_org") + " "
                + sourceWhere(scope)
                + " AND COALESCE(NULLIF(department_code, ''), NULLIF(code, '')) IS NOT NULL "
                + "ORDER BY id";
        return jdbcTemplate.query(sql, params(scope), this::mapOrganization);
    }

    @Override
    public List<PrincipalPosition> loadPositions(PrincipalSyncScope scope) {
        String sql = "SELECT "
                + "COALESCE(NULLIF(post_code, ''), NULLIF(code, '')) AS post_code, "
                + "COALESCE(NULLIF(post_name, ''), NULLIF(name, '')) AS post_name, "
                + "post_en_name, post_type_code, post_type, department_code, department_name, "
                + "position_grade_code, position_grade_name, is_enable, create_time, is_enable_code AS isenable_code "
                + "FROM " + table("dap_sys_post") + " "
                + sourceWhere(scope)
                + " AND COALESCE(NULLIF(post_code, ''), NULLIF(code, '')) IS NOT NULL "
                + "ORDER BY id";
        return jdbcTemplate.query(sql, params(scope), this::mapPosition);
    }

    @Override
    public List<PrincipalUser> loadUsers(PrincipalSyncScope scope) {
        String sql = "SELECT "
                + "COALESCE(NULLIF(user_id, ''), NULLIF(code, '')) AS user_id, "
                + "NULLIF(staff_no, '') AS staff_no, COALESCE(NULLIF(staff_name, ''), NULLIF(name, '')) AS staff_name, "
                + "department_code, department_name, post_code, post_name, "
                + "manage_user_id, manage_staff_no, manage_staff_name, CAST(staff_status AS CHAR) AS staff_status, "
                + "staff_status_code, is_dept_manage, is_dept_portion_manage "
                + "FROM " + table("dap_sys_user") + " "
                + sourceWhere(scope)
                + " AND COALESCE(NULLIF(user_id, ''), NULLIF(code, '')) IS NOT NULL "
                + "ORDER BY id";
        return jdbcTemplate.query(sql, params(scope), this::mapUser);
    }

    private PrincipalOrganization mapOrganization(ResultSet resultSet, int rowNum) throws SQLException {
        Long isEnable = getLong(resultSet, "is_enable");
        return new PrincipalOrganization(
                text(resultSet, "department_code"),
                text(resultSet, "department_name"),
                getLong(resultSet, "department_level"),
                text(resultSet, "department_type_code"),
                text(resultSet, "department_category"),
                text(resultSet, "parent_department_code"),
                text(resultSet, "parent_department_name"),
                text(resultSet, "manage_user_id"),
                text(resultSet, "manage_staff_no"),
                text(resultSet, "manage_name"),
                text(resultSet, "portion_manage_user_id"),
                text(resultSet, "portion_manage_staff_no"),
                text(resultSet, "portion_manage_name"),
                isEnable,
                resultSet.getTimestamp("create_time"),
                text(resultSet, "department_hrbp_list"),
                text(resultSet, "isenable_code"),
                stateFromEnable(isEnable));
    }

    private PrincipalPosition mapPosition(ResultSet resultSet, int rowNum) throws SQLException {
        Long isEnable = getLong(resultSet, "is_enable");
        return new PrincipalPosition(
                text(resultSet, "post_code"),
                text(resultSet, "post_name"),
                text(resultSet, "post_en_name"),
                text(resultSet, "post_type_code"),
                text(resultSet, "post_type"),
                text(resultSet, "department_code"),
                text(resultSet, "department_name"),
                text(resultSet, "position_grade_code"),
                text(resultSet, "position_grade_name"),
                isEnable,
                resultSet.getTimestamp("create_time"),
                text(resultSet, "isenable_code"),
                stateFromEnable(isEnable));
    }

    private PrincipalUser mapUser(ResultSet resultSet, int rowNum) throws SQLException {
        return new PrincipalUser(
                text(resultSet, "user_id"),
                text(resultSet, "staff_no"),
                text(resultSet, "staff_name"),
                text(resultSet, "department_code"),
                text(resultSet, "department_name"),
                text(resultSet, "post_code"),
                text(resultSet, "post_name"),
                text(resultSet, "manage_user_id"),
                text(resultSet, "manage_staff_no"),
                text(resultSet, "manage_staff_name"),
                text(resultSet, "staff_status"),
                text(resultSet, "staff_status_code"),
                getInteger(resultSet, "is_dept_manage"),
                getInteger(resultSet, "is_dept_portion_manage"),
                PrincipalLifecycleState.ACTIVE);
    }

    private String sourceWhere(PrincipalSyncScope scope) {
        String where = "WHERE is_delete = 0 AND tenant_id = ?";
        if (StringUtils.hasText(scope.getSourceAppCode())) {
            where += " AND app_code = ?";
        }
        return where;
    }

    private Object[] params(PrincipalSyncScope scope) {
        List<Object> params = new ArrayList<Object>();
        params.add(scope.getSourceTenantId());
        if (StringUtils.hasText(scope.getSourceAppCode())) {
            params.add(scope.getSourceAppCode());
        }
        return params.toArray();
    }

    private String table(String tableName) {
        String schemaName = properties.getSchemaName();
        if (!StringUtils.hasText(schemaName) || !schemaName.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("Invalid unify engine schema name: " + schemaName);
        }
        return "`" + schemaName + "`.`" + tableName + "`";
    }

    private PrincipalLifecycleState stateFromEnable(Long isEnable) {
        if (isEnable != null && isEnable.longValue() == 0L) {
            return PrincipalLifecycleState.INACTIVE;
        }
        return PrincipalLifecycleState.ACTIVE;
    }

    private String text(ResultSet resultSet, String columnName) throws SQLException {
        String value = resultSet.getString(columnName);
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Long getLong(ResultSet resultSet, String columnName) throws SQLException {
        Object value = resultSet.getObject(columnName);
        return value instanceof Number ? ((Number) value).longValue() : null;
    }

    private Integer getInteger(ResultSet resultSet, String columnName) throws SQLException {
        Object value = resultSet.getObject(columnName);
        return value instanceof Number ? ((Number) value).intValue() : null;
    }
}
