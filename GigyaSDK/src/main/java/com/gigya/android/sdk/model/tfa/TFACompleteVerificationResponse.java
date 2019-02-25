package com.gigya.android.sdk.model.tfa;

import com.gigya.android.sdk.model.BaseGigyaResponse;

public class TFACompleteVerificationResponse extends BaseGigyaResponse {

    private String providerAssertion;

    public String getProviderAssertion() {
        return providerAssertion;
    }
}
