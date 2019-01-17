package com.gigya.android.sdk.login;

import android.content.Context;

import com.gigya.android.sdk.login.provider.FacebookLoginProvider;
import com.gigya.android.sdk.login.provider.GoogleLoginProvider;
import com.gigya.android.sdk.login.provider.LineLoginProvider;
import com.gigya.android.sdk.login.provider.WeChatLoginProvider;
import com.gigya.android.sdk.login.provider.WebViewLoginProvider;
import com.gigya.android.sdk.model.Configuration;

public class LoginProviderFactory {

    public static LoginProvider providerFor(Context context, Configuration configuration, String providerName,
                                            LoginProvider.LoginProviderCallbacks loginCallbacks, LoginProvider.LoginProviderTrackerCallback trackerCallback) {
        switch (providerName.toLowerCase()) {
            case "facebook":
                if (FacebookLoginProvider.isAvailable(context)) {
                    return new FacebookLoginProvider(loginCallbacks, trackerCallback);
                }
                break;
            case "googleplus":
                if (GoogleLoginProvider.isAvailable(context)) {
                    return new GoogleLoginProvider(context, loginCallbacks);
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
        return new WebViewLoginProvider(configuration, loginCallbacks);
    }

}
