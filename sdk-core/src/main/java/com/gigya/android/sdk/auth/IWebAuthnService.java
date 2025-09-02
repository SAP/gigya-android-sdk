package com.gigya.android.sdk.auth;


import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.auth.models.WebAuthnKeyModel;
import com.gigya.android.sdk.auth.passkeys.IPasskeysAuthenticationProvider;

import java.util.List;
import java.util.Map;

public interface IWebAuthnService<A extends GigyaAccount> {

    @Deprecated
    void register(
            ActivityResultLauncher<IntentSenderRequest> resultLauncher,
            GigyaCallback<GigyaApiResponse> gigyaCallback);

    @Deprecated
    void login(
            ActivityResultLauncher<IntentSenderRequest> resultLauncher,
            GigyaLoginCallback<A> gigyaCallback);

    @Deprecated
    void login(
            ActivityResultLauncher<IntentSenderRequest> resultLauncher,
            Map<String, Object> params,
            GigyaLoginCallback<A> gigyaCallback);

    @Deprecated
    void revoke(
            GigyaCallback<GigyaApiResponse> gigyaCallback
    );

    @Deprecated
    void handleFidoResult(
            ActivityResult activityResult);

    void register(
            GigyaCallback<GigyaApiResponse> gigyaCallback);

    void login(GigyaLoginCallback<A> gigyaCallback);

    void login(Map<String, Object> params, GigyaLoginCallback<A> gigyaCallback);

    void getCredentials(
            GigyaCallback<GigyaApiResponse> gigyaCallback
    );

    void revoke(
            String id,
            GigyaCallback<GigyaApiResponse> gigyaCallback
    );

    void setPasskeyAuthenticationProvider(IPasskeysAuthenticationProvider provider);

}
