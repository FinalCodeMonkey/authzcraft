package com.fcm.authzcraft.pdp.domain.repository;

import java.util.List;

import com.fcm.authzcraft.pdp.application.query.DecisionRecordSearchQuery;
import com.fcm.authzcraft.pdp.domain.model.DecisionRecord;

public interface DecisionRecordQueryRepository {
    List<DecisionRecord> search(DecisionRecordSearchQuery query);

    DecisionRecord findLatestByDecisionKey(String decisionKey);
}