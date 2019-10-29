package com.gigya.android.sdk.api;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.network.adapter.RestAdapter;
import com.gigya.android.sdk.session.ISessionService;
import com.gigya.android.sdk.utils.AuthUtils;
import com.gigya.android.sdk.utils.UrlUtils;

public class GigyaApiRequestSigner implements IGigyaApiRequestSigner {

    final private Config _config;
    final private ISessionService _sessionService;

    public GigyaApiRequestSigner(
            Config config,
            ISessionService sessionService) {
        _config = config;
        _sessionService = sessionService;
    }

    @Override
    public void signRequest(GigyaApiRequest request) {

        // The request will need to be resigned. To avoid signature errors we must remove all
        // authentication parameters from the original request.
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
