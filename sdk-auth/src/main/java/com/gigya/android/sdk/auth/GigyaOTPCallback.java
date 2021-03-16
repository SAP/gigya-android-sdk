package com.gigya.android.sdk.auth;

import androidx.annotation.NonNull;

import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.auth.resolvers.IGigyaOtpResult;
import com.gigya.android.sdk.network.GigyaError;

public abstract class GigyaOTPCallback<A extends GigyaAccount> extends GigyaLoginCallback<A> {

    public void onPendingOTPVerification(@NonNull GigyaApiResponse response, @NonNull IGigyaOtpResult resolver) {
        onError(GigyaError.fromResponse(response));
    }
}
