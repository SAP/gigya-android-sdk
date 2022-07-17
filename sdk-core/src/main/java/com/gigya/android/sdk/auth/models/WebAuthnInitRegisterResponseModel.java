package com.gigya.android.sdk.auth.models;

import androidx.annotation.NonNull;

import com.gigya.android.sdk.auth.FidoApiService;
import com.google.gson.Gson;

public class WebAuthnInitRegisterResponseModel {

    public String options;
    public String token;

    public WebAuthnOptionsModel parseOptions() {
        return new Gson().fromJson(this.options, WebAuthnOptionsModel.class);
    }

    @NonNull
    public WebAuthnOptionsToken getOptionsToken() {
        return new WebAuthnOptionsToken(this.token, FidoApiService.FidoApiServiceCodes.REQUEST_CODE_REGISTER.code());
    }
}
