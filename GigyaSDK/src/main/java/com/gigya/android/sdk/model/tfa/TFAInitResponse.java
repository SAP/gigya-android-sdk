package com.gigya.android.sdk.model.tfa;

import com.gigya.android.sdk.model.BaseGigyaResponse;

public class TFAInitResponse extends BaseGigyaResponse {

    private String gigyaAssertion;

    public String getGigyaAssertion() {
        return gigyaAssertion;
    }
}
