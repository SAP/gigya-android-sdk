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
import com.gigya.android.sdk.api.ApiService;
import com.gigya.android.sdk.api.GigyaApiRequest;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.api.IApiRequestFactory;
import com.gigya.android.sdk.api.IApiService;
import com.gigya.android.sdk.auth.models.WebAuthnAssertionResponse;
import com.gigya.android.sdk.auth.models.WebAuthnAttestationResponse;
import com.gigya.android.sdk.auth.models.WebAuthnGetOptionsResponseModel;
import com.gigya.android.sdk.auth.models.WebAuthnInitRegisterResponseModel;
import com.gigya.android.sdk.containers.IoCContainer;
import com.gigya.android.sdk.network.GigyaError;
import com.google.android.gms.fido.Fido;

import java.util.HashMap;
import java.util.Map;

public class WebAuthnService implements IWebAuthnService {

    public static final String LOG_TAG = "WebAuthnService";

    final private IApiService apiService;
    final private IOauthService oauthService;
    final private IFidoApiService fidoApiService;
    final private IApiRequestFactory requestFactory;

    private final IoCContainer container = new IoCContainer();

    public WebAuthnService(
            IOauthService oauthService,
            IApiService apiService,
            IFidoApiService fidoApiService,
            IApiRequestFactory requestFactory
    ) {
        this.oauthService = oauthService;
        this.apiService = apiService;
        this.fidoApiService = fidoApiService;
        this.requestFactory = requestFactory;
    }

    private enum WebAuthnApis {
        initRegisterCredentials("accounts.auth.fido.initRegisterCredentials"),
        getAssertionOptions("accounts.auth.fido.getAssertionOptions"),
        registerCredentials("accounts.auth.fido.registerCredentials"),
        verifyAssertion("accounts.auth.fido.verifyAssertion");

        final String api;

        WebAuthnApis(String api) {
            this.api = api;
        }

        public String api() {
            return this.api;
        }
    }

    /**
     *
     */
    private void initRegistration(ApiService.IApiServiceResponse iApiServiceResponse) {
        GigyaApiRequest request = requestFactory.create(
                WebAuthnApis.initRegisterCredentials.api,
                new HashMap<String, Object>());
        apiService.send(request, iApiServiceResponse);
    }

    /**
     * @param params
     */
    private void registerCredentials(Map<String, Object> params, ApiService.IApiServiceResponse iApiServiceResponse) {
        GigyaApiRequest request = requestFactory.create(
                WebAuthnApis.registerCredentials.api,
                params);
        apiService.send(request, iApiServiceResponse);
    }

    /**
     *
     */
    private void getAssertionOptions(ApiService.IApiServiceResponse iApiServiceResponse) {
        GigyaApiRequest request = requestFactory.create(
                WebAuthnApis.getAssertionOptions.api,
                new HashMap<String, Object>());
        apiService.send(request, iApiServiceResponse);
    }

    /**
     * @param params
     */
    private void verifyAssertion(Map<String, Object> params, ApiService.IApiServiceResponse iApiServiceResponse) {
        GigyaApiRequest request = requestFactory.create(
                WebAuthnApis.verifyAssertion.api,
                params);
        apiService.send(request, iApiServiceResponse);
    }


    @SuppressLint("NewApi")
    @Override
    public void register(
            final ActivityResultLauncher<IntentSenderRequest> resultLauncher,
            final GigyaCallback<GigyaApiResponse> gigyaCallback
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            GigyaLogger.debug(LOG_TAG, "WebAuthn/Fido service is available from Android M only");
            notifyError(new GigyaError(200001, "WebAuthn/Fido service is available from Android M only"));
            return;
        }

        // Register callback.
        container.bind(GigyaCallback.class, gigyaCallback);

        initRegistration(new ApiService.IApiServiceResponse() {
            @Override
            public void onApiSuccess(GigyaApiResponse response) {
                GigyaLogger.debug(LOG_TAG, "initRegistration success:\n" + response.asJson());

                WebAuthnInitRegisterResponseModel webAuthnInitRegisterResponseModel =
                        response.parseTo(WebAuthnInitRegisterResponseModel.class);

                if (webAuthnInitRegisterResponseModel == null) {
                    GigyaLogger.debug(LOG_TAG,
                            "initRegistration webAuthnInitRegisterResponseModel parse error");
                    return;
                }

                fidoApiService.register(resultLauncher, webAuthnInitRegisterResponseModel, new IFidoApiFlowError() {
                    @Override
                    public void onFlowFailedWith(GigyaError error) {

                    }
                });
            }

            @Override
            public void onApiError(GigyaError gigyaError) {
                GigyaLogger.debug(LOG_TAG, "initRegistration error:\n" + gigyaError.getData());
            }
        });
    }

    @SuppressLint("NewApi")
    @Override
    public void login(final ActivityResultLauncher<IntentSenderRequest> resultLauncher, final GigyaCallback<GigyaApiResponse> gigyaCallback) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            GigyaLogger.debug(LOG_TAG, "WebAuthn/Fido service is available from Android M only");
            notifyError(new GigyaError(200001, "WebAuthn/Fido service is available from Android M only"));
            return;
        }

        // Register callback.
        container.bind(GigyaCallback.class, gigyaCallback);

        getAssertionOptions(new ApiService.IApiServiceResponse() {
            @Override
            public void onApiSuccess(GigyaApiResponse response) {
                GigyaLogger.debug(LOG_TAG, "getAssertionOptions success:\n" + response.asJson());

                WebAuthnGetOptionsResponseModel webAuthnGetOptionsResponseModel =
                        response.parseTo(WebAuthnGetOptionsResponseModel.class);

                if (webAuthnGetOptionsResponseModel == null) {
                    GigyaLogger.debug(LOG_TAG,
                            "getAssertionOptions webAuthnGetOptionsResponseModel parse error");
                    return;
                }

                fidoApiService.sign(resultLauncher, webAuthnGetOptionsResponseModel, new IFidoApiFlowError() {
                    @Override
                    public void onFlowFailedWith(GigyaError error) {

                    }
                });
            }

            @Override
            public void onApiError(GigyaError gigyaError) {
                GigyaLogger.debug(LOG_TAG, "getAssertionOptions error:\n" + gigyaError.getData());
            }
        });
    }

    @Override
    public void handleFidoResult(ActivityResult activityResult) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            GigyaLogger.debug(LOG_TAG, "WebAuthn/Fido service is available from Android M only");
            return;
        }
        switch (activityResult.getResultCode()) {
            case RESULT_OK:
                final Intent data = activityResult.getData();
                if (data == null) {
                    GigyaLogger.debug(LOG_TAG, "Fido result error : null intent");
                    notifyError(new GigyaError(200001, "Fido result error : null intent"));
                    return;
                }
                final int requestCode = data.getIntExtra("requestCode",
                        FidoApiService.FidoApiServiceCodes.REQUEST_CODE_INVALID.code());
                if (data.hasExtra(Fido.FIDO2_KEY_ERROR_EXTRA)) {
                    final GigyaError error = fidoApiService.onFidoError(data.getByteArrayExtra(Fido.FIDO2_KEY_ERROR_EXTRA));
                    notifyError(error);
                    return;
                } else if (data.hasExtra(FIDO2_KEY_RESPONSE_EXTRA)) {
                    byte[] fido2Response = data.getByteArrayExtra(FIDO2_KEY_RESPONSE_EXTRA);
                    byte[] fido2Credential = data.getByteArrayExtra(Fido.FIDO2_KEY_CREDENTIAL_EXTRA);
                    if (requestCode == FidoApiService.FidoApiServiceCodes.REQUEST_CODE_INVALID.code()) {
                        GigyaLogger.debug(LOG_TAG, "Fido result error: Invalid request code ");
                        notifyError(new GigyaError(200001, "Fido result error: Invalid request code"));
                        return;
                    }

                    String token = data.getStringExtra("token");
                    if (token == null) {
                        GigyaLogger.debug(LOG_TAG, "Fido result error: token null");
                        notifyError(new GigyaError(200001, "Fido result error: token null"));
                        return;
                    }

                    if (requestCode == FidoApiService.FidoApiServiceCodes.REQUEST_CODE_REGISTER.code()) {
                        onRegistration(token, fido2Response, fido2Credential);
                    } else if (requestCode == FidoApiService.FidoApiServiceCodes.REQUEST_CODE_SIGN.code()) {
                        onLogin(token, fido2Response, fido2Credential);
                    }
                }
                break;
            case RESULT_CANCELED:
                GigyaLogger.debug(LOG_TAG, "Fido result error: result canceled");
                notifyError(new GigyaError(200001, "Fido result error: result canceled"));
                break;
            default:
                break;
        }
    }

    @SuppressLint("NewApi")
    public void onRegistration(final String token, byte[] attestationResponse, byte[] credentialResponse) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            GigyaLogger.debug(LOG_TAG, "WebAuthn/Fido service is available from Android M only");
            return;
        }

        final WebAuthnAttestationResponse webAuthnAttestationResponse =
                fidoApiService.onRegisterResponse(attestationResponse, credentialResponse);

        final Map<String, Object> params = new HashMap<>();
        params.put("attestation", webAuthnAttestationResponse.getAttestation());
        params.put("token", token);

        registerCredentials(params, new ApiService.IApiServiceResponse() {
            @Override
            public void onApiSuccess(GigyaApiResponse response) {
                GigyaLogger.debug(LOG_TAG, "registerCredentials success:\n" + response.asJson());

                final String idToken = response.getField("idToken", String.class);
                if (idToken == null) {
                    GigyaLogger.debug(LOG_TAG, "verifyAssertion: missing idToken");
                    notifyError(new GigyaError(200001, "verifyAssertion: missing idToken"));
                    return;
                }

                oauthService.connect(idToken, new ApiService.IApiServiceResponse() {
                    @Override
                    public void onApiSuccess(GigyaApiResponse response) {
                        GigyaLogger.debug(LOG_TAG, "connect api success response:\n" + response.asJson());
                        notifySuccess(response);
                    }

                    @Override
                    public void onApiError(GigyaError gigyaError) {
                        GigyaLogger.debug(LOG_TAG, "connect api error: \n" + gigyaError.getData());
                        notifyError(gigyaError);
                    }
                });
            }

            @Override
            public void onApiError(GigyaError gigyaError) {
                GigyaLogger.debug(LOG_TAG, "registerCredentials error:\n" + gigyaError.getData());
                notifyError(gigyaError);
            }
        });
    }

    @SuppressLint("NewApi")
    public void onLogin(final String token, byte[] assertionResponse, byte[] fido2Credential) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            GigyaLogger.debug(LOG_TAG, "WebAuthn/Fido service is available from Android M only");
            return;
        }

        WebAuthnAssertionResponse webAuthnAssertionResponse =
                fidoApiService.onSignResponse(assertionResponse, fido2Credential);

        final Map<String, Object> params = new HashMap<>();
        params.put("authenticatorAssertion", webAuthnAssertionResponse.getAssertion());
        params.put("token", token);

        verifyAssertion(params, new ApiService.IApiServiceResponse() {
            @Override
            public void onApiSuccess(GigyaApiResponse response) {
                GigyaLogger.debug(LOG_TAG, "verifyAssertion success:\n" + response.asJson());

                final String idToken = response.getField("idToken", String.class);
                if (idToken == null) {
                    GigyaLogger.debug(LOG_TAG, "verifyAssertion: missing idToken");
                    notifyError(new GigyaError(200001, "verifyAssertion: missing idToken"));
                    return;
                }

                // Authorize idToken.
                oauthService.authorize(idToken, new ApiService.IApiServiceResponse() {
                    @Override
                    public void onApiSuccess(GigyaApiResponse response) {
                        GigyaLogger.debug(LOG_TAG, "authorize api success response:\n" + response.asJson());

                        if (response.contains("code")) {
                            final String code = response.getField("code", String.class);
                            oauthService.token(code, new ApiService.IApiServiceResponse() {
                                @Override
                                public void onApiSuccess(GigyaApiResponse response) {
                                    GigyaLogger.debug(LOG_TAG, "token api success response:\n" + response.asJson());
                                    notifySuccess(response);
                                }

                                @Override
                                public void onApiError(GigyaError gigyaError) {
                                    GigyaLogger.debug(LOG_TAG, "token api error: \n" + gigyaError.getData());
                                    notifyError(gigyaError);
                                }
                            });
                        }
                    }

                    @Override
                    public void onApiError(GigyaError gigyaError) {
                        GigyaLogger.debug(LOG_TAG, "authorize api error: \n" + gigyaError.getData());
                        notifyError(gigyaError);
                    }
                });
            }

            @Override
            public void onApiError(GigyaError gigyaError) {
                GigyaLogger.debug(LOG_TAG, "verifyAssertion error:\n" + gigyaError.getData());
                notifyError(gigyaError);
            }
        });

    }

    /**
     * Clear callback container from flow callback references.
     */
    private void clearContainerCallbacks() {
        container.dispose();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private <T> void notifySuccess(T result) {
        try {
            final GigyaCallback callback = container.get(GigyaCallback.class);
            callback.onSuccess(result);
        } catch (Exception e) {
            GigyaLogger.debug(LOG_TAG, "notifySuccess: Unable to get login callback instance.");
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
            GigyaLogger.debug(LOG_TAG, "notifyError: Unable to get login callback instance.");
            e.printStackTrace();
        }
        clearContainerCallbacks();
    }
}
