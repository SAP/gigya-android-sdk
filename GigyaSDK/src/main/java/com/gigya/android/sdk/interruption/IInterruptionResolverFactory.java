package com.gigya.android.sdk.interruption;

import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.interruption.tfa.GigyaTFARegistrationResolver;
import com.gigya.android.sdk.interruption.tfa.GigyaTFAVerificationResolver;
import com.gigya.android.sdk.network.GigyaApiResponse;

public interface IInterruptionResolverFactory {

    GigyaLinkAccountsResolver createLinkAccountsResolver(GigyaApiResponse originalResponse, GigyaLoginCallback loginCallback);

    GigyaTFARegistrationResolver createTFARegistrationResolver(GigyaApiResponse originalResponse, GigyaLoginCallback loginCallback);

    GigyaTFAVerificationResolver createTFAVerificationResolver(GigyaApiResponse originalResponse, GigyaLoginCallback loginCallback);
}
