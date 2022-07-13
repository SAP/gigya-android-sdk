package com.gigya.android.sdk.api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gigya.android.sdk.network.adapter.RestAdapter;

import java.util.HashMap;
import java.util.TreeMap;

public class GigyaApiRequest {

    @NonNull
    private String api;
    private RestAdapter.HttpMethod method;
    @NonNull
    private TreeMap<String, Object> params;
    @Nullable
    private HashMap<String, String> headers;

    /*
    Anonymous requests will not be signed using timestamp, nonce & signature.
     */
    private boolean isAnonymous = false;

    public GigyaApiRequest(RestAdapter.HttpMethod method,
                           @NonNull String api,
                           @NonNull TreeMap<String, Object> params) {
        this.method = method;
        this.api = api;
        this.params = params;
    }

    public GigyaApiRequest(RestAdapter.HttpMethod method,
                           @NonNull String api,
                           @NonNull TreeMap<String, Object> params,
                           @Nullable HashMap<String, String> headers) {
        this.method = method;
        this.api = api;
        this.params = params;
        this.headers = headers;
    }

    public RestAdapter.HttpMethod getMethod() {
        return this.method;
    }

    public boolean isAnonymous() {
        return this.isAnonymous;
    }

    public void setAnonymous(boolean anonymous) {
        this.isAnonymous = anonymous;
    }

    @NonNull
    public String getTag() {
        return this.api;
    }

    @NonNull
    public String getApi() {
        return this.api;
    }

    @NonNull
    public TreeMap<String, Object> getParams() {
        return this.params;
    }

    @Nullable
    public HashMap<String, String> getHeaders() {
        return this.headers;
    }

}
