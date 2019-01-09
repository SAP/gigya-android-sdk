package com.gigya.android.sdk.login.provider;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.gigya.android.sdk.login.LoginProvider;
import com.gigya.android.sdk.ui.HostActivity;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import org.json.JSONObject;

import java.util.Map;

public class WeChatLoginProvider extends LoginProvider {

    public static final String NAME = "wechat";

    public WeChatLoginProvider(LoginProviderCallbacks loginCallbacks) {
        super(loginCallbacks);
    }

    private IWXAPI _api;
    private String _appId;

    public static boolean isAvailable(Context context) {
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            String appId = (String) appInfo.metaData.get("wechatAppID");
            IWXAPI api = WXAPIFactory.createWXAPI(context, appId, false);
            return (appId != null && api.isWXAppInstalled());
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void login(final Context context, Map<String, Object> loginParams) {
        HostActivity.present(context, new HostActivity.HostActivityLifecycleCallbacks() {
            @Override
            public void onCreate(AppCompatActivity activity, @Nullable Bundle savedInstanceState) {

                try {
                    final ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
                    _appId = (String) appInfo.metaData.get("wechatAppID");
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }

                if (_appId == null) {
                    loginCallbacks.onProviderLoginFailed(NAME, "Failed to fetch application id");
                    activity.finish();
                }

                _api = WXAPIFactory.createWXAPI(context, _appId, true);
                _api.registerApp(_appId);

                final SendAuth.Req req = new SendAuth.Req();
                req.scope = "snsapi_userinfo";
                req.state = "";
                _api.sendReq(req);

                // Finish the activity.
                activity.finish();
            }
        });
    }

    @Override
    public void logout(Context context) {
        if (_api != null) {
            _api.detach();
        }
    }

    public void handleIntent(Intent intent, IWXAPIEventHandler eventHandler) {
        if (_api != null) {
            _api.handleIntent(intent, eventHandler);
        }
    }

    public void handleResponse(BaseResp baseResp) {
        switch (baseResp.errCode) {
            case BaseResp.ErrCode.ERR_OK:
                try {
                    SendAuth.Resp sendResp = (SendAuth.Resp) baseResp;
                    final String authCode = sendResp.code;
                    final String providerSessions = getProviderSessions(authCode, -1L, _appId);
                    // Notify success.
                    loginCallbacks.onProviderLoginSuccess(NAME, providerSessions);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case BaseResp.ErrCode.ERR_USER_CANCEL:
                // Notify error.
                loginCallbacks.onProviderLoginFailed(NAME, Errors.USER_CANCELLED);
                break;
            case BaseResp.ErrCode.ERR_AUTH_DENIED:
                // Notify error.
                loginCallbacks.onProviderLoginFailed(NAME, Errors.AUTHENTICATION_DENIED);
                break;
        }
    }

    @Override
    public String getProviderSessions(String tokenOrCode, long expiration, String uid) {
        /* code is relevant */
        try {
            return new JSONObject()
                    .put(NAME, new JSONObject()
                            .put("code", tokenOrCode).put("providerUID", uid)).toString();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
