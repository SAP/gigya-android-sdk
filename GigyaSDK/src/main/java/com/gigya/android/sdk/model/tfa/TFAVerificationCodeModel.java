package com.gigya.android.sdk.model.tfa;

import com.gigya.android.sdk.model.GigyaResponseModel;

public class TFAVerificationCodeModel extends GigyaResponseModel {

    private String phvToken;

    public String getPhvToken() {
        return phvToken;
    }
}
