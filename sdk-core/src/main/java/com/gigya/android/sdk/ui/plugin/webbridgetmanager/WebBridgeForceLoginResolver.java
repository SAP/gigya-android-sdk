package com.gigya.android.sdk.ui.plugin.webbridgetmanager;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaDefinitions;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.api.IBusinessApiService;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.ui.plugin.GigyaPluginFragment;
import com.gigya.android.sdk.ui.plugin.PluginAuthEventDef;

import java.util.Map;

public class WebBridgeForceLoginResolver<A extends GigyaAccount> implements IWebBridgeInterruptionResolver<A> {

    private static final String LOG_TAG = "WebBridgeForceLoginResolver";

    private static final String LOGIN_MODE_KEY = "loginMode";
    private static final String LOGIN_MODE_CONNECT = "connect";
    private static final String FINALIZE_API = GigyaDefinitions.API.API_FINALIZE_REGISTRATION;

    private final IBusinessApiService<A> businessApiService;
    private final IWebBridgeInterruptionResolverDispose dispose;

    private boolean active = false;

    public WebBridgeForceLoginResolver(
            IBusinessApiService<A> businessApiService,
            IWebBridgeInterruptionResolverDispose dispose) {
        this.businessApiService = businessApiService;
        this.dispose = dispose;
        this.active = true;
    }

    @Override
    public void resolve(String api, Map<String, Object> params, A data,
                        final GigyaPluginFragment.IBridgeCallbacks<A> callback) {
        if (api == null) {
            return;
        }
        GigyaLogger.debug(LOG_TAG, "api = " + api);

        if (api.equals(FINALIZE_API)) {
            getAccountAndNotify(callback);
            return;
        }
        if (params.containsKey(LOGIN_MODE_KEY)) {
            final String loginMode = (String) params.get(LOGIN_MODE_KEY);
            GigyaLogger.debug(LOG_TAG, "resolve with loginMode = " + (loginMode != null ? loginMode : "null"));
            if (loginMode != null && loginMode.equals(LOGIN_MODE_CONNECT)) {
                getAccountAndNotify(callback);
            }
        }
    }

    private void getAccountAndNotify(
            final GigyaPluginFragment.IBridgeCallbacks<A> callback
    ) {
        this.businessApiService.getAccount(true, new GigyaCallback<A>() {
            @Override
            public void onSuccess(A obj) {
                GigyaLogger.debug(LOG_TAG, "resolve: end flow. notify dispose");
                callback.onPluginAuthEvent(PluginAuthEventDef.LOGIN, obj);
                WebBridgeForceLoginResolver.this.active = false;
                WebBridgeForceLoginResolver.this.dispose.dispose(callback, true);
            }

            @Override
            public void onError(GigyaError error) {
                GigyaLogger.debug(LOG_TAG, "error:\n" + error.toString());
                WebBridgeForceLoginResolver.this.active = false;
                WebBridgeForceLoginResolver.this.dispose.dispose(callback, true);
            }
        });
    }

    @Override
    public boolean isActive() {
        return this.active;
    }
}
