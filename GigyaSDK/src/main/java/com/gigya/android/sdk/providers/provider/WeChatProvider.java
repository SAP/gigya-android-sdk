package com.gigya.android.sdk.providers.provider;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.account.IAccountService;
import com.gigya.android.sdk.api.IApiObservable;
import com.gigya.android.sdk.persistence.IPersistenceService;
import com.gigya.android.sdk.session.ISessionService;
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

public class WeChatProvider extends Provider {

    public static final String LOG_TAG = "WeChatProvider";

    public WeChatProvider(Context context,
                          Config config,
                          ISessionService sessionService,
                          IAccountService accountService,
                          IPersistenceService persistenceService,
                          IApiObservable observable,
                          GigyaLoginCallback gigyaLoginCallback) {
        super(context, config, sessionService, accountService, persistenceService, observable, gigyaLoginCallback);
    }

    private IWXAPI _api;

    private static String _appId;
    private static BaseResp resp;

    @Override
    public String getName() {
        return WECHAT;
    }

    public static boolean isAvailable(Context context, FileUtils fileUtils) {
        try {
            _appId = fileUtils.stringFromMetaData("wechatAppID");
            IWXAPI api = WXAPIFactory.createWXAPI(context, _appId, false);
            return (_appId != null && api.isWXAppInstalled());
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void login(Map<String, Object> loginParams, String loginMode) {
        _loginMode = loginMode;
        HostActivity.present(_context, new HostActivity.HostActivityLifecycleCallbacks() {
            @Override
            public void onCreate(AppCompatActivity activity, @Nullable Bundle savedInstanceState) {
                _api = WXAPIFactory.createWXAPI(_context, _appId, true);
                _api.registerApp(_appId);

                final SendAuth.Req req = new SendAuth.Req();
                req.scope = "snsapi_userinfo";
                req.state = "";
                _api.sendReq(req);
            }

            @Override
            public void onResume(AppCompatActivity activity) {
                if (resp != null) {
                    handleResponse(resp, activity);
                }
            }

        });
    }

    @Override
    public void logout() {
        if (_api != null) {
            _api.detach();
        }
    }

    public static void handleIntent(Context context, Intent intent, IWXAPIEventHandler eventHandler) {
        IWXAPI api = WXAPIFactory.createWXAPI(context, _appId, false);
        if (api.isWXAppInstalled()) {
            api.handleIntent(intent, eventHandler);
        }
    }

    public static void onResponse(BaseResp resp) {
        WeChatProvider.resp = resp;
    }

    private void handleResponse(BaseResp baseResp, Activity activity) {
        switch (baseResp.errCode) {
            case BaseResp.ErrCode.ERR_OK:
                try {
                    SendAuth.Resp sendResp = (SendAuth.Resp) baseResp;
                    final String authCode = sendResp.code;
                    final String providerSessions = getProviderSessions(authCode, -1L, _appId);
                    onLoginSuccess(providerSessions, _loginMode);
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
            activity.finish();
        }
    }

    @Override
    public String getProviderSessions(String tokenOrCode, long expiration, String uid) {
        // code is relevant
        try {
            return new JSONObject()
                    .put(getName(), new JSONObject()
                            .put("code", tokenOrCode).put("providerUID", uid)).toString();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean supportsTokenTracking() {
        return false;
    }

    @Override
    public void trackTokenChange() {
        // Stub.
    }
}
