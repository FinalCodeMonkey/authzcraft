package com.fcm.authzcraft.pip.domain.repository;

import com.fcm.authzcraft.pip.application.command.ApplicationRbacPrincipalSyncCommand;
import com.fcm.authzcraft.pip.application.service.ApplicationRbacPrincipalSyncResult;

public interface ApplicationRbacPrincipalSyncRepository {
    ApplicationRbacPrincipalSyncResult sync(ApplicationRbacPrincipalSyncCommand command);
    ApplicationRbacPrincipalSyncResult status(ApplicationRbacPrincipalSyncCommand command);
}