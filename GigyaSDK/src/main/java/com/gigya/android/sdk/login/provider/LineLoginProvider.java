package com.gigya.android.sdk.login.provider;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.gigya.android.sdk.login.LoginProvider;

import java.util.Map;

public class LineLoginProvider extends LoginProvider {

    @Override
    public String getName() {
        return "line";
    }

    public LineLoginProvider(LoginProviderCallbacks loginCallbacks) {
        super(loginCallbacks, null);
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
    public void logout(Context context) {

    }

    @Override
    public String getProviderSessions(String tokenOrCode, long expiration, String uid) {
        return null;
    }

    @Override
    public void login(Context context, Map<String, Object> loginParams) {

    }
}
