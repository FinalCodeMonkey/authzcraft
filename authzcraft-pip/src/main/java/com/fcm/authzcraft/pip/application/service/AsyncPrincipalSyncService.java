package com.fcm.authzcraft.pip.application.service;

import com.fcm.authzcraft.pip.application.command.PrincipalSyncCommand;

public interface AsyncPrincipalSyncService {

    String submit(PrincipalSyncCommand command);

    PrincipalSyncResult getResult(String taskId);
}
