package com.gigya.android.sdk.ui.plugin;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.gigya.android.sdk.GigyaPluginCallback;
import com.gigya.android.sdk.model.Configuration;
import com.gigya.android.sdk.ui.GigyaPresenter;
import com.gigya.android.sdk.ui.HostActivity;

import java.util.HashMap;
import java.util.Map;

public class GigyaPluginPresenter extends GigyaPresenter {

    public static final String SHOW_FULL_SCREEN = "show_full_screen";

    public void showPlugin(Context context,
                           final Configuration configuration,
                           final boolean obfuscate,
                           final String plugin,
                           final Map<String, Object> params,
                           final GigyaPluginCallback callback) {

        if (!params.containsKey("lang")) {
            params.put("lang", "en");
        }
        if (!params.containsKey("deviceType")) {
            params.put("deviceType", "mobile");
        }
        HostActivity.present(context, new HostActivity.HostActivityLifecycleCallbacks() {
            @Override
            public void onCreate(final AppCompatActivity activity, @Nullable Bundle savedInstanceState) {
                Bundle args = new Bundle();
                args.putString(PluginFragment.ARG_API_KEY, configuration.getApiKey());
                args.putString(PluginFragment.ARG_API_DOMAIN, configuration.getApiDomain());
                args.putBoolean(PluginFragment.ARG_OBFUSCATE, obfuscate);
                args.putString(PluginFragment.ARG_PLUGIN, plugin);
                args.putSerializable(PluginFragment.ARG_PARAMS, (HashMap) params);
                PluginFragment.present(activity, args, callback);
            }
        });
    }
}
