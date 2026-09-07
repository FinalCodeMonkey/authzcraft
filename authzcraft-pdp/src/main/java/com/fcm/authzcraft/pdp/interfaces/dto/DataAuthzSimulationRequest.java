package com.fcm.authzcraft.pdp.interfaces.dto;

import java.util.ArrayList;
import java.util.List;

import com.fcm.authzcraft.api.plan.RowFilterPlanRequest;

public class DataAuthzSimulationRequest extends RowFilterPlanRequest {
    private List<DataAuthzSimulationExpectationRequest> expectations = new ArrayList<DataAuthzSimulationExpectationRequest>();

    public List<DataAuthzSimulationExpectationRequest> getExpectations() {
        return expectations;
    }

    public void setExpectations(List<DataAuthzSimulationExpectationRequest> expectations) {
        this.expectations = expectations;
    }
}