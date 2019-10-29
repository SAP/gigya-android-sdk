package com.gigya.android.sdk.api;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.TreeMap;

public class GigyaApiRequest {

    @NonNull
    private String url, api;
    @Nullable
    private String encodedParams;
    private int method;
    @NonNull
    private TreeMap<String, Object> originalParameters;

    public GigyaApiRequest(int method, @NonNull String api, @NonNull TreeMap<String, Object> originalParameters) {
        this.method = method;
        this.api = api;
        this.originalParameters = originalParameters;
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

    @NonNull
    public TreeMap<String, Object> getOriginalParameters() {
        return originalParameters;
    }

    public void setOriginalParameters(@NonNull TreeMap<String, Object> originalParameters) {
        this.originalParameters = originalParameters;
    }

    public void sign(@NonNull String url, @Nullable String encodedParams) {
        this.url = url;
        this.encodedParams = encodedParams;
    }
}
