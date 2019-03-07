package com.gigya.android.sdk;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gigya.android.sdk.api.bloc.GigyaTFAResolver;
import com.gigya.android.sdk.model.tfa.TFAEmail;
import com.gigya.android.sdk.model.tfa.TFARegisteredPhone;
import com.gigya.android.sdk.network.GigyaApiResponse;
import com.gigya.android.sdk.network.GigyaError;

import java.util.List;

public abstract class GigyaLoginCallback<A> extends GigyaCallback<A> {

    public void onPendingVerification(@NonNull GigyaApiResponse response, @Nullable String regToken) {
        forwardError(response);
    }

    public void onPendingRegistration(@NonNull GigyaApiResponse response, @Nullable String regToken) {
        forwardError(response);
    }

    //region Link accounts

//    public void onConflictingAccounts(@NonNull GigyaApiResponse response, @NonNull LinkAccountsResolver resolver) {
//        forwardError(response);
//    }

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

    public void onRegisteredTFAPhoneNumbers(@NonNull List<TFARegisteredPhone> registeredPhoneList) {
        // Stub.
    }

    public void onPhoneTFAVerificationCodeSent() {
        // Stub.
    }

    public void onTotpTFAQrCodeAvailable(@NonNull String qrCode) {
        // Stub.
    }

    public void onEmailTFAAddressesAvailable(@Nullable List<TFAEmail> emails) {
        // Stub.
    }

    public void onEmailTFAVerificationEmailSent() {
        // Stub.
    }

    //endregion

    public void forwardError(@NonNull GigyaApiResponse response) {
        onError(GigyaError.fromResponse(response));
    }

}
