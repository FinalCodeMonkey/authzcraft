package com.fcm.authzcraft.pip.application.service;

import com.fcm.authzcraft.pip.application.command.PrincipalSyncCommand;
import com.fcm.authzcraft.pip.domain.service.PrincipalSyncProgressReporter;

public interface PrincipalSyncService {

    PrincipalSyncResult sync(PrincipalSyncCommand command);

    PrincipalSyncResult sync(PrincipalSyncCommand command, PrincipalSyncProgressReporter progressReporter);
}
