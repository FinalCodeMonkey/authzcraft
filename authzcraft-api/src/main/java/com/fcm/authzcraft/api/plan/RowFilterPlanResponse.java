package com.fcm.authzcraft.api.plan;

import java.util.ArrayList;
import java.util.List;

public class RowFilterPlanResponse {

    private String requestKey;
    private List<RowFilterPlan> plans = new ArrayList<RowFilterPlan>();

    public RowFilterPlanResponse() {
    }

    public RowFilterPlanResponse(String requestKey, List<RowFilterPlan> plans) {
        this.requestKey = requestKey;
        this.plans = plans;
    }

    public String getRequestKey() {
        return requestKey;
    }

    public void setRequestKey(String requestKey) {
        this.requestKey = requestKey;
    }

    public List<RowFilterPlan> getPlans() {
        return plans;
    }

    public void setPlans(List<RowFilterPlan> plans) {
        this.plans = plans;
    }
}