package com.gigya.android.sdk.auth.api;

import android.support.annotation.NonNull;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.api.GigyaApiResponse;

public interface IAuthBusinessApiService {

    void registerDevice(@NonNull final String deviceInfo, @NonNull final GigyaCallback<GigyaApiResponse> gigyaCallback);

    void unregisterDevice(@NonNull final GigyaCallback<GigyaApiResponse> gigyaCallback);

    void verifyPush(@NonNull final String vToken, @NonNull final GigyaCallback<GigyaApiResponse> gigyaCallback);

}
