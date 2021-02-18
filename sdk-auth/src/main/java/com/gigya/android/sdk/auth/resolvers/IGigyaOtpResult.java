package com.gigya.android.sdk.auth.resolvers;

import androidx.annotation.NonNull;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.api.GigyaApiResponse;

public interface IGigyaOtpResult {

    void verify(@NonNull final String code);

}
