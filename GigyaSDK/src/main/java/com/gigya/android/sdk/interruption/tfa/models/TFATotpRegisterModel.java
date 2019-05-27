package com.gigya.android.sdk.interruption.tfa.models;

import com.gigya.android.sdk.network.GigyaResponseModel;

public class TFATotpRegisterModel extends GigyaResponseModel {

    private String qrCode;
    private String sctToken;

    public String getQrCode() {
        return qrCode;
    }

    public String getSctToken() {
        return sctToken;
    }
}
