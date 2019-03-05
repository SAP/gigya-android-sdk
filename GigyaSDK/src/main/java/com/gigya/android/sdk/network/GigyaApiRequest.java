package com.gigya.android.sdk.network;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gigya.android.sdk.network.adapter.NetworkAdapter;

public class GigyaApiRequest {

    @NonNull
    private String url, tag;
    @Nullable
    private String encodedParams;
    private NetworkAdapter.Method method;

    GigyaApiRequest(@NonNull String url, @Nullable String encodedParams, NetworkAdapter.Method method, @NonNull String tag) {
        this.url = url;
        this.encodedParams = encodedParams;
        this.method = method;
        this.tag = tag;
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
        return tag;
    }
}
