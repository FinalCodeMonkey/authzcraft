package com.fcm.authzcraft.pip.domain.repository;

import java.time.LocalDateTime;
import java.util.List;

import com.fcm.authzcraft.pip.domain.model.PrincipalAttributeProjection;

public interface PrincipalAttributeRepository {

    PrincipalAttributeProjection findActiveUser(String tenantKey, String requesterKey);

    List<String> findManagedDepartmentCodes(String tenantKey, String requesterKey, List<String> departmentScopes,
                                            String subDepartmentDepth);

    List<String> findRoleKeys(String tenantKey, String appKey, Long requesterPrincipalId, LocalDateTime decisionTime);

    List<String> findGroupKeys(String tenantKey, String appKey, Long requesterPrincipalId, LocalDateTime decisionTime);
}