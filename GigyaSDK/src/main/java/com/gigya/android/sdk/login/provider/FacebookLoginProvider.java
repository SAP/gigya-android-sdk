package com.gigya.android.sdk.login.provider;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.gigya.android.sdk.login.LoginProvider;

public class FacebookLoginProvider extends LoginProvider {

    private final CallbackManager _callbackManager = CallbackManager.Factory.create();

    public static boolean isAvailable(Context context) {
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            String fbAppId = (String) appInfo.metaData.get("com.facebook.sdk.ApplicationId");
            Class.forName("com.facebook.login.LoginManager");
            return fbAppId != null;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void login() {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        boolean isLoggedIn = accessToken != null && !accessToken.isExpired();
        if (isLoggedIn) {
            return;
        }

    }
}
