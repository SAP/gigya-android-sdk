package com.gigya.android.sdk;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gigya.android.sdk.api.bloc.GigyaTFAResolver;
import com.gigya.android.sdk.interruption.link.LinkAccountsResolver;
import com.gigya.android.sdk.network.GigyaApiResponse;
import com.gigya.android.sdk.network.GigyaError;

public abstract class GigyaLoginCallback<A> extends GigyaCallback<A> {

    public void onPendingVerification(@NonNull GigyaApiResponse response, @Nullable String regToken) {
        forwardError(response);
    }

    public void onPendingRegistration(@NonNull GigyaApiResponse response, @Nullable String regToken) {
        forwardError(response);
    }

    //region Link accounts

    public void onConflictingAccounts(@NonNull GigyaApiResponse response, @NonNull LinkAccountsResolver resolver) {
        forwardError(response);
    }

    //endregion

    //region Password change

    public void onPendingPasswordChange(@NonNull GigyaApiResponse response) {
        forwardError(response);
    }

    //endregion

    //region TFA

    public void onPendingTFARegistration(@NonNull GigyaApiResponse response, @NonNull GigyaTFAResolver resolver) {
        forwardError(response);
    }

    public void onPendingTFAVerification(@NonNull GigyaApiResponse response, @NonNull GigyaTFAResolver resolver) {
        forwardError(response);
    }

    public void onPhoneTFAVerificationCodeSent() {
        // Stub.
    }

    public void onTOTPQrCodeAvailable(@NonNull String qrCode) {
        // Stub.
    }

    //endregion

    public void forwardError(@NonNull GigyaApiResponse response) {
        onError(GigyaError.fromResponse(response));
    }

}
