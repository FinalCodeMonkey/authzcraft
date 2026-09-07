package com.fcm.authzcraft.pep.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fcm.authzcraft.api.plan.PlanRequestContext;
import com.fcm.authzcraft.api.plan.PlanResource;
import com.fcm.authzcraft.api.plan.ResourceQueryRole;
import com.fcm.authzcraft.api.plan.RowFilterPlan;
import com.fcm.authzcraft.api.plan.RowFilterPlanRequest;
import com.fcm.authzcraft.api.plan.RowFilterPlanResponse;
import com.fcm.authzcraft.pep.autoconfigure.AuthzCraftPepProperties;

import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.UUID;

public class DefaultAuthzCraftPlanClient implements AuthzCraftPlanClient {

    private static final String REQUEST_KEY_HEADER = "X-AuthzCraft-Request-Key";

    private final AuthzCraftPepProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public DefaultAuthzCraftPlanClient(AuthzCraftPepProperties properties, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.properties = properties;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public RowFilterPlan plan(String mappedStatementId, String resourceKey, String operationCode, AuthzCraftRequester requester) {
        RowFilterPlanRequest request = new RowFilterPlanRequest();
        request.setTenantKey(properties.getTenantKey());
        request.setAppKey(properties.getAppKey());
        request.setRequesterKind(requester.getRequesterKind());
        request.setRequesterKey(requester.getRequesterKey());
        request.setOperationCode(operationCode);
        request.setResources(Collections.singletonList(new PlanResource(resourceKey, ResourceQueryRole.ROOT)));
        request.setContext(new PlanRequestContext(resolveRequestKey(), properties.getEnvironment()));

        String endpoint = trimTrailingSlash(properties.getCenterBaseUrl()) + "/authzcraft/api/v1/data-authz/plans";
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(endpoint, request, JsonNode.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new AuthzCraftPepException("PDP plan request failed for mappedStatementId=" + mappedStatementId);
        }
        JsonNode body = response.getBody();
        if (!"0".equals(body.path("code").asText())) {
            throw new AuthzCraftPepException("PDP plan response is not successful: " + body.path("message").asText());
        }
        RowFilterPlanResponse planResponse = objectMapper.convertValue(body.path("data"), RowFilterPlanResponse.class);
        if (planResponse.getPlans() == null || planResponse.getPlans().isEmpty()) {
            throw new AuthzCraftPepException("PDP plan response has no resource plan for mappedStatementId=" + mappedStatementId);
        }
        return planResponse.getPlans().get(0);
    }

    private String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            throw new AuthzCraftPepException("authzcraft.pep.center-base-url is required");
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String resolveRequestKey() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes) {
            HttpServletRequest request = ((ServletRequestAttributes) attributes).getRequest();
            Object requestKeyAttribute = request.getAttribute(REQUEST_KEY_HEADER);
            if (requestKeyAttribute instanceof String && StringUtils.hasText((String) requestKeyAttribute)) {
                return ((String) requestKeyAttribute).trim();
            }
            String requestKeyHeader = request.getHeader(REQUEST_KEY_HEADER);
            if (StringUtils.hasText(requestKeyHeader)) {
                return requestKeyHeader.trim();
            }
        }
        return "pep-" + UUID.randomUUID().toString();
    }
}