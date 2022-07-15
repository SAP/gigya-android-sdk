package com.gigya.android.sdk.auth;

import android.app.Activity;
import android.content.Context;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.gigya.android.sdk.auth.models.WebAuthnAssertionResponse;
import com.gigya.android.sdk.auth.models.WebAuthnAttestationResponse;
import com.gigya.android.sdk.auth.models.WebAuthnGetOptionsResponseModel;
import com.gigya.android.sdk.auth.models.WebAuthnInitRegisterResponseModel;

import java.util.Map;

@RequiresApi(api = Build.VERSION_CODES.M)
public interface IFidoApiService {

    void register(Activity activity, WebAuthnInitRegisterResponseModel option);

    WebAuthnAttestationResponse onRegisterResponse(byte[] attestationResponse, byte[] credentialResponse);

    void sign(Activity activity, WebAuthnGetOptionsResponseModel options);

    WebAuthnAssertionResponse onSignResponse(byte[] fidoApiResponse);
}
