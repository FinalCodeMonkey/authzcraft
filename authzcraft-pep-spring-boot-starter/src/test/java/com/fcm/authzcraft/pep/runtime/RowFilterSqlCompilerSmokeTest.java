package com.fcm.authzcraft.pep.runtime;

import com.fcm.authzcraft.api.common.ValueKind;
import com.fcm.authzcraft.api.plan.BindingValue;
import com.fcm.authzcraft.api.plan.PlanDecision;
import com.fcm.authzcraft.api.plan.RowFilterPlan;
import com.fcm.authzcraft.api.predicate.PredicateExpression;
import com.fcm.authzcraft.api.predicate.PredicateExpressionKind;
import com.fcm.authzcraft.api.predicate.PredicateNode;
import com.fcm.authzcraft.api.predicate.PredicateOperator;
import com.fcm.authzcraft.pep.autoconfigure.AuthzCraftPepProperties;
import com.fcm.authzcraft.pep.runtime.CompiledSqlFilter;
import com.fcm.authzcraft.pep.runtime.RowFilterSqlCompiler;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RowFilterSqlCompilerSmokeTest {

    @Test
    void compilesInPredicateBeforeOrderBy() {
        AuthzCraftPepProperties.ResourceMapping mapping = new AuthzCraftPepProperties.ResourceMapping();
        mapping.setResourceKey("WB_MEETING");
        mapping.getFieldMappings().put("host_dept_code", "host_dept_code");

        RowFilterPlan plan = new RowFilterPlan();
        plan.setPlanDecision(PlanDecision.FILTER);
        plan.setPredicate(inPredicate("host_dept_code", "g1_managedDepartmentCodes"));
        plan.getBindings().put("g1_managedDepartmentCodes",
                new BindingValue(ValueKind.STRING_SET, Arrays.asList("000", "000002"), false));

        CompiledSqlFilter filter = new RowFilterSqlCompiler().compile(
                "select meeting_id, host_dept_code from wb_meeting where status = ? order by meeting_id desc",
                plan,
                mapping);

        assertEquals("select meeting_id, host_dept_code from wb_meeting where status = ? AND (host_dept_code IN (?, ?)) order by meeting_id desc",
                filter.getSql());
        assertEquals(Arrays.<Object>asList("000", "000002"), filter.getParameters());
        assertEquals(1, filter.getParameterMappingIndex());
    }

    @Test
    void compilesDenyAllAsFalseCondition() {
        AuthzCraftPepProperties.ResourceMapping mapping = new AuthzCraftPepProperties.ResourceMapping();
        mapping.setResourceKey("WB_MEETING");

        RowFilterPlan plan = new RowFilterPlan();
        plan.setPlanDecision(PlanDecision.DENY_ALL);

        CompiledSqlFilter filter = new RowFilterSqlCompiler().compile("select * from wb_meeting", plan, mapping);

        assertEquals("select * from wb_meeting WHERE (1 = 0)", filter.getSql());
        assertTrue(filter.getParameters().isEmpty());
        assertEquals(0, filter.getParameterMappingIndex());
    }

    @Test
    void compilesSingleStepExistsPath() {
        AuthzCraftPepProperties.ResourceMapping mapping = new AuthzCraftPepProperties.ResourceMapping();
        AuthzCraftPepProperties.AccessPathMapping accessPath = new AuthzCraftPepProperties.AccessPathMapping();
        accessPath.setTargetTable("wb_meeting_participant");
        accessPath.setSourceField("wb_meeting.meeting_id");
        accessPath.setTargetField("meeting_id");
        accessPath.getTargetFieldMappings().put("participant_dept_code", "participant_dept_code");
        mapping.getAccessPaths().put("WB_MEETING_TO_PARTICIPANT", accessPath);

        PredicateNode predicate = new PredicateNode(PredicateOperator.EXISTS_PATH);
        predicate.setAccessPathKey("WB_MEETING_TO_PARTICIPANT");
        predicate.setPathPredicate(inPredicate("participant_dept_code", "g1_managedDepartmentCodes"));

        RowFilterPlan plan = new RowFilterPlan();
        plan.setPlanDecision(PlanDecision.FILTER);
        plan.setTargetResourceKey("WB_MEETING");
        plan.setPredicate(predicate);
        plan.setRequiredAccessPathKeys(Collections.singletonList("WB_MEETING_TO_PARTICIPANT"));
        plan.getBindings().put("g1_managedDepartmentCodes",
                new BindingValue(ValueKind.STRING_SET, Arrays.asList("000", "000002"), false));

        CompiledSqlFilter filter = new RowFilterSqlCompiler().compile(
                "SELECT id, meeting_id FROM wb_meeting WHERE is_deleted = 0 ORDER BY start_time DESC LIMIT ?",
                plan,
                mapping);

        assertEquals("SELECT id, meeting_id FROM wb_meeting WHERE is_deleted = 0 AND (EXISTS (SELECT 1 FROM wb_meeting_participant __authz_path_1 WHERE __authz_path_1.meeting_id = wb_meeting.meeting_id AND (__authz_path_1.participant_dept_code IN (?, ?)))) ORDER BY start_time DESC LIMIT ?",
                filter.getSql());
        assertEquals(Arrays.<Object>asList("000", "000002"), filter.getParameters());
        assertEquals(0, filter.getParameterMappingIndex());
    }

    private PredicateNode inPredicate(String fieldKey, String bindingKey) {
        PredicateExpression left = new PredicateExpression(PredicateExpressionKind.FIELD);
        left.setFieldKey(fieldKey);
        PredicateExpression right = new PredicateExpression(PredicateExpressionKind.BINDING);
        right.setBindingKey(bindingKey);
        PredicateNode predicate = new PredicateNode(PredicateOperator.IN);
        predicate.setLeft(left);
        predicate.setRight(right);
        return predicate;
    }
}
