package com.fcm.authzcraft.pap.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fcm.authzcraft.pap.application.command.CreateAccessGrantCommand;
import com.fcm.authzcraft.pap.application.command.CreateAccessPolicyCommand;
import com.fcm.authzcraft.pap.application.command.CreatePolicyRevisionCommand;
import com.fcm.authzcraft.pap.application.command.CreateRuleBlueprintCommand;
import com.fcm.authzcraft.pap.application.command.GrantArgumentCommand;
import com.fcm.authzcraft.pap.application.command.UpdateAccessGrantCommand;
import com.fcm.authzcraft.pap.application.command.UpdateAccessPolicyCommand;
import com.fcm.authzcraft.pap.application.query.AccessGrantSearchQuery;
import com.fcm.authzcraft.pap.application.query.AccessPolicySearchQuery;
import com.fcm.authzcraft.pap.application.query.PolicyRevisionSearchQuery;
import com.fcm.authzcraft.pap.application.query.PrincipalProjectionSearchQuery;
import com.fcm.authzcraft.pap.application.query.PrincipalSearchQuery;
import com.fcm.authzcraft.pap.application.query.RuleBlueprintSearchQuery;
import com.fcm.authzcraft.pap.domain.model.AccessGrant;
import com.fcm.authzcraft.pap.domain.model.AccessPolicy;
import com.fcm.authzcraft.pap.domain.model.GrantArgument;
import com.fcm.authzcraft.pap.domain.model.PolicyRevision;
import com.fcm.authzcraft.pap.domain.model.Principal;
import com.fcm.authzcraft.pap.domain.model.PrincipalOrganizationProjection;
import com.fcm.authzcraft.pap.domain.model.PrincipalPositionProjection;
import com.fcm.authzcraft.pap.domain.model.PrincipalUserProjection;
import com.fcm.authzcraft.pap.domain.model.RelationResource;
import com.fcm.authzcraft.pap.domain.model.RuleBlueprint;
import com.fcm.authzcraft.pap.domain.repository.DataCatalogRepository;
import com.fcm.authzcraft.pap.domain.repository.PolicyGovernanceRepository;
import com.fcm.authzcraft.pap.domain.service.CatalogIdGenerator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class PolicyGovernanceService {
    private static final String DEFAULT_ACTOR = "authzcraft-pap";

    private final PolicyGovernanceRepository repository;
    private final DataCatalogRepository dataCatalogRepository;
    private final CatalogIdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    public PolicyGovernanceService(PolicyGovernanceRepository repository, DataCatalogRepository dataCatalogRepository,
                                   CatalogIdGenerator idGenerator, ObjectMapper objectMapper) {
        this.repository = repository;
        this.dataCatalogRepository = dataCatalogRepository;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public RuleBlueprint createRuleBlueprint(CreateRuleBlueprintCommand command) {
        requireText(command.getTenantKey(), "tenantKey");
        requireText(command.getBlueprintKey(), "blueprintKey");
        requireText(command.getDisplayName(), "displayName");
        requireText(command.getPredicateTemplate(), "predicateTemplate");
        requireText(command.getInputSchema(), "inputSchema");
        validateJsonObject(command.getPredicateTemplate(), "predicateTemplate");
        validateJsonObject(command.getInputSchema(), "inputSchema");
        int version = command.getBlueprintVersion() == null ? 1 : command.getBlueprintVersion();
        if (version < 1) {
            throw new IllegalArgumentException("blueprintVersion must be greater than 0");
        }
        String blueprintKind = defaultText(command.getBlueprintKind(), "CUSTOM_BLUEPRINT").trim().toUpperCase(Locale.ROOT);
        assertIn(blueprintKind, "blueprintKind", "STANDARD_BLUEPRINT", "CUSTOM_BLUEPRINT");

        RuleBlueprint blueprint = new RuleBlueprint();
        blueprint.setId(idGenerator.nextId());
        blueprint.setTenantKey(command.getTenantKey());
        blueprint.setBlueprintKey(command.getBlueprintKey());
        blueprint.setBlueprintKind(blueprintKind);
        blueprint.setBlueprintVersion(version);
        blueprint.setDisplayName(command.getDisplayName());
        blueprint.setDescription(command.getDescription());
        blueprint.setPredicateTemplate(command.getPredicateTemplate());
        blueprint.setInputSchema(command.getInputSchema());
        blueprint.setLifecycleState("DRAFT");
        blueprint.setContentDigest(sha256(command.getPredicateTemplate() + "|" + command.getInputSchema()));
        repository.saveRuleBlueprint(blueprint);
        return repository.findRuleBlueprint(blueprint.getId());
    }

    public List<RuleBlueprint> searchRuleBlueprints(RuleBlueprintSearchQuery query) {
        return repository.searchRuleBlueprints(query.getTenantKey(), query.getBlueprintKey(), query.getLifecycleState());
    }

    @Transactional
    public RuleBlueprint publishRuleBlueprint(Long id) {
        RuleBlueprint blueprint = requireRuleBlueprint(id);
        if (!"DRAFT".equals(blueprint.getLifecycleState())) {
            throw new IllegalArgumentException("Only DRAFT rule blueprint can be published");
        }
        repository.retirePublishedRuleBlueprints(blueprint.getTenantKey(), blueprint.getBlueprintKey());
        repository.publishRuleBlueprint(id, DEFAULT_ACTOR);
        return repository.findRuleBlueprint(id);
    }

    @Transactional
    public RuleBlueprint updateRuleBlueprint(Long id, CreateRuleBlueprintCommand command) {
        RuleBlueprint blueprint = requireRuleBlueprint(id);
        if (!"DRAFT".equals(blueprint.getLifecycleState())) {
            throw new IllegalArgumentException("Only DRAFT rule blueprint can be updated");
        }
        if (StringUtils.hasText(command.getDisplayName())) {
            blueprint.setDisplayName(command.getDisplayName());
        }
        if (command.getDescription() != null) {
            blueprint.setDescription(command.getDescription());
        }
        if (StringUtils.hasText(command.getPredicateTemplate())) {
            validateJsonObject(command.getPredicateTemplate(), "predicateTemplate");
            blueprint.setPredicateTemplate(command.getPredicateTemplate());
        }
        if (StringUtils.hasText(command.getInputSchema())) {
            validateJsonObject(command.getInputSchema(), "inputSchema");
            blueprint.setInputSchema(command.getInputSchema());
        }
        blueprint.setContentDigest(sha256(blueprint.getPredicateTemplate() + "|" + blueprint.getInputSchema()));
        repository.updateRuleBlueprint(blueprint);
        return repository.findRuleBlueprint(id);
    }

    @Transactional
    public RuleBlueprint retireRuleBlueprint(Long id) {
        RuleBlueprint blueprint = requireRuleBlueprint(id);
        if ("RETIRED".equals(blueprint.getLifecycleState())) {
            return blueprint;
        }
        repository.retireRuleBlueprint(id, DEFAULT_ACTOR);
        return repository.findRuleBlueprint(id);
    }

    @Transactional
    public AccessPolicy createAccessPolicy(CreateAccessPolicyCommand command) {
        requireText(command.getTenantKey(), "tenantKey");
        requireText(command.getAppKey(), "appKey");
        requireText(command.getPolicyKey(), "policyKey");
        requireText(command.getDisplayName(), "displayName");
        assertIn(command.getOperationCode(), "operationCode", "READ", "UPDATE", "DELETE");
        assertIn(command.getEffectKind(), "effectKind", "ALLOW", "DENY");
        Long targetRelationId = parseRequiredLong(command.getTargetRelationId(), "targetRelationId");
        RelationResource resource = requireRelationResource(targetRelationId);
        if (!command.getTenantKey().equals(resource.getTenantKey()) || !command.getAppKey().equals(resource.getAppKey())) {
            throw new IllegalArgumentException("targetRelationId must belong to tenantKey and appKey");
        }

        AccessPolicy policy = new AccessPolicy();
        policy.setId(idGenerator.nextId());
        policy.setTenantKey(command.getTenantKey());
        policy.setAppKey(command.getAppKey());
        policy.setPolicyKey(command.getPolicyKey());
        policy.setDisplayName(command.getDisplayName());
        policy.setDescription(command.getDescription());
        policy.setTargetRelationId(targetRelationId);
        policy.setOperationCode(command.getOperationCode());
        policy.setEffectKind(command.getEffectKind());
        policy.setLifecycleState("DRAFT");
        repository.saveAccessPolicy(policy);
        return repository.findAccessPolicy(policy.getId());
    }

    public List<AccessPolicy> searchAccessPolicies(AccessPolicySearchQuery query) {
        return repository.searchAccessPolicies(query.getTenantKey(), query.getAppKey(), query.getPolicyKey(),
                parseLong(query.getTargetRelationId()), query.getOperationCode(), query.getLifecycleState());
    }

    @Transactional
    public AccessPolicy updateAccessPolicy(Long id, UpdateAccessPolicyCommand command) {
        AccessPolicy policy = requireAccessPolicy(id);
        if (StringUtils.hasText(command.getDisplayName())) {
            policy.setDisplayName(command.getDisplayName());
        }
        if (command.getDescription() != null) {
            policy.setDescription(command.getDescription());
        }
        if (StringUtils.hasText(command.getOperationCode())) {
            assertIn(command.getOperationCode(), "operationCode", "READ", "UPDATE", "DELETE");
            policy.setOperationCode(command.getOperationCode());
        }
        if (StringUtils.hasText(command.getEffectKind())) {
            assertIn(command.getEffectKind(), "effectKind", "ALLOW", "DENY");
            policy.setEffectKind(command.getEffectKind());
        }
        if (StringUtils.hasText(command.getLifecycleState())) {
            assertIn(command.getLifecycleState(), "lifecycleState", "DRAFT", "ACTIVE", "SUSPENDED", "ARCHIVED");
            policy.setLifecycleState(command.getLifecycleState());
        }
        repository.updateAccessPolicy(policy);
        return repository.findAccessPolicy(id);
    }

    @Transactional
    public AccessPolicy activateAccessPolicy(Long id) {
        AccessPolicy policy = requireAccessPolicy(id);
        policy.setLifecycleState("ACTIVE");
        repository.updateAccessPolicy(policy);
        return repository.findAccessPolicy(id);
    }

    @Transactional
    public PolicyRevision createPolicyRevision(Long policyId, CreatePolicyRevisionCommand command) {
        requireAccessPolicy(policyId);
        RuleBlueprint blueprint = resolveRevisionBlueprint(command);
        String predicateAst = compilePredicateAst(command, blueprint);
        String argumentSchema = defaultText(command.getArgumentSchema(), "{}");
        String attributeReferences = defaultText(command.getAttributeReferences(), "[]");
        validateJsonObject(predicateAst, "predicateAst");
        validateJsonObject(argumentSchema, "argumentSchema");
        validateJsonArray(attributeReferences, "attributeReferences");

        PolicyRevision revision = new PolicyRevision();
        revision.setId(idGenerator.nextId());
        revision.setPolicyId(policyId);
        revision.setRevisionNo(repository.nextRevisionNo(policyId));
        revision.setBlueprintId(blueprint == null ? null : blueprint.getId());
        revision.setPredicateAst(predicateAst);
        revision.setArgumentSchema(argumentSchema);
        revision.setAttributeReferences(attributeReferences);
        revision.setContentDigest(sha256(predicateAst + "|" + argumentSchema + "|" + attributeReferences));
        revision.setRevisionState("DRAFT");
        revision.setChangeSummary(command.getChangeSummary());
        repository.savePolicyRevision(revision);
        return repository.findPolicyRevision(revision.getId());
    }

    public List<PolicyRevision> searchPolicyRevisions(Long policyId, PolicyRevisionSearchQuery query) {
        requireAccessPolicy(policyId);
        return repository.searchPolicyRevisions(policyId, query.getRevisionState());
    }

    @Transactional
    public PolicyRevision activatePolicyRevision(Long id) {
        PolicyRevision revision = requirePolicyRevision(id);
        if (!"DRAFT".equals(revision.getRevisionState())) {
            throw new IllegalArgumentException("Only DRAFT policy revision can be activated");
        }
        AccessPolicy policy = requireAccessPolicy(revision.getPolicyId());
        if (!"ACTIVE".equals(policy.getLifecycleState())) {
            throw new IllegalArgumentException("Policy must be ACTIVE before activating revision");
        }
        repository.retireActivePolicyRevisions(revision.getPolicyId());
        repository.activatePolicyRevision(id, DEFAULT_ACTOR);
        return repository.findPolicyRevision(id);
    }

    @Transactional
    public PolicyRevision retirePolicyRevision(Long id) {
        PolicyRevision revision = requirePolicyRevision(id);
        if ("RETIRED".equals(revision.getRevisionState())) {
            return revision;
        }
        repository.retirePolicyRevision(id, DEFAULT_ACTOR);
        return repository.findPolicyRevision(id);
    }

    @Transactional
    public AccessGrant createAccessGrant(CreateAccessGrantCommand command) {
        requireText(command.getTenantKey(), "tenantKey");
        requireText(command.getAppKey(), "appKey");
        requireText(command.getGrantKey(), "grantKey");
        Long principalId = parseRequiredLong(command.getPrincipalId(), "principalId");
        Long policyId = parseRequiredLong(command.getPolicyId(), "policyId");
        if (!repository.principalExists(command.getTenantKey(), principalId)) {
            throw new IllegalArgumentException("principalId must reference an existing principal in tenantKey");
        }
        AccessPolicy policy = requireAccessPolicy(policyId);
        if (!command.getTenantKey().equals(policy.getTenantKey()) || !command.getAppKey().equals(policy.getAppKey())) {
            throw new IllegalArgumentException("policyId must belong to tenantKey and appKey");
        }
        if (!"ACTIVE".equals(policy.getLifecycleState())) {
            throw new IllegalArgumentException("policyId must reference an ACTIVE access policy");
        }

        AccessGrant grant = new AccessGrant();
        grant.setId(idGenerator.nextId());
        grant.setTenantKey(command.getTenantKey());
        grant.setAppKey(command.getAppKey());
        grant.setGrantKey(command.getGrantKey());
        grant.setPrincipalId(principalId);
        grant.setPolicyId(policyId);
        grant.setValidFrom(parseDateTime(command.getValidFrom(), LocalDateTime.now()));
        grant.setValidUntil(parseDateTime(command.getValidUntil(), null));
        if (grant.getValidUntil() != null && !grant.getValidUntil().isAfter(grant.getValidFrom())) {
            throw new IllegalArgumentException("validUntil must be after validFrom");
        }
        grant.setLifecycleState("ACTIVE");
        grant.setGrantSource(defaultText(command.getGrantSource(), "MANUAL"));
        assertIn(grant.getGrantSource(), "grantSource", "MANUAL", "IMPORT", "API", "AI_ASSISTED");
        grant.setReason(command.getReason());
        repository.saveAccessGrant(grant);
        return repository.findAccessGrant(grant.getId());
    }

    public List<AccessGrant> searchAccessGrants(AccessGrantSearchQuery query) {
        return repository.searchAccessGrants(query.getTenantKey(), query.getAppKey(), parseLong(query.getPrincipalId()),
                parseLong(query.getPolicyId()), query.getLifecycleState());
    }

    public List<Principal> searchPrincipals(PrincipalSearchQuery query) {
        requireText(query.getTenantKey(), "tenantKey");
        if (StringUtils.hasText(query.getPrincipalKind())) {
            assertIn(query.getPrincipalKind(), "principalKind", "USER", "ORGANIZATION", "POSITION", "ROLE", "GROUP");
        }
        if (StringUtils.hasText(query.getLifecycleState())) {
            assertIn(query.getLifecycleState(), "lifecycleState", "ACTIVE", "INACTIVE", "RETIRED");
        }
        return repository.searchPrincipals(query.getTenantKey(), query.getPrincipalKind(), query.getPrincipalKey(),
                query.getKeyword(), query.getLifecycleState());
    }

    public List<PrincipalUserProjection> searchPrincipalUsers(PrincipalProjectionSearchQuery query) {
        validatePrincipalProjectionQuery(query);
        return repository.searchPrincipalUsers(query.getTenantKey(), defaultText(query.getUserId(), query.getPrincipalKey()),
                query.getStaffNo(), query.getDepartmentCode(), query.getPostCode(), query.getKeyword(),
                query.getLifecycleState(), boundedLimit(query.getLimit()), boundedOffset(query.getOffset()));
    }

    public List<PrincipalOrganizationProjection> searchPrincipalOrganizations(PrincipalProjectionSearchQuery query) {
        validatePrincipalProjectionQuery(query);
        return repository.searchPrincipalOrganizations(query.getTenantKey(), defaultText(query.getDepartmentCode(), query.getPrincipalKey()),
                query.getParentDepartmentCode(), query.getKeyword(), query.getLifecycleState(),
                boundedLimit(query.getLimit()), boundedOffset(query.getOffset()));
    }

    public List<PrincipalPositionProjection> searchPrincipalPositions(PrincipalProjectionSearchQuery query) {
        validatePrincipalProjectionQuery(query);
        return repository.searchPrincipalPositions(query.getTenantKey(), defaultText(query.getPostCode(), query.getPrincipalKey()),
                query.getDepartmentCode(), query.getKeyword(), query.getLifecycleState(),
                boundedLimit(query.getLimit()), boundedOffset(query.getOffset()));
    }

    @Transactional
    public AccessGrant updateAccessGrant(Long id, UpdateAccessGrantCommand command) {
        AccessGrant grant = requireMutableAccessGrant(id);
        if (StringUtils.hasText(command.getValidFrom())) {
            grant.setValidFrom(parseDateTime(command.getValidFrom(), grant.getValidFrom()));
        }
        if (command.getValidUntil() != null) {
            grant.setValidUntil(parseDateTime(command.getValidUntil(), null));
        }
        if (grant.getValidUntil() != null && !grant.getValidUntil().isAfter(grant.getValidFrom())) {
            throw new IllegalArgumentException("validUntil must be after validFrom");
        }
        if (StringUtils.hasText(command.getGrantSource())) {
            assertIn(command.getGrantSource(), "grantSource", "MANUAL", "IMPORT", "API", "AI_ASSISTED");
            grant.setGrantSource(command.getGrantSource());
        }
        if (command.getReason() != null) {
            grant.setReason(command.getReason());
        }
        repository.updateAccessGrant(grant);
        return repository.findAccessGrant(id);
    }

    @Transactional
    public AccessGrant suspendAccessGrant(Long id) {
        AccessGrant grant = requireMutableAccessGrant(id);
        grant.setLifecycleState("SUSPENDED");
        repository.updateAccessGrant(grant);
        return repository.findAccessGrant(id);
    }

    @Transactional
    public AccessGrant resumeAccessGrant(Long id) {
        AccessGrant grant = requireAccessGrant(id);
        if ("ACTIVE".equals(grant.getLifecycleState())) {
            return grant;
        }
        if (!"SUSPENDED".equals(grant.getLifecycleState())) {
            throw new IllegalArgumentException("Only SUSPENDED access grant can be resumed");
        }
        AccessPolicy policy = requireAccessPolicy(grant.getPolicyId());
        if (!"ACTIVE".equals(policy.getLifecycleState())) {
            throw new IllegalArgumentException("Access grant policy must be ACTIVE before resume");
        }
        if (grant.getValidUntil() != null && !grant.getValidUntil().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Expired access grant cannot be resumed");
        }
        grant.setLifecycleState("ACTIVE");
        repository.updateAccessGrant(grant);
        return repository.findAccessGrant(id);
    }

    @Transactional
    public AccessGrant revokeAccessGrant(Long id) {
        AccessGrant grant = requireAccessGrant(id);
        if ("REVOKED".equals(grant.getLifecycleState())) {
            return grant;
        }
        if ("EXPIRED".equals(grant.getLifecycleState())) {
            throw new IllegalArgumentException("EXPIRED access grant cannot be revoked");
        }
        grant.setLifecycleState("REVOKED");
        grant.setRevokedBy(DEFAULT_ACTOR);
        grant.setRevokedAt(LocalDateTime.now());
        repository.updateAccessGrant(grant);
        return repository.findAccessGrant(id);
    }

    @Transactional
    public List<GrantArgument> replaceGrantArguments(Long accessGrantId, List<GrantArgumentCommand> commands) {
        requireAccessGrant(accessGrantId);
        List<GrantArgument> arguments = new ArrayList<GrantArgument>();
        if (commands != null) {
            for (GrantArgumentCommand command : commands) {
                requireText(command.getArgumentKey(), "argumentKey");
                assertIn(command.getValueKind(), "valueKind", "STRING", "INTEGER", "DECIMAL", "BOOLEAN", "DATE", "DATETIME",
                        "STRING_SET", "NUMBER_SET");
                requireText(command.getArgumentValue(), "argumentValue");
                validateJsonValue(command.getArgumentValue(), "argumentValue");
                GrantArgument argument = new GrantArgument();
                argument.setAccessGrantId(accessGrantId);
                argument.setArgumentKey(command.getArgumentKey());
                argument.setValueKind(command.getValueKind());
                argument.setArgumentValue(command.getArgumentValue());
                argument.setSensitiveFlag(Boolean.TRUE.equals(command.getSensitiveFlag()));
                arguments.add(argument);
            }
        }
        repository.replaceGrantArguments(accessGrantId, arguments);
        return repository.findGrantArguments(accessGrantId);
    }

    public List<GrantArgument> findGrantArguments(Long accessGrantId) {
        requireAccessGrant(accessGrantId);
        return repository.findGrantArguments(accessGrantId);
    }

    private RelationResource requireRelationResource(Long id) {
        RelationResource resource = dataCatalogRepository.findRelationResource(id);
        if (resource == null) {
            throw new IllegalArgumentException("Relation resource not found: " + id);
        }
        return resource;
    }

    private RuleBlueprint requireRuleBlueprint(Long id) {
        RuleBlueprint blueprint = repository.findRuleBlueprint(id);
        if (blueprint == null) {
            throw new IllegalArgumentException("Rule blueprint not found: " + id);
        }
        return blueprint;
    }

    private AccessPolicy requireAccessPolicy(Long id) {
        AccessPolicy policy = repository.findAccessPolicy(id);
        if (policy == null) {
            throw new IllegalArgumentException("Access policy not found: " + id);
        }
        return policy;
    }

    private PolicyRevision requirePolicyRevision(Long id) {
        PolicyRevision revision = repository.findPolicyRevision(id);
        if (revision == null) {
            throw new IllegalArgumentException("Policy revision not found: " + id);
        }
        return revision;
    }

    private AccessGrant requireAccessGrant(Long id) {
        AccessGrant grant = repository.findAccessGrant(id);
        if (grant == null) {
            throw new IllegalArgumentException("Access grant not found: " + id);
        }
        return grant;
    }

    private AccessGrant requireMutableAccessGrant(Long id) {
        AccessGrant grant = requireAccessGrant(id);
        if ("REVOKED".equals(grant.getLifecycleState()) || "EXPIRED".equals(grant.getLifecycleState())) {
            throw new IllegalArgumentException("Access grant is not mutable in state: " + grant.getLifecycleState());
        }
        return grant;
    }

    private void validatePrincipalProjectionQuery(PrincipalProjectionSearchQuery query) {
        requireText(query.getTenantKey(), "tenantKey");
        if (StringUtils.hasText(query.getLifecycleState())) {
            assertIn(query.getLifecycleState(), "lifecycleState", "ACTIVE", "INACTIVE", "RETIRED");
        }
    }

    private int boundedLimit(Integer limit) {
        if (limit == null) {
            return 200;
        }
        if (limit.intValue() < 1 || limit.intValue() > 1000) {
            throw new IllegalArgumentException("limit must be between 1 and 1000");
        }
        return limit.intValue();
    }

    private int boundedOffset(Integer offset) {
        if (offset == null) {
            return 0;
        }
        if (offset.intValue() < 0) {
            throw new IllegalArgumentException("offset must be greater than or equal to 0");
        }
        return offset.intValue();
    }

    private RuleBlueprint resolveRevisionBlueprint(CreatePolicyRevisionCommand command) {
        Long blueprintId = parseLong(command.getBlueprintId());
        if (blueprintId == null) {
            return null;
        }
        RuleBlueprint blueprint = requireRuleBlueprint(blueprintId);
        if (!"PUBLISHED".equals(blueprint.getLifecycleState())) {
            throw new IllegalArgumentException("blueprintId must reference a PUBLISHED rule blueprint");
        }
        return blueprint;
    }

    private String compilePredicateAst(CreatePolicyRevisionCommand command, RuleBlueprint blueprint) {
        if (blueprint == null) {
            return compileCustomAst(command);
        }
        if ("STANDARD_BLUEPRINT".equals(blueprint.getBlueprintKind())) {
            return compileStandardBlueprint(command.getTemplateBinding());
        }
        if ("CUSTOM_BLUEPRINT".equals(blueprint.getBlueprintKind())) {
            return compileCustomBlueprint(command, blueprint);
        }
        throw new IllegalArgumentException("blueprintKind has unsupported value: " + blueprint.getBlueprintKind());
    }

    private String compileStandardBlueprint(String templateBinding) {
        JsonNode binding = readJsonObject(templateBinding, "templateBinding");
        JsonNode conditions = binding.path("conditions");
        if (!conditions.isArray() || conditions.size() == 0) {
            ObjectNode trueNode = objectMapper.createObjectNode();
            trueNode.put("operator", "TRUE");
            return writeJson(trueNode);
        }
        String joiner = binding.path("joiner").asText(binding.path("logicalOperator").asText("AND"));
        String operator = "OR".equalsIgnoreCase(joiner) ? "OR" : "AND";
        if (conditions.size() == 1) {
            return writeJson(toConditionNode(conditions.get(0)));
        }
        ObjectNode root = objectMapper.createObjectNode();
        root.put("operator", operator);
        ArrayNode operands = root.putArray("operands");
        for (JsonNode condition : conditions) {
            operands.add(toConditionNode(condition));
        }
        return writeJson(root);
    }

    private ObjectNode toConditionNode(JsonNode condition) {
        String fieldKey = condition.path("fieldKey").asText(null);
        requireText(fieldKey, "conditions[].fieldKey");
        String operator = normalizePredicateOperator(condition.path("operator").asText(null));
        ObjectNode predicate = objectMapper.createObjectNode();
        predicate.put("operator", operator);
        ObjectNode left = predicate.putObject("left");
        left.put("kind", "FIELD");
        left.put("fieldKey", fieldKey);
        if (!"IS_NULL".equals(operator) && !"IS_NOT_NULL".equals(operator)) {
            predicate.set("right", toRightExpression(condition));
        }
        if (StringUtils.hasText(condition.path("accessPathKey").asText(null))) {
            ObjectNode pathNode = objectMapper.createObjectNode();
            pathNode.put("operator", "EXISTS_PATH");
            pathNode.put("accessPathKey", condition.path("accessPathKey").asText());
            pathNode.set("pathPredicate", predicate);
            return pathNode;
        }
        return predicate;
    }

    private String normalizePredicateOperator(String value) {
        requireText(value, "conditions[].operator");
        String operator = value.trim();
        if ("=".equals(operator)) { return "EQ"; }
        if ("!=".equals(operator) || "<>".equals(operator)) { return "NE"; }
        if (">".equals(operator)) { return "GT"; }
        if (">=".equals(operator)) { return "GE"; }
        if ("<".equals(operator)) { return "LT"; }
        if ("<=".equals(operator)) { return "LE"; }
        if ("isNull".equals(operator)) { return "IS_NULL"; }
        if ("isNotNull".equals(operator)) { return "IS_NOT_NULL"; }
        if ("startsWith".equals(operator)) { return "STARTS_WITH"; }
        if ("endsWith".equals(operator)) { return "ENDS_WITH"; }
        return operator.trim().toUpperCase(Locale.ROOT);
    }

    private ObjectNode toRightExpression(JsonNode condition) {
        JsonNode valueNode = condition.get("value");
        if (valueNode == null || valueNode.isNull()) {
            throw new IllegalArgumentException("conditions[].value must not be null");
        }
        String valueType = condition.path("valueType").asText("LITERAL").trim().toUpperCase(Locale.ROOT);
        ObjectNode right = objectMapper.createObjectNode();
        if ("SUBJECT_PROPERTY".equals(valueType) || "SUBJECT_ATTRIBUTE".equals(valueType) || "ATTRIBUTE".equals(valueType)) {
            right.put("kind", "ATTRIBUTE");
            right.put("attributeKey", normalizeSubjectAttribute(valueNode.asText()));
            return right;
        }
        if ("ARGUMENT".equals(valueType) || "PARAM".equals(valueType) || "PARAMETER".equals(valueType)) {
            right.put("kind", "ARGUMENT");
            right.put("argumentKey", valueNode.asText());
            return right;
        }
        if ("BINDING".equals(valueType)) {
            right.put("kind", "BINDING");
            right.put("bindingKey", valueNode.asText());
            return right;
        }
        if (!"LITERAL".equals(valueType)) {
            throw new IllegalArgumentException("conditions[].valueType has unsupported value: " + valueType);
        }
        right.put("kind", "LITERAL");
        right.set("literalValue", valueNode.deepCopy());
        right.put("valueKind", condition.path("valueKind").asText("STRING").trim().toUpperCase(Locale.ROOT));
        return right;
    }

    private String normalizeSubjectAttribute(String value) {
        requireText(value, "conditions[].value");
        String trimmed = value.trim();
        if (trimmed.startsWith("#sub['") && trimmed.endsWith("']") && trimmed.length() > 8) {
            return trimmed.substring(6, trimmed.length() - 2);
        }
        if (trimmed.startsWith("#sub[\"") && trimmed.endsWith("\"]") && trimmed.length() > 8) {
            return trimmed.substring(6, trimmed.length() - 2);
        }
        return trimmed;
    }

    private String compileCustomBlueprint(CreatePolicyRevisionCommand command, RuleBlueprint blueprint) {
        JsonNode binding = readJsonObject(command.getTemplateBinding(), "templateBinding");
        JsonNode arguments = binding.has("arguments") ? binding.path("arguments") : binding.path("placeholders");
        if (!arguments.isObject()) {
            throw new IllegalArgumentException("CUSTOM_BLUEPRINT templateBinding.arguments must be a JSON object");
        }
        JsonNode template = readJsonObject(blueprint.getPredicateTemplate(), "predicateTemplate");
        JsonNode compiled = instantiateTemplateNode(template, arguments);
        // 如果 templateBinding 携带 accessPathKey，用 EXISTS_PATH 包裹编译后的谓词
        String accessPathKey = binding.path("accessPathKey").asText(null);
        if (StringUtils.hasText(accessPathKey)) {
            ObjectNode pathNode = objectMapper.createObjectNode();
            pathNode.put("operator", "EXISTS_PATH");
            pathNode.put("accessPathKey", accessPathKey);
            pathNode.set("pathPredicate", compiled);
            compiled = pathNode;
        }
        return writeJson(compiled);
    }

    private JsonNode instantiateTemplateNode(JsonNode node, JsonNode arguments) {
        if (node.isObject()) {
            String kind = node.path("kind").asText(null);
            String name = node.path("name").asText(null);
            if (StringUtils.hasText(kind) && kind.endsWith("_PLACEHOLDER") && StringUtils.hasText(name)) {
                return expressionFromPlaceholder(kind, name, arguments, node);
            }
            if (node.has("$placeholder")) {
                return requirePlaceholderValue(node.path("$placeholder").asText(), arguments);
            }
            ObjectNode copy = objectMapper.createObjectNode();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                copy.set(entry.getKey(), instantiateTemplateNode(entry.getValue(), arguments));
            }
            return copy;
        }
        if (node.isArray()) {
            ArrayNode copy = objectMapper.createArrayNode();
            for (JsonNode child : node) {
                copy.add(instantiateTemplateNode(child, arguments));
            }
            return copy;
        }
        if (node.isTextual()) {
            String text = node.asText();
            if (text.startsWith("${") && text.endsWith("}") && text.length() > 3) {
                return requirePlaceholderValue(text.substring(2, text.length() - 1), arguments);
            }
            String replaced = text;
            Iterator<String> names = arguments.fieldNames();
            while (names.hasNext()) {
                String name = names.next();
                replaced = replaced.replace("${" + name + "}", arguments.path(name).asText());
            }
            return objectMapper.getNodeFactory().textNode(replaced);
        }
        return node.deepCopy();
    }

    private ObjectNode expressionFromPlaceholder(String kind, String name, JsonNode arguments, JsonNode placeholderNode) {
        JsonNode value = requirePlaceholderValue(name, arguments);
        ObjectNode expression = objectMapper.createObjectNode();
        if ("FIELD_PLACEHOLDER".equals(kind)) {
            expression.put("kind", "FIELD");
            expression.put("fieldKey", value.asText());
        } else if ("ATTRIBUTE_PLACEHOLDER".equals(kind)) {
            expression.put("kind", "ATTRIBUTE");
            expression.put("attributeKey", value.asText());
        } else if ("ARGUMENT_PLACEHOLDER".equals(kind)) {
            expression.put("kind", "ARGUMENT");
            expression.put("argumentKey", value.asText());
        } else if ("BINDING_PLACEHOLDER".equals(kind)) {
            expression.put("kind", "BINDING");
            expression.put("bindingKey", value.asText());
        } else if ("LITERAL_PLACEHOLDER".equals(kind)) {
            expression.put("kind", "LITERAL");
            expression.set("literalValue", value.deepCopy());
            if (StringUtils.hasText(placeholderNode.path("valueKind").asText(null))) {
                expression.put("valueKind", placeholderNode.path("valueKind").asText());
            }
        } else {
            throw new IllegalArgumentException("Unsupported placeholder kind: " + kind);
        }
        return expression;
    }

    private JsonNode requirePlaceholderValue(String name, JsonNode arguments) {
        requireText(name, "placeholderName");
        JsonNode value = arguments.get(name);
        if (value == null || value.isNull()) {
            throw new IllegalArgumentException("Missing template placeholder argument: " + name);
        }
        return value.deepCopy();
    }

    private String compileCustomAst(CreatePolicyRevisionCommand command) {
        requireText(command.getCustomAst(), "customAst");
        ObjectNode node = objectMapper.createObjectNode();
        node.put("operator", "CUSTOM_AST");
        node.set("customAst", readJsonObject(command.getCustomAst(), "customAst"));
        return writeJson(node);
    }

    private JsonNode readJsonObject(String value, String fieldName) {
        JsonNode node = validateJsonValue(value, fieldName);
        if (!node.isObject()) {
            throw new IllegalArgumentException(fieldName + " must be a JSON object");
        }
        return node;
    }

    private String writeJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Failed to serialize predicateAst", exception);
        }
    }

    private void validateJsonObject(String value, String fieldName) {
        JsonNode node = validateJsonValue(value, fieldName);
        if (!node.isObject()) {
            throw new IllegalArgumentException(fieldName + " must be a JSON object");
        }
    }

    private void validateJsonArray(String value, String fieldName) {
        JsonNode node = validateJsonValue(value, fieldName);
        if (!node.isArray()) {
            throw new IllegalArgumentException(fieldName + " must be a JSON array");
        }
    }

    private JsonNode validateJsonValue(String value, String fieldName) {
        try {
            return objectMapper.readTree(value);
        } catch (Exception exception) {
            throw new IllegalArgumentException(fieldName + " must be valid JSON", exception);
        }
    }

    private void requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    private void assertIn(String value, String fieldName, String... allowedValues) {
        requireText(value, fieldName);
        for (String allowedValue : allowedValues) {
            if (allowedValue.equals(value)) {
                return;
            }
        }
        throw new IllegalArgumentException(fieldName + " has unsupported value: " + value);
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private Long parseRequiredLong(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return parseLong(value);
    }

    private Long parseLong(String value) {
        return StringUtils.hasText(value) ? Long.valueOf(value) : null;
    }

    private LocalDateTime parseDateTime(String value, LocalDateTime defaultValue) {
        return StringUtils.hasText(value) ? LocalDateTime.parse(value) : defaultValue;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 not available", exception);
        }
    }
}