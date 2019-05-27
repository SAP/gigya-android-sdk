package com.gigya.android.sdk.interruption.tfa.models;

import com.gigya.android.sdk.network.GigyaResponseModel;

public class TFAInitModel extends GigyaResponseModel {

    private String gigyaAssertion;

    public String getGigyaAssertion() {
        return gigyaAssertion;
    }
}
