package com.fcm.authzcraft.pdp.interfaces.dto;

public class DataAuthzSimulationAssertionResponse {
    private String assertionKey;
    private String targetResourceKey;
    private boolean passed;
    private String expected;
    private String actual;
    private String detail;

    public DataAuthzSimulationAssertionResponse() {
    }

    public DataAuthzSimulationAssertionResponse(String assertionKey, String targetResourceKey, boolean passed,
                                                String expected, String actual, String detail) {
        this.assertionKey = assertionKey;
        this.targetResourceKey = targetResourceKey;
        this.passed = passed;
        this.expected = expected;
        this.actual = actual;
        this.detail = detail;
    }

    public String getAssertionKey() {
        return assertionKey;
    }

    public void setAssertionKey(String assertionKey) {
        this.assertionKey = assertionKey;
    }

    public String getTargetResourceKey() {
        return targetResourceKey;
    }

    public void setTargetResourceKey(String targetResourceKey) {
        this.targetResourceKey = targetResourceKey;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public String getExpected() {
        return expected;
    }

    public void setExpected(String expected) {
        this.expected = expected;
    }

    public String getActual() {
        return actual;
    }

    public void setActual(String actual) {
        this.actual = actual;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }
}