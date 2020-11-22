package com.gigya.android.sdk.api;

import androidx.annotation.NonNull;

import com.gigya.android.sdk.network.adapter.RestAdapter;

import java.util.TreeMap;

public class GigyaApiRequest {

    @NonNull
    private String api;
    private RestAdapter.HttpMethod method;
    @NonNull
    private TreeMap<String, Object> params;

    /*
    Anonymous requests will not be signed using timestamp, nonce & signature.
     */
    private boolean isAnonymous = false;

    public GigyaApiRequest(RestAdapter.HttpMethod method, @NonNull String api, @NonNull TreeMap<String, Object> params) {
        this.method = method;
        this.api = api;
        this.params = params;
    }

    public RestAdapter.HttpMethod getMethod() {
        return method;
    }

    public boolean isAnonymous() {
        return isAnonymous;
    }

    public void setAnonymous(boolean anonymous) {
        isAnonymous = anonymous;
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
    public TreeMap<String, Object> getParams() {
        return params;
    }

}
