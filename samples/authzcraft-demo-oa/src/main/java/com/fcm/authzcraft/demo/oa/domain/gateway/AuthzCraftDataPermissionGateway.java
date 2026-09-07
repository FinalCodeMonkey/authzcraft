package com.fcm.authzcraft.demo.oa.domain.gateway;

import java.util.List;
import java.util.Map;

public interface AuthzCraftDataPermissionGateway {
    Map<String, Object> findPrincipal(String principalKind, String principalKey);
    List<Map<String, Object>> searchRelationResources();
    List<Map<String, Object>> searchResourceFields(String relationResourceId);
    List<Map<String, Object>> searchAccessPaths(String rootRelationId);
    Map<String, Object> findPublishedRuleBlueprint(String blueprintKey);
    Map<String, Object> findAnyRuleBlueprint(String blueprintKey);
    Map<String, Object> createRuleBlueprint(Map<String, Object> request);
    Map<String, Object> publishRuleBlueprint(String blueprintId);
    Map<String, Object> findAccessPolicy(String policyKey);
    Map<String, Object> createAccessPolicy(Map<String, Object> request);
    Map<String, Object> activateAccessPolicy(String policyId);
    List<Map<String, Object>> searchPolicyRevisions(String policyId);
    Map<String, Object> createPolicyRevision(String policyId, Map<String, Object> request);
    Map<String, Object> activatePolicyRevision(String revisionId);
    Map<String, Object> findActiveAccessGrant(String principalId, String policyId);
    Map<String, Object> createAccessGrant(Map<String, Object> request);
    Map<String, Object> updateAccessGrant(String grantId, Map<String, Object> request);
    List<Map<String, Object>> replaceGrantArguments(String grantId, List<Map<String, Object>> arguments);
    Map<String, Object> simulate(Map<String, Object> request);
}