package com.gigya.android.sample.wxapi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.login.LoginProvider;
import com.gigya.android.sdk.login.provider.WeChatLoginProvider;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;

public class WXEntryActivity extends Activity implements IWXAPIEventHandler {

    private void handleWXIntent() {
        final WeChatLoginProvider loginProvider = getLoginProvider();
        if (loginProvider != null) {
            loginProvider.handleIntent(getIntent(), this);
        }
    }

    private void handleWXResponse(BaseResp baseResp) {
        final WeChatLoginProvider loginProvider = getLoginProvider();
        if (loginProvider != null) {
            loginProvider.handleResponse(baseResp);
        }
        finish();
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
        // Stub. Currently unused.
    }

    @Override
    public void onResp(BaseResp baseResp) {
        handleWXResponse(baseResp);
    }

    @Nullable
    private WeChatLoginProvider getLoginProvider() {
        /* Be sure to create initialize the Gigya instance. */
        final LoginProvider loginProvider = Gigya.getInstance().getCurrentProvider();
        if (loginProvider instanceof WeChatLoginProvider) {
            return (WeChatLoginProvider) loginProvider;
        }
        return null;
    }

    @Override
    public void finish() {
        super.finish();
        /*
        Disable exit animation.
         */
        overridePendingTransition(0, 0);
    }
}
