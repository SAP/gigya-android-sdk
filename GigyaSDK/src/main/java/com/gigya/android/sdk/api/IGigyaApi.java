package com.gigya.android.sdk.api;

import android.support.annotation.NonNull;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.network.GigyaApiResponse;

public interface IGigyaApi<T> {

    void onRequestError(String api, GigyaApiResponse apiResponse, GigyaCallback<T> callback);

    void onRequestSuccess(@NonNull String api, GigyaApiResponse apiResponse, GigyaCallback<T> callback);

}
