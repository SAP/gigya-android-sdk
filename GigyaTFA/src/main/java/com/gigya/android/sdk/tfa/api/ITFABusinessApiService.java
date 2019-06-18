package com.gigya.android.sdk.tfa.api;

import android.support.annotation.NonNull;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.api.GigyaApiResponse;

public interface ITFABusinessApiService {

    void optIntoPush(final String deviceInfo, @NonNull final GigyaCallback<GigyaApiResponse> gigyaCallback);
}
