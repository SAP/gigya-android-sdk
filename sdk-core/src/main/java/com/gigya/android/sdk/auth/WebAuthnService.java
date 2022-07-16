package com.gigya.android.sdk.auth;


import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static com.google.android.gms.fido.Fido.FIDO2_KEY_RESPONSE_EXTRA;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.api.ApiService;
import com.gigya.android.sdk.api.GigyaApiRequest;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.api.IApiRequestFactory;
import com.gigya.android.sdk.api.IApiService;
import com.gigya.android.sdk.auth.models.WebAuthnAssertionResponse;
import com.gigya.android.sdk.auth.models.WebAuthnAttestationResponse;
import com.gigya.android.sdk.auth.models.WebAuthnGetOptionsResponseModel;
import com.gigya.android.sdk.auth.models.WebAuthnInitRegisterResponseModel;
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
    private void verifyAssertion(Map<String, Object> params) {
        GigyaApiRequest request = requestFactory.create(
                WebAuthnApis.verifyAssertion.api,
                params);
        apiService.send(request, new ApiService.IApiServiceResponse() {
            @Override
            public void onApiSuccess(GigyaApiResponse response) {
                GigyaLogger.debug(LOG_TAG, "verifyAssertion success:\n" + response.asJson());

            }

            @Override
            public void onApiError(GigyaError gigyaError) {
                GigyaLogger.debug(LOG_TAG, "verifyAssertion error:\n" + gigyaError.getData());
            }
        });
    }


    @SuppressLint("NewApi")
    @Override
    public void register(final ActivityResultLauncher<IntentSenderRequest> resultLauncher) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            GigyaLogger.debug(LOG_TAG, "WebAuthn/Fido service is available from Android M only");
            return;
        }
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

                fidoApiService.register(resultLauncher, webAuthnInitRegisterResponseModel, new IFidoResponseResult() {

                    @Override
                    public void onIntent(int resultCode, Intent intent) {
                        handleFidoResult(resultCode, intent);
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
    public void login(final ActivityResultLauncher<IntentSenderRequest> resultLauncher) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            GigyaLogger.debug(LOG_TAG, "WebAuthn/Fido service is available from Android M only");
            return;
        }

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

                fidoApiService.sign(resultLauncher, webAuthnGetOptionsResponseModel, new IFidoResponseResult() {
                    @Override
                    public void onIntent(int resultCode, Intent intent) {
                        handleFidoResult(resultCode, intent);
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
    public void handleFidoResult(int resultCode, Intent data) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            GigyaLogger.debug(LOG_TAG, "WebAuthn/Fido service is available from Android M only");
            return;
        }
        switch (resultCode) {
            case RESULT_OK:
                if (data == null) {
                    GigyaLogger.debug(LOG_TAG, "handleIntent: null response intent");
                    return;
                }
                if (data.hasExtra(Fido.FIDO2_KEY_ERROR_EXTRA)) {
                    fidoApiService.onFidoError(data.getByteArrayExtra(Fido.FIDO2_KEY_ERROR_EXTRA));
                    return;
                } else if (data.hasExtra(FIDO2_KEY_RESPONSE_EXTRA)) {
                    byte[] fido2Response = data.getByteArrayExtra(FIDO2_KEY_RESPONSE_EXTRA);
                    byte[] fido2Credential = data.getByteArrayExtra(Fido.FIDO2_KEY_CREDENTIAL_EXTRA);
                    int requestCode = data.getIntExtra("requestCode", FidoApiService.FidoApiServiceCodes.REQUEST_CODE_INVALID.code());
                    if (requestCode == FidoApiService.FidoApiServiceCodes.REQUEST_CODE_INVALID.code()) {
                        GigyaLogger.debug(LOG_TAG, "Invalid request code from Fido response");
                        return;
                    }

                    String token = data.getStringExtra("token");
                    if (requestCode == FidoApiService.FidoApiServiceCodes.REQUEST_CODE_REGISTER.code()) {
                        onRegistration(token, fido2Response, fido2Credential);
                    } else if (requestCode == FidoApiService.FidoApiServiceCodes.REQUEST_CODE_SIGN.code()) {
                        onLogin(token, fido2Response);
                    }
                }
                break;
            case RESULT_CANCELED:
                GigyaLogger.debug(LOG_TAG, "handleIntent: canceled result");
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
            }

            @Override
            public void onApiError(GigyaError gigyaError) {
                GigyaLogger.debug(LOG_TAG, "registerCredentials error:\n" + gigyaError.getData());
            }
        });
    }

    @SuppressLint("NewApi")
    public void onLogin(final String token, byte[] assertionResponse) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            GigyaLogger.debug(LOG_TAG, "WebAuthn/Fido service is available from Android M only");
            return;
        }

        WebAuthnAssertionResponse webAuthnAssertionResponse =
                fidoApiService.onSignResponse(assertionResponse);


    }


}
