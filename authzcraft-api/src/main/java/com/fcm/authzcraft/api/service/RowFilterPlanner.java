package com.fcm.authzcraft.api.service;

import com.fcm.authzcraft.api.plan.RowFilterPlanRequest;
import com.fcm.authzcraft.api.plan.RowFilterPlanResponse;

public interface RowFilterPlanner {

    RowFilterPlanResponse plan(RowFilterPlanRequest request);
}