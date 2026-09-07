package com.fcm.authzcraft.pep.runtime;

import com.fcm.authzcraft.api.plan.RowFilterPlan;

public interface AuthzCraftPlanClient {
    RowFilterPlan plan(String mappedStatementId,
                       String resourceKey,
                       String operationCode,
                       AuthzCraftRequester requester);
}