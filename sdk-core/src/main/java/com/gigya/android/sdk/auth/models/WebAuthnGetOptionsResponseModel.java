package com.gigya.android.sdk.auth.models;

import com.google.gson.Gson;

public class WebAuthnGetOptionsResponseModel {

    public String options;
    public String token;

    public WebAuthnGetOptionsModel parseOptions() {
        return new Gson().fromJson(options, WebAuthnGetOptionsModel.class);
    }
}
