package com.gigya.android.sdk.reporting;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gigya.android.sdk.Gigya;

import java.util.HashMap;
import java.util.Map;

public class ReportingManager implements IReportingManager {

    private static final String PRIORITY_ERROR = "ERROR";
    private static final String PRIORITY_INFO = "INFO";

    public static IReportingManager get() {
        try {
            return Gigya.getContainer().get(IReportingManager.class);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new ReportingManager(null);
    }

    final private IReportingService service;

    public ReportingManager(IReportingService service) {
        this.service = service;
    }

    /**
     * Check if service is available.
     */
    private boolean serviceAvailable() {
        if (service == null) {
            return false;
        }
        return service.isActive();
    }

    @Override
    public void error(@Nullable String version, @NonNull String source, String cause) {
        if (!serviceAvailable()) {
            return;
        }
        Map<String, Object> details = new HashMap<>();
        details.put("source", source);
        details.put("priority", PRIORITY_ERROR);
        this.service.sendErrorReport(cause, version, details);
    }

    @Override
    public void info(String version, String source, Map<String, Object> data) {
        if (!serviceAvailable()) {
            return;
        }
        Map<String, Object> details = new HashMap<>();
        details.put("source", source);
        details.put("priority", PRIORITY_INFO);
        this.service.sendErrorReport("info reporting", version, details);
    }
}
