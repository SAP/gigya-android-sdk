package com.gigya.android.sdk.reporting;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

public interface IReportingService {

    boolean isActive();

    void setErrorReporting(boolean active);

    void sendErrorReport(final @NonNull String message,
                         @Nullable String sdkVersion,
                         @Nullable Map<String, Object> details);
}
