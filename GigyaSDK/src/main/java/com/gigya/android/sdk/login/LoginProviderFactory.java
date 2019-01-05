package com.gigya.android.sdk.login;

import android.content.Context;
import android.support.annotation.Nullable;

import com.gigya.android.sdk.login.provider.FacebookLoginProvider;
import com.gigya.android.sdk.login.provider.GoogleLoginProvider;

public class LoginProviderFactory {

    @Nullable
    public static LoginProvider providerFor(Context context, String providerName, LoginProvider.LoginProviderCallbacks loginCallbacks) {
        switch (providerName) {
            case "facebook":
                if (FacebookLoginProvider.isAvailable(context)) {
                    return new FacebookLoginProvider(loginCallbacks);
                }
                break;
            case "google":
                if (GoogleLoginProvider.isAvailable(context)) {
                    return new GoogleLoginProvider(loginCallbacks);
                }
                break;
        }
        return null;
    }
}
