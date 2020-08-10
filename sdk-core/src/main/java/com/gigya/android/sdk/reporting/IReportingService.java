package com.gigya.android.sdk.reporting;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Map;

public interface IReportingService {

    public void sendErrorReport(final @NonNull String message, @Nullable String sdkVersion, @Nullable Map<String, Object> details);
}
