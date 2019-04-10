package com.gigya.android.sdk.network;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.network.adapter.RestAdapter;
import com.gigya.android.sdk.session.ISessionService;
import com.gigya.android.sdk.utils.AuthUtils;
import com.gigya.android.sdk.utils.UrlUtils;

import java.util.Map;
import java.util.TreeMap;

public class GigyaApiRequest {

    @NonNull
    private String url, api;
    @Nullable
    private String encodedParams;
    private int method;

    public GigyaApiRequest(@NonNull String url, @Nullable String encodedParams, int method, @NonNull String api) {
        this.url = url;
        this.encodedParams = encodedParams;
        this.method = method;
        this.api = api;
    }

    @NonNull
    public String getUrl() {
        return url;
    }

    @Nullable
    public String getEncodedParams() {
        return encodedParams;
    }

    public int getMethod() {
        return method;
    }

    @NonNull
    public String getTag() {
        return api;
    }

    @NonNull
    public String getApi() {
        return api;
    }

    //region NEW INSTANCE

    public static GigyaApiRequest newInstance(Config config, ISessionService sessionService, String api, Map<String, Object> params, int requestMethod) {
        TreeMap<String, Object> urlParams = new TreeMap<>();
        if (params != null) {
            urlParams.putAll(params);
        }

        // Add general parameters.
        urlParams.put("sdk", Gigya.VERSION);
        urlParams.put("targetEnv", "mobile");
        urlParams.put("httpStatusCodes", false);
        urlParams.put("format", "json");

        // Add configuration parameters
        final String gmid = config.getGmid();
        if (gmid != null) {
            urlParams.put("gmid", gmid);
        }
        final String ucid = config.getUcid();
        if (ucid != null) {
            urlParams.put("ucid", ucid);
        }
        // Add authentication parameters.
        if (sessionService.isValid()) {
            @SuppressWarnings("ConstantConditions") final String sessionToken = sessionService.getSession().getSessionToken();
            urlParams.put("oauth_token", sessionToken);
            final String sessionSecret = sessionService.getSession().getSessionSecret();
            AuthUtils.addAuthenticationParameters(sessionSecret,
                    requestMethod,
                    UrlUtils.getBaseUrl(api, config.getApiDomain()),
                    urlParams);
        } else {
            urlParams.put("ApiKey", config.getApiKey());
        }

        GigyaLogger.debug("GigyaApiRequest", "Request parameters:\n" + urlParams.toString());

        // Encode url & generate encoded parameters.
        final String encodedParams = UrlUtils.buildEncodedQuery(urlParams);
        final String url = UrlUtils.getBaseUrl(api, config.getApiDomain()) + (requestMethod == RestAdapter.GET ? "?" + encodedParams : "");

        // Generate new GigyaApiRequest entity.
        return new GigyaApiRequest(url, requestMethod == RestAdapter.POST ? encodedParams : null, requestMethod, api);
    }

    //endregion
}
