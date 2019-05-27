package com.gigya.android.sdk.interruption.tfa.models;

import com.gigya.android.sdk.network.GigyaResponseModel;

public class TFAVerificationCodeModel extends GigyaResponseModel {

    private String phvToken;

    public String getPhvToken() {
        return phvToken;
    }
}
