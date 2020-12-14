package com.gigya.android.sdk.reporting;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.api.GigyaApiRequest;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.adapter.IRestAdapter;
import com.gigya.android.sdk.network.adapter.IRestAdapterCallback;
import com.gigya.android.sdk.network.adapter.RestAdapter;

import java.util.Map;
import java.util.TreeMap;

public class ReportingService implements IReportingService {

    private static final String LOG_TAG = "ReportingService";

    final Config config;
    final IRestAdapter restAdapter;

    public ReportingService(Config config, IRestAdapter restAdapter) {
        this.config = config;
        this.restAdapter = restAdapter;
    }

    @Override
    public void sendErrorReport(final @NonNull String message, @Nullable String sdkVersion, @Nullable Map<String, Object> details) {
        if (sdkVersion == null) {
            sdkVersion = "core_" + Gigya.VERSION;
        }
        final TreeMap<String, Object> parameters = new TreeMap<>();
        parameters.put("message", message);
        parameters.put("apiKey", config.getApiKey());
        parameters.put("sdk", sdkVersion);
        if (details != null) {
            parameters.put("details", details);
        }

        final String url = "https://accounts." + config.getApiDomain() + "/sdk.errorReport";

        GigyaApiRequest request = new GigyaApiRequest(RestAdapter.HttpMethod.POST, url, parameters);
        restAdapter.sendUnsigned(request, new IRestAdapterCallback() {
            @Override
            public void onResponse(String jsonResponse, String responseDateHeader) {
                GigyaLogger.debug(LOG_TAG, "sendErrorReport: success");
            }

            @Override
            public void onError(GigyaError gigyaError) {
                GigyaLogger.debug(LOG_TAG, "sendErrorReport: fail");
            }
        });
    }
}
