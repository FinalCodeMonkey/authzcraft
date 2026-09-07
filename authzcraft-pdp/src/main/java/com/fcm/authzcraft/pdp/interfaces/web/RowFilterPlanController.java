package com.fcm.authzcraft.pdp.interfaces.web;

import com.fcm.authzcraft.api.plan.RowFilterPlanRequest;
import com.fcm.authzcraft.api.plan.RowFilterPlanResponse;
import com.fcm.authzcraft.api.service.RowFilterPlanner;
import com.fcm.authzcraft.common.web.ApiResponse;
import com.fcm.authzcraft.pdp.application.service.NativeRowFilterPlanner;
import com.fcm.authzcraft.pdp.interfaces.dto.DataAuthzSimulationRequest;
import com.fcm.authzcraft.pdp.interfaces.dto.DataAuthzSimulationResponse;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/authzcraft/api/v1/data-authz")
public class RowFilterPlanController {
    private final RowFilterPlanner rowFilterPlanner;
    private final NativeRowFilterPlanner nativeRowFilterPlanner;

    public RowFilterPlanController(RowFilterPlanner rowFilterPlanner, NativeRowFilterPlanner nativeRowFilterPlanner) {
        this.rowFilterPlanner = rowFilterPlanner;
        this.nativeRowFilterPlanner = nativeRowFilterPlanner;
    }

    @PostMapping("/plans")
    public ApiResponse<RowFilterPlanResponse> plan(@RequestBody RowFilterPlanRequest request) {
        return ApiResponse.success(rowFilterPlanner.plan(request));
    }

    @PostMapping("/simulations")
    public ApiResponse<DataAuthzSimulationResponse> simulate(@RequestBody DataAuthzSimulationRequest request) {
        return ApiResponse.success(DataAuthzSimulationResponse.from(request, nativeRowFilterPlanner.simulate(request)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException exception) {
        return ApiResponse.failure("DATA_AUTHZ_INVALID_REQUEST", exception.getMessage());
    }
}