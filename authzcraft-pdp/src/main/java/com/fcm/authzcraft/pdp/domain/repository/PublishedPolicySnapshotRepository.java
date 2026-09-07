package com.fcm.authzcraft.pdp.domain.repository;

import java.time.LocalDateTime;
import java.util.List;

import com.fcm.authzcraft.pdp.domain.model.DecisionRecord;
import com.fcm.authzcraft.pdp.domain.model.EffectiveGrantSnapshot;
import com.fcm.authzcraft.pdp.domain.model.GrantArgumentSnapshot;
import com.fcm.authzcraft.pdp.domain.model.PublishedPolicySnapshot;
import com.fcm.authzcraft.pdp.domain.model.RelationResourceSnapshot;

public interface PublishedPolicySnapshotRepository {
    RelationResourceSnapshot findActiveResource(String tenantKey, String appKey, String resourceKey);

    List<PublishedPolicySnapshot> findPublishedPolicies(String tenantKey, String appKey, String resourceKey, String operationCode);

    Long findActivePrincipalId(String tenantKey, String principalKind, String principalKey);

    List<Long> findActivePrincipalIds(String tenantKey, String principalKind, List<String> principalKeys);

    List<EffectiveGrantSnapshot> findEffectiveGrants(String tenantKey, String appKey, List<Long> principalIds,
                                                     List<Long> policyIds, LocalDateTime decisionTime);

    List<GrantArgumentSnapshot> findGrantArguments(List<Long> grantIds);

    void saveDecisionRecord(DecisionRecord record);
}