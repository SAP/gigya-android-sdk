package com.gigya.android.sdk.auth.models;


import com.google.gson.Gson;

public class WebAuthnInitRegisterResponseModel {

    public String options;
    public String token;

    public WebAuthnOptionsModel parseOptions() {
        return new Gson().fromJson(this.options, WebAuthnOptionsModel.class);
    }
}
