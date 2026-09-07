package com.fcm.authzcraft.pip.application.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fcm.authzcraft.pip.application.command.PrincipalSyncCommand;
import com.fcm.authzcraft.pip.application.service.AsyncPrincipalSyncService;
import com.fcm.authzcraft.pip.application.service.PrincipalSyncResult;
import com.fcm.authzcraft.pip.application.service.PrincipalSyncService;
import com.fcm.authzcraft.pip.domain.service.PrincipalSyncProgressReporter;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class AsyncPrincipalSyncServiceImpl implements AsyncPrincipalSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncPrincipalSyncServiceImpl.class);

    private final PrincipalSyncService principalSyncService;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final ConcurrentHashMap<String, TaskEntry> taskEntries = new ConcurrentHashMap<String, TaskEntry>();

    public AsyncPrincipalSyncServiceImpl(PrincipalSyncService principalSyncService) {
        this.principalSyncService = principalSyncService;
    }

    @Override
    public String submit(PrincipalSyncCommand command) {
        String taskId = UUID.randomUUID().toString();
        TaskEntry entry = new TaskEntry(taskId, "PENDING", null);
        taskEntries.put(taskId, entry);
        Future<?> future = executorService.submit(() -> run(taskId, command));
        entry.future = future;
        return taskId;
    }

    @Override
    public PrincipalSyncResult getResult(String taskId) {
        TaskEntry entry = taskEntries.get(taskId);
        if (entry == null) {
            return new PrincipalSyncResult();
        }
        return new PrincipalSyncResult(
                entry.organizationCount,
                entry.positionCount,
                entry.userCount,
                taskId,
                entry.state,
            entry.currentStage,
            entry.processedCount,
            entry.totalCount,
            format(entry.startedAt),
            format(entry.finishedAt),
                entry.errorMessage);
    }

    private void run(String taskId, PrincipalSyncCommand command) {
        TaskEntry entry = taskEntries.get(taskId);
        if (entry == null) {
            return;
        }
        entry.state = "RUNNING";
        entry.startedAt = Instant.now();
        try {
            PrincipalSyncResult result = principalSyncService.sync(command, new TaskProgressReporter(entry));
            entry.organizationCount = result.getOrganizationCount();
            entry.positionCount = result.getPositionCount();
            entry.userCount = result.getUserCount();
            entry.currentStage = "DONE";
            entry.processedCount = 0;
            entry.totalCount = 0;
            entry.state = "DONE";
            entry.finishedAt = Instant.now();
            LOGGER.info("Async principal sync task finished, taskId={}, organizationCount={}, positionCount={}, userCount={}",
                    taskId, entry.organizationCount, entry.positionCount, entry.userCount);
        } catch (Exception exception) {
            entry.state = "FAILED";
            entry.finishedAt = Instant.now();
            entry.errorMessage = exception.getMessage();
            LOGGER.error("Async principal sync task failed, taskId={}", taskId, exception);
        }
    }

    private String format(Instant instant) {
        return instant == null ? null : instant.toString();
    }

    private static class TaskEntry {
        private final String taskId;
        private volatile String state;
        private volatile String currentStage;
        private volatile int processedCount;
        private volatile int totalCount;
        private volatile Instant startedAt;
        private volatile Instant finishedAt;
        private volatile String errorMessage;
        private volatile int organizationCount;
        private volatile int positionCount;
        private volatile int userCount;
        private volatile Future<?> future;

        private TaskEntry(String taskId, String state, String errorMessage) {
            this.taskId = taskId;
            this.state = state;
            this.errorMessage = errorMessage;
        }
    }

    private static class TaskProgressReporter implements PrincipalSyncProgressReporter {

        private final TaskEntry entry;

        private TaskProgressReporter(TaskEntry entry) {
            this.entry = entry;
        }

        @Override
        public void startStage(String currentStage, int totalCount) {
            updateStage(currentStage, 0, totalCount);
        }

        @Override
        public void updateStage(String currentStage, int processedCount, int totalCount) {
            entry.currentStage = currentStage;
            entry.processedCount = processedCount;
            entry.totalCount = totalCount;
        }

        @Override
        public void finishStage(String currentStage, int totalCount) {
            updateStage(currentStage, totalCount, totalCount);
        }
    }
}
