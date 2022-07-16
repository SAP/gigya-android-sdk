package com.gigya.android.sdk.auth;


import androidx.activity.ComponentActivity;

public interface IWebAuthnService {

    void register(ComponentActivity activity);

    void login(ComponentActivity activity);

}
