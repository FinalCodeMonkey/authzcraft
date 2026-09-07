package com.fcm.authzcraft.pip.domain.service;

public interface PrincipalSyncProgressReporter {

    PrincipalSyncProgressReporter NOOP = new PrincipalSyncProgressReporter() {
        @Override
        public void startStage(String currentStage, int totalCount) {
        }

        @Override
        public void updateStage(String currentStage, int processedCount, int totalCount) {
        }

        @Override
        public void finishStage(String currentStage, int totalCount) {
        }
    };

    void startStage(String currentStage, int totalCount);

    void updateStage(String currentStage, int processedCount, int totalCount);

    void finishStage(String currentStage, int totalCount);
}