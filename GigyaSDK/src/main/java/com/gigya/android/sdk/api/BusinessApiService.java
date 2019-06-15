package com.gigya.android.sdk.api;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaDefinitions;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.account.IAccountService;
import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.interruption.IInterruptionResolverFactory;
import com.gigya.android.sdk.interruption.tfa.models.TFACompleteVerificationModel;
import com.gigya.android.sdk.interruption.tfa.models.TFAGetEmailsModel;
import com.gigya.android.sdk.interruption.tfa.models.TFAGetRegisteredPhoneNumbersModel;
import com.gigya.android.sdk.interruption.tfa.models.TFAInitModel;
import com.gigya.android.sdk.interruption.tfa.models.TFAProvidersModel;
import com.gigya.android.sdk.interruption.tfa.models.TFATotpRegisterModel;
import com.gigya.android.sdk.interruption.tfa.models.TFAVerificationCodeModel;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.adapter.RestAdapter;
import com.gigya.android.sdk.providers.IProviderFactory;
import com.gigya.android.sdk.providers.IProviderPermissionsCallback;
import com.gigya.android.sdk.providers.provider.IProvider;
import com.gigya.android.sdk.session.ISessionService;
import com.gigya.android.sdk.session.SessionInfo;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

/**
 * Service responsible for sending and processing pre-defined API requests
 *
 * @param <A> Typed account instance (extends GigyaAccount).
 */
public class BusinessApiService<A extends GigyaAccount> implements IBusinessApiService<A> {

    private static final String LOG_TAG = "BusinessApiService";

    // Dependencies.
    final private ISessionService _sessionService;
    final private IAccountService<A> _accountService;
    final private IApiService _apiService;
    final private IApiRequestFactory _reqFactory;
    final private IProviderFactory _providerFactory;
    final private IInterruptionResolverFactory _interruptionsHandler;

    public BusinessApiService(ISessionService sessionService,
                              IAccountService<A> accountService,
                              IApiService apiService,
                              IApiRequestFactory requestFactory,
                              IProviderFactory providerFactory,
                              IInterruptionResolverFactory interruptionsHandler) {
        _sessionService = sessionService;
        _accountService = accountService;
        _apiService = apiService;
        _reqFactory = requestFactory;
        _providerFactory = providerFactory;
        _interruptionsHandler = interruptionsHandler;
    }


    //region CONDITIONALS & HELPERS

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

    private void handleAccountApiResponse(GigyaApiResponse response, GigyaLoginCallback<A> loginCallback) {
        final int errorCode = response.getErrorCode();
        if (errorCode != 0) {
            // Handle interruption.
            _interruptionsHandler.resolve(response, loginCallback);
        } else {
            // Parse & success.
            A parsed = response.parseTo(_accountService.getAccountSchema());
            updateWithNewSession(response);
            updateCachedAccount(response);
            loginCallback.onSuccess(parsed);
        }
    }

    //endregion

    //region SEND REQUEST

    /**
     * Base API send request initiator.
     *
     * @param api           Requested API.
     * @param params        Requested parameters map.
     * @param requestMethod HTTP request method {@link RestAdapter}
     * @param clazz         Requested Typed response class.
     * @param gigyaCallback Response callback.
     * @param <V>           Typed response class.
     */
    @Override
    public <V> void send(String api, Map<String, Object> params, int requestMethod, final Class<V> clazz, final GigyaCallback<V> gigyaCallback) {
        final GigyaApiRequest request = _reqFactory.create(api, params, requestMethod);
        _apiService.send(request, false, new ApiService.IApiServiceResponse() {
            @Override
            public void onApiSuccess(GigyaApiResponse response) {
                if (response.getErrorCode() == 0) {
                    if (clazz == GigyaApiResponse.class) {
                        gigyaCallback.onSuccess((V) response);
                    } else {
                        V parsed = response.parseTo(clazz);
                        gigyaCallback.onSuccess(parsed);
                    }
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

    //endregion

    //region LOGOUT

    /**
     * Request to log out of the current active session.
     *
     * @see <a href="https://developers.gigya.com/display/GD/accounts.logout+REST">accounts.logout REST</a>
     */
    @Override
    public void logout(final GigyaCallback<GigyaApiResponse> gigyaCallback) {
        final GigyaApiRequest request = _reqFactory.create(GigyaDefinitions.API.API_LOGOUT, null, RestAdapter.GET);
        _apiService.send(request, false, new ApiService.IApiServiceResponse() {

            @Override
            public void onApiSuccess(GigyaApiResponse response) {
                GigyaLogger.error(LOG_TAG, "logOut: Success");
                if (gigyaCallback != null) {
                    gigyaCallback.onSuccess(response);
                }

            }

            @Override
            public void onApiError(GigyaError gigyaError) {
                GigyaLogger.error(LOG_TAG, "logOut: Failed");
                if (gigyaCallback != null) {
                    gigyaCallback.onError(gigyaError);
                }
            }
        });
    }

    //endregion

    //region LOGIN

    /**
     * Request login given map of parameters.
     *
     * @param params             Parameter map
     * @param gigyaLoginCallback Login response callback.
     * @see <a href="https://developers.gigya.com/display/GD/accounts.login+REST">accounts.login REST</a>
     */
    @Override
    public void login(Map<String, Object> params, final GigyaLoginCallback<A> gigyaLoginCallback) {
        final GigyaApiRequest request = _reqFactory.create(GigyaDefinitions.API.API_LOGIN, params, RestAdapter.POST);
        _apiService.send(request, false, new ApiService.IApiServiceResponse() {
            @Override
            public void onApiSuccess(GigyaApiResponse response) {
                handleAccountApiResponse(response, gigyaLoginCallback);
            }

            @Override
            public void onApiError(GigyaError gigyaError) {
                gigyaLoginCallback.onError(gigyaError);
            }
        });
    }

    /**
     * Request login to specific social provider.
     * Will begin a login flow comprised of two steps.
     * 1. Social login with provider.
     * 2. Login with the Gigya server.
     *
     * @param socialProvider     Requested social provider   {@link GigyaDefinitions.Providers}
     * @param params             Request parameters.
     * @param gigyaLoginCallback Login response callback.
     */
    @Override
    public void login(@GigyaDefinitions.Providers.SocialProvider String socialProvider, Map<String, Object> params, GigyaLoginCallback<A> gigyaLoginCallback) {
        params.put("provider", socialProvider);  // Needed for non native providers.
        IProvider provider = _providerFactory.providerFor(socialProvider, gigyaLoginCallback);
        if (params.containsKey("regToken")) {
            final String regToken = (String) params.get("regToken");
            provider.setRegToken(regToken);
        }
        String loginMode = "standard";
        if (params.containsKey("loginMode")) {
            loginMode = (String) params.get("loginMode");
        }
        provider.login(params, loginMode);
    }

    /**
     * Request to verify the current session state.
     *
     * @param UID           Current user UID.
     * @param gigyaCallback Response callback.
     * @see <a href="https://developers.gigya.com/display/GD/accounts.verifyLogin+REST">accounts.verifyLogin REST</a>
     */
    @Override
    public void verifyLogin(String UID, final GigyaCallback<A> gigyaCallback) {
        if (!_sessionService.isValid()) {
            GigyaLogger.error(LOG_TAG, "Action requires a valid session");
            if (gigyaCallback != null) {
                gigyaCallback.onError(GigyaError.unauthorizedUser());
            }
        }
        final Map<String, Object> params = new HashMap<>();
        if (UID != null) {
            params.put("UID", UID);
        }
        params.put("include", "identities-all,loginIDs,profile,email,data");
        final GigyaApiRequest request = _reqFactory.create(GigyaDefinitions.API.API_VERIFY_LOGIN, params, RestAdapter.POST);
        _apiService.send(request, false, new ApiService.IApiServiceResponse() {
            @Override
            public void onApiSuccess(GigyaApiResponse response) {
                if (response.getErrorCode() == 0) {
                    // No interruption support.
                    A parsed = response.parseTo(_accountService.getAccountSchema());
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

    /**
     * Login to with social provider when the provider session is available (obtained via specific provider login process).
     *
     * @param params                    Request parameters/
     * @param gigyaLoginCallback        Login response callback.
     * @param optionalCompletionHandler additional completion handler Runnable.
     */
    @Override
    public void notifyNativeSocialLogin(Map<String, Object> params, final GigyaLoginCallback<A> gigyaLoginCallback, final Runnable optionalCompletionHandler) {
        final GigyaApiRequest request = _reqFactory.create(GigyaDefinitions.API.API_NOTIFY_SOCIAL_LOGIN, params, RestAdapter.POST);
        _apiService.send(request, false, new ApiService.IApiServiceResponse() {
            @Override
            public void onApiSuccess(GigyaApiResponse response) {
                if (response.getErrorCode() == 0) {
                    updateWithNewSession(response); // Update with new session.
                    getAccount(gigyaLoginCallback); // Request account details. Will need to change when the endpoint will be completed.
                    if (optionalCompletionHandler != null) {
                        optionalCompletionHandler.run();
                    }
                } else {
                    handleAccountApiResponse(response, gigyaLoginCallback);
                }
            }

            @Override
            public void onApiError(GigyaError gigyaError) {
                gigyaLoginCallback.onError(gigyaError);
            }
        });
    }

    //endregion

    //region REGISTER

    /**
     * Finalize current registration process.
     *
     * @param params             Request parameters.
     * @param gigyaLoginCallback Login response callback.
     * @see <a href="https://developers.gigya.com/display/GD/accounts.finalizeRegistration+REST">accounts.finalizeRegistration REST</a>
     */
    @Override
    public void finalizeRegistration(Map<String, Object> params, final GigyaLoginCallback<A> gigyaLoginCallback) {
        final GigyaApiRequest request = _reqFactory.create(GigyaDefinitions.API.API_FINALIZE_REGISTRATION, params, RestAdapter.POST);
        _apiService.send(request, false, new ApiService.IApiServiceResponse() {
            @Override
            public void onApiSuccess(GigyaApiResponse response) {
                handleAccountApiResponse(response, gigyaLoginCallback);
            }

            @Override
            public void onApiError(GigyaError gigyaError) {
                gigyaLoginCallback.onError(gigyaError);
            }
        });
    }

    /**
     * Register request given map of parameters.
     * Login flow is composed of two stages:
     * 1. Request to initialize the registration process.
     * 2. Actual registration process using the registration token obtained from previous request.
     * NOTE: registration set to finalize by default thus not requiring a call to finalize the registration.
     *
     * @param params             Request parameters.
     * @param gigyaLoginCallback Login response callback.
     * @see <a href="https://developers.gigya.com/display/GD/accounts.initRegistration+REST">accounts.initRegistration REST</a>
     * @see <a href="https://developers.gigya.com/display/GD/accounts.register+REST">accounts.register REST</a>
     */
    @Override
    public void register(final Map<String, Object> params, final GigyaLoginCallback<A> gigyaLoginCallback) {
        // #1 Chain init registration.
        final GigyaApiRequest initRequest = _reqFactory.create(GigyaDefinitions.API.API_INIT_REGISTRATION, params, RestAdapter.POST);
        _apiService.send(initRequest, false, new ApiService.IApiServiceResponse() {
            @Override
            public void onApiSuccess(GigyaApiResponse response) {
                if (response.getErrorCode() == 0) {
                    final String regToken = response.getField("regToken", String.class);
                    if (regToken != null) {
                        params.put("regToken", regToken);
                        params.put("finalizeRegistration", true);
                    } else {
                        GigyaLogger.error(LOG_TAG, "register: Init registration produced null regToken");
                        gigyaLoginCallback.onError(GigyaError.generalError());
                        return;
                    }
                    // #2 Chain login.
                    GigyaApiRequest regRequest = _reqFactory.create(GigyaDefinitions.API.API_REGISTER, params, RestAdapter.POST);
                    _apiService.send(regRequest, false, new ApiService.IApiServiceResponse() {
                        @Override
                        public void onApiSuccess(GigyaApiResponse response) {
                            handleAccountApiResponse(response, gigyaLoginCallback);
                        }

                        @Override
                        public void onApiError(GigyaError gigyaError) {
                            gigyaLoginCallback.onError(gigyaError);
                        }
                    });
                } else {
                    gigyaLoginCallback.onError(GigyaError.fromResponse(response));
                }
            }

            @Override
            public void onApiError(GigyaError gigyaError) {
                gigyaLoginCallback.onError(gigyaError);
            }
        });
    }

    //endregion

    //region ACCOUNT

    /**
     * Request account info for current active session.
     *
     * @param gigyaCallback Response callback.
     * @see <a href="https://developers.gigya.com/display/GD/accounts.getAccountInfo+REST">accounts.getAccountInfo REST</a>
     */
    @Override
    public void getAccount(final GigyaCallback<A> gigyaCallback) {
        getAccount(null, gigyaCallback);
    }

    @Override
    public void getAccount(final Map<String, Object> params, final GigyaCallback<A> gigyaCallback) {
        if (gigyaCallback == null) {
            // Callback restricted api call.
            return;
        }
        if (!_sessionService.isValid()) {
            GigyaLogger.error(LOG_TAG, "Action requires a valid session");
            gigyaCallback.onError(GigyaError.unauthorizedUser());
        }
        if (_accountService.isCachedAccount()) {
            gigyaCallback.onSuccess(_accountService.getAccount());
            return;
        }
        final GigyaApiRequest request = _reqFactory.create(GigyaDefinitions.API.API_GET_ACCOUNT_INFO, params, RestAdapter.GET);
        _apiService.send(request, false, new ApiService.IApiServiceResponse() {
            @Override
            public void onApiSuccess(GigyaApiResponse response) {
                if (response.getErrorCode() == 0) {
                    // Parse response & update account service.
                    A parsed = response.parseTo(_accountService.getAccountSchema());
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


    /**
     * Request account update for current active session.
     *
     * @param updatedAccount Updated account instance.
     * @param gigyaCallback  Response callback.
     * @see <a href="https://developers.gigya.com/display/GD/accounts.setAccountInfo+REST">accounts.setAccountInfo REST</a>
     */
    @Override
    public void setAccount(A updatedAccount, final GigyaCallback<A> gigyaCallback) {
        if (gigyaCallback == null) {
            // Callback restricted api call.
            return;
        }
        if (!_sessionService.isValid()) {
            GigyaLogger.error(LOG_TAG, "Action requires a valid session");
            gigyaCallback.onError(GigyaError.unauthorizedUser());
        }
        final Map<String, Object> params = _accountService.calculateDiff(new Gson(), _accountService.getAccount(), updatedAccount);
        final GigyaApiRequest request = _reqFactory.create(GigyaDefinitions.API.API_SET_ACCOUNT_INFO, params, RestAdapter.POST);
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

    /**
     * Request account update for current active session.
     *
     * @param params        Updated parameters.
     * @param gigyaCallback Response callback.
     * @see <a href="https://developers.gigya.com/display/GD/accounts.setAccountInfo+REST">accounts.setAccountInfo REST</a>
     */
    @Override
    public void setAccount(Map<String, Object> params, final GigyaCallback<A> gigyaCallback) {
        if (!_sessionService.isValid()) {
            GigyaLogger.error(LOG_TAG, "Action requires a valid session");
            if (gigyaCallback != null) {
                gigyaCallback.onError(GigyaError.unauthorizedUser());
            }
        }
        final GigyaApiRequest request = _reqFactory.create(GigyaDefinitions.API.API_SET_ACCOUNT_INFO, params, RestAdapter.POST);
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

    //endregion

    //region MISC

    // Non documented. For SDK use only.
    @Override
    public void refreshNativeProviderSession(Map<String, Object> params, final IProviderPermissionsCallback providerPermissionsCallback) {
        final GigyaApiRequest request = _reqFactory.create(GigyaDefinitions.API.API_REFRESH_PROVIDER_SESSION, params, RestAdapter.POST);
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

    /**
     * Issue a reset password request.
     *
     * @param loginId  Current login ID.
     * @param callback Response callback.
     * @see <a href="https://developers.gigya.com/display/GD/accounts.resetPassword+REST">accounts.resetPassword REST</a>
     */
    @Override
    public void forgotPassword(String loginId, final GigyaCallback<GigyaApiResponse> callback) {
        final Map<String, Object> params = new HashMap<>();
        params.put("loginID", loginId);
        final GigyaApiRequest request = _reqFactory.create(GigyaDefinitions.API.API_RESET_PASSWORD, params, RestAdapter.POST);
        _apiService.send(request, false, new ApiService.IApiServiceResponse() {
            @Override
            public void onApiSuccess(GigyaApiResponse response) {
                if (response.getErrorCode() == 0) {
                    callback.onSuccess(response);
                } else {
                    callback.onError(GigyaError.fromResponse(response));
                }
            }

            @Override
            public void onApiError(GigyaError gigyaError) {
                callback.onError(gigyaError);
            }
        });
    }

    /**
     * Request to add a social network connection to existing account.
     * Completion of this request will result in an updated login session.
     *
     * @param socialProvider     Requested social provider.
     * @param gigyaLoginCallback Login response callback.
     */
    @Override
    public void addConnection(String socialProvider, GigyaLoginCallback<A> gigyaLoginCallback) {
        if (!_sessionService.isValid()) {
            GigyaLogger.error(LOG_TAG, "Action requires a valid session");
            if (gigyaLoginCallback != null) {
                gigyaLoginCallback.onError(GigyaError.unauthorizedUser());
            }
        }
        final Map<String, Object> params = new HashMap<>();
        params.put("provider", socialProvider);  // Needed for non native providers.
        IProvider provider = _providerFactory.providerFor(socialProvider, gigyaLoginCallback);
        provider.login(params, "connect");
    }

    /**
     * Request to remove a social network connection from existing account.
     *
     * @param socialProvider Requested social provider.
     * @param gigyaCallback  Response callback.
     * @see <a href="https://developers.gigya.com/display/GD/socialize.removeConnection+REST">socialize.removeConnection REST</a>
     */
    @Override
    public void removeConnection(String socialProvider, final GigyaCallback<GigyaApiResponse> gigyaCallback) {
        if (!_sessionService.isValid()) {
            GigyaLogger.error(LOG_TAG, "Action requires a valid session");
            if (gigyaCallback != null) {
                gigyaCallback.onError(GigyaError.unauthorizedUser());
            }
        }
        final Map<String, Object> params = new HashMap<>();
        params.put("provider", socialProvider);
        final String UID = _accountService.getAccount().getUID();
        if (UID == null) {
            GigyaLogger.error(LOG_TAG, "removeConnection: UID null. UID field is required to remove connection");
            return;
        }
        params.put("UID", UID);
        final GigyaApiRequest request = _reqFactory.create(GigyaDefinitions.API.API_REMOVE_CONNECTION, params, RestAdapter.POST);
        _apiService.send(request, false, new ApiService.IApiServiceResponse() {
            @Override
            public void onApiSuccess(GigyaApiResponse response) {
                if (response.getErrorCode() == 0) {
                    gigyaCallback.onSuccess(response);
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

    //endregion

    //region INTERRUPTIONS RELATED

    @Override
    public void getConflictingAccounts(final String regToken, final GigyaCallback<GigyaApiResponse> callback) {
        final Map<String, Object> params = new HashMap<>();
        params.put("regToken", regToken);
        final GigyaApiRequest request = _reqFactory.create(GigyaDefinitions.API.API_GET_CONFLICTING_ACCOUNTS, params, RestAdapter.POST);
        _apiService.send(request, false, new ApiService.IApiServiceResponse() {
            @Override
            public void onApiSuccess(GigyaApiResponse response) {
                callback.onSuccess(response);
            }

            @Override
            public void onApiError(GigyaError gigyaError) {
                callback.onError(gigyaError);
            }
        });
    }

    @Override
    public void getTFAProviders(String regToken, final GigyaCallback<TFAProvidersModel> callback) {
        final Map<String, Object> params = new HashMap<>();
        params.put("regToken", regToken);
        send(GigyaDefinitions.API.API_TFA_GET_PROVIDERS, params, RestAdapter.GET, TFAProvidersModel.class, callback);
    }

    @Override
    public void initTFA(String regToken, String provider, String mode, GigyaCallback<TFAInitModel> callback) {
        final Map<String, Object> params = new HashMap<>();
        params.put("regToken", regToken);
        params.put("provider", provider);
        params.put("mode", mode);
        send(GigyaDefinitions.API.API_TFA_INIT, params, RestAdapter.POST, TFAInitModel.class, callback);
    }

    @Override
    public void finalizeTFA(String regToken, String gigyaAssertion, String providerAssertion, GigyaCallback<GigyaApiResponse> callback) {
        final Map<String, Object> params = new HashMap<>();
        params.put("regToken", regToken);
        params.put("gigyaAssertion", gigyaAssertion);
        params.put("providerAssertion", providerAssertion);
        send(GigyaDefinitions.API.API_TFA_FINALIZE, params, RestAdapter.POST, null, callback);
    }

    @Override
    public void registerTotp(String gigyaAssertion, GigyaCallback<TFATotpRegisterModel> callback) {
        final Map<String, Object> params = new HashMap<>();
        params.put("gigyaAssertion", gigyaAssertion);
        send(GigyaDefinitions.API.API_TFA_TOTP_REGISTER, params, RestAdapter.POST, TFATotpRegisterModel.class, callback);
    }

    @Override
    public void verifyTotp(String code, String gigyaAssertion, String sctToken, GigyaCallback<TFACompleteVerificationModel> callback) {
        final Map<String, Object> params = new HashMap<>();
        params.put("gigyaAssertion", gigyaAssertion);
        params.put("code", code);
        if (sctToken != null) {
            params.put("sctToken", sctToken);
        }
        send(GigyaDefinitions.API.API_TFA_TOTP_VERIFY, params, RestAdapter.POST, TFACompleteVerificationModel.class, callback);
    }

    @Override
    public void getRegisteredPhoneNumbers(String gigyaAssertion, GigyaCallback<TFAGetRegisteredPhoneNumbersModel> callback) {
        final Map<String, Object> params = new HashMap<>();
        params.put("gigyaAssertion", gigyaAssertion);
        send(GigyaDefinitions.API.API_TFA_PHONE_GET_REGISTERED_NUMBERS, params, RestAdapter.POST, TFAGetRegisteredPhoneNumbersModel.class, callback);
    }

    @Override
    public void registerPhoneNumber(String gigyaAssertion, String phoneNumber, String method, GigyaCallback<TFAVerificationCodeModel> callback) {
        final Map<String, Object> params = new HashMap<>();
        params.put("gigyaAssertion", gigyaAssertion);
        params.put("phone", phoneNumber);
        params.put("method", method);
        params.put("lang", "eng");
        send(GigyaDefinitions.API.API_TFA_PHONE_SEND_VERIFICATION_CODE, params, RestAdapter.POST, TFAVerificationCodeModel.class, callback);
    }

    @Override
    public void verifyPhoneNumber(String gigyaAssertion, String phoneId, String method, GigyaCallback<TFAVerificationCodeModel> callback) {
        final Map<String, Object> params = new HashMap<>();
        params.put("gigyaAssertion", gigyaAssertion);
        params.put("phoneID", phoneId);
        params.put("method", method);
        params.put("lang", "eng");
        send(GigyaDefinitions.API.API_TFA_PHONE_SEND_VERIFICATION_CODE, params, RestAdapter.POST, TFAVerificationCodeModel.class, callback);
    }

    @Override
    public void completePhoneVerification(String gigyaAssertion, String code, String phvToken, GigyaCallback<TFACompleteVerificationModel> callback) {
        final Map<String, Object> params = new HashMap<>();
        params.put("gigyaAssertion", gigyaAssertion);
        params.put("code", code);
        params.put("phvToken", phvToken);
        send(GigyaDefinitions.API.API_TFA_PHONE_COMPLETE_VERIFICATION, params, RestAdapter.POST, TFACompleteVerificationModel.class, callback);
    }

    @Override
    public void getRegisteredEmails(String gigyaAssertion, GigyaCallback<TFAGetEmailsModel> callback) {
        final Map<String, Object> params = new HashMap<>();
        params.put("gigyaAssertion", gigyaAssertion);
        send(GigyaDefinitions.API.API_TFA_EMAIL_GET_EMAILS, params, RestAdapter.GET, TFAGetEmailsModel.class, callback);
    }

    @Override
    public void verifyEmail(String emailId, String gigyaAssertion, GigyaCallback<TFAVerificationCodeModel> callback) {
        final Map<String, Object> params = new HashMap<>();
        params.put("emailID", emailId);
        params.put("gigyaAssertion", gigyaAssertion);
        params.put("lang", "eng");
        send(GigyaDefinitions.API.API_TFA_EMAIL_SEND_VERIFICATION_CODE, params, RestAdapter.GET, TFAVerificationCodeModel.class, callback);
    }

    @Override
    public void completeEmailVerification(String gigyaAssertion, String code, String phvToken, GigyaCallback<TFACompleteVerificationModel> callback) {
        final Map<String, Object> params = new HashMap<>();
        params.put("gigyaAssertion", gigyaAssertion);
        params.put("code", code);
        params.put("phvToken", phvToken);
        send(GigyaDefinitions.API.API_TFA_EMAIL_COMPLETE_VERIFICATION, params, RestAdapter.POST, TFACompleteVerificationModel.class, callback);

    }

    //endregion
}
