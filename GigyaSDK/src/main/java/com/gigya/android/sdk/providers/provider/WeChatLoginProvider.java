package com.gigya.android.sdk.providers.provider;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.gigya.android.sdk.GigyaContext;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.providers.LoginProvider;
import com.gigya.android.sdk.ui.HostActivity;
import com.gigya.android.sdk.utils.FileUtils;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import org.json.JSONObject;

import java.util.Map;

import static com.gigya.android.sdk.GigyaDefinitions.Providers.WECHAT;

public class WeChatLoginProvider extends LoginProvider {

    @Override
    public String getName() {
        return WECHAT;
    }

    public WeChatLoginProvider(GigyaContext gigyaContext, GigyaLoginCallback callback) {
        super(gigyaContext, callback);
    }

    private IWXAPI _api;
    private String _appId;

    public static boolean isAvailable(Context context) {
        try {
            String appId = FileUtils.stringFromMetaData(context, "wechatAppID");
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

                _appId = FileUtils.stringFromMetaData(context, "wechatAppID");
                if (_appId == null) {
                    _loginCallbacks.onProviderLoginFailed(getName(), "Failed to fetch application id");
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
                    final String providerSessions = getProviderSessionsForRequest(authCode, -1L, _appId);
                    _loginCallbacks.onProviderLoginSuccess(this, providerSessions);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case BaseResp.ErrCode.ERR_USER_CANCEL:
                _loginCallbacks.onCanceled();
                break;
            case BaseResp.ErrCode.ERR_AUTH_DENIED:
                _loginCallbacks.onProviderLoginFailed(getName(), Errors.AUTHENTICATION_DENIED);
                break;
        }
    }

    @Override
    public String getProviderSessionsForRequest(String tokenOrCode, long expiration, String uid) {
        /* code is relevant */
        try {
            return new JSONObject()
                    .put(getName(), new JSONObject()
                            .put("code", tokenOrCode).put("providerUID", uid)).toString();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
