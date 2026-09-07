package com.fcm.authzcraft.pip.domain.repository;

import java.util.List;

import com.fcm.authzcraft.pip.domain.model.PrincipalOrganization;
import com.fcm.authzcraft.pip.domain.model.PrincipalPosition;
import com.fcm.authzcraft.pip.domain.model.PrincipalUser;
import com.fcm.authzcraft.pip.domain.service.PrincipalSyncProgressReporter;

public interface PrincipalProjectionRepository {

    void saveOrganizations(String tenantKey, List<PrincipalOrganization> organizations,
                           PrincipalSyncProgressReporter progressReporter);

    void savePositions(String tenantKey, List<PrincipalPosition> positions,
                       PrincipalSyncProgressReporter progressReporter);

    void saveUsers(String tenantKey, List<PrincipalUser> users,
                   PrincipalSyncProgressReporter progressReporter);

    void rebuildOrganizationClosure(String tenantKey, PrincipalSyncProgressReporter progressReporter);
}
