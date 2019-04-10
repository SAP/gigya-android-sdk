package com.gigya.android.sdk.api;

import android.content.Context;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaDefinitions;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.account.IAccountService;
import com.gigya.android.sdk.interruption.IInterruptionsHandler;
import com.gigya.android.sdk.model.GigyaConfigModel;
import com.gigya.android.sdk.model.account.GigyaAccount;
import com.gigya.android.sdk.model.account.SessionInfo;
import com.gigya.android.sdk.network.GigyaApiRequest;
import com.gigya.android.sdk.network.GigyaApiResponse;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.adapter.RestAdapter;
import com.gigya.android.sdk.providers.IProviderFactory;
import com.gigya.android.sdk.providers.IProviderPermissionsCallback;
import com.gigya.android.sdk.providers.provider.IProvider;
import com.gigya.android.sdk.session.ISessionService;
import com.gigya.android.sdk.utils.ObjectUtils;
import com.google.gson.Gson;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

public class BusinessApiService<A extends GigyaAccount> implements IBusinessApiService<A>, Observer {

    private static final String LOG_TAG = "BusinessApiService";

    // Dependencies.
    final private Config _config;
    final private ISessionService _sessionService;
    final private IAccountService<A> _accountService;
    final private IApiService _apiService;
    final private IProviderFactory _providerFactory;
    final private IInterruptionsHandler _interruptionsHandler;

    BusinessApiService(Config config, ISessionService sessionService, IAccountService<A> accountService, IApiService apiService, IProviderFactory providerFactory,
                       IInterruptionsHandler interruptionsHandler) {
        _config = config;
        _sessionService = sessionService;
        _accountService = accountService;
        _apiService = apiService;
        _providerFactory = providerFactory;
        _interruptionsHandler = interruptionsHandler;
    }

    //region CONDITIONALS & HELPERS

    @SuppressWarnings("SameParameterValue")
    private boolean requestRequiresApiKey(String issuerTag) {
        if (_config.getApiKey() == null) {
            GigyaLogger.error(LOG_TAG, issuerTag + " requestRequiresApiKey: ApiService key missing");
            return false;
        }
        return true;
    }

    private void requestRequiresGMID(String tag, GigyaCallback<A> gigyaCallback) {
        if (_config.getGmid() == null) {
            GigyaLogger.debug(LOG_TAG, tag + " requestRequiresGMID - get lazy");
            getSDKConfig(tag, gigyaCallback);
        }
    }

    private void requestRequiresValidSession(String tag, GigyaCallback<A> gigyaCallback) {
        if (!_sessionService.isValid()) {
            gigyaCallback.onError(GigyaError.invalidSession());
            return;
        }
        requestRequiresGMID(tag, gigyaCallback);
    }

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

    private void handleAccountApiResponse(GigyaApiResponse response, GigyaLoginCallback<A> loginCallback, Runnable completionHandler) {
        final int errorCode = response.getErrorCode();
        if (errorCode == 0) {
            // Parse & success.
            A parsed = response.parseTo(_accountService.getAccountScheme());
            updateWithNewSession(response);
            updateCachedAccount(response);
            loginCallback.onSuccess(parsed);
            if (completionHandler != null) {
                completionHandler.run();
            }
        } else if (isSupportedInterruption(errorCode)) {
            resolveInterruption(response, loginCallback);
        } else {
            // Unhandled error.
            loginCallback.onError(GigyaError.fromResponse(response));
        }
    }

    //endregion

    //region CONFIG

    @Override
    public void getSDKConfig(final String nextApiTag, final GigyaCallback<A> gigyaCallback) {
        if (requestRequiresApiKey("getConfig")) {
            // Generate request.
            final Map<String, Object> params = new HashMap<>();
            params.put("include", "permissions,ids,appIds");
            params.put("ApiKey", _config.getApiKey());
            final GigyaApiRequest request = GigyaApiRequest.newInstance(_config, _sessionService, GigyaDefinitions.API.API_GET_SDK_CONFIG, params, RestAdapter.GET);
            _apiService.send(request, true, new ApiService.IApiServiceResponse() {

                @Override
                public void onApiSuccess(GigyaApiResponse response) {
                    final GigyaConfigModel parsed = response.parseTo(GigyaConfigModel.class);
                    if (parsed == null) {
                        // Parsing error.
                        gigyaCallback.onError(GigyaError.fromResponse(response));
                        onConfigError(nextApiTag);
                        return;
                    }
                    onConfigResponse(parsed);
                }

                @Override
                public void onApiError(GigyaError gigyaError) {
                    gigyaCallback.onError(gigyaError);
                    onConfigError(nextApiTag);
                }
            });
        }
    }

    private void onConfigResponse(GigyaConfigModel response) {
        _config.setUcid(response.getIds().getUcid());
        _config.setGmid(response.getIds().getGmid());
        _sessionService.save(null); // Will save only config instances.
        _apiService.release();
    }

    private void onConfigError(String nextApiTag) {
        if (nextApiTag != null) {
            _apiService.cancel(nextApiTag);
        }
        _apiService.release();
    }

    //endregion

    //region INTERRUPTIONS

    // Supported interruptions.
    private final List<Integer> _interruptionList = Arrays.asList(
            GigyaError.Codes.ERROR_ACCOUNT_PENDING_REGISTRATION,
            GigyaError.Codes.ERROR_ACCOUNT_PENDING_VERIFICATION,
            GigyaError.Codes.ERROR_LOGIN_IDENTIFIER_EXISTS,
            GigyaError.Codes.ERROR_PENDING_TWO_FACTOR_REGISTRATION,
            GigyaError.Codes.ERROR_PENDING_TWO_FACTOR_VERIFICATION,
            GigyaError.Codes.SUCCESS_ERROR_ACCOUNT_LINKED);

    private boolean isSupportedInterruption(int errorCode) {
        return _interruptionList.contains(errorCode);
    }

    private void resolveInterruption(GigyaApiResponse response, GigyaLoginCallback<A> gigyaLoginCallback) {
        // Handle interruption.
        final IApiObservable observable = new ApiObservable().register(BusinessApiService.this);
        _interruptionsHandler.resolve(response, observable, gigyaLoginCallback);
    }

    //endregion

    //region OBSERVER

    @Override
    public synchronized void update(Observable observable, Object arg) {
        final ApiObservable.ApiObservableData data = (ApiObservable.ApiObservableData) arg;
        final String api = data.getApi();
        switch (api) {
            case GigyaDefinitions.API.API_FINALIZE_REGISTRATION:
                finalizeRegistration(data.getParams(), data.getLoginCallback());
                ((ApiObservable) observable).dispose();
                break;
            case GigyaDefinitions.API.API_NOTIFY_SOCIAL_LOGIN:
                nativeSocialLogin(data.getParams(), data.getLoginCallback(), data.getCompletionHandler());
                break;
            case GigyaDefinitions.API.API_GET_ACCOUNT_INFO:
                getAccount(data.getLoginCallback());
                break;
        }
    }

    //endregion

    //region LOGOUT

    @Override
    public void logout() {
        requestRequiresValidSession(GigyaDefinitions.API.API_LOGOUT, null);
        GigyaApiRequest request = GigyaApiRequest.newInstance(_config, _sessionService, GigyaDefinitions.API.API_LOGOUT, null, RestAdapter.POST);
        _apiService.send(request, false, new ApiService.IApiServiceResponse() {

            @Override
            public void onApiSuccess(GigyaApiResponse response) {
                GigyaLogger.error(LOG_TAG, "logOut: Success API");
            }

            @Override
            public void onApiError(GigyaError gigyaError) {
                GigyaLogger.error(LOG_TAG, "logOut: Failed API");
            }
        });
    }

    //endregion

    //region LOGIN

    @Override
    public void login(Map<String, Object> params, final GigyaLoginCallback<A> loginCallback) {
        requestRequiresGMID(GigyaDefinitions.API.API_LOGIN, loginCallback);
        final GigyaApiRequest request = GigyaApiRequest.newInstance(_config, _sessionService, GigyaDefinitions.API.API_LOGIN, params, RestAdapter.POST);
        _apiService.send(request, false, new ApiService.IApiServiceResponse() {
            @Override
            public void onApiSuccess(GigyaApiResponse response) {
                handleAccountApiResponse(response, loginCallback, null);
            }

            @Override
            public void onApiError(GigyaError gigyaError) {
                loginCallback.onError(gigyaError);
            }
        });
    }

    @Override
    public void login(Context context, @GigyaDefinitions.Providers.SocialProvider String socialProvider, Map<String, Object> params, GigyaLoginCallback<A> gigyaLoginCallback) {
        IApiObservable observable = new ApiObservable().register(this);
        IProvider provider = _providerFactory.providerFor(socialProvider, observable, gigyaLoginCallback);
        provider.login(context, params, "standard");
    }

    @Override
    public void verifyLogin(String UID, final boolean ignoreSession, final GigyaCallback<A> gigyaCallback) {
        requestRequiresValidSession(GigyaDefinitions.API.API_SET_ACCOUNT_INFO, gigyaCallback);
        final Map<String, Object> params = new HashMap<>();
        if (UID != null) {
            params.put("UID", UID);
        }
        params.put("include", "identities-all,loginIDs,profile,email,data");
        final GigyaApiRequest request = GigyaApiRequest.newInstance(_config, _sessionService, GigyaDefinitions.API.API_VERIFY_LOGIN, params, RestAdapter.POST);
        _apiService.send(request, false, new ApiService.IApiServiceResponse() {
            @Override
            public void onApiSuccess(GigyaApiResponse response) {
                if (response.getErrorCode() == 0) {
                    // No interruption support.
                    A parsed = response.parseTo(_accountService.getAccountScheme());
                    updateWithNewSession(response);
                    updateCachedAccount(response);
                    gigyaCallback.onSuccess(parsed);
                } else {
                    gigyaCallback.onError(GigyaError.fromResponse(response));
                }
            }

            @Override
            public void onApiError(GigyaError gigyaError) {
                gigyaCallback.onError(gigyaError);
            }
        });
    }

    @Override
    public void nativeSocialLogin(Map<String, Object> params, final GigyaLoginCallback<A> loginCallback, final Runnable optionalCompletionHandler) {
        requestRequiresGMID(GigyaDefinitions.API.API_NOTIFY_SOCIAL_LOGIN, loginCallback);
        if (params.containsKey("loginMode")) {
            final String linkMode = (String) params.get("loginMode");
            if (ObjectUtils.safeEquals(linkMode, "link")) {
                requestRequiresValidSession(GigyaDefinitions.API.API_NOTIFY_SOCIAL_LOGIN, loginCallback);
            }
        }
        GigyaApiRequest request = GigyaApiRequest.newInstance(_config, _sessionService, GigyaDefinitions.API.API_NOTIFY_SOCIAL_LOGIN, params, RestAdapter.POST);
        _apiService.send(request, false, new ApiService.IApiServiceResponse() {
            @Override
            public void onApiSuccess(GigyaApiResponse response) {
                handleAccountApiResponse(response, loginCallback, optionalCompletionHandler);
            }

            @Override
            public void onApiError(GigyaError gigyaError) {
                loginCallback.onError(gigyaError);
            }
        });
    }

    //region REGISTER

    @Override
    public void finalizeRegistration(Map<String, Object> params, final GigyaLoginCallback<A> loginCallback) {
        final GigyaApiRequest request = GigyaApiRequest.newInstance(_config, _sessionService, GigyaDefinitions.API.API_FINALIZE_REGISTRATION, params, RestAdapter.POST);
        _apiService.send(request, false, new ApiService.IApiServiceResponse() {
            @Override
            public void onApiSuccess(GigyaApiResponse response) {
                handleAccountApiResponse(response, loginCallback, null);
            }

            @Override
            public void onApiError(GigyaError gigyaError) {
                loginCallback.onError(gigyaError);
            }
        });
    }

    @Override
    public void getAccount(final GigyaCallback<A> gigyaCallback) {
        requestRequiresValidSession(GigyaDefinitions.API.API_GET_ACCOUNT_INFO, gigyaCallback);
        if (_accountService.isCachedAccount()) {
            gigyaCallback.onSuccess(_accountService.getAccount());
            return;
        }
        GigyaApiRequest request = GigyaApiRequest.newInstance(_config,_sessionService, GigyaDefinitions.API.API_GET_ACCOUNT_INFO, null, RestAdapter.POST);
        _apiService.send(request, false, new ApiService.IApiServiceResponse() {
            @Override
            public void onApiSuccess(GigyaApiResponse response) {
                if (response.getErrorCode() == 0) {
                    updateCachedAccount(response);
                } else {
                    gigyaCallback.onError(GigyaError.fromResponse(response));
                }
            }

            @Override
            public void onApiError(GigyaError gigyaError) {
                gigyaCallback.onError(gigyaError);
            }
        });
    }

    @Override
    public void setAccount(A updatedAccount, final GigyaCallback<A> gigyaCallback) {
        requestRequiresValidSession(GigyaDefinitions.API.API_SET_ACCOUNT_INFO, gigyaCallback);
        final Map<String, Object> params = _accountService.calculateDiff(new Gson(), _accountService.getAccount(), updatedAccount);
        GigyaApiRequest request = GigyaApiRequest.newInstance(_config, _sessionService, GigyaDefinitions.API.API_SET_ACCOUNT_INFO, params, RestAdapter.POST);
        _apiService.send(request, false, new ApiService.IApiServiceResponse() {
            @Override
            public void onApiSuccess(GigyaApiResponse response) {
                if (response.getErrorCode() == 0) {
                    // Invalidate cached account and call getAccount API.
                    _accountService.invalidateAccount();
                    getAccount(gigyaCallback);
                } else {
                    gigyaCallback.onError(GigyaError.fromResponse(response));
                }
            }

            @Override
            public void onApiError(GigyaError gigyaError) {
                gigyaCallback.onError(gigyaError);
            }
        });
    }

    @Override
    public void refreshNativeProviderSession(Map<String, Object> params, final IProviderPermissionsCallback providerPermissionsCallback) {
        final GigyaApiRequest request = GigyaApiRequest.newInstance(_config, _sessionService, GigyaDefinitions.API.API_REFRESH_PROVIDER_SESSION, params, RestAdapter.POST);
        _apiService.send(request, false, new ApiService.IApiServiceResponse() {
            @Override
            public void onApiSuccess(GigyaApiResponse response) {
                if (response.getErrorCode() == 0) {
                    _accountService.invalidateAccount();
                    providerPermissionsCallback.granted();
                } else {
                    final GigyaError error = GigyaError.fromResponse(response);
                    providerPermissionsCallback.failed(error.getLocalizedMessage());
                }
            }

            @Override
            public void onApiError(GigyaError gigyaError) {
                providerPermissionsCallback.failed(gigyaError.getLocalizedMessage());
            }
        });
    }

    //endregion


//    @Override
//    public void notifyLogin(String providerSessions, final GigyaLoginCallback<R> loginCallback) {
//        final Map<String, Object> params = new HashMap<>();
//        params.put("providerSessions", providerSessions);
//        final GigyaApiRequest apiRequest = generateRequest(GigyaDefinitions.API.API_NOTIFY_LOGIN, params, RestAdapter.POST);
//        adapterSend(apiRequest, false, _accountService.getAccountScheme(), new IApiAdapterResponse<R>() {
//            @Override
//            public void onApiAdapterSuccess(GigyaApiResponse apiResponse, R response) {
//                if (isSupportedInterruption(apiResponse.getErrorCode())) {
//                    _interruptionsHandler.resolve(apiResponse, loginCallback);
//                } else {
//                    updateWithNewSession(apiResponse);
//                    updateCachedAccount(apiResponse);
//                    loginCallback.onSuccess(response);
//                }
//            }
//
//            @Override
//            public void onApiAdapterError(GigyaApiResponse apiResponse) {
//                if (isSupportedInterruption(apiResponse.getErrorCode())) {
//                    _interruptionsHandler.resolve(apiResponse, loginCallback);
//                } else {
//                    loginCallback.onError(GigyaError.fromResponse(apiResponse));
//                }
//            }
//
//            @Override
//            public void onApiAdapterNetworkError(GigyaError error) {
//                loginCallback.onError(error);
//            }
//        });
//    }
//
//    @Override
//    public void nativeSocialLogin(Map<String, Object> params, final GigyaLoginCallback<R> loginCallback, final Runnable optionalCompletionHandler) {
//        if (params.containsKey("loginMode")) {
//            final String linkMode = (String) params.get("loginMode");
//            if (ObjectUtils.safeEquals(linkMode, "link")) {
//                requestRequiresValidSession(GigyaDefinitions.API.API_NOTIFY_SOCIAL_LOGIN, loginCallback);
//            }
//        }
//        final GigyaApiRequest apiRequest = generateRequest(GigyaDefinitions.API.API_NOTIFY_SOCIAL_LOGIN, params, RestAdapter.POST);
//        adapterSend(apiRequest, false, _accountService.getAccountScheme(), new IApiAdapterResponse<R>() {
//            @Override
//            public void onApiAdapterSuccess(GigyaApiResponse apiResponse, R response) {
//                if (isSupportedInterruption(apiResponse.getErrorCode())) {
//                    _interruptionsHandler.resolve(apiResponse, loginCallback);
//                } else {
//                    updateWithNewSession(apiResponse);
//                    updateCachedAccount(apiResponse);
//                    if (optionalCompletionHandler != null) {
//                        optionalCompletionHandler.run();
//                    }
//                    loginCallback.onSuccess(response);
//                }
//            }
//
//            @Override
//            public void onApiAdapterError(GigyaApiResponse apiResponse) {
//                if (isSupportedInterruption(apiResponse.getErrorCode())) {
//                    _interruptionsHandler.resolve(apiResponse, loginCallback);
//                } else {
//                    loginCallback.onError(GigyaError.fromResponse(apiResponse));
//                }
//            }
//
//            @Override
//            public void onApiAdapterNetworkError(GigyaError error) {
//                loginCallback.onError(error);
//            }
//        });
//    }
//
//    //endregion
//
//    //region REGISTER
//
//    @Override
//    public void register(final Map<String, Object> params, final GigyaLoginCallback<R> loginCallback) {
//        requestRequiresGMID(GigyaDefinitions.API.API_INIT_REGISTRATION, loginCallback);
//        // #1 Chain init registration.
//        GigyaApiRequest apiRequest = generateRequest(GigyaDefinitions.API.API_INIT_REGISTRATION, params, RestAdapter.POST);
//        adapterSend(apiRequest, false, GigyaApiResponse.class, new IApiAdapterResponse<GigyaApiResponse>() {
//            @Override
//            public void onApiAdapterSuccess(GigyaApiResponse apiResponse, GigyaApiResponse response) {
//                final String regToken = apiResponse.getField("regToken", String.class);
//                if (regToken != null) {
//                    params.put("regToken", regToken);
//                    params.put("finalizeRegistration", true);
//                } else {
//                    GigyaLogger.error(LOG_TAG, "register: Init registration produced null regToken");
//                    loginCallback.onError(GigyaError.generalError());
//                    return;
//                }
//                // #2 Chain login.
//                GigyaApiRequest apiRequest = generateRequest(GigyaDefinitions.API.API_REGISTER, params, RestAdapter.POST);
//                adapterSend(apiRequest, false, _accountService.getAccountScheme(), new IApiAdapterResponse<R>() {
//                    @Override
//                    public void onApiAdapterSuccess(GigyaApiResponse apiResponse, R response) {
//                        if (isSupportedInterruption(apiResponse.getErrorCode())) {
//                            _interruptionsHandler.resolve(apiResponse, loginCallback);
//                        } else {
//                            updateWithNewSession(apiResponse);
//                            updateCachedAccount(apiResponse);
//                            loginCallback.onSuccess(response);
//                        }
//                    }
//
//                    @Override
//                    public void onApiAdapterError(GigyaApiResponse apiResponse) {
//                        if (isSupportedInterruption(apiResponse.getErrorCode())) {
//                            _interruptionsHandler.resolve(apiResponse, loginCallback);
//                        } else {
//                            loginCallback.onError(GigyaError.fromResponse(apiResponse));
//                        }
//                    }
//
//                    @Override
//                    public void onApiAdapterNetworkError(GigyaError error) {
//                        loginCallback.onError(error);
//                    }
//                });
//            }
//
//            @Override
//            public void onApiAdapterError(GigyaApiResponse apiResponse) {
//                loginCallback.onError(GigyaError.fromResponse(apiResponse));
//            }
//
//            @Override
//            public void onApiAdapterNetworkError(GigyaError error) {
//                loginCallback.onError(error);
//            }
//        });
//    }
//
//    @Override
//    public void finalizeRegistration(String regToken, final GigyaLoginCallback<R> loginCallback) {
//        final Map<String, Object> params = ObjectUtils.mapOf(Arrays.asList(
//                new Pair<String, Object>("regToken", regToken),
//                new Pair<String, Object>("include", "profile,data,emails,subscriptions,preferences"),
//                new Pair<String, Object>("includeUserInfo", "true")));
//        GigyaApiRequest apiRequest = generateRequest(GigyaDefinitions.API.API_FINALIZE_REGISTRATION, params, RestAdapter.POST);
//        adapterSend(apiRequest, false, _accountService.getAccountScheme(), new IApiAdapterResponse<R>() {
//            @Override
//            public void onApiAdapterSuccess(GigyaApiResponse apiResponse, R response) {
//                if (isSupportedInterruption(apiResponse.getErrorCode())) {
//                    _interruptionsHandler.resolve(apiResponse, loginCallback);
//                } else {
//                    updateWithNewSession(apiResponse);
//                    updateCachedAccount(apiResponse);
//                    loginCallback.onSuccess(response);
//                }
//            }
//
//            @Override
//            public void onApiAdapterError(GigyaApiResponse apiResponse) {
//                if (isSupportedInterruption(apiResponse.getErrorCode())) {
//                    _interruptionsHandler.resolve(apiResponse, loginCallback);
//                } else {
//                    loginCallback.onError(GigyaError.fromResponse(apiResponse));
//                }
//            }
//
//            @Override
//            public void onApiAdapterNetworkError(GigyaError error) {
//                loginCallback.onError(error);
//            }
//        });
//    }
//
//    //endregion
//
//    //region ACCOUNT
//
//    @Override
//    public void getAccount(final GigyaCallback<R> gigyaCallback) {
//        requestRequiresValidSession(GigyaDefinitions.API.API_GET_ACCOUNT_INFO, gigyaCallback);
//        if (_accountService.isCachedAccount()) {
//            gigyaCallback.onSuccess((R) _accountService.getAccount());
//            return;
//        }
//        GigyaApiRequest apiRequest = generateRequest(GigyaDefinitions.API.API_GET_ACCOUNT_INFO, null, RestAdapter.POST);
//        adapterSend(apiRequest, false, _accountService.getAccountScheme(), new IApiAdapterResponse<R>() {
//            @Override
//            public void onApiAdapterSuccess(GigyaApiResponse apiResponse, R response) {
//                updateCachedAccount(apiResponse);
//                gigyaCallback.onSuccess(response);
//            }
//
//            @Override
//            public void onApiAdapterError(GigyaApiResponse apiResponse) {
//                gigyaCallback.onError(GigyaError.fromResponse(apiResponse));
//            }
//
//            @Override
//            public void onApiAdapterNetworkError(GigyaError error) {
//                gigyaCallback.onError(error);
//            }
//        });
//    }
//
//    @Override
//    public void setAccount(R updatedAccount, final GigyaCallback<R> gigyaCallback) {
//        requestRequiresValidSession(GigyaDefinitions.API.API_SET_ACCOUNT_INFO, gigyaCallback);
//        final Map<String, Object> params = _accountService.calculateDiff(new Gson(), _accountService.getAccount(), updatedAccount);
//        final GigyaApiRequest apiRequest = generateRequest(GigyaDefinitions.API.API_SET_ACCOUNT_INFO, params, RestAdapter.POST);
//        adapterSend(apiRequest, false, _accountService.getAccountScheme(), new IApiAdapterResponse<R>() {
//            @Override
//            public void onApiAdapterSuccess(GigyaApiResponse apiResponse, R response) {
//                _accountService.invalidateAccount();
//                getAccount(gigyaCallback);
//            }
//
//            @Override
//            public void onApiAdapterError(GigyaApiResponse apiResponse) {
//                gigyaCallback.onError(GigyaError.fromResponse(apiResponse));
//            }
//
//            @Override
//            public void onApiAdapterNetworkError(GigyaError error) {
//                gigyaCallback.onError(error);
//            }
//        });
//    }
//
//    //endregion
//
//    //region MISC
//
//    @Override
//    public void forgotPassword(String loginId, GigyaCallback<GigyaApiResponse> callback) {
//        Map<String, Object> params = new HashMap<>();
//        params.put("loginID", loginId);
//        send(GigyaDefinitions.API.API_RESET_PASSWORD, params, RestAdapter.POST, callback);
//    }
//
//    @Override
//    public void refreshNativeProviderSession(String providerSession, final IProviderPermissionsCallback providerPermissionsCallback) {
//        final Map<String, Object> params = new HashMap<>();
//        params.put("providerSession", providerSession);
//        final GigyaApiRequest apiRequest = generateRequest(GigyaDefinitions.API.API_REFRESH_PROVIDER_SESSION, params, RestAdapter.POST);
//        adapterSend(apiRequest, false, GigyaApiResponse.class, new IApiAdapterResponse<GigyaApiResponse>() {
//            @Override
//            public void onApiAdapterSuccess(GigyaApiResponse apiResponse, GigyaApiResponse response) {
//                providerPermissionsCallback.granted();
//                // Next get account should fetch new data.
//                _accountService.invalidateAccount();
//            }
//
//            @Override
//            public void onApiAdapterError(GigyaApiResponse apiResponse) {
//                final GigyaError error = GigyaError.fromResponse(apiResponse);
//                providerPermissionsCallback.failed(error.getLocalizedMessage());
//            }
//
//            @Override
//            public void onApiAdapterNetworkError(GigyaError error) {
//                providerPermissionsCallback.failed(error.getLocalizedMessage());
//            }
//        });
//    }
//
//    //endregion
//
//    //region CONDITIONALS & GENERAL RESPONSE HANDLING
//

//
//    //endregion
//
//    //region OBSERVER
//
//    @Override
//    public void update(Observable observable, Object arg) {
//        if (arg instanceof ApiObservable.ObservableApi) {
//            // Send the request.
//            final String api = ((ApiObservable.ObservableApi) arg).getApi();
//            final Map<String, Object> params = ((ApiObservable.ObservableApi) arg).getParams();
//            final GigyaCallback gigyaCallback = ((ApiObservable.ObservableApi) arg).getGigyaCallback();
//            switch (api) {
//                case GigyaDefinitions.API.API_NOTIFY_SOCIAL_LOGIN:
//
//                    break;
//            }
//
//            observable.deleteObservers();
//        }
//    }
//
//    //endregion
//
//    //region ADAPTER SEND
//
//    private <V> void adapterSend(GigyaApiRequest apiRequest, boolean blocking, final Class<V> scheme, final IApiAdapterResponse<V> responseListener) {
//        _restAdapter.send(apiRequest, blocking, new IRestAdapterCallback() {
//            @Override
//            public void onResponse(String jsonResponse) {
//                try {
//                    final GigyaApiResponse apiResponse = new GigyaApiResponse(jsonResponse);
//                    final int statusCode = apiResponse.getStatusCode();
//                    if (statusCode == GigyaApiResponse.OK) {
//                        responseListener.onApiAdapterSuccess(apiResponse, handleApiResponse(apiResponse, scheme));
//                    } else {
//                        responseListener.onApiAdapterError(apiResponse);
//                    }
//                } catch (Exception ex) {
//                    ex.printStackTrace();
//                }
//            }
//
//            @Override
//            public void onError(GigyaError gigyaError) {
//                responseListener.onApiAdapterNetworkError(gigyaError);
//            }
//        });
//    }
//
//
//    private interface IApiAdapterResponse<V> {
//
//        void onApiAdapterSuccess(GigyaApiResponse apiResponse, V response);
//
//        void onApiAdapterError(GigyaApiResponse apiResponse);
//
//        void onApiAdapterNetworkError(GigyaError error);
//    }
//
//    private <V> V handleApiResponse(GigyaApiResponse apiResponse, Class<V> scheme) {
//        try {
//            if (scheme == null) {
//                return (V) apiResponse;
//            } else {
//                return new Gson().fromJson(apiResponse.asJson(), scheme);
//            }
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//        return null;
//    }
//
//    //endregion
//
//    //region REQUEST GENERATION
//
//    private GigyaApiRequest generateRequest(String api, Map<String, Object> params, int requestMethod) {
//        TreeMap<String, Object> urlParams = new TreeMap<>();
//        if (params != null) {
//            urlParams.putAll(params);
//        }
//
//        // Add general parameters.
//        urlParams.put("sdk", Gigya.VERSION);
//        urlParams.put("targetEnv", "mobile");
//        urlParams.put("httpStatusCodes", false);
//        urlParams.put("format", "json");
//
//        // Add configuration parameters
//        final String gmid = _config.getGmid();
//        if (gmid != null) {
//            urlParams.put("gmid", gmid);
//        }
//        final String ucid = _config.getUcid();
//        if (ucid != null) {
//            urlParams.put("ucid", ucid);
//        }
//        // Add authentication parameters.
//        if (_sessionService.isValid()) {
//            @SuppressWarnings("ConstantConditions") final String sessionToken = _sessionService.getSession().getSessionToken();
//            urlParams.put("oauth_token", sessionToken);
//            final String sessionSecret = _sessionService.getSession().getSessionSecret();
//            AuthUtils.addAuthenticationParameters(sessionSecret,
//                    requestMethod,
//                    UrlUtils.getBaseUrl(api, _config.getApiDomain()),
//                    urlParams);
//        } else {
//            urlParams.put("ApiKey", _config.getApiKey());
//        }
//
//        GigyaLogger.debug(LOG_TAG, "Request parameters:\n" + urlParams.toString());
//
//        // Encode url & generate encoded parameters.
//        final String encodedParams = UrlUtils.buildEncodedQuery(urlParams);
//        final String url = UrlUtils.getBaseUrl(api, _config.getApiDomain()) + (requestMethod == RestAdapter.GET ? "?" + encodedParams : "");
//
//        // Generate new GigyaApiRequest entity.
//        return new GigyaApiRequest(url, requestMethod == RestAdapter.POST ? encodedParams : null, requestMethod, api);
//    }
//
//    //endregion
}
