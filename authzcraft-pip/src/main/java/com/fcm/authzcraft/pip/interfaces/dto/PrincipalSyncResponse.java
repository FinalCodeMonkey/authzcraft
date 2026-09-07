package com.fcm.authzcraft.pip.interfaces.dto;

public class PrincipalSyncResponse {

    private String taskId;
    private String state;
    private String currentStage;
    private int processedCount;
    private int totalCount;
    private String startedAt;
    private String finishedAt;
    private int organizationCount;
    private int positionCount;
    private int userCount;
    private String errorMessage;

    public PrincipalSyncResponse() {
    }

    public PrincipalSyncResponse(int organizationCount, int positionCount, int userCount) {
        this.organizationCount = organizationCount;
        this.positionCount = positionCount;
        this.userCount = userCount;
    }

    public PrincipalSyncResponse(String taskId, String state, int organizationCount, int positionCount,
                                 int userCount, String errorMessage) {
        this(taskId, state, null, 0, 0, null, null, organizationCount, positionCount, userCount, errorMessage);
    }

    public PrincipalSyncResponse(String taskId, String state, String currentStage, int processedCount, int totalCount,
                                 String startedAt, String finishedAt, int organizationCount, int positionCount,
                                 int userCount, String errorMessage) {
        this.taskId = taskId;
        this.state = state;
        this.currentStage = currentStage;
        this.processedCount = processedCount;
        this.totalCount = totalCount;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.organizationCount = organizationCount;
        this.positionCount = positionCount;
        this.userCount = userCount;
        this.errorMessage = errorMessage;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCurrentStage() {
        return currentStage;
    }

    public void setCurrentStage(String currentStage) {
        this.currentStage = currentStage;
    }

    public int getProcessedCount() {
        return processedCount;
    }

    public void setProcessedCount(int processedCount) {
        this.processedCount = processedCount;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public String getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(String startedAt) {
        this.startedAt = startedAt;
    }

    public String getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(String finishedAt) {
        this.finishedAt = finishedAt;
    }

    public int getOrganizationCount() {
        return organizationCount;
    }

    public void setOrganizationCount(int organizationCount) {
        this.organizationCount = organizationCount;
    }

    public int getPositionCount() {
        return positionCount;
    }

    public void setPositionCount(int positionCount) {
        this.positionCount = positionCount;
    }

    public int getUserCount() {
        return userCount;
    }

    public void setUserCount(int userCount) {
        this.userCount = userCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
