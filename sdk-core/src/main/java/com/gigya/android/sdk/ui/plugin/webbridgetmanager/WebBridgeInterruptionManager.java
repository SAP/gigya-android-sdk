package com.gigya.android.sdk.ui.plugin.webbridgetmanager;

import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.api.IBusinessApiService;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.ui.plugin.GigyaPluginFragment;
import com.gigya.android.sdk.ui.plugin.PluginAuthEventDef;

import java.util.Map;

public class WebBridgeInterruptionManager<A extends GigyaAccount> implements IWebBridgeInterruptionManager<A> {

    private IWebBridgeInterruptionResolver<A> resolver;

    private final IBusinessApiService<A> businessApiService;

    public WebBridgeInterruptionManager(IBusinessApiService<A> businessApiService) {
        this.businessApiService = businessApiService;
    }

    @Override
    public void responseManager(Map<String, Object> params, A data, GigyaPluginFragment.IBridgeCallbacks<A> callback) {
        if (this.resolver == null) {
            callback.onPluginAuthEvent(PluginAuthEventDef.LOGIN, data);
            return;
        }
        this.resolver.resolve(params, data, callback);
    }

    private final IWebBridgeInterruptionResolverDispose disposeResolver = new IWebBridgeInterruptionResolverDispose() {
        @Override
        public void dispose() {
            WebBridgeInterruptionManager.this.resolver = null;
        }
    };

    @Override
    public void interruptionHandler(GigyaError error) {
        if (error.getErrorCode() == WEB_BRIDGE_INTERRUPTION_FORCE_LINK) {
            this.resolver = new WebBridgeForceLoginResolver(businessApiService, disposeResolver);
        }
    }

    // Supported interruptions.
    public static final int WEB_BRIDGE_INTERRUPTION_FORCE_LINK = 409003;

}
