package com.gigya.android.sdk.network;

import android.support.annotation.Nullable;

import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;
import com.gigya.android.sdk.services.Config;
import com.gigya.android.sdk.services.SessionService;
import com.gigya.android.sdk.utils.AuthUtils;
import com.gigya.android.sdk.utils.UrlUtils;

import java.util.Map;
import java.util.TreeMap;

@Deprecated
public class GigyaApiRequestBuilder {

    private static final String LOG_TAG = "GigyaApiRequestBuilder";

    @Nullable
    private Map<String, Object> params;
    private NetworkAdapter.Method httpMethod = NetworkAdapter.Method.POST;
    private String api;
    final private SessionService sessionService;

    //region BUILDER PATTERN

    public GigyaApiRequestBuilder(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    public GigyaApiRequestBuilder api(String api) {
        this.api = api;
        return this;
    }

    public GigyaApiRequestBuilder httpMethod(NetworkAdapter.Method httpMethod) {
        this.httpMethod = httpMethod;
        return this;
    }

    public GigyaApiRequestBuilder params(Map<String, Object> params) {
        this.params = params;
        return this;
    }

    //endregion

    public GigyaApiRequest build() {
        final Config config = this.sessionService.getConfig();
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
        if (sessionService.isValidSession()) {
            @SuppressWarnings("ConstantConditions") final String sessionToken = sessionService.getSession().getSessionToken();
            urlParams.put("oauth_token", sessionToken);
            final String sessionSecret = sessionService.getSession().getSessionSecret();
            AuthUtils.addAuthenticationParameters(sessionSecret,
                    httpMethod.getValue(),
                    UrlUtils.getBaseUrl(api, config.getApiDomain()),
                    urlParams);
        } else {
            urlParams.put("ApiKey", config.getApiKey());
        }

        GigyaLogger.debug(LOG_TAG, "Request parameters:\n" + urlParams.toString());

        // Encode url & generate encoded parameters.
        final String encodedParams = UrlUtils.buildEncodedQuery(urlParams);
        final String url = UrlUtils.getBaseUrl(api, config.getApiDomain()) + (httpMethod.equals(NetworkAdapter.Method.GET) ? "?" + encodedParams : "");

        // Generate new GigyaApiRequest entity.
        return new GigyaApiRequest(url, httpMethod == NetworkAdapter.Method.POST ? encodedParams : null, 1, api);
    }
}
