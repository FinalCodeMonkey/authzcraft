package com.fcm.authzcraft.pip.domain.gateway;

import java.util.List;

import com.fcm.authzcraft.pip.domain.model.PrincipalOrganization;
import com.fcm.authzcraft.pip.domain.model.PrincipalPosition;
import com.fcm.authzcraft.pip.domain.model.PrincipalSyncScope;
import com.fcm.authzcraft.pip.domain.model.PrincipalUser;

public interface PrincipalSyncSource {

    String sourceCode();

    List<PrincipalOrganization> loadOrganizations(PrincipalSyncScope scope);

    List<PrincipalPosition> loadPositions(PrincipalSyncScope scope);

    List<PrincipalUser> loadUsers(PrincipalSyncScope scope);
}
