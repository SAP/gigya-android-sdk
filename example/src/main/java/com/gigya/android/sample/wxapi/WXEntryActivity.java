package com.gigya.android.sample.wxapi;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.gigya.android.sample.gigya.providers.WechatProviderWrapper;
import com.gigya.android.sample.model.MyAccount;
import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.providers.external.ExternalProvider;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;

public class WXEntryActivity extends AppCompatActivity implements IWXAPIEventHandler {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WechatProviderWrapper provider = getProvider();
        if (provider != null) {
            provider.handleIntent(getIntent(), this);
        }
        finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        WechatProviderWrapper provider = getProvider();
        if (provider != null) {
            provider.handleIntent(getIntent(), this);
        }
    }

    @Override
    public void onReq(BaseReq baseReq) {
        // Stub. Currently unused.
        Log.d("sd", "sd");
    }

    @Override
    public void onResp(BaseResp baseResp) {
        WechatProviderWrapper provider = getProvider();
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
    private WechatProviderWrapper getProvider() {
        ExternalProvider provider = (ExternalProvider) Gigya.getInstance(MyAccount.class).getUsedSocialProvider("wechat");
        return (WechatProviderWrapper) provider.getWrapper();
    }
}
