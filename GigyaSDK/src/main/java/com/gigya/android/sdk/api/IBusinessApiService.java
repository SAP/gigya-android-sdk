package com.gigya.android.sdk.api;

import android.content.Context;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaDefinitions;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.providers.IProviderPermissionsCallback;

import java.util.Map;

public interface IBusinessApiService<A> {

    void getSDKConfig(final String nextApiTag, final GigyaCallback<A> gigyaCallback);

    void logout();

    void login(Map<String, Object> params, final GigyaLoginCallback<A> loginCallback);

    void login(Context context, @GigyaDefinitions.Providers.SocialProvider String socialProvider, Map<String, Object> params, GigyaLoginCallback<A> gigyaLoginCallback);

    void verifyLogin(String UID, final boolean ignoreSession, final GigyaCallback<A> gigyaCallback);

    void nativeSocialLogin(Map<String, Object> params, final GigyaLoginCallback<A> loginCallback, final Runnable optionalCompletionHandler);

    void finalizeRegistration(Map<String, Object> params, final GigyaLoginCallback<A> loginCallback);

    void getAccount(final GigyaCallback<A> gigyaCallback);

    void setAccount(A updatedAccount, final GigyaCallback<A> gigyaCallback);

    void refreshNativeProviderSession(Map<String, Object> params, final IProviderPermissionsCallback providerPermissionsCallback);
}
