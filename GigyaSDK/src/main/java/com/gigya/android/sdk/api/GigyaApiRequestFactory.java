package com.gigya.android.sdk.api;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.network.adapter.RestAdapter;
import com.gigya.android.sdk.session.ISessionService;
import com.gigya.android.sdk.utils.AuthUtils;
import com.gigya.android.sdk.utils.UrlUtils;

import java.util.Map;
import java.util.TreeMap;

public class GigyaApiRequestFactory implements IApiRequestFactory {
    final private Config _config;
    final private ISessionService _sessionService;

    public GigyaApiRequestFactory(Config config, ISessionService sessionService) {
        _config = config;
        _sessionService = sessionService;
    }

    public GigyaApiRequest create(String api, Map<String, Object> params, int requestMethod) {
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
        final String gmid = _config.getGmid();
        if (gmid != null) {
            urlParams.put("gmid", gmid);
        }
        final String ucid = _config.getUcid();
        if (ucid != null) {
            urlParams.put("ucid", ucid);
        }
        // Add authentication parameters.
        if (_sessionService.isValid()) {
            @SuppressWarnings("ConstantConditions") final String sessionToken = _sessionService.getSession().getSessionToken();
            urlParams.put("oauth_token", sessionToken);
            final String sessionSecret = _sessionService.getSession().getSessionSecret();
            AuthUtils.addAuthenticationParameters(sessionSecret,
                    requestMethod,
                    UrlUtils.getBaseUrl(api, _config.getApiDomain()),
                    urlParams);
        } else {
            urlParams.put("ApiKey", _config.getApiKey());
        }

        GigyaLogger.debug("GigyaApiRequest", "Request parameters:\n" + urlParams.toString());

        // Encode url & generate encoded parameters.
        final String encodedParams = UrlUtils.buildEncodedQuery(urlParams);
        final String url = UrlUtils.getBaseUrl(api, _config.getApiDomain()) + (requestMethod == RestAdapter.GET ? "?" + encodedParams : "");

        // Generate new GigyaApiRequest entity.
        return new GigyaApiRequest(url, requestMethod == RestAdapter.POST ? encodedParams : null, requestMethod, api);
    }
}
