package com.gigya.android.sdk;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gigya.android.sdk.interruption.ConflictingProviderResolver;
import com.gigya.android.sdk.model.tfa.TFAProvider;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.GigyaResponse;

import java.util.List;

public abstract class GigyaLoginCallback<T> extends GigyaCallback<T> {

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

    //region TFA

    public void onPendingTFARegistration(@NonNull List<TFAProvider> optionalProviders) {
        // Stub.
    }

    public void onPendingTFAVerification(@NonNull List<TFAProvider> availableProviders) {
        // Stub.
    }

    //endregion

    public void forwardError(@NonNull GigyaResponse response) {
        onError(GigyaError.fromResponse(response));
    }

}
