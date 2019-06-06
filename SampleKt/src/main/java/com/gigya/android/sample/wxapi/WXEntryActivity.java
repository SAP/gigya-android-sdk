package com.gigya.android.sample.wxapi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.gigya.android.sample.model.MyAccount;
import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.providers.provider.WeChatProvider;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;

import static com.gigya.android.sdk.GigyaDefinitions.Providers.WECHAT;

public class WXEntryActivity extends Activity implements IWXAPIEventHandler {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WeChatProvider provider = getProvider();
        if (provider != null) {
            provider.handleIntent(getIntent(), this);
        }
        finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        WeChatProvider provider = getProvider();
        if (provider != null) {
            provider.handleIntent(getIntent(), this);
        }
        finish();
    }

    @Override
    public void onReq(BaseReq baseReq) {
        // Stub. Currently unused.
    }

    @Override
    public void onResp(BaseResp baseResp) {
        WeChatProvider provider = getProvider();
        if (provider != null) {
            provider.onResponse(baseResp);
        }
    }

    @Override
    public void finish() {
        super.finish();
        // Disable exit animation.
        overridePendingTransition(0, 0);
    }

    @Nullable
    private WeChatProvider getProvider() {
        return (WeChatProvider) Gigya.getInstance(MyAccount.class).getUsedSocialProvider(WECHAT);
    }
}
