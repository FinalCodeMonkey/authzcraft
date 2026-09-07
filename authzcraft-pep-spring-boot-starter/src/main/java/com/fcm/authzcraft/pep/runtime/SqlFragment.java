package com.fcm.authzcraft.pep.runtime;

import java.util.ArrayList;
import java.util.List;

class SqlFragment {
    private final String sql;
    private final List<Object> parameters;

    SqlFragment(String sql, List<Object> parameters) {
        this.sql = sql;
        this.parameters = parameters == null ? new ArrayList<Object>() : parameters;
    }

    String getSql() {
        return sql;
    }

    List<Object> getParameters() {
        return parameters;
    }
}