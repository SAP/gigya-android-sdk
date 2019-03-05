package com.gigya.android.sdk.model.tfa;

import com.gigya.android.sdk.model.GigyaModel;

public class TFATotpRegisterModel extends GigyaModel {

    private String qrCode;
    private String sctToken;

    public String getQrCode() {
        return qrCode;
    }

    public String getSctToken() {
        return sctToken;
    }
}
