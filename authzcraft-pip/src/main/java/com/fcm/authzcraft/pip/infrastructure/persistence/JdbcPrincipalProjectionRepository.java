package com.fcm.authzcraft.pip.infrastructure.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.fcm.authzcraft.pip.domain.model.PrincipalKind;
import com.fcm.authzcraft.pip.domain.model.PrincipalLifecycleState;
import com.fcm.authzcraft.pip.domain.model.PrincipalOrganization;
import com.fcm.authzcraft.pip.domain.model.PrincipalPosition;
import com.fcm.authzcraft.pip.domain.model.PrincipalUser;
import com.fcm.authzcraft.pip.domain.repository.PrincipalProjectionRepository;
import com.fcm.authzcraft.pip.domain.service.PrincipalIdGenerator;
import com.fcm.authzcraft.pip.domain.service.PrincipalSyncProgressReporter;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
public class JdbcPrincipalProjectionRepository implements PrincipalProjectionRepository {

    private static final String SYNC_ACTOR = "authzcraft-pip-sync";
    private static final int PROGRESS_LOG_INTERVAL = 500;
    private static final int BATCH_SIZE = 500;
    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcPrincipalProjectionRepository.class);

    private final JdbcTemplate jdbcTemplate;
    private final PrincipalIdGenerator idGenerator;

    public JdbcPrincipalProjectionRepository(JdbcTemplate jdbcTemplate, PrincipalIdGenerator idGenerator) {
        this.jdbcTemplate = jdbcTemplate;
        this.idGenerator = idGenerator;
    }

    @Override
    public void saveOrganizations(String tenantKey, List<PrincipalOrganization> organizations,
                                  PrincipalSyncProgressReporter progressReporter) {
        List<PrincipalOrganization> valid = new ArrayList<PrincipalOrganization>();
        for (PrincipalOrganization organization : organizations) {
            if (StringUtils.hasText(organization.getDepartmentCode())) {
                valid.add(organization);
            }
        }
        batchUpsertOrganizations(tenantKey, valid, progressReporter);
    }

    @Override
    public void savePositions(String tenantKey, List<PrincipalPosition> positions,
                              PrincipalSyncProgressReporter progressReporter) {
        List<PrincipalPosition> valid = new ArrayList<PrincipalPosition>();
        for (PrincipalPosition position : positions) {
            if (StringUtils.hasText(position.getPostCode())) {
                valid.add(position);
            }
        }
        batchUpsertPositions(tenantKey, valid, progressReporter);
    }

    @Override
    public void saveUsers(String tenantKey, List<PrincipalUser> users,
                          PrincipalSyncProgressReporter progressReporter) {
        List<PrincipalUser> valid = new ArrayList<PrincipalUser>();
        for (PrincipalUser user : users) {
            if (StringUtils.hasText(user.getUserId())) {
                valid.add(user);
            }
        }
        batchUpsertUsers(tenantKey, valid, progressReporter);
    }

    @Override
    public void rebuildOrganizationClosure(String tenantKey, PrincipalSyncProgressReporter progressReporter) {
        progressReporter.startStage("REBUILD_ORGANIZATION_CLOSURE", 0);
        List<OrganizationNode> nodes = jdbcTemplate.query(
                "SELECT department_code, parent_department_code FROM authzcraft_principal_organization WHERE tenant_key = ?",
                new Object[]{tenantKey},
                (resultSet, rowNum) -> new OrganizationNode(
                        resultSet.getString("department_code"),
                        resultSet.getString("parent_department_code")));
        jdbcTemplate.update("DELETE FROM authzcraft_organization_closure WHERE tenant_key = ?", tenantKey);
        List<ClosureRow> rows = buildClosureRows(nodes);
        if (rows.isEmpty()) {
            progressReporter.finishStage("REBUILD_ORGANIZATION_CLOSURE", 0);
            return;
        }
        progressReporter.updateStage("REBUILD_ORGANIZATION_CLOSURE", 0, rows.size());
        LOGGER.info("Built organization closure rows, tenantKey={}, organizationCount={}, closureRowCount={}",
            tenantKey, nodes.size(), rows.size());
        jdbcTemplate.batchUpdate(
                "INSERT INTO authzcraft_organization_closure "
                        + "(ancestor_department_code, descendant_department_code, tenant_key, depth, created_by, updated_by) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement preparedStatement, int index) throws SQLException {
                        ClosureRow row = rows.get(index);
                        preparedStatement.setString(1, row.ancestorDepartmentCode);
                        preparedStatement.setString(2, row.descendantDepartmentCode);
                        preparedStatement.setString(3, tenantKey);
                        preparedStatement.setInt(4, row.depth);
                        preparedStatement.setString(5, SYNC_ACTOR);
                        preparedStatement.setString(6, SYNC_ACTOR);
                    }

                    @Override
                    public int getBatchSize() {
                        return rows.size();
                    }
                });
        progressReporter.finishStage("REBUILD_ORGANIZATION_CLOSURE", rows.size());
    }

    private void logProgress(String category, int processed, int total) {
        if (processed == total || processed % PROGRESS_LOG_INTERVAL == 0) {
            LOGGER.info("Saving principal {}, processed={}/{}", category, processed, total);
        }
    }

    private void batchUpsertOrganizations(String tenantKey, List<PrincipalOrganization> organizations,
                                          PrincipalSyncProgressReporter progressReporter) {
        int total = organizations.size();
        int processed = 0;
        for (int offset = 0; offset < total; offset += BATCH_SIZE) {
            List<PrincipalOrganization> batch = organizations.subList(offset, Math.min(offset + BATCH_SIZE, total));
            List<Object[]> principalParams = new ArrayList<Object[]>();
            List<Object[]> orgParams = new ArrayList<Object[]>();
            for (PrincipalOrganization organization : batch) {
                long principalId = resolvePrincipalId(tenantKey, PrincipalKind.ORGANIZATION, organization.getDepartmentCode());
                principalParams.add(new Object[]{
                        principalId, tenantKey, PrincipalKind.ORGANIZATION.name(), organization.getDepartmentCode(),
                        displayName(organization.getDepartmentName(), organization.getDepartmentCode()),
                        organization.getLifecycleState().name(), SYNC_ACTOR, SYNC_ACTOR});
                orgParams.add(new Object[]{
                        principalId, tenantKey, organization.getDepartmentCode(), organization.getDepartmentName(),
                        organization.getDepartmentLevel(), organization.getDepartmentTypeCode(), organization.getDepartmentCategory(),
                        organization.getParentDepartmentCode(), organization.getParentDepartmentName(), organization.getManageUserId(),
                        organization.getManageStaffNo(), organization.getManageName(), organization.getPortionManageUserId(),
                        organization.getPortionManageStaffNo(), organization.getPortionManageName(), organization.getIsEnable(),
                        organization.getCreateTime(), organization.getDepartmentHrbpList(), organization.getIsenableCode(),
                        SYNC_ACTOR, SYNC_ACTOR});
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO authzcraft_principal "
                            + "(id, tenant_key, principal_kind, principal_key, display_name, lifecycle_state, created_by, updated_by) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?) "
                            + "ON DUPLICATE KEY UPDATE display_name = VALUES(display_name), "
                            + "lifecycle_state = VALUES(lifecycle_state), updated_by = VALUES(updated_by), "
                            + "record_version = record_version + 1",
                    principalParams);
            jdbcTemplate.batchUpdate(
                    "INSERT INTO authzcraft_principal_organization "
                            + "(principal_id, principal_kind, tenant_key, department_code, department_name, department_level, "
                            + "department_type_code, department_category, parent_department_code, parent_department_name, "
                            + "manage_user_id, manage_staff_no, manage_name, portion_manage_user_id, portion_manage_staff_no, "
                            + "portion_manage_name, is_enable, create_time, department_hrbp_list, isenable_code, created_by, updated_by) "
                            + "VALUES (?, 'ORGANIZATION', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                            + "ON DUPLICATE KEY UPDATE department_name = VALUES(department_name), "
                            + "department_level = VALUES(department_level), department_type_code = VALUES(department_type_code), "
                            + "department_category = VALUES(department_category), parent_department_code = VALUES(parent_department_code), "
                            + "parent_department_name = VALUES(parent_department_name), manage_user_id = VALUES(manage_user_id), "
                            + "manage_staff_no = VALUES(manage_staff_no), manage_name = VALUES(manage_name), "
                            + "portion_manage_user_id = VALUES(portion_manage_user_id), "
                            + "portion_manage_staff_no = VALUES(portion_manage_staff_no), "
                            + "portion_manage_name = VALUES(portion_manage_name), is_enable = VALUES(is_enable), "
                            + "create_time = VALUES(create_time), department_hrbp_list = VALUES(department_hrbp_list), "
                            + "isenable_code = VALUES(isenable_code), updated_by = VALUES(updated_by)",
                    orgParams);
            processed += batch.size();
            logProgress("organizations", processed, total);
            progressReporter.updateStage("SAVE_ORGANIZATIONS", processed, total);
        }
    }

    private void batchUpsertPositions(String tenantKey, List<PrincipalPosition> positions,
                                      PrincipalSyncProgressReporter progressReporter) {
        int total = positions.size();
        int processed = 0;
        for (int offset = 0; offset < total; offset += BATCH_SIZE) {
            List<PrincipalPosition> batch = positions.subList(offset, Math.min(offset + BATCH_SIZE, total));
            List<Object[]> principalParams = new ArrayList<Object[]>();
            List<Object[]> positionParams = new ArrayList<Object[]>();
            for (PrincipalPosition position : batch) {
                long principalId = resolvePrincipalId(tenantKey, PrincipalKind.POSITION, position.getPostCode());
                principalParams.add(new Object[]{
                        principalId, tenantKey, PrincipalKind.POSITION.name(), position.getPostCode(),
                        displayName(position.getPostName(), position.getPostCode()),
                        position.getLifecycleState().name(), SYNC_ACTOR, SYNC_ACTOR});
                positionParams.add(new Object[]{
                        principalId, tenantKey, position.getPostCode(), position.getPostName(), position.getPostEnName(),
                        position.getPostTypeCode(), position.getPostType(), position.getDepartmentCode(), position.getDepartmentName(),
                        position.getPositionGradeCode(), position.getPositionGradeName(), position.getIsEnable(), position.getCreateTime(),
                        position.getIsenableCode(), SYNC_ACTOR, SYNC_ACTOR});
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO authzcraft_principal "
                            + "(id, tenant_key, principal_kind, principal_key, display_name, lifecycle_state, created_by, updated_by) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?) "
                            + "ON DUPLICATE KEY UPDATE display_name = VALUES(display_name), "
                            + "lifecycle_state = VALUES(lifecycle_state), updated_by = VALUES(updated_by), "
                            + "record_version = record_version + 1",
                    principalParams);
            jdbcTemplate.batchUpdate(
                    "INSERT INTO authzcraft_principal_position "
                            + "(principal_id, principal_kind, tenant_key, post_code, post_name, post_en_name, post_type_code, "
                            + "post_type, department_code, department_name, position_grade_code, position_grade_name, is_enable, "
                            + "create_time, isenable_code, created_by, updated_by) "
                            + "VALUES (?, 'POSITION', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                            + "ON DUPLICATE KEY UPDATE post_name = VALUES(post_name), post_en_name = VALUES(post_en_name), "
                            + "post_type_code = VALUES(post_type_code), post_type = VALUES(post_type), "
                            + "department_code = VALUES(department_code), department_name = VALUES(department_name), "
                            + "position_grade_code = VALUES(position_grade_code), position_grade_name = VALUES(position_grade_name), "
                            + "is_enable = VALUES(is_enable), create_time = VALUES(create_time), "
                            + "isenable_code = VALUES(isenable_code), updated_by = VALUES(updated_by)",
                    positionParams);
            processed += batch.size();
            logProgress("positions", processed, total);
            progressReporter.updateStage("SAVE_POSITIONS", processed, total);
        }
    }

    private void batchUpsertUsers(String tenantKey, List<PrincipalUser> users,
                                  PrincipalSyncProgressReporter progressReporter) {
        int total = users.size();
        int processed = 0;
        for (int offset = 0; offset < total; offset += BATCH_SIZE) {
            List<PrincipalUser> batch = users.subList(offset, Math.min(offset + BATCH_SIZE, total));
            List<Object[]> principalParams = new ArrayList<Object[]>();
            List<Object[]> userParams = new ArrayList<Object[]>();
            for (PrincipalUser user : batch) {
                long principalId = resolvePrincipalId(tenantKey, PrincipalKind.USER, user.getUserId());
                principalParams.add(new Object[]{
                        principalId, tenantKey, PrincipalKind.USER.name(), user.getUserId(),
                        displayName(user.getStaffName(), user.getUserId()),
                        user.getLifecycleState().name(), SYNC_ACTOR, SYNC_ACTOR});
                userParams.add(new Object[]{
                        principalId, tenantKey, user.getUserId(), user.getStaffNo(), user.getStaffName(), user.getDepartmentCode(),
                        user.getDepartmentName(), user.getPostCode(), user.getPostName(), user.getManageUserId(),
                        user.getManageStaffNo(), user.getManageStaffName(), user.getStaffStatus(), user.getStaffStatusCode(),
                        user.getIsDeptManage(), user.getIsDeptPortionManage(), SYNC_ACTOR, SYNC_ACTOR});
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO authzcraft_principal "
                            + "(id, tenant_key, principal_kind, principal_key, display_name, lifecycle_state, created_by, updated_by) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?) "
                            + "ON DUPLICATE KEY UPDATE display_name = VALUES(display_name), "
                            + "lifecycle_state = VALUES(lifecycle_state), updated_by = VALUES(updated_by), "
                            + "record_version = record_version + 1",
                    principalParams);
            jdbcTemplate.batchUpdate(
                    "INSERT INTO authzcraft_principal_user "
                            + "(principal_id, principal_kind, tenant_key, user_id, staff_no, staff_name, department_code, "
                            + "department_name, post_code, post_name, manage_user_id, manage_staff_no, manage_staff_name, "
                            + "staff_status, staff_status_code, is_dept_manage, is_dept_portion_manage, created_by, updated_by) "
                            + "VALUES (?, 'USER', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                            + "ON DUPLICATE KEY UPDATE staff_no = VALUES(staff_no), staff_name = VALUES(staff_name), "
                            + "department_code = VALUES(department_code), department_name = VALUES(department_name), "
                            + "post_code = VALUES(post_code), post_name = VALUES(post_name), "
                            + "manage_user_id = VALUES(manage_user_id), manage_staff_no = VALUES(manage_staff_no), "
                            + "manage_staff_name = VALUES(manage_staff_name), staff_status = VALUES(staff_status), "
                            + "staff_status_code = VALUES(staff_status_code), is_dept_manage = VALUES(is_dept_manage), "
                            + "is_dept_portion_manage = VALUES(is_dept_portion_manage), updated_by = VALUES(updated_by)",
                    userParams);
            processed += batch.size();
            logProgress("users", processed, total);
            progressReporter.updateStage("SAVE_USERS", processed, total);
        }
    }

    private List<ClosureRow> buildClosureRows(List<OrganizationNode> nodes) {
        Map<String, String> parentByCode = new HashMap<String, String>();
        for (OrganizationNode node : nodes) {
            if (StringUtils.hasText(node.departmentCode)) {
                parentByCode.put(node.departmentCode, blankToNull(node.parentDepartmentCode));
            }
        }
        List<ClosureRow> rows = new ArrayList<ClosureRow>();
        for (String descendantCode : parentByCode.keySet()) {
            rows.add(new ClosureRow(descendantCode, descendantCode, 0));
            String ancestorCode = parentByCode.get(descendantCode);
            int depth = 1;
            Set<String> visited = new HashSet<String>();
            visited.add(descendantCode);
            while (StringUtils.hasText(ancestorCode) && parentByCode.containsKey(ancestorCode) && visited.add(ancestorCode)) {
                rows.add(new ClosureRow(ancestorCode, descendantCode, depth));
                ancestorCode = parentByCode.get(ancestorCode);
                depth++;
            }
        }
        return rows;
    }

    private long resolvePrincipalId(String tenantKey, PrincipalKind principalKind, String principalKey) {
        List<Long> existingIds = jdbcTemplate.query(
                "SELECT id FROM authzcraft_principal WHERE tenant_key = ? AND principal_kind = ? AND principal_key = ?",
                new Object[]{tenantKey, principalKind.name(), principalKey},
                (resultSet, rowNum) -> resultSet.getLong("id"));
        if (!existingIds.isEmpty()) {
            return existingIds.get(0);
        }
        return idGenerator.nextId();
    }

    private String displayName(String displayName, String principalKey) {
        return StringUtils.hasText(displayName) ? displayName : principalKey;
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }

    private static class OrganizationNode {
        private final String departmentCode;
        private final String parentDepartmentCode;

        private OrganizationNode(String departmentCode, String parentDepartmentCode) {
            this.departmentCode = departmentCode;
            this.parentDepartmentCode = parentDepartmentCode;
        }
    }

    private static class ClosureRow {
        private final String ancestorDepartmentCode;
        private final String descendantDepartmentCode;
        private final int depth;

        private ClosureRow(String ancestorDepartmentCode, String descendantDepartmentCode, int depth) {
            this.ancestorDepartmentCode = ancestorDepartmentCode;
            this.descendantDepartmentCode = descendantDepartmentCode;
            this.depth = depth;
        }
    }
}
