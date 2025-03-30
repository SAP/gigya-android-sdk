package com.gigya.android.sdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.interruption.IPendingRegistrationResolver;
import com.gigya.android.sdk.interruption.link.ILinkAccountsResolver;
import com.gigya.android.sdk.interruption.tfa.TFAResolverFactory;
import com.gigya.android.sdk.interruption.tfa.models.TFAProviderModel;
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
        onError(GigyaError.fromResponse(response));
    }

    public void onPendingRegistration(@NonNull GigyaApiResponse response, @NonNull IPendingRegistrationResolver resolver) {
        onError(GigyaError.fromResponse(response));
    }

    public void onConflictingAccounts(@NonNull GigyaApiResponse response, @NonNull ILinkAccountsResolver resolver) {
        onError(GigyaError.fromResponse(response));
    }

    public void onPendingPasswordChange(@NonNull GigyaApiResponse response) {
        onError(GigyaError.fromResponse(response));
    }

    public void onPendingTwoFactorRegistration(@NonNull GigyaApiResponse response, @NonNull final List<TFAProviderModel> inactiveProviders,
                                               @NonNull TFAResolverFactory resolverFactory) {
        onError(GigyaError.fromResponse(response));
    }

    public void onPendingTwoFactorVerification(@NonNull GigyaApiResponse response, @NonNull final List<TFAProviderModel> activeProviders,
                                               @NonNull TFAResolverFactory resolverFactory) {
        onError(GigyaError.fromResponse(response));
    }

    public void onCaptchaRequired(@NonNull GigyaApiResponse response) {
        onError(GigyaError.fromResponse(response));
    }

}
