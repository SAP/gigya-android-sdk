package com.gigya.android.sample.wxapi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.login.provider.WeChatLoginProvider;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;

public class WXEntryActivity extends Activity implements IWXAPIEventHandler {

    private static final String TAG = "WXEntryActivity";

    private void handleWXIntent() {
        Gigya gigya = Gigya.getInstance();
        if (gigya != null) {
            WeChatLoginProvider loginProvider = (WeChatLoginProvider) gigya.getLoginProvider();
            if (loginProvider != null) {
                loginProvider.handleIntent(getIntent(), this);
            }
        }
    }

    private void handleWXResponse(BaseResp baseResp) {
        Gigya gigya = Gigya.getInstance();
        if (gigya != null) {
            WeChatLoginProvider loginProvider = (WeChatLoginProvider) Gigya.getInstance().getLoginProvider();
            if (loginProvider != null) {
                loginProvider.handleResponse(baseResp);
            }
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleWXIntent();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleWXIntent();
    }

    @Override
    public void onReq(BaseReq baseReq) {
        // Stub.
    }

    @Override
    public void onResp(BaseResp baseResp) {
        handleWXResponse(baseResp);
    }
}
