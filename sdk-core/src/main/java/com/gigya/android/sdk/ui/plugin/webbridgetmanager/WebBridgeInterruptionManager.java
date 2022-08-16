package com.gigya.android.sdk.ui.plugin.webbridgetmanager;

import static com.gigya.android.sdk.ui.plugin.PluginEventDef.HIDE;

import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.api.IBusinessApiService;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.ui.plugin.GigyaPluginEvent;
import com.gigya.android.sdk.ui.plugin.GigyaPluginFragment;
import com.gigya.android.sdk.ui.plugin.PluginAuthEventDef;

import java.util.Map;

public class WebBridgeInterruptionManager<A extends GigyaAccount> implements IWebBridgeInterruptionManager<A> {

    private static final String LOG_TAG = "WebBridgeInterruptionManager";

    private IWebBridgeInterruptionResolver<A> resolver;

    private final IBusinessApiService<A> businessApiService;

    public WebBridgeInterruptionManager(IBusinessApiService<A> businessApiService) {
        this.businessApiService = businessApiService;
    }

    @Override
    public void responseManager(
            Map<String, Object> params,
            A data,
            GigyaPluginFragment.IBridgeCallbacks<A> callback) {
        if (this.resolver == null) {
            callback.onPluginAuthEvent(PluginAuthEventDef.LOGIN, data);
            return;
        }
        GigyaLogger.debug(LOG_TAG, "responseManager: available resolver");
        this.resolver.resolve(params, data, callback);
    }

    private final IWebBridgeInterruptionResolverDispose disposeResolver = new IWebBridgeInterruptionResolverDispose() {

        @SuppressWarnings("rawtypes")
        @Override
        public void dispose(GigyaPluginFragment.IBridgeCallbacks callbacks, boolean hide) {
            GigyaLogger.debug(LOG_TAG, "dispose resolver");
            if (hide) {
                callbacks.onPluginEvent(GigyaPluginEvent.hide(), "");
            }
            WebBridgeInterruptionManager.this.resolver = null;
        }
    };

    @Override
    public void interruptionHandler(GigyaError error) {
        if (error.getErrorCode() == WEB_BRIDGE_INTERRUPTION_FORCE_LINK) {
            GigyaLogger.debug(LOG_TAG, "interruptionHandler: force link error code interruption");
            this.resolver = new WebBridgeForceLoginResolver(businessApiService, disposeResolver);
        }
    }

    @Override
    public boolean overrideEvent(String event) {
        if (event.equals(HIDE)) {
            if (resolver == null) return false;
            GigyaLogger.debug(LOG_TAG, "resolver active: " + resolver.isActive());
            return resolver.isActive();
        }
        return false;
    }

    // Supported interruptions.
    public static final int WEB_BRIDGE_INTERRUPTION_FORCE_LINK = 409003;

}
