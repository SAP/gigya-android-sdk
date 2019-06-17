package com.gigya.android.sdk.tfa.resolvers.models;

import com.gigya.android.sdk.network.GigyaResponseModel;

public class TOTPRegisterModel extends GigyaResponseModel {

    private String qrCode;
    private String sctToken;

    public String getQrCode() {
        return qrCode;
    }

    public String getSctToken() {
        return sctToken;
    }
}
