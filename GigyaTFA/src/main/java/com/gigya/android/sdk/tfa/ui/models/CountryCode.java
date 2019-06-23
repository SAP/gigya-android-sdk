package com.gigya.android.sdk.tfa.ui.models;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public class CountryCode {

    private String name;
    @SerializedName("dial_code")
    private String dialCode;
    private String code;

    public CountryCode(String name, String dialCode, String code) {
        this.name = name;
        this.dialCode = dialCode;
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public String getDialCode() {
        return dialCode;
    }

    public String getCode() {
        return code;
    }

    @NonNull
    @Override
    public String toString() {
        return name + " " + code + " " + dialCode;
    }
}
