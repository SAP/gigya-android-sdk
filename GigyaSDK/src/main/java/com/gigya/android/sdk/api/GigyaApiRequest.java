package com.gigya.android.sdk.api;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class GigyaApiRequest {

    @NonNull
    private String url, api;
    @Nullable
    private String encodedParams;
    private int method;

    public GigyaApiRequest(@NonNull String url, @Nullable String encodedParams, int method, @NonNull String api) {
        this.url = url;
        this.encodedParams = encodedParams;
        this.method = method;
        this.api = api;
    }

    @NonNull
    public String getUrl() {
        return url;
    }

    @Nullable
    public String getEncodedParams() {
        return encodedParams;
    }

    public int getMethod() {
        return method;
    }

    @NonNull
    public String getTag() {
        return api;
    }

    @NonNull
    public String getApi() {
        return api;
    }
}
