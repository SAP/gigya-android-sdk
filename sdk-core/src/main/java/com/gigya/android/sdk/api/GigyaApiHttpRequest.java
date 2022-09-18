package com.gigya.android.sdk.api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gigya.android.sdk.network.adapter.RestAdapter;

import java.util.HashMap;

public class GigyaApiHttpRequest {

    private RestAdapter.HttpMethod httpMethod;
    private String url;
    private String encodedParams;
    private HashMap<String, String> headers;

    GigyaApiHttpRequest(
            @NonNull RestAdapter.HttpMethod httpMethod,
            @NonNull String url,
            @Nullable String encodedParams) {
        this.httpMethod = httpMethod;
        this.url = url;
        this.encodedParams = encodedParams;
    }

    GigyaApiHttpRequest(
            @NonNull RestAdapter.HttpMethod httpMethod,
            @NonNull String url,
            @Nullable String encodedParams,
            @Nullable HashMap<String, String> headers) {
        this.httpMethod = httpMethod;
        this.url = url;
        this.encodedParams = encodedParams;
        this.headers = headers;
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

    @Nullable
    public HashMap<String, String> getHeaders() {
        return this.headers;
    }

}
