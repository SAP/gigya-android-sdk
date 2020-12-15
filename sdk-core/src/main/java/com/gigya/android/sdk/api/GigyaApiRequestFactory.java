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

    private static final String LOG_TAG = "GigyaApiRequestFactory";

    final private Config _config;
    final private ISessionService _sessionService;

    public GigyaApiRequestFactory(Config config, ISessionService sessionService) {
        _config = config;
        _sessionService = sessionService;
    }

    private String _sdk = "Android_" + Gigya.VERSION;

    @Override
    public void setSDK(String sdk) {
        _sdk = sdk;
    }

    /**
     * Create a new instance of the GigyaApiRequest structure.
     *
     * @param api        Api method.
     * @param params     Request parameters.
     * @param httpMethod Request HTTP method.
     * @return New GigyaApiRequest instance.
     */
    public GigyaApiRequest create(String api, Map<String, Object> params, RestAdapter.HttpMethod httpMethod) {
        TreeMap<String, Object> urlParams = new TreeMap<>();
        if (params != null) {
            urlParams.putAll(params);
        }

        // Add general parameters.
        urlParams.put("sdk", _sdk);
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
        return new GigyaApiRequest(httpMethod, api, urlParams);
    }

    /**
     * Sign the request prior to dispatching it.
     *
     * @param request GigyaApiRequest instance.
     */
    @Override
    public GigyaApiHttpRequest sign(GigyaApiRequest request) {

        // The request will need to be resigned. To avoid signature errors we must remove all
        // authentication parameters from the original request. Order must be kept prior to signing the request.
        AuthUtils.removeAuthenticationParameters(request.getParams());

        GigyaLogger.debug(LOG_TAG, "sign: offset for signer = " + _config.getServerOffset());

        // Add authentication parameters. Get SDK Config request is an exception.
        if (_sessionService.isValid() && !request.isAnonymous()) {
            final String sessionToken = _sessionService.getSession().getSessionToken();
            request.getParams().put("oauth_token", sessionToken);
            final String sessionSecret = _sessionService.getSession().getSessionSecret();
            AuthUtils.addAuthenticationParameters(
                    sessionSecret,
                    request.getMethod().intValue(),
                    UrlUtils.getBaseUrl(request.getApi(), _config.getApiDomain()),
                    request.getParams(),
                    _config.getServerOffset());
        } else {
            request.getParams().put("apiKey", _config.getApiKey());
        }

        GigyaLogger.debug(LOG_TAG, "sign: request parameters:\n" + request.getParams().toString());

        // Encode url & generate encoded parameters.
        final String encodedParams = UrlUtils.buildEncodedQuery(request.getParams());
        final String url = UrlUtils.getBaseUrl(request.getApi(),
                _config.getApiDomain()) + (request.getMethod() == RestAdapter.HttpMethod.GET ? "?" + encodedParams : "");

        // Return a new instance of a signed REST request.
        return new GigyaApiHttpRequest(request.getMethod(), url, encodedParams);
    }

    @Override
    public GigyaApiHttpRequest unsigned(GigyaApiRequest request) {
        if (!request.getParams().containsKey("apiKey")) {
            request.getParams().put("apiKey", _config.getApiKey());
        }
        final String encodedParams = UrlUtils.buildEncodedQuery(request.getParams());

        return new GigyaApiHttpRequest(request.getMethod(), request.getApi(), encodedParams);
    }
}
