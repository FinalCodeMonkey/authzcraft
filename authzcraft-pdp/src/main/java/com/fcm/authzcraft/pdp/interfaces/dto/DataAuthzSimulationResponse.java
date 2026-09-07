package com.fcm.authzcraft.pdp.interfaces.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fcm.authzcraft.api.plan.RowFilterPlan;
import com.fcm.authzcraft.api.plan.RowFilterPlanResponse;

public class DataAuthzSimulationResponse {
    private String requestKey;
    private List<RowFilterPlan> plans;
    private boolean productionDecisionRecorded;
    private boolean assertionPassed;
    private List<DataAuthzSimulationExplainResponse> explains = new ArrayList<DataAuthzSimulationExplainResponse>();
    private List<DataAuthzSimulationAssertionResponse> assertions = new ArrayList<DataAuthzSimulationAssertionResponse>();

    public static DataAuthzSimulationResponse from(RowFilterPlanResponse response) {
        return from(new DataAuthzSimulationRequest(), response);
    }

    public static DataAuthzSimulationResponse from(DataAuthzSimulationRequest request, RowFilterPlanResponse response) {
        DataAuthzSimulationResponse simulation = new DataAuthzSimulationResponse();
        simulation.setRequestKey(response.getRequestKey());
        simulation.setPlans(response.getPlans());
        simulation.setProductionDecisionRecorded(false);
        simulation.setExplains(explains(response.getPlans()));
        simulation.setAssertions(assertions(request, response.getPlans()));
        simulation.setAssertionPassed(allPassed(simulation.getAssertions()));
        return simulation;
    }

    private static List<DataAuthzSimulationExplainResponse> explains(List<RowFilterPlan> plans) {
        List<DataAuthzSimulationExplainResponse> responses = new ArrayList<DataAuthzSimulationExplainResponse>();
        if (plans == null) {
            return responses;
        }
        for (RowFilterPlan plan : plans) {
            DataAuthzSimulationExplainResponse explain = new DataAuthzSimulationExplainResponse();
            explain.setTargetResourceKey(plan.getTargetResourceKey());
            explain.setPlanDecision(value(plan.getPlanDecision()));
            explain.setProtectionMode(value(plan.getProtectionMode()));
            explain.setPredicateOperator(plan.getPredicate() == null ? null : value(plan.getPredicate().getOperator()));
            explain.setBindingKeys(bindingKeys(plan));
            explain.setRequiredAccessPathKeys(plan.getRequiredAccessPathKeys());
            explain.setFailureCode(value(plan.getFailureCode()));
            explain.setSummary(summary(plan));
            responses.add(explain);
        }
        return responses;
    }

    private static List<DataAuthzSimulationAssertionResponse> assertions(DataAuthzSimulationRequest request, List<RowFilterPlan> plans) {
        List<DataAuthzSimulationAssertionResponse> responses = new ArrayList<DataAuthzSimulationAssertionResponse>();
        if (request == null || request.getExpectations() == null || request.getExpectations().isEmpty()) {
            return responses;
        }
        for (DataAuthzSimulationExpectationRequest expectation : request.getExpectations()) {
            RowFilterPlan plan = findPlan(expectation.getTargetResourceKey(), plans);
            String targetResourceKey = expectation.getTargetResourceKey();
            if (plan == null) {
                responses.add(new DataAuthzSimulationAssertionResponse("planExists", targetResourceKey, false,
                        "plan exists", "plan missing", "targetResourceKey is required when multiple plans are returned"));
                continue;
            }
            String actualResourceKey = plan.getTargetResourceKey();
            if (hasText(expectation.getExpectedPlanDecision())) {
                String actual = value(plan.getPlanDecision());
                responses.add(assertion("planDecision", actualResourceKey,
                        expectation.getExpectedPlanDecision().equals(actual), expectation.getExpectedPlanDecision(), actual,
                        "plan decision matches expected value"));
            }
            if (expectation.getExpectedFailureCode() != null) {
                String expected = hasText(expectation.getExpectedFailureCode()) ? expectation.getExpectedFailureCode() : null;
                String actual = value(plan.getFailureCode());
                responses.add(assertion("failureCode", actualResourceKey,
                        sameText(expected, actual), display(expected), display(actual), "failure code matches expected value"));
            }
            if (expectation.getExpectedBindingKeys() != null) {
                List<String> actual = bindingKeys(plan);
                responses.add(assertion("bindingKeys", actualResourceKey,
                        sameSet(expectation.getExpectedBindingKeys(), actual), display(expectation.getExpectedBindingKeys()), display(actual),
                        "binding keys match expected set"));
            }
            if (expectation.getExpectedAccessPathKeys() != null) {
                List<String> actual = plan.getRequiredAccessPathKeys() == null ? Collections.<String>emptyList() : plan.getRequiredAccessPathKeys();
                responses.add(assertion("accessPathKeys", actualResourceKey,
                        sameSet(expectation.getExpectedAccessPathKeys(), actual), display(expectation.getExpectedAccessPathKeys()), display(actual),
                        "required access path keys match expected set"));
            }
        }
        return responses;
    }

    private static RowFilterPlan findPlan(String targetResourceKey, List<RowFilterPlan> plans) {
        if (plans == null || plans.isEmpty()) {
            return null;
        }
        if (!hasText(targetResourceKey)) {
            return plans.size() == 1 ? plans.get(0) : null;
        }
        for (RowFilterPlan plan : plans) {
            if (targetResourceKey.equals(plan.getTargetResourceKey())) {
                return plan;
            }
        }
        return null;
    }

    private static DataAuthzSimulationAssertionResponse assertion(String assertionKey, String targetResourceKey, boolean passed,
                                                                  String expected, String actual, String detail) {
        return new DataAuthzSimulationAssertionResponse(assertionKey, targetResourceKey, passed, expected, actual, detail);
    }

    private static boolean allPassed(List<DataAuthzSimulationAssertionResponse> assertions) {
        if (assertions == null) {
            return true;
        }
        for (DataAuthzSimulationAssertionResponse assertion : assertions) {
            if (!assertion.isPassed()) {
                return false;
            }
        }
        return true;
    }

    private static List<String> bindingKeys(RowFilterPlan plan) {
        Map<String, ?> bindings = plan.getBindings();
        if (bindings == null || bindings.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<String>(bindings.keySet());
    }

    private static boolean sameSet(List<String> expected, List<String> actual) {
        return new LinkedHashSet<String>(nullToEmpty(expected)).equals(new LinkedHashSet<String>(nullToEmpty(actual)));
    }

    private static List<String> nullToEmpty(List<String> values) {
        return values == null ? Collections.<String>emptyList() : values;
    }

    private static boolean sameText(String expected, String actual) {
        return expected == null ? actual == null : expected.equals(actual);
    }

    private static String summary(RowFilterPlan plan) {
        String decision = value(plan.getPlanDecision());
        if ("FILTER".equals(decision)) {
            return "PDP generated a FILTER plan; PEP must apply the predicate, bindings, and required access paths.";
        }
        if ("DENY_ALL".equals(decision)) {
            return "PDP generated DENY_ALL; the requester has no effective allow plan or a deny rule took precedence.";
        }
        if ("ALLOW_ALL".equals(decision)) {
            return "PDP generated ALLOW_ALL; the original query may continue without extra row filters.";
        }
        if ("INDETERMINATE".equals(decision)) {
            return "PDP could not build a safe plan: " + display(value(plan.getFailureCode()));
        }
        return "PDP generated a row filter plan.";
    }

    private static String value(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static boolean hasText(String value) {
        return value != null && value.trim().length() > 0;
    }

    private static String display(Object value) {
        return value == null ? "<none>" : String.valueOf(value);
    }

    public String getRequestKey() {
        return requestKey;
    }

    public void setRequestKey(String requestKey) {
        this.requestKey = requestKey;
    }

    public List<RowFilterPlan> getPlans() {
        return plans;
    }

    public void setPlans(List<RowFilterPlan> plans) {
        this.plans = plans;
    }

    public boolean isProductionDecisionRecorded() {
        return productionDecisionRecorded;
    }

    public void setProductionDecisionRecorded(boolean productionDecisionRecorded) {
        this.productionDecisionRecorded = productionDecisionRecorded;
    }

    public boolean isAssertionPassed() {
        return assertionPassed;
    }

    public void setAssertionPassed(boolean assertionPassed) {
        this.assertionPassed = assertionPassed;
    }

    public List<DataAuthzSimulationExplainResponse> getExplains() {
        return explains;
    }

    public void setExplains(List<DataAuthzSimulationExplainResponse> explains) {
        this.explains = explains;
    }

    public List<DataAuthzSimulationAssertionResponse> getAssertions() {
        return assertions;
    }

    public void setAssertions(List<DataAuthzSimulationAssertionResponse> assertions) {
        this.assertions = assertions;
    }
}