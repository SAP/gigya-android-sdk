package com.gigya.android.sdk.model.tfa;

import com.gigya.android.sdk.model.BaseGigyaResponse;

public class TFATotpRegisterResponse extends BaseGigyaResponse {

    private String qrCode;
    private String sctToken;

    public String getQrCode() {
        return qrCode;
    }

    public String getSctToken() {
        return sctToken;
    }
}
