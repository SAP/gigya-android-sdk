package com.gigya.android.sdk.api;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.Gigya;

import java.util.Map;
import java.util.TreeMap;

public class GigyaApiRequestFactory implements IApiRequestFactory {
    final private Config _config;

    public GigyaApiRequestFactory(Config config) {
        _config = config;
    }

    public GigyaApiRequest create(String api, Map<String, Object> params, int requestMethod) {
        TreeMap<String, Object> urlParams = new TreeMap<>();
        if (params != null) {
            urlParams.putAll(params);
        }

        // Add general parameters.
        urlParams.put("sdk", "Android_" + Gigya.VERSION);
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

        // Generate new GigyaApiRequest entity.
        return new GigyaApiRequest(requestMethod, api, urlParams);
    }
}
