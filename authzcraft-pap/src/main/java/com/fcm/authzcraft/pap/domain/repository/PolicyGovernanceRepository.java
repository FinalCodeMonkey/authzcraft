package com.fcm.authzcraft.pap.domain.repository;

import java.util.List;

import com.fcm.authzcraft.pap.domain.model.AccessGrant;
import com.fcm.authzcraft.pap.domain.model.AccessPolicy;
import com.fcm.authzcraft.pap.domain.model.GrantArgument;
import com.fcm.authzcraft.pap.domain.model.PolicyRevision;
import com.fcm.authzcraft.pap.domain.model.Principal;
import com.fcm.authzcraft.pap.domain.model.PrincipalOrganizationProjection;
import com.fcm.authzcraft.pap.domain.model.PrincipalPositionProjection;
import com.fcm.authzcraft.pap.domain.model.PrincipalUserProjection;
import com.fcm.authzcraft.pap.domain.model.RuleBlueprint;

public interface PolicyGovernanceRepository {
    void saveRuleBlueprint(RuleBlueprint blueprint);
    RuleBlueprint findRuleBlueprint(Long id);
    List<RuleBlueprint> searchRuleBlueprints(String tenantKey, String blueprintKey, String lifecycleState);
    void retirePublishedRuleBlueprints(String tenantKey, String blueprintKey);
    void publishRuleBlueprint(Long id, String actor);
    void updateRuleBlueprint(RuleBlueprint blueprint);
    void retireRuleBlueprint(Long id, String actor);

    void saveAccessPolicy(AccessPolicy policy);
    AccessPolicy findAccessPolicy(Long id);
    List<AccessPolicy> searchAccessPolicies(String tenantKey, String appKey, String policyKey, Long targetRelationId,
                                            String operationCode, String lifecycleState);
    void updateAccessPolicy(AccessPolicy policy);

    void savePolicyRevision(PolicyRevision revision);
    PolicyRevision findPolicyRevision(Long id);
    List<PolicyRevision> searchPolicyRevisions(Long policyId, String revisionState);
    int nextRevisionNo(Long policyId);
    void retireActivePolicyRevisions(Long policyId);
    void activatePolicyRevision(Long id, String actor);
    void retirePolicyRevision(Long id, String actor);

    void saveAccessGrant(AccessGrant grant);
    AccessGrant findAccessGrant(Long id);
    List<AccessGrant> searchAccessGrants(String tenantKey, String appKey, Long principalId, Long policyId,
                                         String lifecycleState);
    void updateAccessGrant(AccessGrant grant);
    boolean principalExists(String tenantKey, Long principalId);
    List<Principal> searchPrincipals(String tenantKey, String principalKind, String principalKey, String keyword,
                                     String lifecycleState);
    List<PrincipalUserProjection> searchPrincipalUsers(String tenantKey, String userId, String staffNo,
                                                       String departmentCode, String postCode, String keyword,
                                                       String lifecycleState, int limit, int offset);
    List<PrincipalOrganizationProjection> searchPrincipalOrganizations(String tenantKey, String departmentCode,
                                                                       String parentDepartmentCode, String keyword,
                                                                       String lifecycleState, int limit, int offset);
    List<PrincipalPositionProjection> searchPrincipalPositions(String tenantKey, String postCode, String departmentCode,
                                                               String keyword, String lifecycleState, int limit, int offset);
    void replaceGrantArguments(Long accessGrantId, List<GrantArgument> arguments);
    List<GrantArgument> findGrantArguments(Long accessGrantId);
}