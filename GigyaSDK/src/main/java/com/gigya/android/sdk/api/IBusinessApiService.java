package com.gigya.android.sdk.api;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaDefinitions;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.interruption.tfa.models.TFACompleteVerificationModel;
import com.gigya.android.sdk.interruption.tfa.models.TFAGetEmailsModel;
import com.gigya.android.sdk.interruption.tfa.models.TFAGetRegisteredPhoneNumbersModel;
import com.gigya.android.sdk.interruption.tfa.models.TFAInitModel;
import com.gigya.android.sdk.interruption.tfa.models.TFAProvidersModel;
import com.gigya.android.sdk.interruption.tfa.models.TFATotpRegisterModel;
import com.gigya.android.sdk.interruption.tfa.models.TFAVerificationCodeModel;
import com.gigya.android.sdk.providers.IProviderPermissionsCallback;

import java.util.Map;

public interface IBusinessApiService<A> {

    <V> void send(String api, Map<String, Object> params, int requestMethod, Class<V> clazz, GigyaCallback<V> gigyaCallback);

    void logout(final GigyaCallback<GigyaApiResponse> gigyaCallback);

    void login(Map<String, Object> params, final GigyaLoginCallback<A> loginCallback);

    void login(@GigyaDefinitions.Providers.SocialProvider String socialProvider, Map<String, Object> params, final GigyaLoginCallback<A> gigyaLoginCallback);

    void verifyLogin(String UID, final GigyaCallback<A> gigyaCallback);

    void notifyNativeSocialLogin(Map<String, Object> params, final GigyaLoginCallback<A> loginCallback, final Runnable optionalCompletionHandler);

    void finalizeRegistration(Map<String, Object> params, final GigyaLoginCallback<A> loginCallback);

    void register(final Map<String, Object> params, final GigyaLoginCallback<A> loginCallback);

    void getAccount(final GigyaCallback<A> gigyaCallback);

    void getAccount(final Map<String, Object> params, final GigyaCallback<A> gigyaCallback);

    void setAccount(A updatedAccount, final GigyaCallback<A> gigyaCallback);

    void setAccount(final Map<String, Object> params, final GigyaCallback<A> gigyaCallback);

    void refreshNativeProviderSession(Map<String, Object> params, final IProviderPermissionsCallback providerPermissionsCallback);

    void forgotPassword(String loginId, final GigyaCallback<GigyaApiResponse> callback);

    void addConnection(@GigyaDefinitions.Providers.SocialProvider String socialProvider, final GigyaLoginCallback<A> gigyaLoginCallback);

    void removeConnection(@GigyaDefinitions.Providers.SocialProvider String socialProvider, GigyaCallback<GigyaApiResponse> gigyaCallback);

    void getConflictingAccounts(final String regToken, final GigyaCallback<GigyaApiResponse> callback);

    void getTFAProviders(final String regToken, final GigyaCallback<TFAProvidersModel> callback);

    void initTFA(final String regToken, final String provider, final String mode, GigyaCallback<TFAInitModel> callback);

    void finalizeTFA(final String regToken, final String gigyaAssertion, final String providerAssertion, GigyaCallback<GigyaApiResponse> callback);

    void registerTotp(final String gigyaAssertion, GigyaCallback<TFATotpRegisterModel> callback);

    void verifyTotp(final String code, final String gigyaAssertion, final String sctToken, GigyaCallback<TFACompleteVerificationModel> callback);

    void getRegisteredPhoneNumbers(final String gigyaAssertion, GigyaCallback<TFAGetRegisteredPhoneNumbersModel> callback);

    void registerPhoneNumber(final String gigyaAssertion, String phoneNumber, String method, GigyaCallback<TFAVerificationCodeModel> callback);

    void verifyPhoneNumber(final String gigyaAssertion, String phoneId, String method, GigyaCallback<TFAVerificationCodeModel> callback);

    void completePhoneVerification(final String gigyaAssertion, final String code, final String phvToken, GigyaCallback<TFACompleteVerificationModel> callback);

    void getRegisteredEmails(final String gigyaAssertion, GigyaCallback<TFAGetEmailsModel> callback);

    void verifyEmail(final String emailId, final String gigyaAssertion, GigyaCallback<TFAVerificationCodeModel> callback);

    void completeEmailVerification(final String gigyaAssertion, final String code, final String phvToken, GigyaCallback<TFACompleteVerificationModel> callback);
}
