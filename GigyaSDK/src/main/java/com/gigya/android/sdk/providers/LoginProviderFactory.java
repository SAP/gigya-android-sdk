package com.gigya.android.sdk.providers;

import android.content.Context;
import android.support.annotation.NonNull;

import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.providers.provider.FacebookLoginProvider;
import com.gigya.android.sdk.providers.provider.GoogleLoginProvider;
import com.gigya.android.sdk.providers.provider.LineLoginProvider;
import com.gigya.android.sdk.providers.provider.WeChatLoginProvider;
import com.gigya.android.sdk.providers.provider.WebViewLoginProvider;
import com.gigya.android.sdk.services.ApiService;

public class LoginProviderFactory {

    public static LoginProvider providerFor(Context context, ApiService apiService,
                                            @NonNull String providerName, GigyaLoginCallback callback) {
        switch (providerName.toLowerCase()) {
            case "facebook":
                if (FacebookLoginProvider.isAvailable(context)) {
                    return new FacebookLoginProvider(apiService, callback);
                }
                break;
            case "googleplus":
                if (GoogleLoginProvider.isAvailable(context)) {
                    return new GoogleLoginProvider(context, apiService, callback);
                }
                break;
            case "line":
                if (LineLoginProvider.isAvailable(context)) {
                    return new LineLoginProvider(apiService, callback);
                }
                break;
            case "wechat":
                if (WeChatLoginProvider.isAvailable(context)) {
                    return new WeChatLoginProvider(apiService, callback);
                }
                break;
        }
        return new WebViewLoginProvider(apiService, callback);
    }

}
