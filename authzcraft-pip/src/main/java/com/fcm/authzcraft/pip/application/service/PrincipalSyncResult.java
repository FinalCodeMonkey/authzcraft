package com.fcm.authzcraft.pip.application.service;

public class PrincipalSyncResult {

    private final int organizationCount;
    private final int positionCount;
    private final int userCount;

    private final String taskId;
    private final String state;
    private final String currentStage;
    private final int processedCount;
    private final int totalCount;
    private final String startedAt;
    private final String finishedAt;
    private final String errorMessage;

    public PrincipalSyncResult() {
        this(0, 0, 0, null, "NOT_FOUND", null, 0, 0, null, null, null);
    }

    public PrincipalSyncResult(int organizationCount, int positionCount, int userCount) {
        this(organizationCount, positionCount, userCount, null, "DONE", "DONE", 0, 0, null, null, null);
    }

    public PrincipalSyncResult(int organizationCount, int positionCount, int userCount,
                               String taskId, String state, String errorMessage) {
        this(organizationCount, positionCount, userCount, taskId, state, null, 0, 0, null, null, errorMessage);
    }

    public PrincipalSyncResult(int organizationCount, int positionCount, int userCount,
                               String taskId, String state, String currentStage, int processedCount, int totalCount,
                               String startedAt, String finishedAt, String errorMessage) {
        this.organizationCount = organizationCount;
        this.positionCount = positionCount;
        this.userCount = userCount;
        this.taskId = taskId;
        this.state = state;
        this.currentStage = currentStage;
        this.processedCount = processedCount;
        this.totalCount = totalCount;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.errorMessage = errorMessage;
    }

    public int getOrganizationCount() {
        return organizationCount;
    }

    public int getPositionCount() {
        return positionCount;
    }

    public int getUserCount() {
        return userCount;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getState() {
        return state;
    }

    public String getCurrentStage() {
        return currentStage;
    }

    public int getProcessedCount() {
        return processedCount;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public String getStartedAt() {
        return startedAt;
    }

    public String getFinishedAt() {
        return finishedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
