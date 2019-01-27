package com.gigya.android.sdk.ui.plugin;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.gigya.android.sdk.ApiManager;
import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.PersistenceManager;
import com.gigya.android.sdk.model.Configuration;
import com.gigya.android.sdk.ui.GigyaPresenter;
import com.gigya.android.sdk.ui.HostActivity;

import java.util.HashMap;
import java.util.Map;

public class GigyaPluginPresenter extends GigyaPresenter {
    public GigyaPluginPresenter(ApiManager apiManager, PersistenceManager persistenceManager) {
        super(apiManager, persistenceManager);
    }

    public <T> void showPlugin(Context context, final Configuration configuration, final boolean obfuscate,
                               final String plugin, final Map<String, Object> params, final GigyaCallback<T> callback) {
        HostActivity.present(context, new HostActivity.HostActivityLifecycleCallbacks() {
            @Override
            public void onCreate(AppCompatActivity activity, @Nullable Bundle savedInstanceState) {
                Bundle args = new Bundle();
                args.putString(PluginFragment.ARG_API_KEY, configuration.getApiKey());
                args.putString(PluginFragment.ARG_API_DOMAIN, configuration.getApiDomain());
                args.putBoolean(PluginFragment.ARG_OBFUSCATE, obfuscate);
                args.putString(PluginFragment.ARG_PLUGIN, plugin);
                args.putSerializable(PluginFragment.ARG_PARAMS, (HashMap) params);
                PluginFragment.present(activity, args);
            }
        });
    }
}
