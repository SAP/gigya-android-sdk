package com.gigya.android.sdk.reporting;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

/**
 * Internal error reporting service used to notify critical/crashes errors.
 * <p>
 * Service is set to disabled by default and can be activated by demand.
 */
public class ReportingService implements IReportingService {

    private static final String LOG_TAG = "ReportingService";

    final Config config;
    final IRestAdapter restAdapter;

    public ReportingService(Config config, IRestAdapter restAdapter) {
        this.config = config;
        this.restAdapter = restAdapter;
    }

    boolean disabled = true;

    @Override
    public boolean isActive() {
        return !disabled;
    }

    /**
     * Turn Gigya error reporting on or off.
     *
     * @param active True for active.
     */
    @Override
    public void setErrorReporting(boolean active) {
        disabled = !active;
    }

    @Override
    public void sendErrorReport(final @NonNull String message, @Nullable String sdkVersion, @Nullable Map<String, Object> details, @Nullable final ISentReport sentCallback) {
        if (disabled) return;
        if (sdkVersion == null) {
            sdkVersion = "Android_" + Gigya.VERSION;
        }
        final TreeMap<String, Object> parameters = new TreeMap<>();
        parameters.put("message", message);
        parameters.put("apiKey", config.getApiKey());
        parameters.put("sdk", sdkVersion);
        if (details != null) {
            parameters.put("details", details);
        }

        final String url = "https://accounts." + config.getApiDomain() + "/sdk.errorReport";

        final GigyaApiRequest request = new GigyaApiRequest(RestAdapter.HttpMethod.POST, url, parameters);
        restAdapter.sendUnsigned(request, new IRestAdapterCallback() {
            @Override
            public void onResponse(String jsonResponse, String responseDateHeader) {
                GigyaLogger.debug(LOG_TAG, "sendErrorReport: success");
                if (sentCallback != null) {
                    sentCallback.done();
                }
            }

            @Override
            public void onError(GigyaError gigyaError) {
                GigyaLogger.debug(LOG_TAG, "sendErrorReport: success");
                if (sentCallback != null) {
                    sentCallback.done();
                }
                GigyaLogger.debug(LOG_TAG, "sendErrorReport: fail");
            }
        });
    }
}
