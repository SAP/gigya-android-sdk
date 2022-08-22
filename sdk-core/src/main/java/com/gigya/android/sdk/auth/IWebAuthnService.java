package com.gigya.android.sdk.auth;


import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.api.GigyaApiResponse;

public interface IWebAuthnService<A extends GigyaAccount> {

    void register(
            ActivityResultLauncher<IntentSenderRequest> resultLauncher,
            GigyaCallback<GigyaApiResponse> gigyaCallback);

    void login(
            ActivityResultLauncher<IntentSenderRequest> resultLauncher,
            GigyaLoginCallback<A> gigyaCallback);

    void revoke(
            String uid,
            ActivityResultLauncher<IntentSenderRequest> resultLauncher,
            GigyaCallback<GigyaApiResponse> gigyaCallback
    );

    void handleFidoResult(
            ActivityResult activityResult);

}
