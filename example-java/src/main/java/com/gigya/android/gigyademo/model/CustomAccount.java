package com.gigya.android.gigyademo.model;

import com.gigya.android.sdk.account.models.GigyaAccount;

public class CustomAccount extends GigyaAccount {

    private CustomData data;

    public CustomData getData() {
        return data;
    }

    public void setData(CustomData data) {
        this.data = data;
    }

    /**
     * Custom data object. Use it to expand your basic account scheme.
     */
    public static class CustomData {

    }
}
