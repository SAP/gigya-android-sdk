package com.gigya.android.sdk.auth;

import android.app.Activity;
import android.content.Intent;

public interface IWebAuthnService {

    void register(Activity activity);

    void login(Activity activity);

    void handleIntent(int requestCode, int resultCode, Intent data);

}
