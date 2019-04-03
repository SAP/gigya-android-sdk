package com.gigya.android.sdk.plugin_view;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import com.gigya.android.sdk.GigyaPluginCallback;
import com.gigya.android.sdk.model.account.GigyaAccount;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.ui.WebBridge;
import com.gigya.android.sdk.ui.WebViewFragment;
import com.gigya.android.sdk.ui.plugin.GigyaPluginEvent;

public class PluginFragment extends WebViewFragment {

    public static final String LOG_TAG = "PluginFragment";

    public static final String PLUGIN_SCREENSETS = "accounts.screenSet";
    public static final String PLUGIN_COMMENTS = "comments.commentsUI";

    public static final String ARG_OBFUSCATE = "arg_obfuscate";
    public static final String ARG_PLUGIN = "arg_plugin";

    private IWebBridgeFactory _wbFactory;
    private GigyaPluginCallback _gigyaPluginCallback;
    private boolean _obfuscate;

    public static void present(AppCompatActivity activity, Bundle args, IWebBridgeFactory _wbFactory, GigyaPluginCallback gigyaPluginCallback) {
        PluginFragment fragment = new PluginFragment();
        fragment.setArguments(args);
        fragment.inject(_wbFactory, gigyaPluginCallback);
        FragmentTransaction fragmentTransaction = activity.getSupportFragmentManager().beginTransaction();
        fragmentTransaction.add(fragment, LOG_TAG);
        fragmentTransaction.commitAllowingStateLoss();
    }

    public void inject(IWebBridgeFactory wbFactory, GigyaPluginCallback gigyaPluginCallback) {
        _wbFactory = wbFactory;
        _gigyaPluginCallback = gigyaPluginCallback;
    }

    @Override
    protected void parseArguments() {
        Bundle args = getArguments();
        if (args != null) {
            _obfuscate = args.getBoolean(ARG_OBFUSCATE);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final WebBridge webBridge = _wbFactory.create(_obfuscate, new WebBridge.WebBridgeInteractions() {
            @Override
            public void onPluginEvent(GigyaPluginEvent event, String containerID) {

            }

            @Override
            public void onAuthEvent(WebBridge.AuthEvent authEvent, GigyaAccount obj) {

            }

            @Override
            public void onCancel() {

            }

            @Override
            public void onError(GigyaError error) {

            }
        });
        webBridge.attach(_webView);
    }


}
