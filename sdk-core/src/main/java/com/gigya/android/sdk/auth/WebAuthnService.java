package com.gigya.android.sdk.auth;


import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static com.google.android.gms.fido.Fido.FIDO2_KEY_RESPONSE_EXTRA;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.api.ApiService;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.api.IApiRequestFactory;
import com.gigya.android.sdk.api.IApiService;
import com.gigya.android.sdk.api.IBusinessApiService;
import com.gigya.android.sdk.auth.models.WebAuthnAssertionResponse;
import com.gigya.android.sdk.auth.models.WebAuthnAttestationResponse;
import com.gigya.android.sdk.auth.models.WebAuthnGetOptionsModel;
import com.gigya.android.sdk.auth.models.WebAuthnGetOptionsResponseModel;
import com.gigya.android.sdk.auth.models.WebAuthnInitRegisterResponseModel;
import com.gigya.android.sdk.auth.models.WebAuthnKeyModel;
import com.gigya.android.sdk.auth.models.WebAuthnOptionsModel;
import com.gigya.android.sdk.auth.models.WebAuthnOptionsBinding;
import com.gigya.android.sdk.containers.IoCContainer;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.adapter.RestAdapter;
import com.gigya.android.sdk.persistence.IPersistenceService;
import com.gigya.android.sdk.session.ISessionService;
import com.gigya.android.sdk.session.SessionInfo;
import com.google.android.gms.fido.Fido;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebAuthnService<A extends GigyaAccount> implements IWebAuthnService<A> {

    public static final String LOG_TAG = "WebAuthnService";

    final private IOauthService oauthService;
    final private IFidoApiService fidoApiService;
    final private IApiRequestFactory requestFactory;
    final private ISessionService sessionService;
    final private IPersistenceService persistenceService;
    final private IBusinessApiService<A> businessApiService;

    private final IoCContainer container = new IoCContainer();

    public WebAuthnService(
            IOauthService oauthService,
            IFidoApiService fidoApiService,
            IApiRequestFactory requestFactory,
            ISessionService sessionService,
            IPersistenceService persistenceService,
            IBusinessApiService<A> businessApiService
    ) {
        this.oauthService = oauthService;
        this.fidoApiService = fidoApiService;
        this.requestFactory = requestFactory;
        this.sessionService = sessionService;
        this.persistenceService = persistenceService;
        this.businessApiService = businessApiService;
    }

    private enum WebAuthnApis {
        initRegisterCredentials("accounts.auth.fido.initRegisterCredentials"),
        getAssertionOptions("accounts.auth.fido.getAssertionOptions"),
        registerCredentials("accounts.auth.fido.registerCredentials"),
        verifyAssertion("accounts.auth.fido.verifyAssertion"),
        removeCredential("accounts.auth.fido.removeCredential");

        final String api;

        WebAuthnApis(String api) {
            this.api = api;
        }

        public String api() {
            return this.api;
        }
    }

    //region APIS

    /**
     *
     */
    private void initRegistration(GigyaCallback<WebAuthnInitRegisterResponseModel> callback) {
        this.businessApiService.send(
                WebAuthnApis.initRegisterCredentials.api,
                new HashMap<String, Object>(),
                RestAdapter.HttpMethod.POST.intValue(),
                WebAuthnInitRegisterResponseModel.class,
                callback
        );
    }

    /**
     *
     */
    private void registerCredentials(Map<String, Object> params, GigyaCallback<GigyaApiResponse> callback) {
        this.businessApiService.send(
                WebAuthnApis.registerCredentials.api,
                params,
                RestAdapter.HttpMethod.POST.intValue(),
                GigyaApiResponse.class,
                callback
        );
    }

    /**
     *
     */
    private void getAssertionOptions(GigyaCallback<WebAuthnGetOptionsResponseModel> callback) {
        this.businessApiService.send(
                WebAuthnApis.getAssertionOptions.api,
                new HashMap<String, Object>(),
                RestAdapter.HttpMethod.POST.intValue(),
                WebAuthnGetOptionsResponseModel.class,
                callback
        );
    }

    /**
     *
     */
    private void verifyAssertion(Map<String, Object> params, GigyaCallback<GigyaApiResponse> callback) {
        this.businessApiService.send(
                WebAuthnApis.verifyAssertion.api,
                params,
                RestAdapter.HttpMethod.POST.intValue(),
                GigyaApiResponse.class,
                callback
        );
    }

    /**
     *
     */
    private void removeCredential(Map<String, Object> params, GigyaCallback<GigyaApiResponse> callback) {
        this.businessApiService.send(
                WebAuthnApis.removeCredential.api,
                params,
                RestAdapter.HttpMethod.POST.intValue(),
                GigyaApiResponse.class,
                callback
        );
    }

    //endregion

    //region REGISTER

    /**
     * Initialize WebAuthn/Fido registration flow.
     * 1. initRegistration - connect with site to retrieve configuration data.
     * 2. Register with FIDO2 service - retrieve key data - Sender intent will be sent.
     *
     * @param resultLauncher Activity result launcher for intent sender request.
     * @param gigyaCallback  Result callback.
     */
    @SuppressLint("NewApi")
    @Override
    public void register(
            final ActivityResultLauncher<IntentSenderRequest> resultLauncher,
            final GigyaCallback<GigyaApiResponse> gigyaCallback
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            GigyaLogger.error(LOG_TAG, "WebAuthn/Fido service is available from Android M only");
            notifyError(new GigyaError(200001, "WebAuthn/Fido service is available from Android M only"));
            return;
        }

        // Register callback.
        container.bind(GigyaCallback.class, gigyaCallback);

        initRegistration(new GigyaCallback<WebAuthnInitRegisterResponseModel>() {
            @Override
            public void onSuccess(WebAuthnInitRegisterResponseModel webAuthnInitRegisterResponseModel) {
                if (webAuthnInitRegisterResponseModel == null) {
                    GigyaLogger.error(LOG_TAG,
                            "initRegistration webAuthnInitRegisterResponseModel parse error");
                    notifyError(new GigyaError(
                            200001,
                            "initRegistration webAuthnInitRegisterResponseModel parse error"
                    ));
                    return;
                }

                final WebAuthnOptionsModel options = webAuthnInitRegisterResponseModel.parseOptions();

                // Register helper model.
                container.bind(WebAuthnOptionsBinding.class,
                        new WebAuthnOptionsBinding(
                                webAuthnInitRegisterResponseModel.token,
                                FidoApiService.FidoApiServiceCodes.REQUEST_CODE_REGISTER.code(),
                                options.authenticatorSelection.authenticatorAttachment,
                                options.user
                        ));

                fidoApiService.register(resultLauncher, webAuthnInitRegisterResponseModel, new IFidoApiFlowError() {
                    @Override
                    public void onFlowFailedWith(GigyaError error) {
                        notifyError(error);
                    }
                });
            }


            @Override
            public void onError(GigyaError error) {
                GigyaLogger.error(LOG_TAG, "initRegistration error:\n" + error.getData());
                notifyError(error);
            }
        });
    }

    /**
     * Handle WebAuthn/Fido registration result.
     * <p>
     * 1. registerCredentials - register device.
     * 2. oauthservice - connect.
     *
     * @param optionsBinding
     * @param attestationResponse
     * @param credentialResponse
     */
    @SuppressLint("NewApi")
    public void onRegistration(final WebAuthnOptionsBinding optionsBinding, byte[] attestationResponse, byte[] credentialResponse) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            GigyaLogger.error(LOG_TAG, "WebAuthn/Fido service is available from Android M only");
            return;
        }

        final WebAuthnAttestationResponse webAuthnAttestationResponse =
                fidoApiService.onRegisterResponse(attestationResponse, credentialResponse);

        final Map<String, Object> params = new HashMap<>();
        params.put("attestation", webAuthnAttestationResponse.getAttestation());
        params.put("token", optionsBinding.token);

        registerCredentials(params, new GigyaCallback<GigyaApiResponse>() {
            @Override
            public void onSuccess(GigyaApiResponse response) {
                GigyaLogger.debug(LOG_TAG, "registerCredentials success:\n" + response.asJson());

                if (response.getErrorCode() != 0) {
                    GigyaLogger.error(LOG_TAG, "Response error: \n" + response.asJson());
                    notifyError(GigyaError.fromResponse(response));
                    return;
                }

                // Store public key.
                storePublicKey(
                        new WebAuthnKeyModel(
                                optionsBinding.userModel.name,
                                optionsBinding.userModel.displayName,
                                optionsBinding.type,
                                webAuthnAttestationResponse.rawIdBase64)

                );

                final String idToken = response.getField("idToken", String.class);
                if (idToken == null) {
                    GigyaLogger.error(LOG_TAG, "registerCredentials: missing idToken");
                    notifyError(new GigyaError(200001, "registerCredentials: missing idToken"));
                    return;
                }

                oauthService.connect(idToken, new ApiService.IApiServiceResponse() {
                    @Override
                    public void onApiSuccess(GigyaApiResponse response) {
                        GigyaLogger.debug(LOG_TAG, "connect api success response:\n" + response.asJson());

                        if (response.getErrorCode() != 0) {
                            GigyaLogger.error(LOG_TAG, "Response error: \n" + response.asJson());
                            notifyError(GigyaError.fromResponse(response));
                            return;
                        }

                        notifySuccess(response);
                    }

                    @Override
                    public void onApiError(GigyaError gigyaError) {
                        GigyaLogger.error(LOG_TAG, "connect api error: \n" + gigyaError.getData());
                        notifyError(gigyaError);
                    }
                });
            }

            @Override
            public void onError(GigyaError error) {
                GigyaLogger.error(LOG_TAG, "registerCredentials error:\n" + error.getData());
                notifyError(error);
            }
        });
    }

    //endregion

    //region LOGIN

    /**
     * Initialize WebAuthn/Fido login/signing flow.
     * 1. getAssertionOptions - retrieve relevant assertion verification data.
     * 2. Sign with FIDO2 service - retrieve key data - Sender intent will be sent.
     *
     * @param resultLauncher Activity result launcher for intent sender request.
     * @param gigyaCallback  Result callback.
     */
    @SuppressLint("NewApi")
    @Override
    public void login(
            final ActivityResultLauncher<IntentSenderRequest> resultLauncher,
            final GigyaLoginCallback<A> gigyaCallback) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            GigyaLogger.error(LOG_TAG, "WebAuthn/Fido service is available from Android M only");
            notifyError(new GigyaError(200001, "WebAuthn/Fido service is available from Android M only"));
            return;
        }

        // Register callback.
        container.bind(GigyaLoginCallback.class, gigyaCallback);

        getAssertionOptions(new GigyaCallback<WebAuthnGetOptionsResponseModel>() {
            @Override
            public void onSuccess(WebAuthnGetOptionsResponseModel webAuthnGetOptionsResponseModel) {
                GigyaLogger.debug(LOG_TAG, "getAssertionOptions success:\n");

                if (webAuthnGetOptionsResponseModel == null) {
                    GigyaLogger.error(LOG_TAG,
                            "getAssertionOptions webAuthnGetOptionsResponseModel parse error");
                    notifyError(
                            new GigyaError(200001,
                                    "getAssertionOptions webAuthnGetOptionsResponseModel parse error")
                    );
                    return;
                }

                final WebAuthnGetOptionsModel options = webAuthnGetOptionsResponseModel.parseOptions();

                // Register helper model.
                container.bind(WebAuthnOptionsBinding.class,
                        new WebAuthnOptionsBinding(
                                webAuthnGetOptionsResponseModel.token,
                                FidoApiService.FidoApiServiceCodes.REQUEST_CODE_SIGN.code()
                        ));

                // Load allowed keys.
                List<WebAuthnKeyModel> keys = getKeys();

                fidoApiService.sign(resultLauncher, webAuthnGetOptionsResponseModel, keys, new IFidoApiFlowError() {
                    @Override
                    public void onFlowFailedWith(GigyaError error) {
                        notifyError(error);
                    }
                });
            }

            @Override
            public void onError(GigyaError error) {
                GigyaLogger.error(LOG_TAG, "getAssertionOptions error:\n" + error.getData());
                notifyError(error);
            }

        });
    }

    /**
     * Handle WebAuthn/Fido login result.
     * <p>
     * 1. verifyAssertion - Verify assertion data.
     * 2. oauthservice - authorize.
     * 3. oauthservice - token.
     *
     * @param optionsBinding
     * @param assertionResponse
     * @param fido2Credential
     */
    @SuppressLint("NewApi")
    public void onLogin(
            final WebAuthnOptionsBinding optionsBinding,
            byte[] assertionResponse, byte[] fido2Credential) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            GigyaLogger.error(LOG_TAG, "WebAuthn/Fido service is available from Android M only");
            return;
        }

        final WebAuthnAssertionResponse webAuthnAssertionResponse =
                fidoApiService.onSignResponse(assertionResponse, fido2Credential);

        final Map<String, Object> params = new HashMap<>();
        params.put("authenticatorAssertion", webAuthnAssertionResponse.getAssertion());
        params.put("token", optionsBinding.token);

        verifyAssertion(params, new GigyaCallback<GigyaApiResponse>() {
            @Override
            public void onSuccess(GigyaApiResponse response) {
                GigyaLogger.debug(LOG_TAG, "verifyAssertion success:\n" + response.asJson());

                if (response.getErrorCode() != 0) {
                    GigyaLogger.error(LOG_TAG, "Response error: \n" + response.asJson());
                    notifyLoginError(GigyaError.fromResponse(response));
                    return;
                }

                final String idToken = response.getField("idToken", String.class);
                if (idToken == null) {
                    GigyaLogger.error(LOG_TAG, "verifyAssertion: missing idToken");
                    notifyError(new GigyaError(200001, "verifyAssertion: missing idToken"));
                    return;
                }

                // Authorize idToken.
                oauthService.authorize(idToken, new ApiService.IApiServiceResponse() {
                    @Override
                    public void onApiSuccess(GigyaApiResponse response) {
                        GigyaLogger.debug(LOG_TAG, "authorize api success response:\n" + response.asJson());

                        if (response.getErrorCode() != 0) {
                            GigyaLogger.error(LOG_TAG, "Response error: \n" + response.asJson());
                            notifyLoginError(GigyaError.fromResponse(response));
                            return;
                        }

                        if (response.contains("code")) {
                            final String code = response.getField("code", String.class);
                            oauthService.token(code, new ApiService.IApiServiceResponse() {
                                @Override
                                public void onApiSuccess(GigyaApiResponse response) {
                                    GigyaLogger.debug(LOG_TAG, "token api success response:\n" + response.asJson());

                                    if (response.getErrorCode() != 0) {
                                        GigyaLogger.error(LOG_TAG, "Response error: \n" + response.asJson());
                                        notifyLoginError(GigyaError.fromResponse(response));
                                        return;
                                    }

                                    // Session received. Update session service.
                                    notifySession(response);
                                }

                                @Override
                                public void onApiError(GigyaError gigyaError) {
                                    GigyaLogger.error(LOG_TAG, "token api error: \n" + gigyaError.getData());
                                    notifyLoginError(gigyaError);
                                }
                            });
                        }
                    }

                    @Override
                    public void onApiError(GigyaError gigyaError) {
                        GigyaLogger.error(LOG_TAG, "authorize api error: \n" + gigyaError.getData());
                        notifyLoginError(gigyaError);
                    }
                });
            }

            @Override
            public void onError(GigyaError error) {
                GigyaLogger.error(LOG_TAG, "verifyAssertion error:\n" + error.getData());
                notifyLoginError(error);
            }
        });
    }

    //endregion

    //region REVOKE

    /**
     * Initialize revoke key flow.
     * Revoking a key will perform its removal from Gigya services only. You cannot delete a fido key from the device
     * once it has been created.
     *
     * @param resultLauncher Activity result launcher for intent sender request.
     * @param gigyaCallback  Result callback.
     */
    @Override
    public void revoke(
            final String uid,
            final ActivityResultLauncher<IntentSenderRequest> resultLauncher,
            GigyaCallback<GigyaApiResponse> gigyaCallback) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            GigyaLogger.error(LOG_TAG, "WebAuthn/Fido service is available from Android M only");
            notifyError(new GigyaError(200001, "WebAuthn/Fido service is available from Android M only"));
            return;
        }

        // Register callback.
        container.bind(GigyaCallback.class, gigyaCallback);

//        getAssertionOptions(new ApiService.IApiServiceResponse() {
//            @Override
//            public void onApiSuccess(GigyaApiResponse response) {
//                GigyaLogger.debug(LOG_TAG, "getAssertionOptions success:\n" + response.asJson());
//
//                if (gigyaResponseError(response)) {
//                    return;
//                }
//
//                final WebAuthnGetOptionsResponseModel webAuthnGetOptionsResponseModel =
//                        response.parseTo(WebAuthnGetOptionsResponseModel.class);
//
//                if (webAuthnGetOptionsResponseModel == null) {
//                    GigyaLogger.error(LOG_TAG,
//                            "getAssertionOptions webAuthnGetOptionsResponseModel parse error");
//                    notifyError(
//                            new GigyaError(200001,
//                                    "getAssertionOptions webAuthnGetOptionsResponseModel parse error")
//                    );
//                    return;
//                }
//
//                // Load allowed keys.
//                List<WebAuthnKeyModel> keys = getKeys();
//
//                fidoApiService.sign(resultLauncher, webAuthnGetOptionsResponseModel, keys,
//                        new IFidoApiFlowError() {
//                            @Override
//                            public void onFlowFailedWith(GigyaError error) {
//                                notifyError(error);
//                            }
//                        });
//            }
//
//            @Override
//            public void onApiError(GigyaError gigyaError) {
//                GigyaLogger.error(LOG_TAG, "getAssertionOptions error:\n" + gigyaError.getData());
//            }
//        });
    }


    /**
     * Handle WebAuthn/Fido revoke key result.
     * <p>
     * 1. verifyAssertion - Verify assertion data.
     * 2. oauthservice - authorize.
     * 3. oauthservice - token.
     *
     * @param optionsBinding
     * @param assertionResponse
     * @param fido2Credential
     */
    private void onRevoke(
            final WebAuthnOptionsBinding optionsBinding,
            byte[] assertionResponse, byte[] fido2Credential) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            GigyaLogger.error(LOG_TAG, "WebAuthn/Fido service is available from Android M only");
            return;
        }

        final WebAuthnAssertionResponse webAuthnAssertionResponse =
                fidoApiService.onSignResponse(assertionResponse, fido2Credential);

        final Map<String, Object> params = new HashMap<>();
        params.put("credentialId", webAuthnAssertionResponse.rawIdBase64);
        removeCredential(params, new GigyaCallback<GigyaApiResponse>() {
            @Override
            public void onSuccess(GigyaApiResponse response) {
                GigyaLogger.debug(LOG_TAG, "removeCredential success response:\n" + response.asJson());

                if (response.getErrorCode() != 0) {
                    GigyaLogger.error(LOG_TAG, "Response error: \n" + response.asJson());
                    notifyError(GigyaError.fromResponse(response));
                    return;
                }

                notifySuccess(response);
            }

            @Override
            public void onError(GigyaError error) {
                GigyaLogger.error(LOG_TAG, "removeCredential error:\n" + error.getData());
                notifyError(error);
            }

        });
    }

    //endregion

    //region RESULT

    /**
     * Handle service activity sender result.
     * Make sure to initialize the ActivityResultLauncher before onCreate method or as a member variable.
     *
     * @param activityResult ActivityResult from sender intent.
     */
    @Override
    public void handleFidoResult(ActivityResult activityResult) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            GigyaLogger.error(LOG_TAG, "WebAuthn/Fido service is available from Android M only");
            return;
        }
        switch (activityResult.getResultCode()) {
            case RESULT_OK:
                final Intent data = activityResult.getData();
                if (data == null) {
                    GigyaLogger.error(LOG_TAG, "Fido result error : null intent");
                    notifyError(new GigyaError(200001, "Fido result error : null intent"));
                    return;
                }
                if (data.hasExtra(Fido.FIDO2_KEY_ERROR_EXTRA)) {
                    final GigyaError error = fidoApiService.onFidoError(data.getByteArrayExtra(Fido.FIDO2_KEY_ERROR_EXTRA));
                    notifyError(error);
                    return;
                } else if (data.hasExtra(FIDO2_KEY_RESPONSE_EXTRA)) {
                    byte[] fido2Response = data.getByteArrayExtra(FIDO2_KEY_RESPONSE_EXTRA);
                    byte[] fido2Credential = data.getByteArrayExtra(Fido.FIDO2_KEY_CREDENTIAL_EXTRA);

                    final WebAuthnOptionsBinding tokenBinding = getWebAuthnOptionsBinding();
                    if (tokenBinding == null) {
                        GigyaLogger.error(LOG_TAG, "Failed to fetch options token from container");
                        notifyError(new GigyaError(200001, "Failed to fetch options token from container"));
                        return;
                    }

                    if (tokenBinding.requestCode == FidoApiService.FidoApiServiceCodes.REQUEST_CODE_REGISTER.code()) {
                        onRegistration(tokenBinding, fido2Response, fido2Credential);
                    } else if (tokenBinding.requestCode == FidoApiService.FidoApiServiceCodes.REQUEST_CODE_SIGN.code()) {
                        onLogin(tokenBinding, fido2Response, fido2Credential);
                    } else if (tokenBinding.requestCode == FidoApiService.FidoApiServiceCodes.REQUEST_CODE_REVOKE.code()) {
                        onRevoke(tokenBinding, fido2Response, fido2Credential);
                    }
                }
                break;
            case RESULT_CANCELED:
                GigyaLogger.error(LOG_TAG, "Fido result error: result canceled");
                notifyError(new GigyaError(200001, "Fido result error: result canceled"));
                break;
            default:
                break;
        }
    }

    //endregion

    /**
     * Clear callback container from flow callback references.
     */
    private void clearContainerCallbacks() {
        container.clear();
    }

    private WebAuthnOptionsBinding getWebAuthnOptionsBinding() {
        try {
            return container.get(WebAuthnOptionsBinding.class);
        } catch (Exception e) {
            GigyaLogger.error(LOG_TAG, "currentToken: Unable to get options token from container");
            e.printStackTrace();
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private void notifySession(GigyaApiResponse response) {
        final SessionInfo sessionInfo = response.getField("sessionInfo", SessionInfo.class);
        this.sessionService.setSession(sessionInfo);
        try {
            final GigyaLoginCallback<A> callback = container.get(GigyaLoginCallback.class);
            this.businessApiService.getAccount(callback);
        } catch (Exception e) {
            GigyaLogger.error(LOG_TAG, "notifySuccess: Unable to get login callback instance.");
            e.printStackTrace();
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private <T> void notifySuccess(T result) {
        try {
            final GigyaCallback callback = container.get(GigyaCallback.class);
            callback.onSuccess(result);
        } catch (Exception e) {
            GigyaLogger.error(LOG_TAG, "notifySuccess: Unable to get login callback instance.");
            e.printStackTrace();
        }
        clearContainerCallbacks();
    }

    @SuppressWarnings("rawtypes")
    private void notifyError(GigyaError error) {
        try {
            final GigyaCallback callback = container.get(GigyaCallback.class);
            callback.onError(error);
        } catch (Exception e) {
            GigyaLogger.error(LOG_TAG, "notifyError: Unable to get login callback instance.");
            e.printStackTrace();
        }
        clearContainerCallbacks();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void notifyLoginError(GigyaError error) {
        try {
            final GigyaLoginCallback<A> callback = container.get(GigyaLoginCallback.class);
            callback.onError(error);
        } catch (Exception e) {
            GigyaLogger.error(LOG_TAG, "notifyError: Unable to get login callback instance.");
            e.printStackTrace();
        }
        clearContainerCallbacks();
    }

    //region PERSISTENCE

    private void storePublicKey(WebAuthnKeyModel key) {
        final List<WebAuthnKeyModel> keys = getKeys();
        keys.add(key);
        this.persistenceService.saveWebAuthnKeys(WebAuthnKeyModel.toJsonList(keys));
    }

    public List<WebAuthnKeyModel> getKeys() {
        final String json = this.persistenceService.getWebAuthnKeys();
        final List<WebAuthnKeyModel> keys = WebAuthnKeyModel.parseList(json);
        return keys;
    }

    //endregion
}
