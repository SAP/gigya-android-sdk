package com.gigya.android.sdk.auth;

import androidx.activity.ComponentActivity;

import com.gigya.android.sdk.auth.models.WebAuthnAssertionResponse;
import com.gigya.android.sdk.auth.models.WebAuthnAttestationResponse;
import com.gigya.android.sdk.auth.models.WebAuthnGetOptionsResponseModel;
import com.gigya.android.sdk.auth.models.WebAuthnInitRegisterResponseModel;

public interface IFidoApiService {

    void register(ComponentActivity activity, WebAuthnInitRegisterResponseModel option, IFidoResponseResult fidoResult);

    WebAuthnAttestationResponse onRegisterResponse(byte[] attestationResponse, byte[] credentialResponse);

    void sign(ComponentActivity activity, WebAuthnGetOptionsResponseModel options, IFidoResponseResult fidoResult);

    WebAuthnAssertionResponse onSignResponse(byte[] fidoApiResponse);
}
