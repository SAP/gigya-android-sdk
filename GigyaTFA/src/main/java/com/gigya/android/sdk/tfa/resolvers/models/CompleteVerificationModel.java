package com.gigya.android.sdk.tfa.resolvers.models;

import com.gigya.android.sdk.network.GigyaResponseModel;

public class CompleteVerificationModel extends GigyaResponseModel {

    private String providerAssertion;

    public String getProviderAssertion() {
        return providerAssertion;
    }
}
