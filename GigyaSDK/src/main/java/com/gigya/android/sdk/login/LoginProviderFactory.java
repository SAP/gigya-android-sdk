package com.gigya.android.sdk.login;

import android.content.Context;
import android.support.annotation.Nullable;

import com.gigya.android.sdk.login.provider.FacebookLoginProvider;
import com.gigya.android.sdk.login.provider.GoogleLoginProvider;
import com.gigya.android.sdk.login.provider.LineLoginProvider;
import com.gigya.android.sdk.login.provider.WeChatLoginProvider;

public class LoginProviderFactory {

    @Nullable
    public static LoginProvider providerFor(Context context, String providerName, LoginProvider.LoginProviderCallbacks loginCallbacks) {
        switch (providerName) {
            case "facebook":
                if (FacebookLoginProvider.isAvailable(context)) {
                    return new FacebookLoginProvider(loginCallbacks);
                }
                break;
            case "googlePlus":
                if (GoogleLoginProvider.isAvailable(context)) {
                    return new GoogleLoginProvider(loginCallbacks);
                }
                break;
            case "line":
                if (LineLoginProvider.isAvailable(context)) {
                    return new LineLoginProvider(loginCallbacks);
                }
                break;
            case "wechat":
                if (WeChatLoginProvider.isAvailable(context)) {
                    return new WeChatLoginProvider(loginCallbacks);
                }
                break;
        }
        return null;
    }
}
