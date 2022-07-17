package com.gigya.android.sdk.auth.models;

import androidx.annotation.NonNull;

import com.gigya.android.sdk.auth.FidoApiService;
import com.google.gson.Gson;

public class WebAuthnGetOptionsResponseModel {

    public String options;
    public String token;

    public WebAuthnGetOptionsModel parseOptions() {
        return new Gson().fromJson(options, WebAuthnGetOptionsModel.class);
    }

    @NonNull
    public WebAuthnOptionsToken getOptionsToken() {
        return new WebAuthnOptionsToken(this.token, FidoApiService.FidoApiServiceCodes.REQUEST_CODE_SIGN.code());
    }
}
