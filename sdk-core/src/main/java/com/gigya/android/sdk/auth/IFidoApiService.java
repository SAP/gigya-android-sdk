package com.gigya.android.sdk.auth;

import android.app.Activity;
import android.content.Context;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.gigya.android.sdk.auth.models.WebAuthnInitRegisterResponseModel;

@RequiresApi(api = Build.VERSION_CODES.M)
public interface IFidoApiService {

    void register(Activity activity, WebAuthnInitRegisterResponseModel option);

    void onRegisterResponse(byte[] fidoApiResponse);

    void sign();

    void onSignResponse(byte[] fidoApiResponse);
}
