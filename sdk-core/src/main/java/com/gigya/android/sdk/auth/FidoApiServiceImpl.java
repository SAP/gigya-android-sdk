package com.gigya.android.sdk.auth;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.auth.models.WebAuthnAssertionResponse;
import com.gigya.android.sdk.auth.models.WebAuthnAttestationResponse;
import com.gigya.android.sdk.auth.models.WebAuthnGetOptionsResponseModel;
import com.gigya.android.sdk.auth.models.WebAuthnInitRegisterResponseModel;
import com.gigya.android.sdk.auth.models.WebAuthnKeyModel;
import com.gigya.android.sdk.network.GigyaError;

import java.util.List;

/**
 * Stub service class.
 * Android OS lower than M do not support WebAuthn/Fido operations.
 */
public class FidoApiServiceImpl implements IFidoApiService {

    private static final String LOG_TAG = "FidoApiService";

    @Override
    public void register(ActivityResultLauncher<IntentSenderRequest> resultLauncher,
                         WebAuthnInitRegisterResponseModel option, IFidoApiFlowError flowError) {
        // Stub
        GigyaLogger.debug(LOG_TAG, "register operation not supported for Android OS lower than M");
    }

    @Override
    public WebAuthnAttestationResponse onRegisterResponse(byte[] attestationResponse, byte[] credentialResponse) {
        // Stub
        GigyaLogger.debug(LOG_TAG, "onRegisterResponse operation not supported for Android OS lower than M");
        return null;
    }

    @Override
    public void sign(ActivityResultLauncher<IntentSenderRequest> resultLauncher,
                     WebAuthnGetOptionsResponseModel options,
                     List<WebAuthnKeyModel> allowedKeys,
                     IFidoApiFlowError flowError) {
        // Stub
        GigyaLogger.debug(LOG_TAG, "sign operation not supported for Android OS lower than M");
    }

    @Override
    public WebAuthnAssertionResponse onSignResponse(byte[] fidoApiResponse, byte[] credentialResponse) {
        // Stub
        GigyaLogger.debug(LOG_TAG, "onSignResponse operation not supported for Android OS lower than M");
        return null;
    }

    @Override
    public GigyaError onFidoError(byte[] errorResponse) {
        // Stub
        GigyaLogger.debug(LOG_TAG, "onFidoError operation not supported for Android OS lower than M");
        return null;
    }
}
