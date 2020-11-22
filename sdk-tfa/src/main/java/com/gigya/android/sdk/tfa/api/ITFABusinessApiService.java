package com.gigya.android.sdk.tfa.api;

import androidx.annotation.NonNull;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.api.GigyaApiResponse;

public interface ITFABusinessApiService {

    void optIntoPush(@NonNull final String deviceInfo, @NonNull final GigyaCallback<GigyaApiResponse> gigyaCallback);

    void finalizePushOptIn(@NonNull final String gigyaAssertion, @NonNull final String verificationToken, @NonNull final GigyaCallback<GigyaApiResponse> gigyaCallback);

    void verifyPush(@NonNull final String gigyaAssertion, @NonNull final String verificationToken, @NonNull final GigyaCallback<GigyaApiResponse> gigyaCallback);
}
