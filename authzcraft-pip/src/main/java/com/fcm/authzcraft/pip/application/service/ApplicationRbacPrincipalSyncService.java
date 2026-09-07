package com.fcm.authzcraft.pip.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fcm.authzcraft.pip.application.command.ApplicationRbacPrincipalSyncCommand;
import com.fcm.authzcraft.pip.domain.repository.ApplicationRbacPrincipalSyncRepository;

@Service
public class ApplicationRbacPrincipalSyncService {
    private final ApplicationRbacPrincipalSyncRepository repository;

    public ApplicationRbacPrincipalSyncService(ApplicationRbacPrincipalSyncRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ApplicationRbacPrincipalSyncResult sync(ApplicationRbacPrincipalSyncCommand command) {
        if (command == null || !StringUtils.hasText(command.getTenantKey()) || !StringUtils.hasText(command.getAppKey())) {
            throw new IllegalArgumentException("tenantKey and appKey are required");
        }
        return repository.sync(command);
    }

    @Transactional(readOnly = true)
    public ApplicationRbacPrincipalSyncResult status(ApplicationRbacPrincipalSyncCommand command) {
        if (command == null || !StringUtils.hasText(command.getTenantKey()) || !StringUtils.hasText(command.getAppKey())) {
            throw new IllegalArgumentException("tenantKey and appKey are required");
        }
        return repository.status(command);
    }
}