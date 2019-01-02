package com.gigya.android.sdk.network;

import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.model.Configuration;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;
import com.gigya.android.sdk.utils.AuthUtils;
import com.gigya.android.sdk.utils.UrlUtils;

import java.util.Map;
import java.util.TreeMap;

public class GigyaRequestBuilder {

    private Configuration configuration;
    private Map<String, Object> params;
    private NetworkAdapter.Method httpMethod = NetworkAdapter.Method.POST;
    private String api;
    private SessionManager sessionManager;

    //region Builder pattern

    public GigyaRequestBuilder(Configuration configuration) {
        this.configuration = configuration;
    }

    public GigyaRequestBuilder api(String api) {
        this.api = api;
        return this;
    }

    public GigyaRequestBuilder httpMethod(NetworkAdapter.Method httpMethod) {
        this.httpMethod = httpMethod;
        return this;
    }

    public GigyaRequestBuilder params(Map<String, Object> params) {
        this.params = params;
        return this;
    }

    public GigyaRequestBuilder sessionManager(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
        return this;
    }

    //endregion

    public GigyaRequest build() {
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
        final String gmid = configuration.getGMID();
        if (gmid != null) {
            urlParams.put("gmid", gmid);
        }
        final String ucid = configuration.getUCID();
        if (ucid != null) {
            urlParams.put("ucid", ucid);
        }

        // Add authentication parameters.
        if (sessionManager != null) {
            if (sessionManager.isValidSession()) {
                @SuppressWarnings("ConstantConditions") final String sessionToken = sessionManager.getSession().getSessionToken();
                urlParams.put("oauth_token", sessionToken);
                final String sessionSecret = sessionManager.getSession().getSessionSecret();
                AuthUtils.addAuthenticationParameters(sessionSecret,
                        httpMethod.getValue(),
                        UrlUtils.getBaseUrl(api, configuration.getApiDomain()),
                        urlParams);
            } else {
                urlParams.put("ApiKey", configuration.getApiKey());
            }
        }

        // Encode url & generate encoded parameters.
        final String encodedParams = UrlUtils.buildEncodedQuery(urlParams);
        final String url = UrlUtils.getBaseUrl(api, configuration.getApiDomain()) + (httpMethod.equals(NetworkAdapter.Method.GET) ? "?" + encodedParams : "");

        // Generate new GigyaRequest entity.
        return new GigyaRequest(url, httpMethod == NetworkAdapter.Method.POST ? encodedParams : null, httpMethod, api);
    }
}
