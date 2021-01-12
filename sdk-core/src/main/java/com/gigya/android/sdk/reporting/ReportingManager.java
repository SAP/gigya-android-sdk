package com.gigya.android.sdk.reporting;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gigya.android.sdk.Gigya;

import java.util.HashMap;
import java.util.Map;

public class ReportingManager implements IReportingManager {

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

    private void addSource(Map<String, Object> map, String source) {
        map.put("source", source);
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
    public void runtimeException(@Nullable String version,
                                 @NonNull String source, String cause,
                                 @Nullable Map<String, Object> details,
                                 @NonNull ISentReport sentCallback) {
        if (!serviceAvailable()) {
            sentCallback.done();
            return;
        }
        if (details == null) {
            details = new HashMap<>();
        }
        addSource(details, source);
        this.service.sendErrorReport(cause, version, details, sentCallback);
    }

    @Override
    public void alert(@Nullable String version, @NonNull String source, String cause) {
        if (!serviceAvailable()) {
            return;
        }
        Map<String, Object> details = new HashMap<>();
        addSource(details, source);
        this.service.sendErrorReport(cause, version, details, null);
    }
}
