package com.gigya.android.sdk.tfa.resolvers.models;

import com.gigya.android.sdk.network.GigyaResponseModel;

public class VerificationCodeModel extends GigyaResponseModel {

    private String phvToken;

    public String getPhvToken() {
        return phvToken;
    }
}
