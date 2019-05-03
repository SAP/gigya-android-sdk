package com.gigya.android.sdk.model.tfa;

import com.gigya.android.sdk.model.GigyaResponseModel;

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
