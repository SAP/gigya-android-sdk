package com.gigya.android.sdk.auth;


import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static com.google.android.gms.fido.Fido.FIDO2_KEY_RESPONSE_EXTRA;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.util.Base64;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.annotation.Nullable;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.api.IBusinessApiService;
import com.gigya.android.sdk.auth.models.WebAuthnAssertionResponse;
import com.gigya.android.sdk.auth.models.WebAuthnAttestationResponse;
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
    final private ISessionService sessionService;
    final private IPersistenceService persistenceService;
    final private IBusinessApiService<A> businessApiService;

    private final IoCContainer container = new IoCContainer();

    public WebAuthnService(
            IOauthService oauthService,
            IFidoApiService fidoApiService,
            ISessionService sessionService,
            IPersistenceService persistenceService,
            IBusinessApiService<A> businessApiService
    ) {
        this.oauthService = oauthService;
        this.fidoApiService = fidoApiService;
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
     * Initialize registration flow. Request options.
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
     * Register new passkey.
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
     * Request assertion options from WebAuthn service.
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
     * Verify Fido assertion with WebAuthn service.
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
     * Remove request passkey.
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
            gigyaCallback.onError(new GigyaError(200001, "WebAuthn/Fido service is available from Android M only"));
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
            notifyError(new GigyaError(200001, "WebAuthn/Fido service is available from Android M only"));
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

                final String idToken = response.getField("idToken", String.class);
                if (idToken == null) {
                    GigyaLogger.error(LOG_TAG, "registerCredentials: missing idToken");
                    notifyError(new GigyaError(200001, "registerCredentials: missing idToken"));
                    return;
                }

                // Account UID is required to align unique user to passkey.
                businessApiService.getAccount(new GigyaCallback<A>() {

                    @Override
                    public void onSuccess(final A account) {
                        oauthService.connect(idToken, new GigyaCallback<GigyaApiResponse>() {
                            @Override
                            public void onSuccess(GigyaApiResponse response) {
                                GigyaLogger.debug(LOG_TAG, "connect api success response:\n" + response.asJson());

                                // Store public key. Current passkey should be revoked.
                                final WebAuthnKeyModel revokeKeyModel = getPassKey();
                                storePassKey(
                                        new WebAuthnKeyModel(
                                                optionsBinding.userModel.name,
                                                optionsBinding.userModel.displayName,
                                                account.getUID(),
                                                optionsBinding.type,
                                                webAuthnAttestationResponse.rawIdBase64)
                                );

                                if (response.getErrorCode() != 0) {
                                    GigyaLogger.error(LOG_TAG, "Response error: \n" + response.asJson());
                                    notifyError(GigyaError.fromResponse(response));
                                    return;
                                }

                                notifySuccess(response);

                                if (revokeKeyModel == null) {
                                    // No last key to revoke.
                                    return;
                                }

                                // decode last key
                                final byte[] decoded = Base64.decode(revokeKeyModel.key.getBytes(),
                                        Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);

                                // Remove old credential before submitting success response.
                                final Map<String, Object> params = new HashMap<>();
                                params.put("credentialId", Base64.encodeToString(decoded, Base64.NO_WRAP).trim());
                                removeCredential(params, new GigyaCallback<GigyaApiResponse>() {
                                    @Override
                                    public void onSuccess(GigyaApiResponse revokeResponse) {
                                        if (revokeResponse.getErrorCode() != 0) {
                                            GigyaLogger.error(LOG_TAG, "Response error: \n" + revokeResponse.asJson());
                                        }
                                    }

                                    @Override
                                    public void onError(GigyaError error) {
                                        GigyaLogger.error(LOG_TAG, "removeCredential api error: \n" + error.getData());
                                    }
                                });
                            }

                            @Override
                            public void onError(GigyaError error) {
                                GigyaLogger.error(LOG_TAG, "connect api error: \n" + error.getData());
                                notifyError(error);
                            }
                        });
                    }

                    @Override
                    public void onError(GigyaError error) {
                        GigyaLogger.error(LOG_TAG, "registerCredentials: Failed to obtain account information");
                        notifyError(error);
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
            gigyaCallback.onError(new GigyaError(200001, "WebAuthn/Fido service is available from Android M only"));
            return;
        }

        // Load allowed keys.
        final List<WebAuthnKeyModel> keys = getPassKeys();
        if (keys.isEmpty()) {
            GigyaLogger.debug(LOG_TAG, "login error : PassKey not available");
            gigyaCallback.onError(new GigyaError(200001, "PassKey not available"));
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
                    notifyLoginError(
                            new GigyaError(200001,
                                    "getAssertionOptions webAuthnGetOptionsResponseModel parse error")
                    );
                    return;
                }

                // Register helper model.
                container.bind(WebAuthnOptionsBinding.class,
                        new WebAuthnOptionsBinding(
                                webAuthnGetOptionsResponseModel.token,
                                FidoApiService.FidoApiServiceCodes.REQUEST_CODE_SIGN.code()
                        ));

                fidoApiService.sign(resultLauncher, webAuthnGetOptionsResponseModel, keys, new IFidoApiFlowError() {
                    @Override
                    public void onFlowFailedWith(GigyaError error) {
                        notifyLoginError(error);
                    }
                });
            }

            @Override
            public void onError(GigyaError error) {
                GigyaLogger.error(LOG_TAG, "getAssertionOptions error:\n" + error.getData());
                notifyLoginError(error);
            }

        });
    }

    /***
     * Initialize WebAuthn/Fido login/signing flow. Additional login parameters applied.
     *
     * @param resultLauncher Activity result launcher for intent sender request.
     * @param params Available login parameters.
     * @param gigyaCallback Result callback.
     */
    @Override
    public void login(ActivityResultLauncher<IntentSenderRequest> resultLauncher,
                      Map<String, Object> params,
                      GigyaLoginCallback<A> gigyaCallback) {
        oauthService.setLoginParams(params);
        login(resultLauncher, gigyaCallback);
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
                    notifyLoginError(new GigyaError(200001, "verifyAssertion: missing idToken"));
                    return;
                }

                // Authorize idToken.
                oauthService.authorize(idToken, new GigyaCallback<GigyaApiResponse>() {
                    @Override
                    public void onSuccess(GigyaApiResponse response) {
                        GigyaLogger.debug(LOG_TAG, "authorize api success response:\n" + response.asJson());

                        if (response.getErrorCode() != 0) {
                            GigyaLogger.error(LOG_TAG, "Response error: \n" + response.asJson());
                            notifyLoginError(GigyaError.fromResponse(response));
                            return;
                        }

                        if (response.contains("code")) {
                            final String code = response.getField("code", String.class);
                            oauthService.token(code, new GigyaCallback<GigyaApiResponse>() {
                                @Override
                                public void onSuccess(GigyaApiResponse response) {
                                    GigyaLogger.debug(LOG_TAG, "token api success response:\n" + response.asJson());

                                    if (response.getErrorCode() != 0) {
                                        GigyaLogger.error(LOG_TAG, "Response error: \n" + response.asJson());
                                        notifyLoginError(GigyaError.fromResponse(response));
                                        return;
                                    }

                                    oauthService.clearLoginParams();

                                    // Session received. Update session service.
                                    notifySession(response);
                                }

                                @Override
                                public void onError(GigyaError error) {
                                    GigyaLogger.error(LOG_TAG, "token api error: \n" + error.getData());
                                    notifyLoginError(error);
                                }

                            });
                        }
                    }

                    @Override
                    public void onError(GigyaError error) {
                        GigyaLogger.error(LOG_TAG, "authorize api error: \n" + error.getData());
                        notifyLoginError(error);
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
     * @param gigyaCallback Result callback.
     */
    @Override
    public void revoke(
            final GigyaCallback<GigyaApiResponse> gigyaCallback) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            GigyaLogger.error(LOG_TAG, "WebAuthn/Fido service is available from Android M only");
            gigyaCallback.onError(new GigyaError(200001, "WebAuthn/Fido service is available from Android M only"));
            return;
        }

        // Currently only one passkey allowed at one time. list will always contain 1 entry.
        final WebAuthnKeyModel keyModel = getPassKey();
        if (keyModel == null) {
            GigyaLogger.error(LOG_TAG, "PassKey not available");
            gigyaCallback.onError(new GigyaError(200001, "PassKey not available"));
            return;
        }

        final byte[] decoded = Base64.decode(keyModel.key.getBytes(),
                Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);

        final Map<String, Object> params = new HashMap<>();
        params.put("credentialId", Base64.encodeToString(decoded, Base64.NO_WRAP).trim());
        removeCredential(params, new GigyaCallback<GigyaApiResponse>() {
            @Override
            public void onSuccess(GigyaApiResponse obj) {
                clearPassKey();
                gigyaCallback.onSuccess(obj);
            }

            @Override
            public void onError(GigyaError error) {
                gigyaCallback.onError(error);
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

    /**
     * Store new created passkey.
     * Only one passkey available at any time.
     *
     * @param key Key model.
     * @return Last Stored passkey. Null if none available.
     */
    private void storePassKey(WebAuthnKeyModel key) {
        final List<WebAuthnKeyModel> keys = getPassKeys();
        WebAuthnKeyModel lastKey = null;
        keys.clear();
        keys.add(key);
        final String json = WebAuthnKeyModel.toJsonList(keys);
        this.persistenceService.savePassKeys(json);
        GigyaLogger.debug(LOG_TAG, "storePassKey: " + json);
    }

    /**
     * Clears all passkeys from preferences.
     */
    private void clearPassKey() {
        this.persistenceService.clearPassKeys();
    }

    /**
     * Get stored passkeys.
     *
     * @return Will return a list of stored passkeys (currently only one available at any time).
     */
    public List<WebAuthnKeyModel> getPassKeys() {
        final String json = this.persistenceService.getPassKeys();
        final List<WebAuthnKeyModel> keys = WebAuthnKeyModel.parseList(json);
        GigyaLogger.debug(LOG_TAG, "getPassKeys: " + keys.toString());
        return keys;
    }

    public boolean passkeyForUser(String uid) {
        if (uid == null) return false;
        List<WebAuthnKeyModel> keys = getPassKeys();
        if (keys.size() > 0) {
            return keys.get(0).uid.equals(uid);
        }
        return false;
    }

    @Nullable
    private WebAuthnKeyModel getPassKey() {
        final List<WebAuthnKeyModel> keys = getPassKeys();
        if (keys.isEmpty()) return null;
        return keys.get(0);
    }

    //endregion
}
