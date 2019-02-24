package com.gigya.android.sdk;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gigya.android.sdk.interruption.ConflictingProviderResolver;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.GigyaResponse;

public abstract class GigyaLoginCallback<T> extends GigyaCallback<T> {

    public void onCancelledOperation() {
        // Stub.
    }

    public void onIntermediateLoad() {
        // Stub.
    }

    public void onPendingVerification(@NonNull GigyaResponse response, @Nullable String regToken) {
        forwardError(response);
    }

    public void onPendingRegistration(@NonNull GigyaResponse response, @Nullable String regToken) {
        forwardError(response);
    }

    //region Link accounts

    public void onConflictingAccounts(@NonNull GigyaResponse response, ConflictingProviderResolver resolver) {
        forwardError(response);
    }

    //endregion

    //region Password change

    public void onPendingPasswordChange(@NonNull GigyaResponse response) {
        forwardError(response);
    }

    //endregion

    public void forwardError(@NonNull GigyaResponse response) {
        onError(GigyaError.fromResponse(response));
    }

}
