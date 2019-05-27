package com.gigya.android.sdk.interruption.tfa.models;

import com.gigya.android.sdk.network.GigyaResponseModel;

public class TFACompleteVerificationModel extends GigyaResponseModel {

    private String providerAssertion;

    public String getProviderAssertion() {
        return providerAssertion;
    }
}
