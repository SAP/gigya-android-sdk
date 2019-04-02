package com.gigya.android.sdk.managers;

import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaDefinitions;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.model.GigyaConfigModel;
import com.gigya.android.sdk.network.GigyaApiRequest;
import com.gigya.android.sdk.network.GigyaApiResponse;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.adapter.INetworkCallbacks;
import com.gigya.android.sdk.network.adapter.IRestAdapter;
import com.gigya.android.sdk.network.adapter.RestAdapter;
import com.gigya.android.sdk.services.Config;
import com.gigya.android.sdk.utils.AuthUtils;
import com.gigya.android.sdk.utils.UrlUtils;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class ApiService<R> implements IApiService<R> {

    private static final String LOG_TAG = "ApiService";

    final private Config _config;
    final private IRestAdapter _restAdapter;
    final private ISessionService _sessionService;
    final private IAccountService _accountService;

    public ApiService(Config config, IRestAdapter restAdapter, ISessionService sessionService, IAccountService accountService) {
        _config = config;
        _restAdapter = restAdapter;
        _sessionService = sessionService;
        _accountService = accountService;
    }

    @Override
    public void send(final String api, Map<String, Object> params, int requestMethod, final GigyaCallback<GigyaApiResponse> callback) {
        GigyaApiRequest apiRequest = generateRequest(api, params, requestMethod);
        adapterSend(apiRequest, false, null, callback);
    }

    @Override
    public void send(String api, Map<String, Object> params, Class<R> scheme, final GigyaCallback<R> callback) {
        GigyaApiRequest apiRequest = generateRequest(api, params, RestAdapter.POST);
        adapterSend(apiRequest, false, scheme, callback);
    }

    @Override
    public void getConfig(final GigyaCallback<R> callback) {
        if (requestRequiresApiKey("getConfig")) {
            final Map<String, Object> params = new HashMap<>();
            params.put("include", "permissions,ids,appIds");
            params.put("ApiKey", _config.getApiKey());
            GigyaApiRequest apiRequest = generateRequest(GigyaDefinitions.API.API_GET_SDK_CONFIG, params, RestAdapter.GET);
            adapterSend(apiRequest, true, GigyaConfigModel.class, new GigyaCallback<GigyaConfigModel>() {

                @Override
                public void onSuccess(GigyaConfigModel obj) {
                    _config.setUcid(obj.getIds().getUcid());
                    _config.setGmid(obj.getIds().getGmid());
                    _sessionService.save(null); // Will save only config instances.
                    _restAdapter.release();
                }

                @Override
                public void onError(GigyaError error) {
                    if (callback != null) {
                        callback.onError(error);
                    }
                    _restAdapter.release();
                }
            });
        }
    }

    private boolean requestRequiresApiKey(String tag) {
        if (_config.getApiKey() == null) {
            GigyaLogger.error(LOG_TAG, tag + " requestRequiresApiKey: Api key missing");
            return false;
        }
        return true;
    }

    //region ADAPTER SEND

    private <V> void adapterSend(GigyaApiRequest apiRequest, boolean blocking, final Class<V> scheme, final GigyaCallback<V> callback) {
        _restAdapter.send(apiRequest, blocking, new INetworkCallbacks() {
            @Override
            public void onResponse(String jsonResponse) {
                try {
                    final GigyaApiResponse apiResponse = new GigyaApiResponse(jsonResponse);
                    final int statusCode = apiResponse.getStatusCode();
                    if (statusCode == GigyaApiResponse.OK) {
                        callback.onSuccess(handleApiResponse(apiResponse, scheme));
                    } else {
                        callback.onError(GigyaError.fromResponse(apiResponse));
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            @Override
            public void onError(GigyaError gigyaError) {
                if (callback != null) {
                    callback.onError(gigyaError);
                }
            }
        });
    }

    private <V> V handleApiResponse(GigyaApiResponse apiResponse, Class<V> scheme) {
        try {
            if (scheme == null) {
                return (V) apiResponse;
            } else {
                return new Gson().fromJson(apiResponse.asJson(), scheme);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    //endregion

    //region REQUEST GENERATION

    private GigyaApiRequest generateRequest(String api, Map<String, Object> params, int requestMethod) {
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

        GigyaLogger.debug(LOG_TAG, "Request parameters:\n" + urlParams.toString());

        // Encode url & generate encoded parameters.
        final String encodedParams = UrlUtils.buildEncodedQuery(urlParams);
        final String url = UrlUtils.getBaseUrl(api, _config.getApiDomain()) + (requestMethod == RestAdapter.GET ? "?" + encodedParams : "");

        // Generate new GigyaApiRequest entity.
        return new GigyaApiRequest(url, requestMethod == RestAdapter.POST ? encodedParams : null, requestMethod, api);
    }

    //endregion
}
