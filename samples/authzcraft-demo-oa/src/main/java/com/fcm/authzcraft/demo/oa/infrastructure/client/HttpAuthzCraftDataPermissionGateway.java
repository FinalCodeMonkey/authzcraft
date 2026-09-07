package com.fcm.authzcraft.demo.oa.infrastructure.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fcm.authzcraft.demo.oa.domain.gateway.AuthzCraftDataPermissionGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class HttpAuthzCraftDataPermissionGateway implements AuthzCraftDataPermissionGateway {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    @Value("${authzcraft.pep.center-base-url:http://localhost:8088}")
    private String centerBaseUrl;

    @Value("${authzcraft.pep.tenant-key:platform}")
    private String tenantKey;

    @Value("${authzcraft.pep.app-key:authzcraft-demo-oa}")
    private String appKey;

    public HttpAuthzCraftDataPermissionGateway(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, Object> findPrincipal(String principalKind, String principalKey) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("tenantKey", tenantKey);
        body.put("principalKind", principalKind);
        body.put("principalKey", principalKey);
        body.put("lifecycleState", "ACTIVE");
        JsonNode data = postForData("/authzcraft/api/v1/principals/search", body);
        return firstMap(data);
    }

    @Override
    public List<Map<String, Object>> searchRelationResources() {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("tenantKey", tenantKey);
        body.put("appKey", appKey);
        return toList(postForData("/authzcraft/api/v1/catalog/relation-resources/search", body));
    }

    @Override
    public List<Map<String, Object>> searchResourceFields(String relationResourceId) {
        return toList(postForData("/authzcraft/api/v1/catalog/relation-resources/" + relationResourceId + "/fields/search",
                new LinkedHashMap<String, Object>()));
    }

    @Override
    public List<Map<String, Object>> searchAccessPaths(String rootRelationId) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("tenantKey", tenantKey);
        body.put("appKey", appKey);
        body.put("rootRelationId", rootRelationId);
        body.put("lifecycleState", "ACTIVE");
        return toList(postForData("/authzcraft/api/v1/catalog/access-paths/search", body));
    }

    @Override
    public Map<String, Object> findPublishedRuleBlueprint(String blueprintKey) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("tenantKey", tenantKey);
        body.put("blueprintKey", blueprintKey);
        body.put("lifecycleState", "PUBLISHED");
        return firstMap(postForData("/authzcraft/api/v1/policies/rule-blueprints/search", body));
    }

    @Override
    public Map<String, Object> findAnyRuleBlueprint(String blueprintKey) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("tenantKey", tenantKey);
        body.put("blueprintKey", blueprintKey);
        return firstMap(postForData("/authzcraft/api/v1/policies/rule-blueprints/search", body));
    }

    @Override
    public Map<String, Object> createRuleBlueprint(Map<String, Object> request) {
        request.put("tenantKey", tenantKey);
        return toMap(postForData("/authzcraft/api/v1/policies/rule-blueprints", request));
    }

    @Override
    public Map<String, Object> publishRuleBlueprint(String blueprintId) {
        return toMap(postForData("/authzcraft/api/v1/policies/rule-blueprints/" + blueprintId + "/publish",
                new LinkedHashMap<String, Object>()));
    }

    @Override
    public Map<String, Object> findAccessPolicy(String policyKey) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("tenantKey", tenantKey);
        body.put("appKey", appKey);
        body.put("policyKey", policyKey);
        return firstMap(postForData("/authzcraft/api/v1/policies/access-policies/search", body));
    }

    @Override
    public Map<String, Object> createAccessPolicy(Map<String, Object> request) {
        request.put("tenantKey", tenantKey);
        request.put("appKey", appKey);
        return toMap(postForData("/authzcraft/api/v1/policies/access-policies", request));
    }

    @Override
    public Map<String, Object> activateAccessPolicy(String policyId) {
        return toMap(postForData("/authzcraft/api/v1/policies/access-policies/" + policyId + "/activate",
            new LinkedHashMap<String, Object>()));
    }

    @Override
    public List<Map<String, Object>> searchPolicyRevisions(String policyId) {
        return toList(postForData("/authzcraft/api/v1/policies/access-policies/" + policyId + "/revisions/search",
            new LinkedHashMap<String, Object>()));
    }

    @Override
    public Map<String, Object> createPolicyRevision(String policyId, Map<String, Object> request) {
        return toMap(postForData("/authzcraft/api/v1/policies/access-policies/" + policyId + "/revisions", request));
    }

    @Override
    public Map<String, Object> activatePolicyRevision(String revisionId) {
        return toMap(postForData("/authzcraft/api/v1/policies/revisions/" + revisionId + "/activate",
            new LinkedHashMap<String, Object>()));
    }

    @Override
    public Map<String, Object> findActiveAccessGrant(String principalId, String policyId) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("tenantKey", tenantKey);
        body.put("appKey", appKey);
        body.put("principalId", principalId);
        body.put("policyId", policyId);
        body.put("lifecycleState", "ACTIVE");
        return firstMap(postForData("/authzcraft/api/v1/grants/access-grants/search", body));
    }

    @Override
    public Map<String, Object> createAccessGrant(Map<String, Object> request) {
        request.put("tenantKey", tenantKey);
        request.put("appKey", appKey);
        return toMap(postForData("/authzcraft/api/v1/grants/access-grants", request));
    }

    @Override
    public Map<String, Object> updateAccessGrant(String grantId, Map<String, Object> request) {
        return toMap(postForData("/authzcraft/api/v1/grants/access-grants/" + grantId + "/update", request));
    }

    @Override
    public List<Map<String, Object>> replaceGrantArguments(String grantId, List<Map<String, Object>> arguments) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("arguments", arguments);
        return toList(postForData("/authzcraft/api/v1/grants/access-grants/" + grantId + "/arguments/replace", body));
    }

    @Override
    public Map<String, Object> simulate(Map<String, Object> request) {
        request.put("tenantKey", tenantKey);
        request.put("appKey", appKey);
        return toMap(postForData("/authzcraft/api/v1/data-authz/simulations", request));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(JsonNode data) {
        return objectMapper.convertValue(data, Map.class);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> toList(JsonNode data) {
        return objectMapper.convertValue(data, List.class);
    }

    private Map<String, Object> firstMap(JsonNode data) {
        if (!data.isArray() || data.size() == 0) {
            return null;
        }
        return toMap(data.get(0));
    }

    private JsonNode postForData(String path, Object body) {
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(trimTrailingSlash(centerBaseUrl) + path, body, JsonNode.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("AuthzCraft data permission API failed: " + path);
        }
        JsonNode responseBody = response.getBody();
        if (responseBody == null) {
            throw new IllegalStateException("AuthzCraft data permission API failed: " + path);
        }
        if (!"0".equals(responseBody.path("code").asText())) {
            throw new IllegalStateException("AuthzCraft data permission API error: " + responseBody.path("message").asText());
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