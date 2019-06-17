package com.gigya.android.sdk.tfa.models;

import com.gigya.android.sdk.network.GigyaResponseModel;

public class InitTFAModel extends GigyaResponseModel {

    private String gigyaAssertion;

    public String getGigyaAssertion() {
        return gigyaAssertion;
    }
}
