package com.gigya.android.sdk.auth;


import android.content.Intent;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;

public interface IWebAuthnService {

    void register(ActivityResultLauncher<IntentSenderRequest> resultLauncher);

    void login(ActivityResultLauncher<IntentSenderRequest> resultLauncher);

    void handleFidoResult(int resultCode, Intent data);

}
