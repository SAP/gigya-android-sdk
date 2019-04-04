package com.gigya.android.sdk.managers;

import android.support.v4.util.Pair;

import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaDefinitions;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.api.interruption.IInterruptionsResolver;
import com.gigya.android.sdk.model.GigyaConfigModel;
import com.gigya.android.sdk.model.account.GigyaAccount;
import com.gigya.android.sdk.model.account.SessionInfo;
import com.gigya.android.sdk.network.GigyaApiRequest;
import com.gigya.android.sdk.network.GigyaApiResponse;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.adapter.INetworkCallbacks;
import com.gigya.android.sdk.network.adapter.IRestAdapter;
import com.gigya.android.sdk.network.adapter.RestAdapter;
import com.gigya.android.sdk.providers.IProviderPermissionsCallback;
import com.gigya.android.sdk.services.Config;
import com.gigya.android.sdk.utils.AuthUtils;
import com.gigya.android.sdk.utils.ObjectUtils;
import com.gigya.android.sdk.utils.UrlUtils;
import com.google.gson.Gson;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class ApiService<R extends GigyaAccount> implements IApiService<R> {

    private static final String LOG_TAG = "ApiService";

    final private Config _config;
    final private IRestAdapter _restAdapter;
    final private ISessionService _sessionService;
    final private IAccountService _accountService;
    final private IInterruptionsResolver _interruptionsResolver;

    public ApiService(Config config, IRestAdapter restAdapter, ISessionService sessionService, IAccountService accountService, IInterruptionsResolver interruptionsResolver) {
        _config = config;
        _restAdapter = restAdapter;
        _sessionService = sessionService;
        _accountService = accountService;
        _interruptionsResolver = interruptionsResolver;
    }

    @Override
    public void send(final String api, Map<String, Object> params, int requestMethod, final GigyaCallback<GigyaApiResponse> gigyaCallback) {
        send(api, params, requestMethod, GigyaApiResponse.class, gigyaCallback);
    }

    @Override
    public <V> void send(String api, Map<String, Object> params, int requestMethod, Class<V> scheme, final GigyaCallback<V> gigyaCallback) {
        GigyaApiRequest apiRequest = generateRequest(api, params, requestMethod);
        adapterSend(apiRequest, false, scheme, new IApiAdapterResponse<V>() {
            @Override
            public void onApiAdapterSuccess(GigyaApiResponse apiResponse, V response) {
                gigyaCallback.onSuccess(response);
            }

            @Override
            public void onApiAdapterError(GigyaApiResponse apiResponse) {
                gigyaCallback.onError(GigyaError.fromResponse(apiResponse));
            }

            @Override
            public void onApiAdapterNetworkError(GigyaError error) {
                gigyaCallback.onError(error);
            }
        });
    }

    //region ONE LINERS APIS

    @Override
    public void getConfig(final String nextApiTag, final GigyaCallback<R> gigyaCallback) {
        if (requestRequiresApiKey("getConfig")) {
            final Map<String, Object> params = new HashMap<>();
            params.put("include", "permissions,ids,appIds");
            params.put("ApiKey", _config.getApiKey());
            GigyaApiRequest apiRequest = generateRequest(GigyaDefinitions.API.API_GET_SDK_CONFIG, params, RestAdapter.GET);
            adapterSend(apiRequest, true, GigyaConfigModel.class, new IApiAdapterResponse<GigyaConfigModel>() {
                @Override
                public void onApiAdapterSuccess(GigyaApiResponse apiResponse, GigyaConfigModel response) {
                    _config.setUcid(response.getIds().getUcid());
                    _config.setGmid(response.getIds().getGmid());
                    _sessionService.save(null); // Will save only config instances.
                    _restAdapter.release();
                }

                @Override
                public void onApiAdapterError(GigyaApiResponse apiResponse) {
                    gigyaCallback.onError(GigyaError.fromResponse(apiResponse));
                    if (nextApiTag != null) {
                        _restAdapter.cancel(nextApiTag);
                    }
                    _restAdapter.release();
                }

                @Override
                public void onApiAdapterNetworkError(GigyaError error) {
                    gigyaCallback.onError(error);
                    if (nextApiTag != null) {
                        _restAdapter.cancel(nextApiTag);
                    }
                    _restAdapter.release();
                }
            });
        }
    }

    @Override
    public void logout() {
        if (_sessionService.isValid()) {
            send(GigyaDefinitions.API.API_LOGOUT, null, RestAdapter.POST, new GigyaCallback<GigyaApiResponse>() {
                @Override
                public void onSuccess(GigyaApiResponse obj) {
                    GigyaLogger.debug(LOG_TAG, "logOut: Success");
                }

                @Override
                public void onError(GigyaError error) {
                    GigyaLogger.error(LOG_TAG, "logOut: Failed API");
                }
            });
        }
    }

    @Override
    public void login(Map<String, Object> params, final GigyaLoginCallback<R> loginCallback) {
        requestRequiresGMID(GigyaDefinitions.API.API_LOGIN, loginCallback);
        GigyaApiRequest apiRequest = generateRequest(GigyaDefinitions.API.API_LOGIN, params, RestAdapter.POST);
        adapterSend(apiRequest, false, _accountService.getAccountScheme(), new IApiAdapterResponse<R>() {
            @Override
            public void onApiAdapterSuccess(GigyaApiResponse apiResponse, R response) {
                if (!_interruptionsResolver.evaluateInterruptionSuccess(apiResponse)) {
                    updateWithNewSession(apiResponse);
                    updateCachedAccount(apiResponse);
                    loginCallback.onSuccess(response);
                }
            }

            @Override
            public void onApiAdapterError(GigyaApiResponse apiResponse) {
                if (!_interruptionsResolver.evaluateInterruptionError(apiResponse, loginCallback)) {
                    loginCallback.onError(GigyaError.fromResponse(apiResponse));
                }
            }

            @Override
            public void onApiAdapterNetworkError(GigyaError error) {
                loginCallback.onError(error);
            }
        });
    }

    @Override
    public void register(final Map<String, Object> params, final GigyaLoginCallback<R> loginCallback) {
        requestRequiresGMID(GigyaDefinitions.API.API_INIT_REGISTRATION, loginCallback);
        // #1 Chain init registration.
        GigyaApiRequest apiRequest = generateRequest(GigyaDefinitions.API.API_INIT_REGISTRATION, params, RestAdapter.POST);
        adapterSend(apiRequest, false, GigyaApiResponse.class, new IApiAdapterResponse<GigyaApiResponse>() {
            @Override
            public void onApiAdapterSuccess(GigyaApiResponse apiResponse, GigyaApiResponse response) {
                final String regToken = apiResponse.getField("regToken", String.class);
                if (regToken != null) {
                    params.put("regToken", regToken);
                    params.put("finalizeRegistration", true);
                } else {
                    GigyaLogger.error(LOG_TAG, "register: Init registration produced null regToken");
                    loginCallback.onError(GigyaError.generalError());
                    return;
                }
                // #2 Chain login.
                GigyaApiRequest apiRequest = generateRequest(GigyaDefinitions.API.API_REGISTER, params, RestAdapter.POST);
                adapterSend(apiRequest, false, _accountService.getAccountScheme(), new IApiAdapterResponse<R>() {
                    @Override
                    public void onApiAdapterSuccess(GigyaApiResponse apiResponse, R response) {
                        if (!_interruptionsResolver.evaluateInterruptionSuccess(apiResponse)) {
                            updateWithNewSession(apiResponse);
                            updateCachedAccount(apiResponse);
                            loginCallback.onSuccess(response);
                        }
                    }

                    @Override
                    public void onApiAdapterError(GigyaApiResponse apiResponse) {
                        if (!_interruptionsResolver.evaluateInterruptionError(apiResponse, loginCallback)) {
                            loginCallback.onError(GigyaError.fromResponse(apiResponse));
                        }
                    }

                    @Override
                    public void onApiAdapterNetworkError(GigyaError error) {
                        loginCallback.onError(error);
                    }
                });
            }

            @Override
            public void onApiAdapterError(GigyaApiResponse apiResponse) {
                loginCallback.onError(GigyaError.fromResponse(apiResponse));
            }

            @Override
            public void onApiAdapterNetworkError(GigyaError error) {
                loginCallback.onError(error);
            }
        });
    }

    @Override
    public void getAccount(final GigyaCallback<R> gigyaCallback) {
        requestRequiresValidSession(GigyaDefinitions.API.API_GET_ACCOUNT_INFO, gigyaCallback);
        if (_accountService.isCachedAccount()) {
            gigyaCallback.onSuccess((R) _accountService.getAccount());
            return;
        }
        GigyaApiRequest apiRequest = generateRequest(GigyaDefinitions.API.API_GET_ACCOUNT_INFO, null, RestAdapter.POST);
        adapterSend(apiRequest, false, _accountService.getAccountScheme(), new IApiAdapterResponse<R>() {
            @Override
            public void onApiAdapterSuccess(GigyaApiResponse apiResponse, R response) {
                updateCachedAccount(apiResponse);
                gigyaCallback.onSuccess(response);
            }

            @Override
            public void onApiAdapterError(GigyaApiResponse apiResponse) {
                gigyaCallback.onError(GigyaError.fromResponse(apiResponse));
            }

            @Override
            public void onApiAdapterNetworkError(GigyaError error) {
                gigyaCallback.onError(error);
            }
        });
    }

    @Override
    public void setAccount(R updatedAccount, final GigyaCallback<R> gigyaCallback) {
        requestRequiresValidSession(GigyaDefinitions.API.API_SET_ACCOUNT_INFO, gigyaCallback);
        final Map<String, Object> params = _accountService.calculateDiff(new Gson(), _accountService.getAccount(), updatedAccount);
        final GigyaApiRequest apiRequest = generateRequest(GigyaDefinitions.API.API_SET_ACCOUNT_INFO, params, RestAdapter.POST);
        adapterSend(apiRequest, false, _accountService.getAccountScheme(), new IApiAdapterResponse<R>() {
            @Override
            public void onApiAdapterSuccess(GigyaApiResponse apiResponse, R response) {
                _accountService.invalidateAccount();
                getAccount(gigyaCallback);
            }

            @Override
            public void onApiAdapterError(GigyaApiResponse apiResponse) {
                gigyaCallback.onError(GigyaError.fromResponse(apiResponse));
            }

            @Override
            public void onApiAdapterNetworkError(GigyaError error) {
                gigyaCallback.onError(error);
            }
        });
    }

    @Override
    public void verifyLogin(String UID, final boolean ignoreSession, final GigyaCallback<R> gigyaCallback) {
        requestRequiresValidSession(GigyaDefinitions.API.API_SET_ACCOUNT_INFO, gigyaCallback);
        final Map<String, Object> params = new HashMap<>();
        if (UID != null) {
            params.put("UID", UID);
        }
        params.put("include", "identities-all,loginIDs,profile,email,data");
        GigyaApiRequest apiRequest = generateRequest(GigyaDefinitions.API.API_VERIFY_LOGIN, params, RestAdapter.POST);
        adapterSend(apiRequest, false, _accountService.getAccountScheme(), new IApiAdapterResponse<R>() {
            @Override
            public void onApiAdapterSuccess(GigyaApiResponse apiResponse, R response) {
                if (!ignoreSession) {
                    updateWithNewSession(apiResponse);
                    updateCachedAccount(apiResponse);
                }
                gigyaCallback.onSuccess(response);
            }

            @Override
            public void onApiAdapterError(GigyaApiResponse apiResponse) {
                gigyaCallback.onError(GigyaError.fromResponse(apiResponse));
            }

            @Override
            public void onApiAdapterNetworkError(GigyaError error) {
                gigyaCallback.onError(error);
            }
        });
    }

    @Override
    public void forgotPassword(String loginId, GigyaCallback<GigyaApiResponse> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("loginID", loginId);
        send(GigyaDefinitions.API.API_RESET_PASSWORD, params, RestAdapter.POST, callback);
    }

    @Override
    public void finalizeRegistration(String regToken, final GigyaLoginCallback<R> loginCallback) {
        final Map<String, Object> params = ObjectUtils.mapOf(Arrays.asList(
                new Pair<String, Object>("regToken", regToken),
                new Pair<String, Object>("include", "profile,data,emails,subscriptions,preferences"),
                new Pair<String, Object>("includeUserInfo", "true")));
        GigyaApiRequest apiRequest = generateRequest(GigyaDefinitions.API.API_FINALIZE_REGISTRATION, params, RestAdapter.POST);
        adapterSend(apiRequest, false, _accountService.getAccountScheme(), new IApiAdapterResponse<R>() {
            @Override
            public void onApiAdapterSuccess(GigyaApiResponse apiResponse, R response) {
                // Finalizing the registration is the final step when suing any interruption resolver. Therefore
                // We will always clear all resolvers on completion or on errors.
                _interruptionsResolver.clearAll();
                if (!_interruptionsResolver.evaluateInterruptionSuccess(apiResponse)) {
                    updateWithNewSession(apiResponse);
                    updateCachedAccount(apiResponse);
                    loginCallback.onSuccess(response);
                }
            }

            @Override
            public void onApiAdapterError(GigyaApiResponse apiResponse) {
                // Clearing all resolvers on error.
                _interruptionsResolver.clearAll();
                if (!_interruptionsResolver.evaluateInterruptionError(apiResponse, loginCallback)) {
                    loginCallback.onError(GigyaError.fromResponse(apiResponse));
                }
            }

            @Override
            public void onApiAdapterNetworkError(GigyaError error) {
                loginCallback.onError(error);
            }
        });
    }

    @Override
    public void notifyLogin(String providerSessions, final GigyaLoginCallback<R> loginCallback) {
        final Map<String, Object> params = new HashMap<>();
        params.put("providerSessions", providerSessions);
        final GigyaApiRequest apiRequest = generateRequest(GigyaDefinitions.API.API_NOTIFY_LOGIN, params, RestAdapter.POST);
        adapterSend(apiRequest, false, _accountService.getAccountScheme(), new IApiAdapterResponse<R>() {
            @Override
            public void onApiAdapterSuccess(GigyaApiResponse apiResponse, R response) {
                if (!_interruptionsResolver.evaluateInterruptionSuccess(apiResponse)) {
                    updateWithNewSession(apiResponse);
                    updateCachedAccount(apiResponse);
                    loginCallback.onSuccess(response);
                }
            }

            @Override
            public void onApiAdapterError(GigyaApiResponse apiResponse) {
                if (!_interruptionsResolver.evaluateInterruptionError(apiResponse, loginCallback)) {
                    loginCallback.onError(GigyaError.fromResponse(apiResponse));
                }
            }

            @Override
            public void onApiAdapterNetworkError(GigyaError error) {
                loginCallback.onError(error);
            }
        });
    }

    @Override
    public void nativeSocialLogin(Map<String, Object> params, final GigyaLoginCallback<R> loginCallback, final Runnable optionalCompletionHandler) {
        if (params.containsKey("loginMode")) {
            final String linkMode = (String) params.get("loginMode");
            if (ObjectUtils.safeEquals(linkMode,"link")) {
                requestRequiresValidSession(GigyaDefinitions.API.API_NOTIFY_SOCIAL_LOGIN, loginCallback);
            }
        }
        final GigyaApiRequest apiRequest = generateRequest(GigyaDefinitions.API.API_NOTIFY_SOCIAL_LOGIN, params, RestAdapter.POST);
        adapterSend(apiRequest, false, _accountService.getAccountScheme(), new IApiAdapterResponse<R>() {
            @Override
            public void onApiAdapterSuccess(GigyaApiResponse apiResponse, R response) {
                if (!_interruptionsResolver.evaluateInterruptionSuccess(apiResponse)) {
                    updateWithNewSession(apiResponse);
                    updateCachedAccount(apiResponse);
                    if (optionalCompletionHandler != null) {
                        optionalCompletionHandler.run();
                    }
                    loginCallback.onSuccess(response);
                }
            }

            @Override
            public void onApiAdapterError(GigyaApiResponse apiResponse) {
                if (!_interruptionsResolver.evaluateInterruptionError(apiResponse, loginCallback)) {
                    loginCallback.onError(GigyaError.fromResponse(apiResponse));
                }
            }

            @Override
            public void onApiAdapterNetworkError(GigyaError error) {
                loginCallback.onError(error);
            }
        });
    }

    @Override
    public void refreshNativeProviderSession(String providerSession, final IProviderPermissionsCallback providerPermissionsCallback) {
        final Map<String, Object> params = new HashMap<>();
        params.put("providerSession", providerSession);
        final GigyaApiRequest apiRequest = generateRequest(GigyaDefinitions.API.API_REFRESH_PROVIDER_SESSION, params, RestAdapter.POST);
        adapterSend(apiRequest, false, GigyaApiResponse.class, new IApiAdapterResponse<GigyaApiResponse>() {
            @Override
            public void onApiAdapterSuccess(GigyaApiResponse apiResponse, GigyaApiResponse response) {
                providerPermissionsCallback.granted();
                // Next get account should fetch new data.
                _accountService.invalidateAccount();
            }

            @Override
            public void onApiAdapterError(GigyaApiResponse apiResponse) {
                final GigyaError error = GigyaError.fromResponse(apiResponse);
                providerPermissionsCallback.failed(error.getLocalizedMessage());
            }

            @Override
            public void onApiAdapterNetworkError(GigyaError error) {
                providerPermissionsCallback.failed(error.getLocalizedMessage());
            }
        });
    }

    //endregion

    //region CONDITIONALS & GENERAL RESPONSE HANDLING

    private void updateWithNewSession(GigyaApiResponse apiResponse) {
        if (apiResponse.containsNested("sessionInfo.sessionSecret")) {
            final SessionInfo newSession = apiResponse.getField("sessionInfo", SessionInfo.class);
            _sessionService.setSession(newSession);
            _accountService.invalidateAccount();
        }
    }

    private void updateCachedAccount(GigyaApiResponse apiResponse) {
        _accountService.setAccount(apiResponse.asJson());
    }

    private boolean requestRequiresApiKey(String tag) {
        if (_config.getApiKey() == null) {
            GigyaLogger.error(LOG_TAG, tag + " requestRequiresApiKey: Api key missing");
            return false;
        }
        return true;
    }

    private void requestRequiresGMID(String tag, GigyaCallback<R> gigyaCallback) {
        if (_config.getGmid() == null) {
            GigyaLogger.debug(LOG_TAG, tag + " requestRequiresGMID - get lazy");
            getConfig(tag, gigyaCallback);
        }
    }

    private void requestRequiresValidSession(String tag, GigyaCallback<R> gigyaCallback) {
        if (!_sessionService.isValid()) {
            gigyaCallback.onError(GigyaError.invalidSession());
            return;
        }
        requestRequiresGMID(tag, gigyaCallback);
    }


    //endregion

    //region ADAPTER SEND

    private <V> void adapterSend(GigyaApiRequest apiRequest, boolean blocking, final Class<V> scheme, final IApiAdapterResponse<V> responseListener) {
        _restAdapter.send(apiRequest, blocking, new INetworkCallbacks() {
            @Override
            public void onResponse(String jsonResponse) {
                try {
                    final GigyaApiResponse apiResponse = new GigyaApiResponse(jsonResponse);
                    final int statusCode = apiResponse.getStatusCode();
                    if (statusCode == GigyaApiResponse.OK) {
                        responseListener.onApiAdapterSuccess(apiResponse, handleApiResponse(apiResponse, scheme));
                    } else {
                        responseListener.onApiAdapterError(apiResponse);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            @Override
            public void onError(GigyaError gigyaError) {
                responseListener.onApiAdapterNetworkError(gigyaError);
            }
        });
    }

    private interface IApiAdapterResponse<V> {

        void onApiAdapterSuccess(GigyaApiResponse apiResponse, V response);

        void onApiAdapterError(GigyaApiResponse apiResponse);

        void onApiAdapterNetworkError(GigyaError error);
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
