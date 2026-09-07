package com.fcm.authzcraft.pep.runtime;

import java.util.ArrayList;
import java.util.List;

public class CompiledSqlFilter {
    private final String sql;
    private final List<Object> parameters;
    private final int parameterMappingIndex;

    public CompiledSqlFilter(String sql, List<Object> parameters, int parameterMappingIndex) {
        this.sql = sql;
        this.parameters = parameters == null ? new ArrayList<Object>() : parameters;
        this.parameterMappingIndex = parameterMappingIndex;
    }

    public String getSql() {
        return sql;
    }

    public List<Object> getParameters() {
        return parameters;
    }

    public int getParameterMappingIndex() {
        return parameterMappingIndex;
    }
}