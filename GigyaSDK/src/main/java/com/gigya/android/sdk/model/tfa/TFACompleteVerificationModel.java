package com.gigya.android.sdk.model.tfa;

import com.gigya.android.sdk.model.GigyaResponseModel;

public class TFACompleteVerificationModel extends GigyaResponseModel {

    private String providerAssertion;

    public String getProviderAssertion() {
        return providerAssertion;
    }
}
