package com.gigya.android.sdk.api;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gigya.android.sdk.network.adapter.RestAdapter;

public class GigyaApiHttpRequest {

    private RestAdapter.HttpMethod httpMethod;
    private String url;
    private String encodedParams;

    GigyaApiHttpRequest(
            @NonNull RestAdapter.HttpMethod httpMethod,
            @NonNull String url,
            @Nullable String encodedParams) {
        this.httpMethod = httpMethod;
        this.url = url;
        this.encodedParams = encodedParams;
    }

    @NonNull
    public RestAdapter.HttpMethod getHttpMethod() {
        return httpMethod;
    }

    @NonNull
    public String getUrl() {
        return url;
    }

    @Nullable
    public String getEncodedParams() {
        return encodedParams;
    }

}
