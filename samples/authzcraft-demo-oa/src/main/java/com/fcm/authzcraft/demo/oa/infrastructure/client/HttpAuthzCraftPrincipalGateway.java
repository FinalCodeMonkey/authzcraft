package com.fcm.authzcraft.demo.oa.infrastructure.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fcm.authzcraft.demo.oa.domain.gateway.AuthzCraftPrincipalGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class HttpAuthzCraftPrincipalGateway implements AuthzCraftPrincipalGateway {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    @Value("${authzcraft.pep.center-base-url:http://localhost:8088}")
    private String centerBaseUrl;

    @Value("${authzcraft.pep.tenant-key:platform}")
    private String tenantKey;

    @Value("${authzcraft.pep.app-key:authzcraft-demo-oa}")
    private String appKey;

    public HttpAuthzCraftPrincipalGateway(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, Object> findActiveUserProjection(String userKey) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("tenantKey", tenantKey);
        body.put("principalKey", userKey);
        body.put("lifecycleState", "ACTIVE");
        JsonNode data = postForData("/authzcraft/api/v1/principals/users/search", body);
        if (!data.isArray() || data.size() == 0) {
            return null;
        }
        return objectMapper.convertValue(data.get(0), Map.class);
    }

    @Override
    public List<Map<String, Object>> searchUsers() {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("tenantKey", tenantKey);
        body.put("lifecycleState", "ACTIVE");
        return objectMapper.convertValue(postForData("/authzcraft/api/v1/principals/users/search", body), List.class);
    }

    @Override
    public List<Map<String, Object>> searchRoles() {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("tenantKey", tenantKey);
        body.put("appKey", appKey);
        return objectMapper.convertValue(postForData("/authzcraft/api/v1/principals/roles/search", body), List.class);
    }

    @Override
    public List<Map<String, Object>> searchGroups() {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("tenantKey", tenantKey);
        body.put("appKey", appKey);
        return objectMapper.convertValue(postForData("/authzcraft/api/v1/principals/groups/search", body), List.class);
    }

    @Override
    public List<Map<String, Object>> searchMemberships(String containerKind, String containerCode, String userKey) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("tenantKey", tenantKey);
        body.put("appKey", appKey);
        if (StringUtils.hasText(containerKind)) { body.put("containerKind", containerKind); }
        if (StringUtils.hasText(containerCode)) { body.put("containerCode", containerCode); }
        if (StringUtils.hasText(userKey)) { body.put("userKey", userKey); }
        return objectMapper.convertValue(postForData("/authzcraft/api/v1/principals/memberships/search", body), List.class);
    }

    @Override
    public Map<String, Object> upsertRole(String roleCode, String roleName, String description, String lifecycleState) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("tenantKey", tenantKey);
        body.put("appKey", appKey);
        body.put("roleCode", roleCode);
        body.put("roleName", roleName);
        body.put("description", description);
        body.put("lifecycleState", lifecycleState);
        return objectMapper.convertValue(postForData("/authzcraft/api/v1/principals/roles", body), Map.class);
    }

    @Override
    public Map<String, Object> retireRole(String roleCode) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("tenantKey", tenantKey);
        body.put("appKey", appKey);
        return objectMapper.convertValue(postForData("/authzcraft/api/v1/principals/roles/" + roleCode + "/retire", body), Map.class);
    }

    @Override
    public Map<String, Object> upsertGroup(String groupCode, String groupName, String description, String lifecycleState) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("tenantKey", tenantKey);
        body.put("appKey", appKey);
        body.put("groupCode", groupCode);
        body.put("groupName", groupName);
        body.put("description", description);
        body.put("lifecycleState", lifecycleState);
        return objectMapper.convertValue(postForData("/authzcraft/api/v1/principals/groups", body), Map.class);
    }

    @Override
    public Map<String, Object> retireGroup(String groupCode) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("tenantKey", tenantKey);
        body.put("appKey", appKey);
        return objectMapper.convertValue(postForData("/authzcraft/api/v1/principals/groups/" + groupCode + "/retire", body), Map.class);
    }

    @Override
    public Map<String, Object> upsertMembership(String containerKind, String containerCode, String userKey) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("tenantKey", tenantKey);
        body.put("appKey", appKey);
        body.put("containerKind", containerKind);
        body.put("containerCode", containerCode);
        body.put("userKey", userKey);
        return objectMapper.convertValue(postForData("/authzcraft/api/v1/principals/memberships", body), Map.class);
    }

    @Override
    public void closeMembership(String containerKind, String containerCode, String userKey) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("tenantKey", tenantKey);
        body.put("appKey", appKey);
        body.put("containerKind", containerKind);
        body.put("containerCode", containerCode);
        body.put("userKey", userKey);
        postForData("/authzcraft/api/v1/principals/memberships/close", body);
    }

    @Override
    public Map<String, Object> syncApplicationRbac(List<Map<String, Object>> roles, List<Map<String, Object>> groups, List<Map<String, Object>> memberships) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("tenantKey", tenantKey);
        body.put("appKey", appKey);
        body.put("deactivateMissing", true);
        body.put("roles", roles);
        body.put("groups", groups);
        body.put("memberships", memberships);
        return objectMapper.convertValue(postForData("/authzcraft/api/v1/principals/sync/application-rbac", body), Map.class);
    }

    @Override
    public Map<String, Object> searchApplicationRbacStatus(List<Map<String, Object>> roles, List<Map<String, Object>> groups, List<Map<String, Object>> memberships) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("tenantKey", tenantKey);
        body.put("appKey", appKey);
        body.put("roles", roles);
        body.put("groups", groups);
        body.put("memberships", memberships);
        return objectMapper.convertValue(postForData("/authzcraft/api/v1/principals/sync/application-rbac/status/search", body), Map.class);
    }

    private JsonNode postForData(String path, Object body) {
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(trimTrailingSlash(centerBaseUrl) + path, body, JsonNode.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("AuthzCraft principal API failed: " + path);
        }
        JsonNode responseBody = response.getBody();
        if (!"0".equals(responseBody.path("code").asText())) {
            throw new IllegalStateException("AuthzCraft principal API error: " + responseBody.path("message").asText());
        }
        return responseBody.path("data");
    }

    private String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("authzcraft.pep.center-base-url is required");
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}