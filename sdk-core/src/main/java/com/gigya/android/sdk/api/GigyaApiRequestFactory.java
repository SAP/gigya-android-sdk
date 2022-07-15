package com.gigya.android.sdk.api;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.GigyaDefinitions;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.account.GigyaAccountConfig;
import com.gigya.android.sdk.network.adapter.RestAdapter;
import com.gigya.android.sdk.session.ISessionService;
import com.gigya.android.sdk.utils.AuthUtils;
import com.gigya.android.sdk.utils.UrlUtils;

import java.security.SecureRandom;
import java.util.HashMap;
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

    @Override
    public GigyaApiRequest create(String api, Map<String, Object> params) {
        return create(api, params, RestAdapter.HttpMethod.POST);
    }

    /**
     * Create a new instance of the GigyaApiRequest structure.
     *
     * @param api        Api method.
     * @param params     Request parameters.
     * @param httpMethod Request HTTP method.
     * @return New GigyaApiRequest instance.
     */
    @Override
    public GigyaApiRequest create(String api,
                                  Map<String, Object> params,
                                  RestAdapter.HttpMethod httpMethod) {
        return create(api, params, httpMethod, null);
    }

    /**
     * Create a new instance of the GigyaApiRequest structure.
     *
     * @param api        Api method.
     * @param params     Request parameters.
     * @param httpMethod Request HTTP method.
     * @param headers    Custom header map.
     * @return New GigyaApiRequest instance.
     */
    @Override
    public GigyaApiRequest create(String api,
                                  Map<String, Object> params,
                                  RestAdapter.HttpMethod httpMethod,
                                  @Nullable HashMap<String, String> headers) {
        TreeMap<String, Object> urlParams = new TreeMap<>();
        if (params != null) {
            urlParams.putAll(params);
        }

        // Add general parameters.
        urlParams.put("sdk", _sdk);
        urlParams.put("targetEnv", "mobile");
        urlParams.put("httpStatusCodes", false);
        urlParams.put("format", "json");

        // Add nonce.
        final SecureRandom random = new SecureRandom();
        String nonce = System.currentTimeMillis() + "_" + random.nextInt();
        urlParams.put("nonce", nonce);

        // Add configuration parameters.
        final String gmid = _config.getGmid();
        if (gmid != null) {
            urlParams.put("gmid", gmid);
        }
        final String ucid = _config.getUcid();
        if (ucid != null) {
            urlParams.put("ucid", ucid);
        }

        // Add global configuration request parameters.
        addAccountConfigParameters(api, urlParams);

        // Generate new GigyaApiRequest entity.
        return new GigyaApiRequest(httpMethod, api, urlParams, headers);
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

    /**
     * Adding specific account APIs related parameters.
     *
     * @param api    Requested API.
     * @param params Request provided parameter map.
     */
    private void addAccountConfigParameters(String api, Map<String, Object> params) {
        final GigyaAccountConfig gigyaAccountConfig = _config.getGigyaAccountConfig();
        if (gigyaAccountConfig == null) {
            return;
        }
        final String accountConfigInclude = gigyaAccountConfig.getInclude() != null ? TextUtils.join(",", gigyaAccountConfig.getInclude()) : null;
        final String accountConfigExtraProfileFields = gigyaAccountConfig.getExtraProfileFields() != null ? TextUtils.join(",", gigyaAccountConfig.getExtraProfileFields()) : null;
        switch (api) {
            case GigyaDefinitions.API.API_GET_ACCOUNT_INFO:
                if (!params.containsKey("include") && accountConfigInclude != null) {
                    params.put("include", accountConfigInclude);
                }
                if (!params.containsKey("extraProfileFields") && accountConfigExtraProfileFields != null) {
                    params.put("extraProfileFields", accountConfigExtraProfileFields);
                }
                break;
            case GigyaDefinitions.API.API_LOGIN:
            case GigyaDefinitions.API.API_REGISTER:
            case GigyaDefinitions.API.API_VERIFY_LOGIN:
                if (!params.containsKey("include") && accountConfigInclude != null) {
                    params.put("include", accountConfigInclude);
                }
                break;
        }
    }
}
