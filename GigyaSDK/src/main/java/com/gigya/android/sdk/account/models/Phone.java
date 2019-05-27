package com.gigya.android.sdk.account.models;

import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

public class Phone {

    @Nullable
    @SerializedName("default")
    private String _default;
    @Nullable
    private String number;
    @Nullable
    private String type;

    @Nullable
    public String get_default() {
        return _default;
    }

    public void set_default(@Nullable String _default) {
        this._default = _default;
    }

    @Nullable
    public String getNumber() {
        return number;
    }

    public void setNumber(@Nullable String number) {
        this.number = number;
    }

    @Nullable
    public String getType() {
        return type;
    }

    public void setType(@Nullable String type) {
        this.type = type;
    }
}
