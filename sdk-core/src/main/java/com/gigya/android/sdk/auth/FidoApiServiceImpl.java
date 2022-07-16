package com.gigya.android.sdk.auth;

import androidx.activity.ComponentActivity;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.auth.models.WebAuthnAssertionResponse;
import com.gigya.android.sdk.auth.models.WebAuthnAttestationResponse;
import com.gigya.android.sdk.auth.models.WebAuthnGetOptionsResponseModel;
import com.gigya.android.sdk.auth.models.WebAuthnInitRegisterResponseModel;

/**
 * Stub service class.
 * Android OS lower than M do not support WebAuthn/Fido operations.
 */
public class FidoApiServiceImpl implements IFidoApiService {

    private static final String LOG_TAG = "FidoApiService";

    @Override
    public void register(ComponentActivity activity, WebAuthnInitRegisterResponseModel option, IFidoResponseResult fidoResult) {
        GigyaLogger.debug(LOG_TAG, "register operation not supported for Android OS lower than M");
    }

    @Override
    public WebAuthnAttestationResponse onRegisterResponse(byte[] attestationResponse, byte[] credentialResponse) {
        GigyaLogger.debug(LOG_TAG, "onRegisterResponse operation not supported for Android OS lower than M");
        return null;
    }

    @Override
    public void sign(ComponentActivity activity, WebAuthnGetOptionsResponseModel options, IFidoResponseResult fidoResult) {
        GigyaLogger.debug(LOG_TAG, "sign operation not supported for Android OS lower than M");
    }

    @Override
    public WebAuthnAssertionResponse onSignResponse(byte[] fidoApiResponse) {
        GigyaLogger.debug(LOG_TAG, "onSignResponse operation not supported for Android OS lower than M");
        return null;
    }
}
