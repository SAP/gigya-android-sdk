package com.gigya.android.sdk.api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaDefinitions;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.account.IAccountService;
import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.interruption.tfa.models.TFAProvidersModel;
import com.gigya.android.sdk.providers.IProviderPermissionsCallback;
import com.gigya.android.sdk.schema.GigyaSchema;

import java.util.Map;

public interface IBusinessApiService<A extends GigyaAccount> {

    IAccountService<A> getAccountService();

    <V> void send(String api, Map<String, Object> params, int requestMethod, Class<V> clazz, GigyaCallback<V> gigyaCallback);

    <V> void send(String api, Map<String, Object> params, Map<String, String> headers, Class<V> clazz, GigyaCallback<V> gigyaCallback);

    void logout(final GigyaCallback<GigyaApiResponse> gigyaCallback);

    void login(Map<String, Object> params, final GigyaLoginCallback<A> loginCallback);

    void login(@GigyaDefinitions.Providers.SocialProvider String socialProvider, Map<String, Object> params, final GigyaLoginCallback<A> gigyaLoginCallback);

    void verifyLogin(String UID, final GigyaCallback<A> gigyaCallback);

    void verifyLogin(String UID, Map<String, Object> params, final GigyaCallback<A> gigyaCallback);

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

    void addConnection(@GigyaDefinitions.Providers.SocialProvider String socialProvider, @NonNull Map<String, Object> params, final GigyaLoginCallback<A> gigyaLoginCallback);

    void removeConnection(@GigyaDefinitions.Providers.SocialProvider String socialProvider, GigyaCallback<GigyaApiResponse> gigyaCallback);

    void getConflictingAccounts(final String regToken, final GigyaCallback<GigyaApiResponse> callback);

    void getTFAProviders(final String regToken, final GigyaCallback<TFAProvidersModel> callback);

    void updateDevice(@NonNull String pushToken, @NonNull final GigyaCallback<GigyaApiResponse> gigyaCallback);

    void handleAccountApiResponse(GigyaApiResponse response, GigyaLoginCallback<A> loginCallback);

    void isAvailableLoginId(@NonNull final String id, @NonNull final GigyaCallback<Boolean> gigyaCallback);

    void getSchema(@Nullable Map<String, Object> params, @NonNull final GigyaCallback<GigyaSchema> gigyaCallback);

    void getSDKConfig();

}