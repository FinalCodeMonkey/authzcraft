package com.fcm.authzcraft.pdp.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fcm.authzcraft.api.attribute.AttributeReference;
import com.fcm.authzcraft.api.attribute.AttributeSnapshot;
import com.fcm.authzcraft.api.attribute.AttributeSnapshotRequest;
import com.fcm.authzcraft.api.attribute.AttributeValue;
import com.fcm.authzcraft.api.attribute.ResolveKind;
import com.fcm.authzcraft.api.common.RequesterKind;
import com.fcm.authzcraft.api.common.ValueKind;
import com.fcm.authzcraft.api.plan.BindingValue;
import com.fcm.authzcraft.api.plan.FailureCode;
import com.fcm.authzcraft.api.plan.PlanDecision;
import com.fcm.authzcraft.api.plan.PlanResource;
import com.fcm.authzcraft.api.plan.PlannerKind;
import com.fcm.authzcraft.api.plan.ProtectionMode;
import com.fcm.authzcraft.api.plan.RowFilterPlan;
import com.fcm.authzcraft.api.plan.RowFilterPlanRequest;
import com.fcm.authzcraft.api.plan.RowFilterPlanResponse;
import com.fcm.authzcraft.api.predicate.PredicateExpression;
import com.fcm.authzcraft.api.predicate.PredicateExpressionKind;
import com.fcm.authzcraft.api.predicate.PredicateNode;
import com.fcm.authzcraft.api.predicate.PredicateOperator;
import com.fcm.authzcraft.api.service.AttributeSnapshotProvider;
import com.fcm.authzcraft.api.service.RowFilterPlanner;
import com.fcm.authzcraft.pdp.domain.model.DecisionRecord;
import com.fcm.authzcraft.pdp.domain.model.EffectiveGrantSnapshot;
import com.fcm.authzcraft.pdp.domain.model.GrantArgumentSnapshot;
import com.fcm.authzcraft.pdp.domain.model.PublishedPolicySnapshot;
import com.fcm.authzcraft.pdp.domain.model.RelationResourceSnapshot;
import com.fcm.authzcraft.pdp.domain.repository.PublishedPolicySnapshotRepository;
import com.fcm.authzcraft.pdp.domain.service.DecisionIdGenerator;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class NativeRowFilterPlanner implements RowFilterPlanner {
    private final PublishedPolicySnapshotRepository repository;
    private final AttributeSnapshotProvider attributeSnapshotProvider;
    private final DecisionIdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    public NativeRowFilterPlanner(PublishedPolicySnapshotRepository repository,
                                  AttributeSnapshotProvider attributeSnapshotProvider,
                                  DecisionIdGenerator idGenerator,
                                  ObjectMapper objectMapper) {
        this.repository = repository;
        this.attributeSnapshotProvider = attributeSnapshotProvider;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
    }

    @Override
    public RowFilterPlanResponse plan(RowFilterPlanRequest request) {
        return planInternal(request, true);
    }

    public RowFilterPlanResponse simulate(RowFilterPlanRequest request) {
        return planInternal(request, false);
    }

    private RowFilterPlanResponse planInternal(RowFilterPlanRequest request, boolean recordDecision) {
        validateRequest(request);
        String requestKey = resolveRequestKey(request);
        List<RowFilterPlan> plans = new ArrayList<RowFilterPlan>();
        for (PlanResource resource : request.getResources()) {
            long startedAt = System.nanoTime();
            RowFilterPlan plan = planSingleResource(request, resource);
            plan.setDecisionKey(UUID.randomUUID().toString());
            plan.setPlannerKind(PlannerKind.NATIVE);
            if (!StringUtils.hasText(plan.getAttributeDigest())) {
                plan.setAttributeDigest(attributeDigest(request));
            }
            plan.setPlanDigest(planDigest(plan));
            if (plan.getExpiresAt() == null) {
                plan.setExpiresAt(Instant.now().plusSeconds(60));
            }
            if (recordDecision) {
                repository.saveDecisionRecord(toDecisionRecord(request, requestKey, plan, startedAt));
            }
            plans.add(plan);
        }
        return new RowFilterPlanResponse(requestKey, plans);
    }

    private RowFilterPlan planSingleResource(RowFilterPlanRequest request, PlanResource requestedResource) {
        requireText(requestedResource.getResourceKey(), "resources[].resourceKey");
        RelationResourceSnapshot resource = repository.findActiveResource(request.getTenantKey(), request.getAppKey(), requestedResource.getResourceKey());
        if (resource == null) {
            return failurePlan(requestedResource.getResourceKey(), null, PlanDecision.INDETERMINATE,
                    FailureCode.RESOURCE_UNAVAILABLE, "relation resource is unavailable");
        }
        ProtectionMode protectionMode = ProtectionMode.valueOf(resource.getProtectionMode());
        if (ProtectionMode.DISABLED.equals(protectionMode)) {
            return successPlan(resource, PlanDecision.ALLOW_ALL, null, Collections.<String>emptyList(), Collections.<String, BindingValue>emptyMap());
        }
        List<PublishedPolicySnapshot> policies = repository.findPublishedPolicies(request.getTenantKey(), request.getAppKey(),
                resource.getResourceKey(), request.getOperationCode());
        if (policies.isEmpty()) {
            return successPlan(resource, PlanDecision.DENY_ALL, null, Collections.<String>emptyList(), Collections.<String, BindingValue>emptyMap());
        }
        try {
            Long principalId = resolveRequesterPrincipalId(request);
            if (principalId == null && RequesterKind.USER.equals(request.getRequesterKind())) {
                return failurePlan(resource.getResourceKey(), protectionMode, PlanDecision.INDETERMINATE,
                        FailureCode.SUBJECT_UNAVAILABLE, "requester principal is unavailable");
            }
            AttributeSnapshot attributeSnapshot = resolveAttributeSnapshot(request, policies);
            List<Long> principalIds = candidatePrincipalIds(request, principalId, attributeSnapshot);
            List<Long> policyIds = policyIds(policies);
            List<EffectiveGrantSnapshot> grants = repository.findEffectiveGrants(request.getTenantKey(), request.getAppKey(),
                    principalIds, policyIds, LocalDateTime.now());
            if (grants.isEmpty()) {
                RowFilterPlan plan = successPlan(resource, PlanDecision.DENY_ALL, null, Collections.<String>emptyList(), Collections.<String, BindingValue>emptyMap());
                applyAttributeMetadata(plan, attributeSnapshot);
                return plan;
            }
            Map<Long, List<EffectiveGrantSnapshot>> grantsByPolicy = grantsByPolicy(grants);
            Map<Long, Map<String, GrantArgumentSnapshot>> argumentsByGrant = argumentsByGrant(grants);
            List<PredicateNode> denyPredicates = instantiatePredicates(request, policies, grantsByPolicy, argumentsByGrant, "DENY", null, attributeSnapshot);
            if (!denyPredicates.isEmpty()) {
                RowFilterPlan plan = successPlan(resource, PlanDecision.DENY_ALL, null, Collections.<String>emptyList(), Collections.<String, BindingValue>emptyMap());
                applyAttributeMetadata(plan, attributeSnapshot);
                return plan;
            }

            Map<String, BindingValue> bindings = new LinkedHashMap<String, BindingValue>();
            List<PredicateNode> allowPredicates = instantiatePredicates(request, policies, grantsByPolicy, argumentsByGrant, "ALLOW", bindings, attributeSnapshot);
            if (allowPredicates.isEmpty()) {
                RowFilterPlan plan = successPlan(resource, PlanDecision.DENY_ALL, null, Collections.<String>emptyList(), Collections.<String, BindingValue>emptyMap());
                applyAttributeMetadata(plan, attributeSnapshot);
                return plan;
            }
            PredicateNode predicate = mergePredicates(allowPredicates);
            if (PredicateOperator.TRUE.equals(predicate.getOperator())) {
                RowFilterPlan plan = successPlan(resource, PlanDecision.ALLOW_ALL, null, Collections.<String>emptyList(), bindings);
                applyAttributeMetadata(plan, attributeSnapshot);
                return plan;
            }
            RowFilterPlan plan = successPlan(resource, PlanDecision.FILTER, predicate, collectAccessPathKeys(predicate), bindings);
            applyAttributeMetadata(plan, attributeSnapshot);
            return plan;
        } catch (PlanBuildException exception) {
            return failurePlan(resource.getResourceKey(), protectionMode, PlanDecision.INDETERMINATE,
                    exception.getFailureCode(), exception.getMessage());
        }
    }

    private List<PredicateNode> instantiatePredicates(RowFilterPlanRequest request,
                                                      List<PublishedPolicySnapshot> policies,
                                                      Map<Long, List<EffectiveGrantSnapshot>> grantsByPolicy,
                                                      Map<Long, Map<String, GrantArgumentSnapshot>> argumentsByGrant,
                                                      String effectKind,
                                                      Map<String, BindingValue> bindings,
                                                      AttributeSnapshot attributeSnapshot) {
        List<PredicateNode> predicates = new ArrayList<PredicateNode>();
        for (PublishedPolicySnapshot policy : policies) {
            if (!effectKind.equals(policy.getEffectKind())) {
                continue;
            }
            List<EffectiveGrantSnapshot> grants = grantsByPolicy.get(policy.getPolicyId());
            if (grants == null) {
                continue;
            }
            for (EffectiveGrantSnapshot grant : grants) {
                predicates.add(instantiatePredicate(policy, grant, argumentsByGrant.get(grant.getGrantId()), request, bindings, attributeSnapshot));
            }
        }
        return predicates;
    }

    private PredicateNode instantiatePredicate(PublishedPolicySnapshot policy, EffectiveGrantSnapshot grant,
                                               Map<String, GrantArgumentSnapshot> arguments,
                                               RowFilterPlanRequest request,
                                               Map<String, BindingValue> bindings,
                                               AttributeSnapshot attributeSnapshot) {
        try {
            validateGrantArguments(policy, arguments == null ? Collections.<String, GrantArgumentSnapshot>emptyMap() : arguments);
            PredicateNode predicate = objectMapper.readValue(policy.getPredicateAst(), PredicateNode.class);
            return rewritePredicate(predicate, grant, arguments == null ? Collections.<String, GrantArgumentSnapshot>emptyMap() : arguments,
                    request, bindings == null ? new LinkedHashMap<String, BindingValue>() : bindings, attributeSnapshot);
        } catch (JsonProcessingException exception) {
            throw new PlanBuildException(FailureCode.PLAN_INVALID, "predicateAst is invalid", exception);
        }
    }

    private void validateGrantArguments(PublishedPolicySnapshot policy, Map<String, GrantArgumentSnapshot> arguments) {
        try {
            JsonNode schema = objectMapper.readTree(StringUtils.hasText(policy.getArgumentSchema()) ? policy.getArgumentSchema() : "{}");
            if (!schema.isObject()) {
                throw new PlanBuildException(FailureCode.GRANT_ARGUMENT_INVALID, "argumentSchema must be a JSON object");
            }
            Set<String> declaredKeys = new LinkedHashSet<String>();
            Iterator<Map.Entry<String, JsonNode>> fields = schema.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String argumentKey = field.getKey();
                JsonNode spec = field.getValue();
                declaredKeys.add(argumentKey);
                boolean required = spec.path("required").asBoolean(false);
                GrantArgumentSnapshot argument = arguments.get(argumentKey);
                if (argument == null) {
                    if (required) {
                        throw new PlanBuildException(FailureCode.GRANT_ARGUMENT_INVALID, "required grant argument is missing: " + argumentKey);
                    }
                    continue;
                }
                ValueKind expectedKind = expectedValueKind(argumentKey, spec);
                ValueKind actualKind = ValueKind.valueOf(argument.getValueKind());
                if (!expectedKind.equals(actualKind)) {
                    throw new PlanBuildException(FailureCode.GRANT_ARGUMENT_INVALID,
                            "grant argument valueKind mismatch: " + argumentKey);
                }
                validateArgumentValueShape(argumentKey, expectedKind, argument.getArgumentValue());
                if (spec.has("sensitiveFlag") && spec.path("sensitiveFlag").asBoolean(false) != argument.isSensitive()) {
                    throw new PlanBuildException(FailureCode.GRANT_ARGUMENT_INVALID,
                            "grant argument sensitiveFlag mismatch: " + argumentKey);
                }
            }
            for (String argumentKey : arguments.keySet()) {
                if (!declaredKeys.contains(argumentKey)) {
                    throw new PlanBuildException(FailureCode.GRANT_ARGUMENT_INVALID, "undeclared grant argument: " + argumentKey);
                }
            }
        } catch (PlanBuildException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new PlanBuildException(FailureCode.GRANT_ARGUMENT_INVALID, "grant arguments do not match argumentSchema", exception);
        }
    }

    private ValueKind expectedValueKind(String argumentKey, JsonNode spec) {
        String valueKind = spec.path("valueKind").asText(null);
        if (!StringUtils.hasText(valueKind)) {
            throw new PlanBuildException(FailureCode.GRANT_ARGUMENT_INVALID, "argumentSchema.valueKind is missing: " + argumentKey);
        }
        return ValueKind.valueOf(valueKind);
    }

    private void validateArgumentValueShape(String argumentKey, ValueKind valueKind, String argumentValue) {
        try {
            JsonNode value = objectMapper.readTree(argumentValue);
            if (ValueKind.STRING_SET.equals(valueKind) || ValueKind.NUMBER_SET.equals(valueKind)) {
                if (!value.isArray()) {
                    throw new PlanBuildException(FailureCode.GRANT_ARGUMENT_INVALID, "grant argument must be an array: " + argumentKey);
                }
                for (JsonNode item : value) {
                    if (ValueKind.STRING_SET.equals(valueKind) && !item.isTextual()) {
                        throw new PlanBuildException(FailureCode.GRANT_ARGUMENT_INVALID, "grant argument array item must be string: " + argumentKey);
                    }
                    if (ValueKind.NUMBER_SET.equals(valueKind) && !item.isNumber()) {
                        throw new PlanBuildException(FailureCode.GRANT_ARGUMENT_INVALID, "grant argument array item must be number: " + argumentKey);
                    }
                }
                return;
            }
            if (ValueKind.STRING.equals(valueKind) || ValueKind.DATE.equals(valueKind) || ValueKind.DATETIME.equals(valueKind)) {
                if (!value.isTextual()) {
                    throw new PlanBuildException(FailureCode.GRANT_ARGUMENT_INVALID, "grant argument must be a string: " + argumentKey);
                }
                return;
            }
            if ((ValueKind.INTEGER.equals(valueKind) || ValueKind.DECIMAL.equals(valueKind)) && !value.isNumber()) {
                throw new PlanBuildException(FailureCode.GRANT_ARGUMENT_INVALID, "grant argument must be a number: " + argumentKey);
            }
            if (ValueKind.BOOLEAN.equals(valueKind) && !value.isBoolean()) {
                throw new PlanBuildException(FailureCode.GRANT_ARGUMENT_INVALID, "grant argument must be a boolean: " + argumentKey);
            }
        } catch (PlanBuildException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new PlanBuildException(FailureCode.GRANT_ARGUMENT_INVALID, "grant argument value is invalid JSON: " + argumentKey, exception);
        }
    }

    private PredicateNode rewritePredicate(PredicateNode node, EffectiveGrantSnapshot grant,
                                           Map<String, GrantArgumentSnapshot> arguments,
                                           RowFilterPlanRequest request,
                                           Map<String, BindingValue> bindings,
                                           AttributeSnapshot attributeSnapshot) {
        PredicateNode copy = new PredicateNode();
        copy.setOperator(node.getOperator());
        copy.setAccessPathKey(node.getAccessPathKey());
        copy.setCustomAst(node.getCustomAst());
        copy.setLeft(rewriteExpression(node.getLeft(), grant, arguments, request, bindings, attributeSnapshot));
        copy.setRight(rewriteExpression(node.getRight(), grant, arguments, request, bindings, attributeSnapshot));
        if (node.getPathPredicate() != null) {
            copy.setPathPredicate(rewritePredicate(node.getPathPredicate(), grant, arguments, request, bindings, attributeSnapshot));
        }
        List<PredicateNode> operands = new ArrayList<PredicateNode>();
        for (PredicateNode operand : node.getOperands()) {
            operands.add(rewritePredicate(operand, grant, arguments, request, bindings, attributeSnapshot));
        }
        copy.setOperands(operands);
        return copy;
    }

    private PredicateExpression rewriteExpression(PredicateExpression expression, EffectiveGrantSnapshot grant,
                                                  Map<String, GrantArgumentSnapshot> arguments,
                                                  RowFilterPlanRequest request,
                                                  Map<String, BindingValue> bindings,
                                                  AttributeSnapshot attributeSnapshot) {
        if (expression == null || expression.getKind() == null) {
            return expression;
        }
        if (PredicateExpressionKind.ARGUMENT.equals(expression.getKind())) {
            GrantArgumentSnapshot argument = arguments.get(expression.getArgumentKey());
            if (argument == null) {
                throw new PlanBuildException(FailureCode.GRANT_ARGUMENT_INVALID, "grant argument is missing: " + expression.getArgumentKey());
            }
            String bindingKey = "g" + grant.getGrantId() + "_" + expression.getArgumentKey();
            bindings.put(bindingKey, toBindingValue(argument));
            PredicateExpression binding = new PredicateExpression(PredicateExpressionKind.BINDING);
            binding.setBindingKey(bindingKey);
            return binding;
        }
        if (PredicateExpressionKind.ATTRIBUTE.equals(expression.getKind())) {
            String bindingKey = "g" + grant.getGrantId() + "_" + expression.getAttributeKey();
            AttributeValue attributeValue = resolveAttributeValue(expression.getAttributeKey(), expression.getValueKind(), request,
                    attributeSnapshot, arguments);
            bindings.put(bindingKey, new BindingValue(attributeValue.getValueKind(), attributeValue.getValue(), false));
            PredicateExpression binding = new PredicateExpression(PredicateExpressionKind.BINDING);
            binding.setBindingKey(bindingKey);
            return binding;
        }
        return expression;
    }

    private BindingValue toBindingValue(GrantArgumentSnapshot argument) {
        try {
            ValueKind valueKind = ValueKind.valueOf(argument.getValueKind());
            Object value = objectMapper.readValue(argument.getArgumentValue(), Object.class);
            return new BindingValue(valueKind, value, argument.isSensitive());
        } catch (Exception exception) {
            throw new PlanBuildException(FailureCode.GRANT_ARGUMENT_INVALID, "grant argument value is invalid: " + argument.getArgumentKey(), exception);
        }
    }

    private AttributeValue resolveAttributeValue(String attributeKey, ValueKind valueKind, RowFilterPlanRequest request,
                                                AttributeSnapshot attributeSnapshot,
                                                Map<String, GrantArgumentSnapshot> arguments) {
        if ("managedDepartmentCodes".equals(attributeKey) && hasManagedDepartmentArguments(arguments)) {
            return resolveGrantScopedAttributeValue(attributeKey, valueKind, request, arguments);
        }
        AttributeValue attributeValue = attributeSnapshot == null ? null : attributeSnapshot.getAttributes().get(attributeKey);
        if (attributeValue == null && isRequesterAlias(attributeKey)) {
            attributeValue = attributeSnapshot == null ? null : attributeSnapshot.getAttributes().get("currentUserId");
        }
        if (attributeValue == null && isRequesterAlias(attributeKey)) {
            return new AttributeValue(attributeKey, ValueKind.STRING, request.getRequesterKey());
        }
        if (attributeValue == null || !attributeValue.isAvailable()) {
            FailureCode failureCode = attributeValue != null && attributeValue.getFailure() != null
                    ? attributeValue.getFailure().getFailureCode() : FailureCode.ATTRIBUTE_MISSING;
            throw new PlanBuildException(failureCode, "attribute is unavailable: " + attributeKey);
        }
        return attributeValue;
    }

    private boolean hasManagedDepartmentArguments(Map<String, GrantArgumentSnapshot> arguments) {
        return arguments != null && (arguments.containsKey("departmentScopes") || arguments.containsKey("subDepartmentDepth"));
    }

    private AttributeValue resolveGrantScopedAttributeValue(String attributeKey, ValueKind valueKind, RowFilterPlanRequest request,
                                                           Map<String, GrantArgumentSnapshot> arguments) {
        AttributeSnapshotRequest snapshotRequest = new AttributeSnapshotRequest();
        snapshotRequest.setTenantKey(request.getTenantKey());
        snapshotRequest.setAppKey(request.getAppKey());
        snapshotRequest.setRequesterKind(request.getRequesterKind());
        snapshotRequest.setRequesterKey(request.getRequesterKey());
        snapshotRequest.setRequestKey(resolveRequestKey(request));
        if (request.getContext() != null && request.getContext().getTrustedAttributes() != null) {
            snapshotRequest.setTrustedEnvironment(request.getContext().getTrustedAttributes());
        }
        snapshotRequest.setGrantArguments(grantArgumentValues(arguments));
        ValueKind resolvedValueKind = valueKind == null ? ValueKind.STRING_SET : valueKind;
        snapshotRequest.setAttributeReferences(Collections.singletonList(new AttributeReference(attributeKey, resolvedValueKind,
                ResolveKind.MANAGED_DEPARTMENT_CODES, true)));
        AttributeSnapshot snapshot = attributeSnapshotProvider.resolve(snapshotRequest);
        AttributeValue attributeValue = snapshot == null ? null : snapshot.getAttributes().get(attributeKey);
        if (attributeValue == null || !attributeValue.isAvailable()) {
            FailureCode failureCode = attributeValue != null && attributeValue.getFailure() != null
                    ? attributeValue.getFailure().getFailureCode() : FailureCode.ATTRIBUTE_MISSING;
            throw new PlanBuildException(failureCode, "attribute is unavailable: " + attributeKey);
        }
        return attributeValue;
    }

    private Map<String, Object> grantArgumentValues(Map<String, GrantArgumentSnapshot> arguments) {
        Map<String, Object> values = new LinkedHashMap<String, Object>();
        if (arguments == null) {
            return values;
        }
        for (GrantArgumentSnapshot argument : arguments.values()) {
            try {
                values.put(argument.getArgumentKey(), objectMapper.readValue(argument.getArgumentValue(), Object.class));
            } catch (Exception exception) {
                throw new PlanBuildException(FailureCode.GRANT_ARGUMENT_INVALID,
                        "grant argument value is invalid: " + argument.getArgumentKey(), exception);
            }
        }
        return values;
    }

    private boolean isRequesterAlias(String attributeKey) {
        return "currentUserId".equals(attributeKey) || "requesterKey".equals(attributeKey)
                || "requesterUserId".equals(attributeKey) || "currentPrincipalKey".equals(attributeKey);
    }

    private RowFilterPlan successPlan(RelationResourceSnapshot resource, PlanDecision decision, PredicateNode predicate,
                                      List<String> requiredAccessPathKeys, Map<String, BindingValue> bindings) {
        RowFilterPlan plan = new RowFilterPlan();
        plan.setPlanDecision(decision);
        plan.setTargetResourceKey(resource.getResourceKey());
        plan.setProtectionMode(ProtectionMode.valueOf(resource.getProtectionMode()));
        plan.setPredicate(predicate);
        plan.setRequiredAccessPathKeys(requiredAccessPathKeys);
        plan.setBindings(bindings);
        return plan;
    }

    private RowFilterPlan failurePlan(String resourceKey, ProtectionMode protectionMode, PlanDecision decision,
                                      FailureCode failureCode, String failureMessage) {
        RowFilterPlan plan = new RowFilterPlan();
        plan.setPlanDecision(decision);
        plan.setTargetResourceKey(resourceKey);
        plan.setProtectionMode(protectionMode);
        plan.setFailureCode(failureCode);
        plan.setFailureMessage(failureMessage);
        return plan;
    }

    private PredicateNode mergePredicates(List<PredicateNode> predicates) {
        if (predicates.size() == 1) {
            return predicates.get(0);
        }
        PredicateNode root = new PredicateNode(PredicateOperator.OR);
        root.setOperands(predicates);
        return root;
    }

    private List<String> collectAccessPathKeys(PredicateNode predicate) {
        Set<String> keys = new LinkedHashSet<String>();
        collectAccessPathKeys(predicate, keys);
        return new ArrayList<String>(keys);
    }

    private void collectAccessPathKeys(PredicateNode predicate, Set<String> keys) {
        if (predicate == null) {
            return;
        }
        if (StringUtils.hasText(predicate.getAccessPathKey())) {
            keys.add(predicate.getAccessPathKey());
        }
        if (predicate.getPathPredicate() != null) {
            collectAccessPathKeys(predicate.getPathPredicate(), keys);
        }
        for (PredicateNode operand : predicate.getOperands()) {
            collectAccessPathKeys(operand, keys);
        }
    }

    private Long resolveRequesterPrincipalId(RowFilterPlanRequest request) {
        if (RequesterKind.SERVICE.equals(request.getRequesterKind())) {
            return null;
        }
        return repository.findActivePrincipalId(request.getTenantKey(), request.getRequesterKind().name(), request.getRequesterKey());
    }

    private AttributeSnapshot resolveAttributeSnapshot(RowFilterPlanRequest request, List<PublishedPolicySnapshot> policies) {
        AttributeSnapshotRequest snapshotRequest = new AttributeSnapshotRequest();
        snapshotRequest.setTenantKey(request.getTenantKey());
        snapshotRequest.setAppKey(request.getAppKey());
        snapshotRequest.setRequesterKind(request.getRequesterKind());
        snapshotRequest.setRequesterKey(request.getRequesterKey());
        snapshotRequest.setRequestKey(resolveRequestKey(request));
        if (request.getContext() != null && request.getContext().getTrustedAttributes() != null) {
            snapshotRequest.setTrustedEnvironment(request.getContext().getTrustedAttributes());
        }
        snapshotRequest.setAttributeReferences(attributeReferences(policies));
        return attributeSnapshotProvider.resolve(snapshotRequest);
    }

    private List<AttributeReference> attributeReferences(List<PublishedPolicySnapshot> policies) {
        Map<String, AttributeReference> references = new LinkedHashMap<String, AttributeReference>();
        addReference(references, new AttributeReference("currentUserId", ValueKind.STRING, ResolveKind.REQUESTER_USER_ID, false));
        addReference(references, new AttributeReference("currentDepartmentCode", ValueKind.STRING, ResolveKind.REQUESTER_DEPARTMENT_CODE, false));
        addReference(references, new AttributeReference("currentPositionCode", ValueKind.STRING, ResolveKind.REQUESTER_POSITION_CODE, false));
        addReference(references, new AttributeReference("managedDepartmentCodes", ValueKind.STRING_SET, ResolveKind.MANAGED_DEPARTMENT_CODES, false));
        addReference(references, new AttributeReference("roleKeys", ValueKind.STRING_SET, ResolveKind.ROLE_KEYS, false));
        addReference(references, new AttributeReference("groupKeys", ValueKind.STRING_SET, ResolveKind.GROUP_KEYS, false));
        for (PublishedPolicySnapshot policy : policies) {
            if (!StringUtils.hasText(policy.getAttributeReferences())) {
                continue;
            }
            try {
                List<AttributeReference> policyReferences = objectMapper.readValue(policy.getAttributeReferences(),
                        new TypeReference<List<AttributeReference>>() {});
                for (AttributeReference reference : policyReferences) {
                    addReference(references, reference);
                }
            } catch (JsonProcessingException exception) {
                throw new PlanBuildException(FailureCode.PLAN_INVALID, "attributeReferences is invalid", exception);
            }
        }
        return new ArrayList<AttributeReference>(references.values());
    }

    private void addReference(Map<String, AttributeReference> references, AttributeReference reference) {
        if (reference != null && StringUtils.hasText(reference.getAttributeKey())) {
            references.put(reference.getAttributeKey(), reference);
        }
    }

    private List<Long> candidatePrincipalIds(RowFilterPlanRequest request, Long requesterPrincipalId, AttributeSnapshot attributeSnapshot) {
        Set<Long> ids = new LinkedHashSet<Long>();
        if (requesterPrincipalId != null) {
            ids.add(requesterPrincipalId);
        }
        addPrincipalIds(ids, request.getTenantKey(), "ORGANIZATION", attributeStrings(attributeSnapshot, "currentDepartmentCode"));
        addPrincipalIds(ids, request.getTenantKey(), "POSITION", attributeStrings(attributeSnapshot, "currentPositionCode"));
        addPrincipalIds(ids, request.getTenantKey(), "ROLE", attributeStrings(attributeSnapshot, "roleKeys"));
        addPrincipalIds(ids, request.getTenantKey(), "GROUP", attributeStrings(attributeSnapshot, "groupKeys"));
        return new ArrayList<Long>(ids);
    }

    private void addPrincipalIds(Set<Long> ids, String tenantKey, String principalKind, List<String> principalKeys) {
        ids.addAll(repository.findActivePrincipalIds(tenantKey, principalKind, principalKeys));
    }

    private List<String> attributeStrings(AttributeSnapshot attributeSnapshot, String attributeKey) {
        if (attributeSnapshot == null || attributeSnapshot.getAttributes() == null) {
            return Collections.emptyList();
        }
        AttributeValue attributeValue = attributeSnapshot.getAttributes().get(attributeKey);
        if (attributeValue == null || !attributeValue.isAvailable() || attributeValue.getValue() == null) {
            return Collections.emptyList();
        }
        Object value = attributeValue.getValue();
        List<String> values = new ArrayList<String>();
        if (value instanceof Iterable) {
            for (Object current : (Iterable<?>) value) {
                if (current != null && StringUtils.hasText(String.valueOf(current))) {
                    values.add(String.valueOf(current));
                }
            }
            return values;
        }
        if (StringUtils.hasText(String.valueOf(value))) {
            values.add(String.valueOf(value));
        }
        return values;
    }

    private void applyAttributeMetadata(RowFilterPlan plan, AttributeSnapshot attributeSnapshot) {
        if (attributeSnapshot == null) {
            return;
        }
        plan.setAttributeDigest(attributeSnapshot.getAttributeDigest());
        plan.setExpiresAt(attributeSnapshot.getExpiresAt());
    }

    private List<Long> policyIds(List<PublishedPolicySnapshot> policies) {
        List<Long> ids = new ArrayList<Long>();
        for (PublishedPolicySnapshot policy : policies) {
            ids.add(policy.getPolicyId());
        }
        return ids;
    }

    private Map<Long, List<EffectiveGrantSnapshot>> grantsByPolicy(List<EffectiveGrantSnapshot> grants) {
        Map<Long, List<EffectiveGrantSnapshot>> grouped = new LinkedHashMap<Long, List<EffectiveGrantSnapshot>>();
        for (EffectiveGrantSnapshot grant : grants) {
            List<EffectiveGrantSnapshot> current = grouped.get(grant.getPolicyId());
            if (current == null) {
                current = new ArrayList<EffectiveGrantSnapshot>();
                grouped.put(grant.getPolicyId(), current);
            }
            current.add(grant);
        }
        return grouped;
    }

    private Map<Long, Map<String, GrantArgumentSnapshot>> argumentsByGrant(List<EffectiveGrantSnapshot> grants) {
        List<Long> grantIds = new ArrayList<Long>();
        for (EffectiveGrantSnapshot grant : grants) {
            grantIds.add(grant.getGrantId());
        }
        Map<Long, Map<String, GrantArgumentSnapshot>> grouped = new LinkedHashMap<Long, Map<String, GrantArgumentSnapshot>>();
        for (GrantArgumentSnapshot argument : repository.findGrantArguments(grantIds)) {
            Map<String, GrantArgumentSnapshot> current = grouped.get(argument.getAccessGrantId());
            if (current == null) {
                current = new LinkedHashMap<String, GrantArgumentSnapshot>();
                grouped.put(argument.getAccessGrantId(), current);
            }
            current.put(argument.getArgumentKey(), argument);
        }
        return grouped;
    }

    private DecisionRecord toDecisionRecord(RowFilterPlanRequest request, String requestKey, RowFilterPlan plan, long startedAt) {
        DecisionRecord record = new DecisionRecord();
        record.setId(idGenerator.nextId());
        record.setDecisionKey(plan.getDecisionKey());
        record.setRequestKey(requestKey);
        record.setTenantKey(request.getTenantKey());
        record.setAppKey(request.getAppKey());
        record.setRequesterKind(request.getRequesterKind().name());
        record.setRequesterKey(request.getRequesterKey());
        record.setOperationCode(request.getOperationCode());
        record.setTargetResourceKey(plan.getTargetResourceKey());
        record.setPlanDecision(plan.getPlanDecision().name());
        record.setPlannerKind(PlannerKind.NATIVE.name());
        record.setPlanDigest(plan.getPlanDigest());
        record.setAttributeDigest(plan.getAttributeDigest());
        record.setFailureCode(plan.getFailureCode() == null ? null : plan.getFailureCode().name());
        record.setPlanningCostMs((int) ((System.nanoTime() - startedAt) / 1000000L));
        return record;
    }

    private void validateRequest(RowFilterPlanRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body must not be null");
        }
        requireText(request.getTenantKey(), "tenantKey");
        requireText(request.getAppKey(), "appKey");
        if (request.getRequesterKind() == null) {
            throw new IllegalArgumentException("requesterKind must not be null");
        }
        if (!RequesterKind.USER.equals(request.getRequesterKind()) && !RequesterKind.SERVICE.equals(request.getRequesterKind())) {
            throw new IllegalArgumentException("requesterKind only supports USER or SERVICE in MVP native planner");
        }
        requireText(request.getRequesterKey(), "requesterKey");
        requireText(request.getOperationCode(), "operationCode");
        if (!"READ".equals(request.getOperationCode()) && !"UPDATE".equals(request.getOperationCode()) && !"DELETE".equals(request.getOperationCode())) {
            throw new IllegalArgumentException("operationCode has unsupported value: " + request.getOperationCode());
        }
        if (request.getResources() == null || request.getResources().isEmpty()) {
            throw new IllegalArgumentException("resources must not be empty");
        }
    }

    private String resolveRequestKey(RowFilterPlanRequest request) {
        if (request.getContext() != null && StringUtils.hasText(request.getContext().getRequestKey())) {
            return request.getContext().getRequestKey();
        }
        return UUID.randomUUID().toString();
    }

    private void requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    private String attributeDigest(RowFilterPlanRequest request) {
        return sha256(request.getTenantKey() + "|" + request.getAppKey() + "|" + request.getRequesterKind() + "|" + request.getRequesterKey());
    }

    private String planDigest(RowFilterPlan plan) {
        try {
            return sha256(objectMapper.writeValueAsString(plan.getPlanDecision()) + "|" + plan.getTargetResourceKey()
                    + "|" + objectMapper.writeValueAsString(plan.getPredicate()) + "|" + objectMapper.writeValueAsString(plan.getBindings()));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Failed to calculate planDigest", exception);
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte current : hash) {
                builder.append(String.format("%02x", current));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalArgumentException("Failed to calculate sha256", exception);
        }
    }

    private static class PlanBuildException extends RuntimeException {
        private final FailureCode failureCode;

        PlanBuildException(FailureCode failureCode, String message) {
            super(message);
            this.failureCode = failureCode;
        }

        PlanBuildException(FailureCode failureCode, String message, Throwable cause) {
            super(message, cause);
            this.failureCode = failureCode;
        }

        FailureCode getFailureCode() {
            return failureCode;
        }
    }
}