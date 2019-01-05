package com.gigya.android.sdk.login.provider;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.gigya.android.sdk.login.LoginProvider;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import java.util.Map;

public class WeChatLoginProvider extends LoginProvider {

    public WeChatLoginProvider(LoginProviderCallbacks loginCallbacks) {
        super(loginCallbacks);
    }

    public static boolean isAvailable(Context context) {
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            String wechatAppId = (String) appInfo.metaData.get("wechatAppID");
            IWXAPI api = WXAPIFactory.createWXAPI(context, wechatAppId, false);
            boolean appRegistered = api.registerApp(wechatAppId);
            return (wechatAppId != null && appRegistered && api.isWXAppInstalled());
        }
        catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void login(Context context, Map<String, Object> loginParams) {

    }

    @Override
    public void logout() {

    }
}
