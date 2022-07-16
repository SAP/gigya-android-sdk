package com.gigya.android.sdk.providers.provider;

import static com.gigya.android.sdk.GigyaDefinitions.Providers.WECHAT;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.persistence.IPersistenceService;
import com.gigya.android.sdk.ui.HostActivity;
import com.gigya.android.sdk.utils.FileUtils;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import org.json.JSONObject;

import java.util.Map;

public class WeChatProvider extends Provider {

    private static final String LOG_TAG = "WeChatProvider";

    public WeChatProvider(Context context,
                          FileUtils fileUtils,
                          IPersistenceService persistenceService,
                          ProviderCallback providerCallback) {
        super(context, persistenceService, providerCallback);
        _appId = fileUtils.stringFromMetaData("wechatAppID");
        _api = WXAPIFactory.createWXAPI(_context, _appId, true);
        _api.registerApp(_appId);
    }

    private String _appId;
    private IWXAPI _api;
    private BaseResp _resp;

    @Override
    public String getName() {
        return WECHAT;
    }

    public static boolean isAvailable(Context context, FileUtils fileUtils) {
        try {
            String appId = fileUtils.stringFromMetaData("wechatAppID");
            IWXAPI api = WXAPIFactory.createWXAPI(context, appId, false);
            return (appId != null && api.isWXAppInstalled());
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void login(final Map<String, Object> loginParams, String loginMode) {
        if (_connecting) {
            return;
        }
        _connecting = true;
        _loginMode = loginMode;
        HostActivity.present(_context, new HostActivity.HostActivityLifecycleCallbacks() {
            @Override
            public void onCreate(AppCompatActivity activity, @Nullable Bundle savedInstanceState) {
                final SendAuth.Req req = new SendAuth.Req();
                req.scope = "snsapi_userinfo";
                req.state = "";
                _api.sendReq(req);
            }

            @Override
            public void onResume(AppCompatActivity activity) {
                if (_resp != null) {
                    handleResponse(loginParams, _resp, activity);
                }
            }

        });
    }

    @Override
    public void logout() {
        super.logout();
        if (_api != null) {
            _api.detach();
        }
    }

    public void handleIntent(Intent intent, IWXAPIEventHandler eventHandler) {
        if (_api != null && _api.isWXAppInstalled()) {
            _api.handleIntent(intent, eventHandler);
        }
    }

    public void onResponse(BaseResp resp) {
        _resp = resp;
    }

    private void handleResponse(Map<String, Object> loginParams, BaseResp baseResp, Activity activity) {
        switch (baseResp.errCode) {
            case BaseResp.ErrCode.ERR_OK:
                try {
                    SendAuth.Resp sendResp = (SendAuth.Resp) baseResp;
                    final String authCode = sendResp.code;
                    final String providerSessions = getProviderSessions(authCode, -1L, _appId);
                    onLoginSuccess(loginParams, providerSessions, _loginMode);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case BaseResp.ErrCode.ERR_USER_CANCEL:
                onCanceled();
                break;
            case BaseResp.ErrCode.ERR_AUTH_DENIED:
                onLoginFailed("authentication_denied");
                break;
        }
        if (activity != null) {
            _resp = null;
            activity.finish();
        }
    }

    @Override
    public String getProviderSessions(String tokenOrCode, long expiration, String uid) {
        // code is relevant
        try {
            final String json = new JSONObject()
                    .put(getName(), new JSONObject()
                            .put("code", tokenOrCode).put("providerUID", uid)).toString();
            GigyaLogger.debug(LOG_TAG, "Provider sessions: " + json);
            return json;
        } catch (Exception ex) {
            _connecting = false;
            ex.printStackTrace();
        }
        return null;
    }
}
