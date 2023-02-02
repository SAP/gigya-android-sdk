package com.gigya.android.sample.gigya.providers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.gigya.android.sample.R;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.providers.external.IProviderWrapper;
import com.gigya.android.sdk.providers.external.IProviderWrapperCallback;
import com.gigya.android.sdk.providers.external.ProviderWrapper;
import com.gigya.android.sdk.ui.HostActivity;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import java.util.HashMap;
import java.util.Map;

public class WechatProviderWrapper extends ProviderWrapper implements IProviderWrapper {

    private IWXAPI _api;
    private BaseResp _resp;

    public WechatProviderWrapper(Context context) {
        super(context, R.string.wechat_app_id);
        if (pId != null) {
            _api = WXAPIFactory.createWXAPI(context, pId, false);
            _api.registerApp(pId);
        } else {
            GigyaLogger.error("WechatProviderWrapper", "Missing App ID.");
        }
    }

    @Override
    public void login(Context context, Map<String, Object> params, final IProviderWrapperCallback callback) {
        HostActivity.present(context, new HostActivity.HostActivityLifecycleCallbacks() {
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
                    handleResponse(params, _resp, activity, callback);
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

    public void onResponse(BaseResp resp) {
        _resp = resp;
    }

    private void handleResponse(Map<String, Object> loginParams, BaseResp baseResp, Activity activity, final IProviderWrapperCallback callback) {
        switch (baseResp.errCode) {
            case BaseResp.ErrCode.ERR_OK:
                try {
                    SendAuth.Resp sendResp = (SendAuth.Resp) baseResp;
                    final String authCode = sendResp.code;
                    final Map<String, Object> loginMap = new HashMap<>();
                    loginMap.put("code", authCode);
                    loginMap.put("uid", pId);
                    callback.onLogin(loginMap);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case BaseResp.ErrCode.ERR_USER_CANCEL:
                callback.onCanceled();
                break;
            case BaseResp.ErrCode.ERR_AUTH_DENIED:
                callback.onFailed("authentication_denied");
                break;
        }
        if (activity != null) {
            _resp = null;
            activity.finish();
        }
    }

    public void handleIntent(Intent intent, IWXAPIEventHandler eventHandler) {
        if (_api != null) {
            _api.handleIntent(intent, eventHandler);
        }
    }
}
