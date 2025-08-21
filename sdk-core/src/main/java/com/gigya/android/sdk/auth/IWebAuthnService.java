package com.gigya.android.sdk.auth;


import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.auth.passkeys.IPasskeysAuthenticationProvider;
import com.gigya.android.sdk.auth.passkeys.PasswordLessKey;
import com.gigya.android.sdk.auth.passkeys.PasswordLessKeyType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface IWebAuthnService<A extends GigyaAccount> {

    void register(
            ActivityResultLauncher<IntentSenderRequest> resultLauncher,
            GigyaCallback<GigyaApiResponse> gigyaCallback);

    void register(
            GigyaCallback<GigyaApiResponse> gigyaCallback);

    void login(
            ActivityResultLauncher<IntentSenderRequest> resultLauncher,
            GigyaLoginCallback<A> gigyaCallback);

    void login(
            ActivityResultLauncher<IntentSenderRequest> resultLauncher,
            Map<String, Object> params,
            GigyaLoginCallback<A> gigyaCallback);

    void login(GigyaLoginCallback<A> gigyaCallback);

    void login(Map<String, Object> params, GigyaLoginCallback<A> gigyaCallback);

    void revoke(
            GigyaCallback<GigyaApiResponse> gigyaCallback
    );

    void getCredentials(
            GigyaCallback<GigyaApiResponse> gigyaCallback
    );

    List<PasswordLessKey> getKeys(String id);

    void handleFidoResult(
            ActivityResult activityResult);

    void setPasskeyAuthenticationProvider(IPasskeysAuthenticationProvider provider);

}
