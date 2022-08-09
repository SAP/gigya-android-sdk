package com.gigya.android.sdk.ui.plugin.webbridgetmanager;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.api.IBusinessApiService;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.ui.plugin.GigyaPluginFragment;
import com.gigya.android.sdk.ui.plugin.PluginAuthEventDef;

import java.util.Map;

public class WebBridgeForceLoginResolver<A extends GigyaAccount> implements IWebBridgeInterruptionResolver<A> {

    private final IBusinessApiService<A> businessApiService;
    private final IWebBridgeInterruptionResolverDispose dispose;

    public WebBridgeForceLoginResolver(
            IBusinessApiService<A> businessApiService,
            IWebBridgeInterruptionResolverDispose dispose) {
        this.businessApiService = businessApiService;
        this.dispose = dispose;
    }

    @Override
    public void resolve(Map<String, Object> params, A data, final GigyaPluginFragment.IBridgeCallbacks<A> callback) {
        if (params.containsKey("loginMode")) {
            final String loginMode = (String) params.get("loginMode");
            if (loginMode != null && loginMode.equals("connect")) {
                this.businessApiService.getAccount(new GigyaCallback<A>() {
                    @Override
                    public void onSuccess(A obj) {
                        callback.onPluginAuthEvent(PluginAuthEventDef.LOGIN, obj);
                        WebBridgeForceLoginResolver.this.dispose.dispose();
                    }

                    @Override
                    public void onError(GigyaError error) {
                        WebBridgeForceLoginResolver.this.dispose.dispose();
                    }
                });
            }
        }
    }
}
