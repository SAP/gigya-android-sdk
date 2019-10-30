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

    /**
     * Create a new instance of the GigyaApiRequest structure.
     *
     * @param api           Api method.
     * @param params        Request parameters.
     * @param requestMethod Request method.
     * @return New GigyaApiRequest instance.
     */
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

    /**
     * Sign the request prior to dispatching it.
     *
     * @param request GigyaApiRequest instance.
     */
    @Override
    public void sign(GigyaApiRequest request) {

        // The request will need to be resigned. To avoid signature errors we must remove all
        // authentication parameters from the original request. Order must be kept prior to signing the request.
        AuthUtils.removeAuthenticationParameters(request.getOriginalParameters());

        GigyaLogger.debug("ServerTime", "offset for signer = " + _config.getServerOffset());

        // Add authentication parameters. Get SDK Config request is an exception.
        if (_sessionService.isValid()) {
            final String sessionToken = _sessionService.getSession().getSessionToken();
            request.getOriginalParameters().put("oauth_token", sessionToken);
            final String sessionSecret = _sessionService.getSession().getSessionSecret();
            AuthUtils.addAuthenticationParameters(sessionSecret,
                    request.getMethod(),
                    UrlUtils.getBaseUrl(request.getApi(), _config.getApiDomain()),
                    request.getOriginalParameters(),
                    _config.getServerOffset());
        } else {
            request.getOriginalParameters().put("ApiKey", _config.getApiKey());
        }

        GigyaLogger.debug("GigyaApiRequest", "Request parameters:\n" + request.getOriginalParameters().toString());

        // Encode url & generate encoded parameters.
        final String encodedParams = UrlUtils.buildEncodedQuery(request.getOriginalParameters());
        final String url = UrlUtils.getBaseUrl(request.getApi(), _config.getApiDomain()) + (request.getMethod() == RestAdapter.GET ? "?" + encodedParams : "");

        request.sign(url, encodedParams);
    }
}
