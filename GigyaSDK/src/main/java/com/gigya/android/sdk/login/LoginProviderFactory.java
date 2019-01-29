package com.gigya.android.sdk.login;

import android.content.Context;
import android.support.annotation.NonNull;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.login.provider.FacebookLoginProvider;
import com.gigya.android.sdk.login.provider.GoogleLoginProvider;
import com.gigya.android.sdk.login.provider.LineLoginProvider;
import com.gigya.android.sdk.login.provider.WeChatLoginProvider;
import com.gigya.android.sdk.login.provider.WebViewLoginProvider;
import com.gigya.android.sdk.model.Configuration;

public class LoginProviderFactory {

    public static LoginProvider providerFor(Context context, Configuration configuration, @NonNull String providerName, GigyaCallback callback) {
        switch (providerName.toLowerCase()) {
            case "facebook":
                if (FacebookLoginProvider.isAvailable(context)) {
                    return new FacebookLoginProvider(callback);
                }
                break;
            case "googleplus":
                if (GoogleLoginProvider.isAvailable(context)) {
                    return new GoogleLoginProvider(context, callback);
                }
                break;
            case "line":
                if (LineLoginProvider.isAvailable(context)) {
                    return new LineLoginProvider(callback);
                }
                break;
            case "wechat":
                if (WeChatLoginProvider.isAvailable(context)) {
                    return new WeChatLoginProvider(callback);
                }
                break;
        }
        return new WebViewLoginProvider(configuration, callback);
    }

}
