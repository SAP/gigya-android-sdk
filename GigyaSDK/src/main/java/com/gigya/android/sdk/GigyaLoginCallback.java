package com.gigya.android.sdk;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gigya.android.sdk.interruption.link.IGigyaLinkAccountsResolver;
import com.gigya.android.sdk.interruption.tfa.IGigyaTFARegistrationResolver;
import com.gigya.android.sdk.interruption.tfa.IGigyaTFAVerificationResolver;
import com.gigya.android.sdk.interruption.tfa.TfaResolverFactory;
import com.gigya.android.sdk.interruption.tfa.models.TFAEmail;
import com.gigya.android.sdk.interruption.tfa.models.TFAProvider;
import com.gigya.android.sdk.interruption.tfa.models.TFARegisteredPhone;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.network.GigyaError;

import java.util.List;

/**
 * Gigya login response abstract callback.
 * Non abstract function are used for optional interruption handling.
 *
 * @param <A> Custom account type provided int the Gigya interface initialization. If non specified will use basic GigyaAccount type.
 */
public abstract class GigyaLoginCallback<A> extends GigyaCallback<A> {

    public void onPendingVerification(@NonNull GigyaApiResponse response, @Nullable String regToken) {
        forwardError(response);
    }

    public void onPendingRegistration(@NonNull GigyaApiResponse response, @Nullable String regToken) {
        forwardError(response);
    }

    //region LINK ACCOUNTS

    public void onConflictingAccounts(@NonNull GigyaApiResponse response, @NonNull IGigyaLinkAccountsResolver resolver) {
        forwardError(response);
    }

    //endregion

    //region PASSWORD CHANGE

    public void onPendingPasswordChange(@NonNull GigyaApiResponse response) {
        forwardError(response);
    }

    //endregion

    //region TFA

    public void onPendingTFARegistration(@NonNull GigyaApiResponse response, @NonNull IGigyaTFARegistrationResolver resolver) {
        forwardError(response);
    }

    public void onPendingTwoFactorRegistration(@NonNull GigyaApiResponse response, @NonNull List<TFAProvider> inactiveProviders,
                                               @NonNull TfaResolverFactory resolverFactory) {
        forwardError(response);
    }

    public void onPendingTwoFactorVerification(@NonNull GigyaApiResponse response, @NonNull List<TFAProvider> activeProviders,
                                               @NonNull TfaResolverFactory resolverFactory) {
        forwardError(response);
    }

    public void onPendingTFAVerification(@NonNull GigyaApiResponse response, @NonNull IGigyaTFAVerificationResolver resolver) {
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
