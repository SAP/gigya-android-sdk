package com.gigya.android.sdk.login.provider;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.gigya.android.sdk.login.LoginProvider;

import java.util.Map;

public class LineLoginProvider extends LoginProvider {

    public LineLoginProvider(LoginProviderCallbacks loginCallbacks) {
        super(loginCallbacks);
    }

    public static boolean isAvailable(Context context) {
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            String lineChannelID = (String) appInfo.metaData.get("lineChannelID");
            Class.forName("com.linecorp.linesdk.auth.LineLoginApi");
            return lineChannelID != null;
        } catch (Throwable t) {
            return false;
        }
    }
    @Override
    public void logout() {

    }

    @Override
    public void login(Context context, Map<String, Object> loginParams) {

    }
}
