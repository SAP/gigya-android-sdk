package com.gigya.android.sdk.network;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gigya.android.sdk.network.adapter.NetworkAdapter;

public class GigyaApiRequest {

    @NonNull
    private String url, api;
    @Nullable
    private String encodedParams;
    private NetworkAdapter.Method method;

    GigyaApiRequest(@NonNull String url, @Nullable String encodedParams, NetworkAdapter.Method method, @NonNull String api) {
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

    public NetworkAdapter.Method getMethod() {
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
