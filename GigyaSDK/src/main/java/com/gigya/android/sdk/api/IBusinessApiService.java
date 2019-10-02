package com.gigya.android.sdk.api;

import android.support.annotation.NonNull;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaDefinitions;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.interruption.tfa.models.TFAProvidersModel;
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

    void getAccount(@NonNull final String[] include, @NonNull final String[] profileExtraFields, GigyaCallback<A> gigyaCallback);

    void setAccount(A updatedAccount, final GigyaCallback<A> gigyaCallback);

    void setAccount(final Map<String, Object> params, final GigyaCallback<A> gigyaCallback);

    void refreshNativeProviderSession(Map<String, Object> params, final IProviderPermissionsCallback providerPermissionsCallback);

    void forgotPassword(Map<String, Object> params, final GigyaCallback<GigyaApiResponse> callback);

    void addConnection(@GigyaDefinitions.Providers.SocialProvider String socialProvider, final GigyaLoginCallback<A> gigyaLoginCallback);

    void removeConnection(@GigyaDefinitions.Providers.SocialProvider String socialProvider, GigyaCallback<GigyaApiResponse> gigyaCallback);

    void getConflictingAccounts(final String regToken, final GigyaCallback<GigyaApiResponse> callback);

    void getTFAProviders(final String regToken, final GigyaCallback<TFAProvidersModel> callback);
}
