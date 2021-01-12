package com.gigya.android.sdk.reporting;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

public interface IReportingManager {

    void runtimeException(String version, String source,
                          String cause,
                          @Nullable Map<String, Object> details,
                          @NonNull ISentReport sentCallback);

    void alert(String version, String source, String cause);
}
