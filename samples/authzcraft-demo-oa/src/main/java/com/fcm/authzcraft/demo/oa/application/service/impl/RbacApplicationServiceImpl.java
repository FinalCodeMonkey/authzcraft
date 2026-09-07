package com.fcm.authzcraft.demo.oa.application.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fcm.authzcraft.demo.oa.application.service.RbacApplicationService;
import com.fcm.authzcraft.demo.oa.domain.gateway.AuthzCraftDataPermissionGateway;
import com.fcm.authzcraft.demo.oa.domain.gateway.PrincipalAdminGateway;
import com.fcm.authzcraft.demo.oa.domain.gateway.PrincipalDirectory;
import com.fcm.authzcraft.demo.oa.domain.gateway.RoleAuthorizationStore;
import com.fcm.authzcraft.demo.oa.domain.model.CurrentUser;
import com.fcm.authzcraft.demo.oa.domain.model.RbacFieldGrant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class RbacApplicationServiceImpl implements RbacApplicationService {

    private final PrincipalDirectory principalDirectory;
    private final PrincipalAdminGateway principalAdminGateway;
    private final RoleAuthorizationStore roleAuthorizationStore;
    private final AuthzCraftDataPermissionGateway dataPermissionGateway;
    private final ObjectMapper objectMapper;

    public RbacApplicationServiceImpl(PrincipalDirectory principalDirectory,
                                      PrincipalAdminGateway principalAdminGateway,
                                      RoleAuthorizationStore roleAuthorizationStore,
                                      AuthzCraftDataPermissionGateway dataPermissionGateway,
                                      ObjectMapper objectMapper) {
        this.principalDirectory = principalDirectory;
        this.principalAdminGateway = principalAdminGateway;
        this.roleAuthorizationStore = roleAuthorizationStore;
        this.dataPermissionGateway = dataPermissionGateway;
        this.objectMapper = objectMapper;
    }

    @Override
    public CurrentUser resolveCurrentUser(String userKey, String requestKey) {
        if (!StringUtils.hasText(userKey)) {
            return null;
        }
        Map<String, Object> principal = principalDirectory.findActiveUserProjection(userKey.trim());
        if (principal == null) {
            return null;
        }
        Set<String> roleCodes = principalDirectory.listRoleCodes(userKey.trim());
        return new CurrentUser(
                userKey.trim(),
                displayName(principal, userKey.trim()),
                roleCodes,
                permissionCodesForRoles(roleCodes),
                fieldGrantsForRoles(roleCodes),
                requestKey);
    }

    @Override
    public Map<String, Object> currentPermissions(CurrentUser currentUser) {
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("userKey", currentUser.getUserKey());
        response.put("displayName", currentUser.getDisplayName());
        response.put("roles", new ArrayList<String>(currentUser.getRoleCodes()));
        response.put("permissions", new ArrayList<String>(currentUser.getPermissionCodes()));
        response.put("fieldPermissions", fieldGrantMaps(currentUser.getFieldGrants()));
        response.put("requestKey", currentUser.getRequestKey());
        return response;
    }

    @Override
    public List<Map<String, Object>> projectRows(CurrentUser currentUser, String resourceKey, List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return rows == null ? Collections.<Map<String, Object>>emptyList() : rows;
        }
        Set<String> configuredFields = configuredFieldKeys(resourceKey);
        if (configuredFields.isEmpty()) {
            return rows;
        }
        Map<String, String> fieldSecurityRequirements = configuredFieldSecurityRequirements(resourceKey);
        Map<String, String[]> grantByField = strongestGrantByField(currentUser, resourceKey);
        List<Map<String, Object>> projectedRows = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> projected = new LinkedHashMap<String, Object>(row);
            for (String fieldKey : configuredFields) {
                String[] grant = grantByField.get(fieldKey);
                // security_requirement 统一从回路2 authzcraft_resource_field 表取（字段固有属性）
                String securityRequirement = fieldSecurityRequirements.getOrDefault(fieldKey, "PLAIN").toUpperCase(Locale.ROOT);
                String accessLevel;
                if (grant == null) {
                    // 用户没有该字段的任何权限绑定——使用 security_requirement 对应的默认 access_level
                    accessLevel = defaultAccessLevel(securityRequirement);
                } else {
                    // grant 只提供 access_level（授权可变属性），security_requirement 从回路2取
                    accessLevel = grant[1];
                }
                if ("NONE".equals(accessLevel)) {
                    projected.remove(fieldKey);
                } else if ("MASKED".equals(securityRequirement) && !"PLAIN_READ".equals(accessLevel) && !"WRITE".equals(accessLevel)) {
                    projected.put(fieldKey, "***");
                }
            }
            projectedRows.add(projected);
        }
        return projectedRows;
    }

    @Override
    public List<Map<String, Object>> listRoles() { return principalDirectory.listRoles(); }

    @Override
    public List<Map<String, Object>> listGroups() { return principalDirectory.listGroups(); }

    @Override
    public List<Map<String, Object>> listPermissions() { return roleAuthorizationStore.listPermissions(); }

    @Override
    public List<Map<String, Object>> listFieldPermissions() {
        List<Map<String, Object>> fields = roleAuthorizationStore.listFieldPermissions();
        // security_requirement 从回路2 authzcraft_resource_field 表取（字段固有属性），不使用回路1的值
        Map<String, Map<String, String>> securityByResource = new java.util.HashMap<String, Map<String, String>>();
        for (Map<String, Object> field : fields) {
            String resourceKey = text(field.get("resourceKey"));
            if (!securityByResource.containsKey(resourceKey)) {
                securityByResource.put(resourceKey, fieldSecurityRequirementsFromCenter(resourceKey));
            }
            Map<String, String> fieldSecs = securityByResource.get(resourceKey);
            String fieldKey = text(field.get("fieldKey"));
            String securityReq = fieldSecs != null ? fieldSecs.getOrDefault(fieldKey, "PLAIN") : "PLAIN";
            field.put("securityRequirement", securityReq);
        }
        return fields;
    }

    @Override
    public List<Map<String, Object>> listUsers() {
        return principalDirectory.listUsers();
    }

    @Override
    public List<String> listRolePermissionCodes(String roleId) {
        return roleAuthorizationStore.listRolePermissionCodesByRoleCode(principalDirectory.roleCode(roleId));
    }

    @Override
    public List<String> listRoleFieldPermissionCodes(String roleId) {
        return roleAuthorizationStore.listRoleFieldPermissionCodesByRoleCode(principalDirectory.roleCode(roleId));
    }

    @Override
    public List<Map<String, Object>> listRoleUsers(String roleId) { return principalDirectory.listRoleUsers(roleId); }

    @Override
    public Map<String, Object> searchApplicationRbacStatus() {
        return principalAdminGateway.searchApplicationRbacStatus();
    }

    @Override
    @Transactional
    public Map<String, Object> syncApplicationRbac(Map<String, Object> request) {
        return principalAdminGateway.syncApplicationRbac(Boolean.TRUE.equals(request == null ? null : request.get("dryRun")));
    }

    @Override
    @Transactional
    public Map<String, Object> createRole(Map<String, Object> request) {
        String code = requiredText(request, "code");
        String name = requiredText(request, "name");
        String description = text(request.get("description"));
        return principalAdminGateway.createRole(code, name, description);
    }

    @Override
    @Transactional
    public Map<String, Object> updateRole(String roleId, Map<String, Object> request) {
        String name = requiredText(request, "name");
        String description = text(request.get("description"));
        return principalAdminGateway.updateRole(roleId, name, description);
    }

    @Override
    public Map<String, Object> deleteRoleImpact(String roleId) {
        Map<String, Object> impact = singletonId(roleId);
        String roleCode = principalDirectory.roleCode(roleId);
        impact.put("userCount", principalDirectory.countRoleUsers(roleId));
        impact.put("permissionCount", roleAuthorizationStore.countRolePermissionsByRoleCode(roleCode));
        impact.put("fieldPermissionCount", roleAuthorizationStore.countRoleFieldPermissionsByRoleCode(roleCode));
        impact.put("requiresConfirmation", true);
        return impact;
    }

    @Override
    @Transactional
    public void deleteRole(String roleId, Map<String, Object> request) {
        if (!Boolean.TRUE.equals(request == null ? null : request.get("confirmed"))) {
            throw new IllegalArgumentException("confirmed=true is required before deleting a role");
        }
        String roleCode = principalDirectory.roleCode(roleId);
        roleAuthorizationStore.replaceRolePermissionsByRoleCode(roleCode, Collections.<String>emptyList());
        roleAuthorizationStore.replaceRoleFieldPermissionsByRoleCode(roleCode, Collections.<String>emptyList());
        principalAdminGateway.deleteRole(roleId);
    }

    @Override
    @Transactional
    public void replaceRolePermissions(String roleId, Map<String, Object> request) {
        List<String> codes = stringList(request, "permissionCodes");
        validateKnownPermissions(codes);
        roleAuthorizationStore.replaceRolePermissionsByRoleCode(principalDirectory.roleCode(roleId), codes);
    }

    @Override
    @Transactional
    public void replaceRoleFieldPermissions(String roleId, Map<String, Object> request) {
        List<String> codes = stringList(request, "fieldPermissionCodes");
        validateKnownFieldPermissions(codes);
        roleAuthorizationStore.replaceRoleFieldPermissionsByRoleCode(principalDirectory.roleCode(roleId), codes);
    }

    @Override
    public List<String> listGroupPermissionCodes(String groupId) {
        return roleAuthorizationStore.listGroupPermissionCodesByGroupCode(principalDirectory.groupCode(groupId));
    }

    @Override
    public List<String> listGroupFieldPermissionCodes(String groupId) {
        return roleAuthorizationStore.listGroupFieldPermissionCodesByGroupCode(principalDirectory.groupCode(groupId));
    }

    @Override
    @Transactional
    public void replaceGroupPermissions(String groupId, Map<String, Object> request) {
        List<String> codes = stringList(request, "permissionCodes");
        validateKnownPermissions(codes);
        roleAuthorizationStore.replaceGroupPermissionsByGroupCode(principalDirectory.groupCode(groupId), codes);
    }

    @Override
    @Transactional
    public void replaceGroupFieldPermissions(String groupId, Map<String, Object> request) {
        List<String> codes = stringList(request, "fieldPermissionCodes");
        validateKnownFieldPermissions(codes);
        roleAuthorizationStore.replaceGroupFieldPermissionsByGroupCode(principalDirectory.groupCode(groupId), codes);
    }

    @Override
    @Transactional
    public void addRoleUser(String roleId, Map<String, Object> request) {
        String userKey = requiredText(request, "userKey");
        principalAdminGateway.addRoleUser(roleId, userKey, text(request.get("displayName")));
    }

    @Override
    @Transactional
    public void removeRoleUser(String roleId, Map<String, Object> request) {
        if (!Boolean.TRUE.equals(request == null ? null : request.get("confirmed"))) {
            throw new IllegalArgumentException("confirmed=true is required before removing a role member");
        }
        String userKey = requiredText(request, "userKey");
        principalAdminGateway.removeRoleUser(roleId, userKey);
    }

    @Override
    @Transactional
    public Map<String, Object> createGroup(Map<String, Object> request) {
        String code = requiredText(request, "code");
        String name = requiredText(request, "name");
        String description = text(request.get("description"));
        return principalAdminGateway.createGroup(code, name, description);
    }

    @Override
    @Transactional
    public Map<String, Object> updateGroup(String groupId, Map<String, Object> request) {
        String name = requiredText(request, "name");
        String description = text(request.get("description"));
        return principalAdminGateway.updateGroup(groupId, name, description);
    }

    @Override
    public Map<String, Object> deleteGroupImpact(String groupId) {
        Map<String, Object> impact = singletonId(groupId);
        impact.put("userCount", principalDirectory.countGroupUsers(groupId));
        impact.put("roleCount", 0);
        impact.put("dataPermissionCount", 0);
        impact.put("requiresConfirmation", true);
        return impact;
    }

    @Override
    @Transactional
    public void deleteGroup(String groupId, Map<String, Object> request) {
        if (!Boolean.TRUE.equals(request == null ? null : request.get("confirmed"))) {
            throw new IllegalArgumentException("confirmed=true is required before deleting a group");
        }
        principalAdminGateway.deleteGroup(groupId);
    }

    @Override
    public List<Map<String, Object>> listGroupUsers(String groupId) {
        return principalDirectory.listGroupUsers(groupId);
    }

    @Override
    public List<Map<String, Object>> listUserMemberships(String userKey) {
        return principalDirectory.listUserMemberships(userKey);
    }

    @Override
    @Transactional
    public void addGroupUser(String groupId, Map<String, Object> request) {
        String userKey = requiredText(request, "userKey");
        principalAdminGateway.addGroupUser(groupId, userKey, text(request.get("displayName")));
    }

    @Override
    @Transactional
    public void removeGroupUser(String groupId, Map<String, Object> request) {
        if (!Boolean.TRUE.equals(request == null ? null : request.get("confirmed"))) {
            throw new IllegalArgumentException("confirmed=true is required before removing a group member");
        }
        String userKey = requiredText(request, "userKey");
        principalAdminGateway.removeGroupUser(groupId, userKey);
    }

    @Override
    public Map<String, Object> saveDataPermissionTemplate(CurrentUser currentUser, Map<String, Object> request) {
        if (!principalDirectory.supportsDataPermissionTemplates()) {
            throw new IllegalArgumentException("data permission templates require AUTHZCRAFT storage mode");
        }
        String principalType = normalizePrincipalType(defaultText(text(request == null ? null : request.get("principalType")), "ROLE"));
        String principalCode = requiredText(request, "principalCode");
        String template = normalizeTemplate(requiredText(request, "ruleTemplate"));
        String resourceId = requiredText(request, "resourceId");
        String operationCode = normalizeOperation(defaultText(text(request == null ? null : request.get("operationCode")), "READ"));
        String anchorField = requiredText(request, "anchorField");
        String accessPathKey = text(request == null ? null : request.get("accessPathKey"));
        String simulationRequesterKey = defaultText(text(request == null ? null : request.get("simulationRequesterKey")), currentUser.getUserKey());

        Map<String, Object> principal = requirePrincipal(principalType, principalCode);
        Map<String, Object> resource = requireResource(resourceId, text(request == null ? null : request.get("resourceKey")));
        Map<String, Object> accessPath = requireAccessPathIfPresent(resourceId, accessPathKey);
        String anchorResourceId = accessPath == null ? resourceId : text(accessPath.get("destinationRelationId"));
        Map<String, Object> field;
        if ("ALL_DATA".equals(template)) {
            field = null;
            anchorField = "id";
        } else {
            field = requireFilterableField(anchorResourceId, anchorField);
        }
        if ("PERSON_SHARE".equals(template)) {
            requirePrincipal("USER", requiredText(request, "fromUserKey"));
        }
        if ("MANAGED_DEPARTMENTS".equals(template)) {
            validateManagedDepartmentScope(request);
        }
        if ("SPECIFIED_DEPARTMENTS".equals(template)) {
            validateSpecifiedDepartments(request);
        }

        String resourceKey = text(resource.get("resourceKey"));
        String policyKey = policyKey(template, resourceKey, operationCode, accessPathKey, anchorField);
        // 按模板创建独立蓝图：ALL_DATA 用 STANDARD_BLUEPRINT（无占位符），其余 4 个用 CUSTOM_BLUEPRINT（含占位符骨架）
        Map<String, Object> blueprint = ensureTemplateBlueprint(template);
        Map<String, Object> policy = ensureAccessPolicy(policyKey, template, resource, operationCode, anchorField);
        String templateBinding = templateBinding(template, accessPathKey, anchorField);
        String argumentSchema = argumentSchema(template);
        String attributeReferences = attributeReferences(template);
        Map<String, Object> revision = ensurePolicyRevision(policy, blueprint, templateBinding,
            argumentSchema, attributeReferences);
        Map<String, Object> grant = ensureAccessGrant(principal, policy, template, principalType, principalCode, request);
        List<Map<String, Object>> arguments = dataPermissionGateway.replaceGrantArguments(text(grant.get("id")), grantArguments(template, request));
        Map<String, Object> simulation = simulateTemplate(template, resourceKey, operationCode, simulationRequesterKey);

        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("status", "ACTIVE");
        response.put("ruleTemplate", template);
        response.put("principalType", principalType);
        response.put("principalCode", principalCode);
        response.put("principalId", text(principal.get("id")));
        response.put("resourceId", resourceId);
        response.put("resourceKey", resourceKey);
        response.put("resourceName", displayName(resource, resourceKey));
        response.put("accessPathKey", defaultText(accessPathKey, ""));
        response.put("fieldKey", field != null ? text(field.get("fieldKey")) : "id");
        response.put("operationCode", operationCode);
        response.put("policyId", text(policy.get("id")));
        response.put("policyKey", text(policy.get("policyKey")));
        response.put("revisionId", text(revision.get("id")));
        response.put("grantId", text(grant.get("id")));
        response.put("arguments", arguments);
        response.put("simulation", simulation);
        response.put("summary", dataPermissionSummary(template, principal, resource, field, request));
        return response;
    }

    private Map<String, Object> requireAccessPathIfPresent(String rootRelationId, String accessPathKey) {
        if (!StringUtils.hasText(accessPathKey)) {
            return null;
        }
        for (Map<String, Object> accessPath : dataPermissionGateway.searchAccessPaths(rootRelationId)) {
            if (accessPathKey.equals(text(accessPath.get("pathKey")))) {
                return accessPath;
            }
        }
        throw new IllegalArgumentException("accessPathKey was not found on selected resource: " + accessPathKey);
    }

    private Map<String, Object> requirePrincipal(String principalType, String principalCode) {
        Map<String, Object> principal = dataPermissionGateway.findPrincipal(principalType, principalCode);
        if (principal == null) {
            throw new IllegalArgumentException("principal was not found in AuthzCraft principal domain: " + principalType + ":" + principalCode);
        }
        return principal;
    }

    private Map<String, Object> requireResource(String resourceId, String expectedResourceKey) {
        for (Map<String, Object> resource : dataPermissionGateway.searchRelationResources()) {
            if (!resourceId.equals(text(resource.get("id")))) {
                continue;
            }
            if (StringUtils.hasText(expectedResourceKey) && !expectedResourceKey.equals(text(resource.get("resourceKey")))) {
                throw new IllegalArgumentException("resourceKey does not match selected resourceId");
            }
            if (StringUtils.hasText(text(resource.get("lifecycleState"))) && !"ACTIVE".equals(text(resource.get("lifecycleState")))) {
                throw new IllegalArgumentException("relation resource is not ACTIVE: " + resourceId);
            }
            return resource;
        }
        throw new IllegalArgumentException("relation resource was not found: " + resourceId);
    }

    private Map<String, Object> requireFilterableField(String resourceId, String fieldKey) {
        for (Map<String, Object> field : dataPermissionGateway.searchResourceFields(resourceId)) {
            if (!fieldKey.equals(text(field.get("fieldKey")))) {
                continue;
            }
            if (Boolean.FALSE.equals(field.get("filterableFlag"))) {
                throw new IllegalArgumentException("anchorField is not filterable: " + fieldKey);
            }
            if (StringUtils.hasText(text(field.get("lifecycleState"))) && !"ACTIVE".equals(text(field.get("lifecycleState")))) {
                throw new IllegalArgumentException("anchorField is not ACTIVE: " + fieldKey);
            }
            return field;
        }
        throw new IllegalArgumentException("anchorField was not found on resource: " + fieldKey);
    }

    private Map<String, Object> ensureTemplateBlueprint(String template) {
        String displayName = dataPermissionTemplateName(template) + "模板";
        Map<String, Object> published = dataPermissionGateway.findPublishedRuleBlueprint(template);
        if (published != null) {
            return published;
        }
        Map<String, Object> existing = dataPermissionGateway.findAnyRuleBlueprint(template);
        if (existing != null && "DRAFT".equals(text(existing.get("lifecycleState")))) {
            return dataPermissionGateway.publishRuleBlueprint(text(existing.get("id")));
        }
        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("blueprintKey", template);
        request.put("blueprintVersion", 1);
        request.put("displayName", displayName);
        request.put("description", "Demo OA data permission template: " + template);
        if ("ALL_DATA".equals(template)) {
            request.put("blueprintKind", "STANDARD_BLUEPRINT");
            request.put("predicateTemplate", "{}");
            request.put("inputSchema", "{}");
        } else {
            request.put("blueprintKind", "CUSTOM_BLUEPRINT");
            request.put("predicateTemplate", blueprintPredicateTemplate(template));
            request.put("inputSchema", blueprintInputSchema(template));
        }
        Map<String, Object> created = dataPermissionGateway.createRuleBlueprint(request);
        return dataPermissionGateway.publishRuleBlueprint(text(created.get("id")));
    }

    /** CUSTOM_BLUEPRINT 蓝图的谓词骨架——用占位符表达字段、属性、参数 */
    private String blueprintPredicateTemplate(String template) {
        Map<String, Object> left = new LinkedHashMap<String, Object>();
        left.put("kind", "FIELD_PLACEHOLDER");
        left.put("name", "anchorField");
        Map<String, Object> right = new LinkedHashMap<String, Object>();
        if ("MY_DATA".equals(template)) {
            right.put("kind", "ATTRIBUTE_PLACEHOLDER");
            right.put("name", "currentUserId");
        } else if ("MANAGED_DEPARTMENTS".equals(template)) {
            right.put("kind", "ATTRIBUTE_PLACEHOLDER");
            right.put("name", "managedDepartmentCodes");
        } else if ("SPECIFIED_DEPARTMENTS".equals(template)) {
            right.put("kind", "ARGUMENT_PLACEHOLDER");
            right.put("name", "specifiedDepartmentCodes");
        } else if ("PERSON_SHARE".equals(template)) {
            right.put("kind", "ARGUMENT_PLACEHOLDER");
            right.put("name", "sharedOwnerUserKey");
        } else {
            throw new IllegalArgumentException("Unsupported CUSTOM_BLUEPRINT template: " + template);
        }
        Map<String, Object> node = new LinkedHashMap<String, Object>();
        boolean inOperator = "MANAGED_DEPARTMENTS".equals(template) || "SPECIFIED_DEPARTMENTS".equals(template);
        node.put("operator", inOperator ? "IN" : "EQ");
        node.put("left", left);
        node.put("right", right);
        return toJson(node);
    }

    /** CUSTOM_BLUEPRINT 蓝图的 input_schema——声明占位符如何填写，供前端动态渲染配置表单 */
    private String blueprintInputSchema(String template) {
        Map<String, Object> schema = new LinkedHashMap<String, Object>();
        List<Map<String, Object>> fieldInputs = new ArrayList<Map<String, Object>>();
        fieldInputs.add(inputField("anchorField", "数据归属字段", "FILTERABLE_FIELD", true));
        schema.put("fieldInputs", fieldInputs);
        List<Map<String, Object>> attributeInputs = new ArrayList<Map<String, Object>>();
        if ("MY_DATA".equals(template)) {
            attributeInputs.add(inputAttribute("currentUserId", "currentUserId", "STRING", "REQUESTER_USER_ID"));
        } else if ("MANAGED_DEPARTMENTS".equals(template)) {
            attributeInputs.add(inputAttribute("managedDepartmentCodes", "managedDepartmentCodes", "STRING_SET", "MANAGED_DEPARTMENT_CODES"));
        }
        schema.put("attributeInputs", attributeInputs);
        List<Map<String, Object>> argumentInputs = new ArrayList<Map<String, Object>>();
        if ("MANAGED_DEPARTMENTS".equals(template)) {
            argumentInputs.add(inputArgument("departmentScopes", "departmentScopes", "STRING_SET", true, "部门范围"));
            argumentInputs.add(inputArgument("subDepartmentDepth", "subDepartmentDepth", "STRING", false, "下级部门层级"));
        } else if ("SPECIFIED_DEPARTMENTS".equals(template)) {
            argumentInputs.add(inputArgument("specifiedDepartmentCodes", "specifiedDepartmentCodes", "STRING_SET", true, "指定部门"));
        } else if ("PERSON_SHARE".equals(template)) {
            argumentInputs.add(inputArgument("sharedOwnerUserKey", "sharedOwnerUserKey", "STRING", true, "数据归属人"));
            argumentInputs.add(inputArgument("allowReshare", "allowReshare", "BOOLEAN", false, "是否允许再次分享"));
        }
        schema.put("argumentInputs", argumentInputs);
        return toJson(schema);
    }

    private Map<String, Object> inputField(String key, String label, String candidate, boolean required) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("key", key);
        m.put("label", label);
        m.put("candidate", candidate);
        m.put("required", required);
        return m;
    }

    private Map<String, Object> inputAttribute(String key, String attributeKey, String valueKind, String resolveKind) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("key", key);
        m.put("attributeKey", attributeKey);
        m.put("valueKind", valueKind);
        m.put("resolveKind", resolveKind);
        return m;
    }

    private Map<String, Object> inputArgument(String key, String argumentKey, String valueKind, boolean required, String label) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("key", key);
        m.put("argumentKey", argumentKey);
        m.put("valueKind", valueKind);
        m.put("required", required);
        m.put("label", label);
        return m;
    }

    private Map<String, Object> ensureAccessPolicy(String policyKey, String template, Map<String, Object> resource,
                                                   String operationCode, String anchorField) {
        Map<String, Object> existing = dataPermissionGateway.findAccessPolicy(policyKey);
        if (existing != null) {
            if ("ARCHIVED".equals(text(existing.get("lifecycleState")))) {
                throw new IllegalArgumentException("access policy is archived: " + policyKey);
            }
            if (!"ACTIVE".equals(text(existing.get("lifecycleState")))) {
                return dataPermissionGateway.activateAccessPolicy(text(existing.get("id")));
            }
            return existing;
        }
        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("policyKey", policyKey);
        request.put("displayName", dataPermissionPolicyName(template, resource, anchorField));
        request.put("description", "Created by Demo OA RBAC data permission template wizard.");
        request.put("targetRelationId", text(resource.get("id")));
        request.put("operationCode", operationCode);
        request.put("effectKind", "ALLOW");
        Map<String, Object> created = dataPermissionGateway.createAccessPolicy(request);
        return dataPermissionGateway.activateAccessPolicy(text(created.get("id")));
    }

    private Map<String, Object> ensurePolicyRevision(Map<String, Object> policy, Map<String, Object> blueprint,
                                                     String templateBinding, String argumentSchema,
                                                     String attributeReferences) {
        // 幂等：查找已有 ACTIVE 修订，如果 blueprintId 匹配则复用
        // 不用 contentDigest 或 JSON 字符串比较——MySQL JSON 列存储时按 key 字母排序，导致不稳定
        for (Map<String, Object> revision : dataPermissionGateway.searchPolicyRevisions(text(policy.get("id")))) {
            String state = text(revision.get("revisionState"));
            if (!"ACTIVE".equals(state) && !"DRAFT".equals(state)) {
                continue;
            }
            String revBlueprintId = text(revision.get("blueprintId"));
            if (text(blueprint.get("id")).equals(revBlueprintId)) {
                if ("DRAFT".equals(state)) {
                    return dataPermissionGateway.activatePolicyRevision(text(revision.get("id")));
                }
                return revision;
            }
        }
        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("blueprintId", text(blueprint.get("id")));
        request.put("templateBinding", templateBinding);
        request.put("argumentSchema", argumentSchema);
        request.put("attributeReferences", attributeReferences);
        request.put("changeSummary", "Compiled from Demo OA data permission template wizard.");
        Map<String, Object> created = dataPermissionGateway.createPolicyRevision(text(policy.get("id")), request);
        return dataPermissionGateway.activatePolicyRevision(text(created.get("id")));
    }

    private Map<String, Object> ensureAccessGrant(Map<String, Object> principal, Map<String, Object> policy, String template,
                                                  String principalType, String principalCode, Map<String, Object> sourceRequest) {
        String principalId = text(principal.get("id"));
        String policyId = text(policy.get("id"));
        Map<String, Object> existing = dataPermissionGateway.findActiveAccessGrant(principalId, policyId);
        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("validFrom", defaultText(text(sourceRequest == null ? null : sourceRequest.get("validFrom")), null));
        request.put("validUntil", defaultText(text(sourceRequest == null ? null : sourceRequest.get("validUntil")), null));
        request.put("grantSource", "AI_ASSISTED");
        request.put("reason", defaultText(text(sourceRequest == null ? null : sourceRequest.get("remark")), "Demo OA data permission template: " + template));
        if (existing != null) {
            return dataPermissionGateway.updateAccessGrant(text(existing.get("id")), request);
        }
        request.put("grantKey", grantKey(template, principalType, principalCode, text(policy.get("policyKey"))));
        request.put("principalId", principalId);
        request.put("policyId", policyId);
        return dataPermissionGateway.createAccessGrant(request);
    }

    private String templateBinding(String template, String accessPathKey, String anchorField) {
        if ("ALL_DATA".equals(template)) {
            // STANDARD_BLUEPRINT 路径：conditions[] 格式
            Map<String, Object> root = new LinkedHashMap<String, Object>();
            root.put("joiner", "AND");
            root.put("conditions", new ArrayList<Map<String, Object>>());
            return toJson(root);
        }
        // CUSTOM_BLUEPRINT 路径：arguments 格式（占位符替换值）
        Map<String, Object> arguments = new LinkedHashMap<String, Object>();
        arguments.put("anchorField", anchorField);
        if ("MY_DATA".equals(template)) {
            arguments.put("currentUserId", "currentUserId");
        } else if ("MANAGED_DEPARTMENTS".equals(template)) {
            arguments.put("managedDepartmentCodes", "managedDepartmentCodes");
        } else if ("SPECIFIED_DEPARTMENTS".equals(template)) {
            arguments.put("specifiedDepartmentCodes", "specifiedDepartmentCodes");
        } else if ("PERSON_SHARE".equals(template)) {
            arguments.put("sharedOwnerUserKey", "sharedOwnerUserKey");
        }
        Map<String, Object> binding = new LinkedHashMap<String, Object>();
        binding.put("arguments", arguments);
        // accessPath 场景：PAP 的 compileCustomBlueprint 会在占位符替换后用 EXISTS_PATH 包裹
        if (StringUtils.hasText(accessPathKey)) {
            binding.put("accessPathKey", accessPathKey);
        }
        return toJson(binding);
    }

    private String argumentSchema(String template) {
        Map<String, Object> schema = new LinkedHashMap<String, Object>();
        if ("ALL_DATA".equals(template)) {
            return toJson(schema);
        }
        if ("MANAGED_DEPARTMENTS".equals(template)) {
            schema.put("departmentScopes", schemaItem("STRING_SET", true, "部门范围"));
            schema.put("subDepartmentDepth", schemaItem("STRING", false, "下级部门层级"));
        }
        if ("SPECIFIED_DEPARTMENTS".equals(template)) {
            schema.put("specifiedDepartmentCodes", schemaItem("STRING_SET", true, "指定部门"));
        }
        if ("PERSON_SHARE".equals(template)) {
            schema.put("sharedOwnerUserKey", schemaItem("STRING", true, "数据归属人"));
            schema.put("allowReshare", schemaItem("BOOLEAN", false, "是否允许再次分享"));
        }
        return toJson(schema);
    }

    private String attributeReferences(String template) {
        List<Map<String, Object>> references = new ArrayList<Map<String, Object>>();
        if ("ALL_DATA".equals(template)) {
            return toJson(references);
        }
        if ("MY_DATA".equals(template)) {
            references.add(attributeReference("currentUserId", "STRING", "REQUESTER_USER_ID"));
        } else if ("MANAGED_DEPARTMENTS".equals(template)) {
            references.add(attributeReference("managedDepartmentCodes", "STRING_SET", "MANAGED_DEPARTMENT_CODES"));
        }
        return toJson(references);
    }

    private List<Map<String, Object>> grantArguments(String template, Map<String, Object> request) {
        List<Map<String, Object>> arguments = new ArrayList<Map<String, Object>>();
        if ("ALL_DATA".equals(template)) {
            return arguments;
        }
        if ("MANAGED_DEPARTMENTS".equals(template)) {
            List<String> scopes = stringList(request, "departmentScopes");
            arguments.add(grantArgument("departmentScopes", "STRING_SET", toJson(scopes), false));
            String depth = text(request == null ? null : request.get("subDepartmentDepth"));
            if (StringUtils.hasText(depth)) {
                arguments.add(grantArgument("subDepartmentDepth", "STRING", toJson(depth), false));
            }
            return arguments;
        }
        if ("SPECIFIED_DEPARTMENTS".equals(template)) {
            List<String> codes = stringList(request, "departmentCodes");
            arguments.add(grantArgument("specifiedDepartmentCodes", "STRING_SET", toJson(codes), false));
            return arguments;
        }
        if (!"PERSON_SHARE".equals(template)) {
            return arguments;
        }
        arguments.add(grantArgument("sharedOwnerUserKey", "STRING", toJson(requiredText(request, "fromUserKey")), false));
        arguments.add(grantArgument("allowReshare", "BOOLEAN", toJson(Boolean.TRUE.equals(request == null ? null : request.get("allowReshare"))), false));
        return arguments;
    }

    private Map<String, Object> simulateTemplate(String template, String resourceKey, String operationCode, String simulationRequesterKey) {
        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("requesterKind", "USER");
        request.put("requesterKey", simulationRequesterKey);
        request.put("operationCode", operationCode);
        List<Map<String, Object>> resources = new ArrayList<Map<String, Object>>();
        Map<String, Object> resource = new LinkedHashMap<String, Object>();
        resource.put("resourceKey", resourceKey);
        resources.add(resource);
        request.put("resources", resources);
        List<Map<String, Object>> expectations = new ArrayList<Map<String, Object>>();
        Map<String, Object> expectation = new LinkedHashMap<String, Object>();
        expectation.put("targetResourceKey", resourceKey);
        expectation.put("expectedPlanDecision", "ALL_DATA".equals(template) ? "ALLOW_ALL" : "FILTER");
        expectation.put("expectedFailureCode", "");
        expectations.add(expectation);
        request.put("expectations", expectations);
        return dataPermissionGateway.simulate(request);
    }

    private void validateSpecifiedDepartments(Map<String, Object> request) {
        List<String> codes = stringList(request, "departmentCodes");
        if (codes.isEmpty()) {
            throw new IllegalArgumentException("departmentCodes is required for SPECIFIED_DEPARTMENTS template");
        }
    }

    private void validateManagedDepartmentScope(Map<String, Object> request) {
        List<String> scopes = stringList(request, "departmentScopes");
        if (scopes.isEmpty()) {
            throw new IllegalArgumentException("departmentScopes is required for MANAGED_DEPARTMENTS template");
        }
        if (scopes.contains("ALL_MANAGED") && scopes.size() > 1) {
            throw new IllegalArgumentException("ALL_MANAGED cannot be combined with other departmentScopes");
        }
        for (String scope : scopes) {
            if (!"ALL_MANAGED".equals(scope) && !"DIRECT_MANAGED".equals(scope)
                    && !"SUB_MANAGED".equals(scope) && !"PORTION_MANAGED".equals(scope)) {
                throw new IllegalArgumentException("unsupported departmentScope: " + scope);
            }
        }
        String depth = text(request == null ? null : request.get("subDepartmentDepth"));
        if (scopes.contains("SUB_MANAGED")) {
            if (!StringUtils.hasText(depth)) {
                throw new IllegalArgumentException("subDepartmentDepth is required when departmentScopes contains SUB_MANAGED");
            }
            if (!"1".equals(depth) && !"2".equals(depth) && !"3".equals(depth) && !"ALL".equals(depth)) {
                throw new IllegalArgumentException("subDepartmentDepth must be 1, 2, 3, or ALL");
            }
        } else if (StringUtils.hasText(depth)) {
            throw new IllegalArgumentException("subDepartmentDepth is only allowed when departmentScopes contains SUB_MANAGED");
        }
    }

    private Map<String, Object> schemaItem(String valueKind, boolean required, String label) {
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("valueKind", valueKind);
        item.put("required", required);
        item.put("label", label);
        return item;
    }

    private Map<String, Object> attributeReference(String attributeKey, String valueKind, String resolveKind) {
        Map<String, Object> reference = new LinkedHashMap<String, Object>();
        reference.put("attributeKey", attributeKey);
        reference.put("valueKind", valueKind);
        reference.put("resolveKind", resolveKind);
        reference.put("required", true);
        return reference;
    }

    private Map<String, Object> grantArgument(String argumentKey, String valueKind, String argumentValue, boolean sensitiveFlag) {
        Map<String, Object> argument = new LinkedHashMap<String, Object>();
        argument.put("argumentKey", argumentKey);
        argument.put("valueKind", valueKind);
        argument.put("argumentValue", argumentValue);
        argument.put("sensitiveFlag", sensitiveFlag);
        return argument;
    }

    private String normalizePrincipalType(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!"ROLE".equals(normalized) && !"GROUP".equals(normalized) && !"USER".equals(normalized)) {
            throw new IllegalArgumentException("principalType must be ROLE, GROUP, or USER");
        }
        return normalized;
    }

    private String normalizeTemplate(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!"ALL_DATA".equals(normalized) && !"MY_DATA".equals(normalized) && !"MANAGED_DEPARTMENTS".equals(normalized)
                && !"SPECIFIED_DEPARTMENTS".equals(normalized) && !"PERSON_SHARE".equals(normalized)) {
            throw new IllegalArgumentException("ruleTemplate is unsupported: " + value);
        }
        return normalized;
    }

    private String normalizeOperation(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!"READ".equals(normalized) && !"UPDATE".equals(normalized) && !"DELETE".equals(normalized)) {
            throw new IllegalArgumentException("operationCode must be READ, UPDATE, or DELETE");
        }
        return normalized;
    }

    private String policyKey(String template, String resourceKey, String operationCode, String accessPathKey, String anchorField) {
        String pathPart = StringUtils.hasText(accessPathKey) ? "_VIA_" + safeKey(accessPathKey) : "";
        return boundedKey("DEMO_OA_DP_" + safeKey(template) + "_" + safeKey(resourceKey) + "_" + safeKey(operationCode) + pathPart + "_" + safeKey(anchorField));
    }

    private String grantKey(String template, String principalType, String principalCode, String policyKey) {
        return boundedKey("DEMO_OA_GRANT_" + safeKey(template) + "_" + safeKey(principalType) + "_" + safeKey(principalCode) + "_" + safeKey(policyKey));
    }

    private String boundedKey(String value) {
        String normalized = safeKey(value);
        if (normalized.length() <= 108) {
            return normalized;
        }
        return normalized.substring(0, 91) + "_" + digest(normalized).substring(0, 16);
    }

    private String safeKey(String value) {
        String text = defaultText(value, "UNKNOWN").trim().toUpperCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < text.length(); index++) {
            char current = text.charAt(index);
            if ((current >= 'A' && current <= 'Z') || (current >= '0' && current <= '9')) {
                builder.append(current);
            } else {
                builder.append('_');
            }
        }
        return builder.toString().replaceAll("_+", "_");
    }

    private String displayName(Map<String, Object> value, String fallback) {
        String displayName = text(value.get("displayName"));
        return StringUtils.hasText(displayName) ? displayName : fallback;
    }

    private String dataPermissionPolicyName(String template, Map<String, Object> resource, String anchorField) {
        return dataPermissionTemplateName(template) + " - " + displayName(resource, text(resource.get("resourceKey"))) + " - " + anchorField;
    }

    private String dataPermissionTemplateName(String template) {
        if ("ALL_DATA".equals(template)) {
            return "全部数据";
        }
        if ("MY_DATA".equals(template)) {
            return "我的数据";
        }
        if ("MANAGED_DEPARTMENTS".equals(template)) {
            return "我管理部门的数据";
        }
        if ("SPECIFIED_DEPARTMENTS".equals(template)) {
            return "指定部门数据";
        }
        return "分享某人的数据权限给某人";
    }

    private String dataPermissionSummary(String template, Map<String, Object> principal, Map<String, Object> resource,
                                         Map<String, Object> field, Map<String, Object> request) {
        String principalName = displayName(principal, text(principal.get("principalKey")));
        String resourceName = displayName(resource, text(resource.get("resourceKey")));
        if ("ALL_DATA".equals(template)) {
            return "允许" + principalName + "访问" + resourceName + "的全部数据";
        }
        String fieldName = field != null ? displayName(field, text(field.get("fieldKey"))) : "id";
        String accessPathKey = text(request == null ? null : request.get("accessPathKey"));
        String fieldScope = fieldName;
        if (StringUtils.hasText(accessPathKey)) {
            Map<String, Object> accessPath = requireAccessPathIfPresent(text(resource.get("id")), accessPathKey);
            Map<String, Object> relatedResource = requireResource(text(accessPath.get("destinationRelationId")), null);
            fieldScope = "通过" + displayName(relatedResource, text(relatedResource.get("resourceKey"))) + "关联的" + fieldName;
        }
        if ("MANAGED_DEPARTMENTS".equals(template)) {
            return "允许" + principalName + "访问" + resourceName + "中" + fieldScope + "属于本人" + managedDepartmentScopeSummary(request) + "的数据";
        }
        if ("SPECIFIED_DEPARTMENTS".equals(template)) {
            return "允许" + principalName + "访问" + resourceName + "中" + fieldScope + "属于指定部门（" + String.join("、", stringList(request, "departmentCodes")) + "）的数据";
        }
        if ("PERSON_SHARE".equals(template)) {
            return "将" + requiredText(request, "fromUserKey") + "负责的" + resourceName + "访问权限分享给" + principalName;
        }
        return "允许" + principalName + "访问" + resourceName + "中" + fieldScope + "是本人的数据";
    }

    private String managedDepartmentScopeSummary(Map<String, Object> request) {
        List<String> scopes = stringList(request, "departmentScopes");
        if (scopes.contains("ALL_MANAGED")) {
            return "全部管理部门";
        }
        List<String> labels = new ArrayList<String>();
        if (scopes.contains("DIRECT_MANAGED")) {
            labels.add("直接管理部门");
        }
        if (scopes.contains("SUB_MANAGED")) {
            String depth = text(request == null ? null : request.get("subDepartmentDepth"));
            labels.add("ALL".equals(depth) ? "全部下级部门" : depth + "级下级部门");
        }
        if (scopes.contains("PORTION_MANAGED")) {
            labels.add("分管部门");
        }
        return labels.isEmpty() ? "管理部门" : String.join("、", labels);
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private String digest(String value) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] hash = messageDigest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte current : hash) {
                builder.append(String.format("%02x", current));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to calculate data permission key digest", exception);
        }
    }

    private List<Map<String, Object>> fieldGrantMaps(List<RbacFieldGrant> grants) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (RbacFieldGrant grant : grants) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("code", grant.getCode());
            item.put("resourceKey", grant.getResourceKey());
            item.put("fieldKey", grant.getFieldKey());
            item.put("accessLevel", grant.getAccessLevel());
            item.put("name", grant.getName());
            result.add(item);
        }
        return result;
    }

    private Map<String, String[]> strongestGrantByField(CurrentUser currentUser, String resourceKey) {
        // grant 只提供 access_level（授权可变属性），security_requirement 从回路2取
        Map<String, String[]> grants = new LinkedHashMap<String, String[]>();
        for (RbacFieldGrant grant : currentUser.getFieldGrants()) {
            if (!resourceKey.equals(grant.getResourceKey())) {
                continue;
            }
            String accessLevel = grant.getAccessLevel() == null ? "" : grant.getAccessLevel().toUpperCase(Locale.ROOT);
            String[] previous = grants.get(grant.getFieldKey());
            if (previous == null || accessRank(accessLevel) > accessRank(previous[1])) {
                grants.put(grant.getFieldKey(), new String[]{null, accessLevel});
            }
        }
        return grants;
    }

    private int accessRank(String accessLevel) {
        if ("WRITE".equals(accessLevel)) { return 4; }
        if ("PLAIN_READ".equals(accessLevel)) { return 3; }
        if ("READ".equals(accessLevel)) { return 2; }
        if ("NONE".equals(accessLevel)) { return 1; }
        return 0;
    }

    private String requiredText(Map<String, Object> request, String key) {
        String value = text(request == null ? null : request.get(key));
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(key + " is required");
        }
        return value;
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private List<String> stringList(Map<String, Object> request, String key) {
        Object value = request == null ? null : request.get(key);
        if (!(value instanceof List)) {
            return Collections.emptyList();
        }
        Set<String> result = new LinkedHashSet<String>();
        for (Object item : (List<?>) value) {
            String text = text(item);
            if (StringUtils.hasText(text)) {
                result.add(text);
            }
        }
        return new ArrayList<String>(result);
    }

    private Map<String, Object> singletonId(Long id) {
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("id", id == null ? null : String.valueOf(id));
        return response;
    }

    private Map<String, Object> singletonId(String id) {
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("id", id);
        return response;
    }

    private Set<String> permissionCodesForRoles(Set<String> roleCodes) {
        Set<String> result = new LinkedHashSet<String>();
        for (String roleCode : roleCodes) {
            result.addAll(permissionCodesForRole(roleCode));
        }
        return result;
    }

    private Set<String> permissionCodesForRole(String roleCode) {
        return new LinkedHashSet<String>(roleAuthorizationStore.listRolePermissionCodesByRoleCode(roleCode));
    }

    private List<RbacFieldGrant> fieldGrantsForRoles(Set<String> roleCodes) {
        return roleAuthorizationStore.listFieldGrantsByRoleCodes(roleCodes);
    }

    private Set<String> configuredFieldKeys(String resourceKey) {
        // 从回路2查该资源的所有字段（已登记的字段都是受控字段）
        Map<String, String> securityReqs = fieldSecurityRequirementsFromCenter(resourceKey);
        return securityReqs.keySet();
    }

    private Map<String, String> configuredFieldSecurityRequirements(String resourceKey) {
        return fieldSecurityRequirementsFromCenter(resourceKey);
    }

    /** 从回路2 PAP 查字段的 security_requirement（字段固有安全属性，定义在 authzcraft_resource_field 表） */
    private final java.util.Map<String, Map<String, String>> fieldSecurityCache = new java.util.concurrent.ConcurrentHashMap<>();

    private Map<String, String> fieldSecurityRequirementsFromCenter(String resourceKey) {
        return fieldSecurityCache.computeIfAbsent(resourceKey, key -> {
            Map<String, String> result = new LinkedHashMap<String, String>();
            try {
                // 查回路2的资源字段
                for (Map<String, Object> resource : dataPermissionGateway.searchRelationResources()) {
                    if (key.equals(text(resource.get("resourceKey")))) {
                        String resourceId = text(resource.get("id"));
                        for (Map<String, Object> field : dataPermissionGateway.searchResourceFields(resourceId)) {
                            String fieldKey = text(field.get("fieldKey"));
                            String securityReq = text(field.get("securityRequirement"));
                            if (securityReq == null || securityReq.isEmpty()) {
                                securityReq = "PLAIN";
                            }
                            result.put(fieldKey, securityReq);
                        }
                        break;
                    }
                }
            } catch (Exception e) {
                // 回路2不可用时回退到回路1的 field_permissions 表
                Map<String, String> fallback = roleAuthorizationStore.listConfiguredFieldSecurityRequirements(key);
                if (fallback != null) {
                    result.putAll(fallback);
                }
            }
            return result;
        });
    }

    /** 根据 security_requirement 返回默认 access_level（无绑定时使用） */
    private String defaultAccessLevel(String securityRequirement) {
        if ("HIDDEN".equals(securityRequirement)) { return "NONE"; }
        if ("MASKED".equals(securityRequirement)) { return "READ"; }
        return "READ"; // PLAIN
    }

    private void validateKnownPermissions(List<String> codes) {
        if (roleAuthorizationStore.countPermissionsByCodes(codes) != codes.size()) {
            throw new IllegalArgumentException("permissionCodes contains unknown code");
        }
    }

    private void validateKnownFieldPermissions(List<String> codes) {
        if (roleAuthorizationStore.countFieldPermissionsByCodes(codes) != codes.size()) {
            throw new IllegalArgumentException("fieldPermissionCodes contains unknown code");
        }
    }

    private String jsonPayload(String key, String value) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("tenantKey", "platform");
        payload.put("appKey", "authzcraft-demo-oa");
        payload.put(key, value);
        return toJson(payload);
    }

    private String syncFailureReason(Map<String, Object> syncResult) {
        Object failedCount = syncResult == null ? null : syncResult.get("failedCount");
        if (failedCount instanceof Number && ((Number) failedCount).intValue() > 0) {
            return "AuthzCraft RBAC sync returned failedCount=" + failedCount;
        }
        if (failedCount != null) {
            try {
                if (Integer.parseInt(String.valueOf(failedCount)) > 0) {
                    return "AuthzCraft RBAC sync returned failedCount=" + failedCount;
                }
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    @Override
    @Transactional
    public Map<String, Object> createFieldPermission(Map<String, Object> request) {
        String resourceKey = requiredText(request, "resourceKey");
        String fieldKey = requiredText(request, "fieldKey");
        // security_requirement 从回路2 authzcraft_resource_field 表取（字段固有属性），不从请求参数取
        Map<String, String> fieldSecs = fieldSecurityRequirementsFromCenter(resourceKey);
        String securityRequirement = fieldSecs.getOrDefault(fieldKey, "PLAIN").toUpperCase(Locale.ROOT);
        String name = text(request.get("name"));
        if (name == null) { name = fieldKey; }
        String description = text(request.get("description"));

        // 根据 security_requirement 生成 access_level 选项（含 NONE，让"无权限"可显式绑定）
        java.util.List<String[]> options = new java.util.ArrayList<String[]>();
        options.add(new String[]{"NONE", "无权限"});
        if ("PLAIN".equals(securityRequirement)) {
            options.add(new String[]{"READ", "可读"});
            options.add(new String[]{"WRITE", "可写"});
        } else if ("MASKED".equals(securityRequirement)) {
            options.add(new String[]{"READ", "可读"});
            options.add(new String[]{"PLAIN_READ", "可明文读"});
            options.add(new String[]{"WRITE", "可写"});
        } else {
            options.add(new String[]{"READ", "可读"});
            options.add(new String[]{"WRITE", "可写"});
        }

        boolean exists = false;
        for (Map<String, Object> field : roleAuthorizationStore.listFieldPermissions()) {
            if (resourceKey.equals(text(field.get("resourceKey"))) && fieldKey.equals(text(field.get("fieldKey")))) {
                exists = true;
                break;
            }
        }

        if (!exists) {
            for (String[] option : options) {
                String accessLevel = option[0];
                String code = resourceKey + "." + fieldKey + ":" + accessLevel;
                roleAuthorizationStore.createFieldPermission(code, resourceKey, fieldKey, accessLevel, name, description);
            }
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("resourceKey", resourceKey);
        result.put("fieldKey", fieldKey);
        result.put("securityRequirement", securityRequirement);
        result.put("optionCount", options.size());
        return result;
    }

    @Override
    @Transactional
    public void deleteFieldPermission(String resourceKey, String fieldKey) {
        roleAuthorizationStore.deleteFieldPermission(resourceKey, fieldKey);
    }

    @Override
    @Transactional
    public Map<String, Object> updateFieldPermission(Map<String, Object> request) {
        String resourceKey = requiredText(request, "resourceKey");
        String fieldKey = requiredText(request, "fieldKey");
        String name = text(request.get("name"));
        if (name == null) { name = fieldKey; }
        // security_requirement 从回路2 authzcraft_resource_field 表取（字段固有属性，运行期不可修改）
        Map<String, String> fieldSecs = fieldSecurityRequirementsFromCenter(resourceKey);
        String securityRequirement = fieldSecs.getOrDefault(fieldKey, "PLAIN").toUpperCase(Locale.ROOT);

        // Delete old options then create new ones
        roleAuthorizationStore.deleteFieldPermission(resourceKey, fieldKey);

        java.util.List<String[]> options = new java.util.ArrayList<String[]>();
        options.add(new String[]{"NONE", "无权限"});
        if ("PLAIN".equals(securityRequirement)) {
            options.add(new String[]{"READ", "可读"});
            options.add(new String[]{"WRITE", "可写"});
        } else if ("MASKED".equals(securityRequirement)) {
            options.add(new String[]{"READ", "可读"});
            options.add(new String[]{"PLAIN_READ", "可明文读"});
            options.add(new String[]{"WRITE", "可写"});
        } else {
            options.add(new String[]{"READ", "可读"});
            options.add(new String[]{"WRITE", "可写"});
        }

        for (String[] option : options) {
            String accessLevel = option[0];
            String code = resourceKey + "." + fieldKey + ":" + accessLevel;
            roleAuthorizationStore.createFieldPermission(code, resourceKey, fieldKey, accessLevel, name, null);
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("resourceKey", resourceKey);
        result.put("fieldKey", fieldKey);
        result.put("securityRequirement", securityRequirement);
        result.put("optionCount", options.size());
        return result;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize RBAC sync payload", exception);
        }
    }
}