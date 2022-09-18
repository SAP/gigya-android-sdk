package com.gigya.android.sdk.auth;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;

import com.gigya.android.sdk.auth.models.WebAuthnAssertionResponse;
import com.gigya.android.sdk.auth.models.WebAuthnAttestationResponse;
import com.gigya.android.sdk.auth.models.WebAuthnGetOptionsResponseModel;
import com.gigya.android.sdk.auth.models.WebAuthnInitRegisterResponseModel;
import com.gigya.android.sdk.auth.models.WebAuthnKeyModel;
import com.gigya.android.sdk.network.GigyaError;

import java.util.List;

public interface IFidoApiService {

    void register(
            ActivityResultLauncher<IntentSenderRequest> resultLauncher,
            WebAuthnInitRegisterResponseModel option,
            IFidoApiFlowError flowError
    );

    WebAuthnAttestationResponse onRegisterResponse(
            byte[] attestationResponse,
            byte[] credentialResponse
    );

    void sign(
            ActivityResultLauncher<IntentSenderRequest> resultLauncher,
            WebAuthnGetOptionsResponseModel options,
            List<WebAuthnKeyModel> allowedKeys,
            IFidoApiFlowError flowError
    );

    WebAuthnAssertionResponse onSignResponse(
            byte[] fidoApiResponse,
            byte[] credentialResponse
    );

    GigyaError onFidoError(byte[] errorBytes);
}
