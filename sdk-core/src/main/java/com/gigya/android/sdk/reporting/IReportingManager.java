package com.gigya.android.sdk.reporting;


import java.util.Map;

public interface IReportingManager {
    void error(String version, String source, String cause);

    void info(String version, String source, Map<String, Object> data);
}
