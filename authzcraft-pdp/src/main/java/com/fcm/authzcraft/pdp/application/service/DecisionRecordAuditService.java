package com.fcm.authzcraft.pdp.application.service;

import org.springframework.stereotype.Service;

import com.fcm.authzcraft.pdp.application.query.DecisionRecordSearchQuery;
import com.fcm.authzcraft.pdp.domain.model.DecisionRecord;
import com.fcm.authzcraft.pdp.domain.repository.DecisionRecordQueryRepository;

import java.util.List;

@Service
public class DecisionRecordAuditService {
    private final DecisionRecordQueryRepository repository;

    public DecisionRecordAuditService(DecisionRecordQueryRepository repository) {
        this.repository = repository;
    }

    public List<DecisionRecord> search(DecisionRecordSearchQuery query) {
        return repository.search(query);
    }

    public DecisionRecord findLatestByDecisionKey(String decisionKey) {
        return repository.findLatestByDecisionKey(decisionKey);
    }
}