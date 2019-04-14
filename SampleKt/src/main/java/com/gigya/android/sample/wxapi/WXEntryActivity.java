package com.gigya.android.sample.wxapi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.gigya.android.sdk.providers.provider.WeChatProvider;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;

public class WXEntryActivity extends Activity implements IWXAPIEventHandler {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WeChatProvider.handleIntent(this, getIntent(), this);
        finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        WeChatProvider.handleIntent(this, getIntent(), this);
        finish();
    }

    @Override
    public void onReq(BaseReq baseReq) {
        // Stub. Currently unused.
    }

    @Override
    public void onResp(BaseResp baseResp) {
        WeChatProvider.onResponse(baseResp);
    }

    @Override
    public void finish() {
        super.finish();
        // Disable exit animation.
        overridePendingTransition(0, 0);
    }
}
