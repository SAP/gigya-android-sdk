package com.gigya.android.sdk;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gigya.android.sdk.interruption.ConflictingProviderResolver;
import com.gigya.android.sdk.interruption.PendingPasswordChangeResolver;

import java.util.List;

public abstract class GigyaLoginCallback<T> extends GigyaCallback<T> {

    public void onCancelledOperation() {
        // Stub.
    }

    public void onIntermediateLoad() {
        // Stub.
    }

    public void onPendingVerification(@Nullable String regToken) {
        // Stub.
    }

    public void onPendingRegistration(@Nullable String regToken) {
        // Stub.
    }

    //region Link accounts

    public void onConflictingAccounts(List<String> conflictingProviders, ConflictingProviderResolver resolver) {
        // Stub.
    }

    //endregion

    //region Password change

    public void onPendingPasswordChange(@NonNull PendingPasswordChangeResolver resolver) {
        // Stub.
    }

    public void onResetPasswordEmailSent() {
        // Stub.
    }

    public void onResetPasswordSecurityVerificationFailed(@NonNull PendingPasswordChangeResolver resolver) {
        // Stub.
    }

    //endregion


}
