package com.gigya.android.sdk.model.tfa;

import com.gigya.android.sdk.model.BaseGigyaResponse;

public class TFAVerificationCodeResponse extends BaseGigyaResponse {

    private String phvToken;

    public String getPhvToken() {
        return phvToken;
    }
}
