package com.fcm.authzcraft.pip.application.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import com.fcm.authzcraft.pip.application.command.PrincipalSyncCommand;
import com.fcm.authzcraft.pip.application.service.PrincipalSyncResult;
import com.fcm.authzcraft.pip.application.service.PrincipalSyncService;
import com.fcm.authzcraft.pip.domain.gateway.PrincipalSyncSource;
import com.fcm.authzcraft.pip.domain.model.PrincipalOrganization;
import com.fcm.authzcraft.pip.domain.model.PrincipalPosition;
import com.fcm.authzcraft.pip.domain.model.PrincipalSyncScope;
import com.fcm.authzcraft.pip.domain.model.PrincipalUser;
import com.fcm.authzcraft.pip.domain.repository.PrincipalProjectionRepository;
import com.fcm.authzcraft.pip.domain.service.PrincipalSyncProgressReporter;

import java.util.List;

@Service
public class PrincipalSyncServiceImpl implements PrincipalSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrincipalSyncServiceImpl.class);

    private final List<PrincipalSyncSource> syncSources;
    private final PrincipalProjectionRepository projectionRepository;
    private final TransactionTemplate transactionTemplate;

    public PrincipalSyncServiceImpl(List<PrincipalSyncSource> syncSources, PrincipalProjectionRepository projectionRepository,
                                    PlatformTransactionManager transactionManager) {
        this.syncSources = syncSources;
        this.projectionRepository = projectionRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public PrincipalSyncResult sync(PrincipalSyncCommand command) {
        return sync(command, PrincipalSyncProgressReporter.NOOP);
    }

    @Override
    public PrincipalSyncResult sync(PrincipalSyncCommand command, PrincipalSyncProgressReporter progressReporter) {
        validate(command);
        PrincipalSyncProgressReporter reporter = progressReporter == null
                ? PrincipalSyncProgressReporter.NOOP
                : progressReporter;
        PrincipalSyncSource syncSource = findSource(command.getSourceCode());
        PrincipalSyncScope scope = new PrincipalSyncScope(
                command.getTenantKey(),
                command.getAppKey(),
                firstText(command.getSourceTenantId(), command.getTenantKey()),
                command.getSourceAppCode());

        long startedAt = System.currentTimeMillis();
        LOGGER.info("Starting principal sync, source={}, tenantKey={}, appKey={}, sourceTenantId={}, sourceAppCode={}",
                syncSource.sourceCode(), command.getTenantKey(), command.getAppKey(),
                scope.getSourceTenantId(), scope.getSourceAppCode());

        long stageStartedAt = System.currentTimeMillis();
        reporter.startStage("LOAD_ORGANIZATIONS", 0);
        List<PrincipalOrganization> organizations = syncSource.loadOrganizations(scope);
        reporter.finishStage("LOAD_ORGANIZATIONS", organizations.size());
        LOGGER.info("Loaded organizations from source, count={}, costMs={}",
                organizations.size(), elapsed(stageStartedAt));
        stageStartedAt = System.currentTimeMillis();
        reporter.startStage("SAVE_ORGANIZATIONS", organizations.size());
        transactionTemplate.execute(status -> {
            projectionRepository.saveOrganizations(command.getTenantKey(), organizations, reporter);
            projectionRepository.rebuildOrganizationClosure(command.getTenantKey(), reporter);
            return null;
        });
        reporter.finishStage("SAVE_ORGANIZATIONS", organizations.size());
        LOGGER.info("Saved organizations and closure into authzcraft, count={}, costMs={}",
                organizations.size(), elapsed(stageStartedAt));

        stageStartedAt = System.currentTimeMillis();
        reporter.startStage("LOAD_POSITIONS", 0);
        List<PrincipalPosition> positions = syncSource.loadPositions(scope);
        reporter.finishStage("LOAD_POSITIONS", positions.size());
        LOGGER.info("Loaded positions from source, count={}, costMs={}",
                positions.size(), elapsed(stageStartedAt));
        stageStartedAt = System.currentTimeMillis();
        reporter.startStage("SAVE_POSITIONS", positions.size());
        transactionTemplate.execute(status -> {
            projectionRepository.savePositions(command.getTenantKey(), positions, reporter);
            return null;
        });
        reporter.finishStage("SAVE_POSITIONS", positions.size());
        LOGGER.info("Saved positions into authzcraft, count={}, costMs={}",
                positions.size(), elapsed(stageStartedAt));

        stageStartedAt = System.currentTimeMillis();
        reporter.startStage("LOAD_USERS", 0);
        List<PrincipalUser> users = syncSource.loadUsers(scope);
        reporter.finishStage("LOAD_USERS", users.size());
        LOGGER.info("Loaded users from source, count={}, costMs={}",
                users.size(), elapsed(stageStartedAt));
        stageStartedAt = System.currentTimeMillis();
        reporter.startStage("SAVE_USERS", users.size());
        transactionTemplate.execute(status -> {
            projectionRepository.saveUsers(command.getTenantKey(), users, reporter);
            return null;
        });
        reporter.finishStage("SAVE_USERS", users.size());
        LOGGER.info("Saved users into authzcraft, count={}, costMs={}",
                users.size(), elapsed(stageStartedAt));

        LOGGER.info("Finished principal sync, organizationCount={}, positionCount={}, userCount={}, totalCostMs={}",
                organizations.size(), positions.size(), users.size(), elapsed(startedAt));

        return new PrincipalSyncResult(organizations.size(), positions.size(), users.size());
    }

    private PrincipalSyncSource findSource(String sourceCode) {
        for (PrincipalSyncSource syncSource : syncSources) {
            if (syncSource.sourceCode().equals(sourceCode)) {
                return syncSource;
            }
        }
        throw new IllegalArgumentException("Unsupported principal sync source: " + sourceCode);
    }

    private void validate(PrincipalSyncCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Principal sync command must not be null");
        }
        if (!StringUtils.hasText(command.getTenantKey())) {
            throw new IllegalArgumentException("tenantKey must not be blank");
        }
        if (!StringUtils.hasText(command.getSourceCode())) {
            throw new IllegalArgumentException("sourceCode must not be blank");
        }
    }

    private String firstText(String first, String fallback) {
        return StringUtils.hasText(first) ? first : fallback;
    }

    private long elapsed(long startedAt) {
        return System.currentTimeMillis() - startedAt;
    }
}
