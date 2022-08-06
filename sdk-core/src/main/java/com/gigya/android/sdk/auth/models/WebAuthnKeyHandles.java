package com.gigya.android.sdk.auth.models;

import androidx.annotation.Nullable;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class WebAuthnKeyHandles {
    public List<String> handles = new ArrayList<>();

    public static WebAuthnKeyHandles parse(@Nullable String fromString) {
        if (fromString == null) {
            return new WebAuthnKeyHandles();
        }
        return new Gson().fromJson(fromString, WebAuthnKeyHandles.class);
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}
