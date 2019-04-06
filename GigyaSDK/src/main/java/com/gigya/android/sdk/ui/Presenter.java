package com.gigya.android.sdk.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.SparseArray;

import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.GigyaPluginCallback;
import com.gigya.android.sdk.services.Config;
import com.gigya.android.sdk.ui.plugin.IWebViewFragmentFactory;
import com.gigya.android.sdk.ui.plugin.PluginFragment;
import com.gigya.android.sdk.ui.provider.ProviderFragment;
import com.gigya.android.sdk.utils.UrlUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Presenter implements IPresenter {

    final private Context _context;
    final private Config _config;
    final private IWebViewFragmentFactory _pfgFactory;

    private static final String REDIRECT_URI = "gsapi://result/";

    public Presenter(Context context, Config config, IWebViewFragmentFactory pfgFactory) {
        _context = context;
        _config = config;
        _pfgFactory = pfgFactory;
    }

    @Override
    public void showPlugin(final boolean obfuscate, final String plugin, final Map<String, Object> params, final GigyaPluginCallback gigyaPluginCallback) {
        if (!params.containsKey("lang")) {
            params.put("lang", "en");
        }
        if (!params.containsKey("deviceType")) {
            params.put("deviceType", "mobile");
        }
        HostActivity.present(_context, new HostActivity.HostActivityLifecycleCallbacks() {
            @Override
            public void onCreate(final AppCompatActivity activity, @Nullable Bundle savedInstanceState) {
                Bundle args = new Bundle();
                args.putBoolean(PluginFragment.ARG_OBFUSCATE, obfuscate);
                args.putString(PluginFragment.ARG_PLUGIN, plugin);
                args.putSerializable(WebViewFragment.ARG_PARAMS, (HashMap) params);
                _pfgFactory.showPluginFragment(activity, args, gigyaPluginCallback);
            }
        });
    }

    @Override
    public void showNativeLoginProviders(List<String> providers, final Map<String, Object> params, final GigyaLoginCallback gigyaLoginCallback) {
        params.put("enabledProviders", TextUtils.join(",", providers));
        final String url = getPresentationUrl(params, "login");
        HostActivity.present(_context, new HostActivity.HostActivityLifecycleCallbacks() {
            @Override
            public void onCreate(final AppCompatActivity activity, @Nullable Bundle savedInstanceState) {
                Bundle args = new Bundle();
                args.putString(ProviderFragment.ARG_TITLE, "Sign in");
                args.putString(ProviderFragment.ARG_URL, url);
                args.putString(ProviderFragment.ARG_REDIRECT_PREFIX, "gsapi");
                args.putSerializable(WebViewFragment.ARG_PARAMS, (HashMap) params);
                _pfgFactory.showProviderFragment(activity, params, args, gigyaLoginCallback);
            }
        });
    }

    @Override
    public String getPresentationUrl(Map<String, Object> params, String requestType) {
        // Setup parameters.
        final Map<String, Object> urlParams = new HashMap<>();
        urlParams.put("apiKey", _config.getApiKey());
        urlParams.put("requestType", requestType);
        if (params.containsKey("enabledProviders")) {
            final String enabledProviders = (String) params.get("enabledProviders");
            if (enabledProviders != null)
                urlParams.put("enabledProviders", enabledProviders);
        }
        if (params.containsKey("disabledProviders")) {
            final String disabledProviders = (String) params.get("disabledProviders");
            if (disabledProviders != null)
                urlParams.put("disabledProviders", disabledProviders);
        }
        if (params.containsKey("lang")) {
            final String lang = (String) params.get("lang");
            if (lang != null)
                urlParams.put("lang", lang);
        }
        if (params.containsKey("cid")) {
            final String cid = (String) params.get("cid");
            if (cid != null)
                urlParams.put("cid", cid);
        }
        urlParams.put("sdk", Gigya.VERSION);
        urlParams.put("redirect_uri", REDIRECT_URI);
        final String qs = UrlUtils.buildEncodedQuery(urlParams);

        // Setup url.
        final String endpoint = "gs/mobile/loginui.aspx";
        final String protocol = "https";
        final String domainPrefix = "socialize";
        return String.format("%s://%s.%s/%s?%s", protocol, domainPrefix, _config.getApiDomain(), endpoint, qs);
    }

    public static final String SHOW_FULL_SCREEN = "style_show_full_screen";
    public static final String PROGRESS_COLOR = "style_progress_color";
    public static final String CORNER_RADIUS = "style_corner_radius";
    public static final String DIALOG_MAX_WIDTH = "dialog_max_width";
    public static final String DIALOG_MAX_HEIGHT = "dialog_max_height";

    //region HOST ACTIVITY LIFECYCLE CALLBACKS TRACKING

    // TODO: 03/01/2019 When dropping support for <17 devices remove static references!!! Use Binder instead to attach the callbacks to the activity intent.

    private static SparseArray<HostActivity.HostActivityLifecycleCallbacks> lifecycleSparse = new SparseArray<>();

    public static int addLifecycleCallbacks(HostActivity.HostActivityLifecycleCallbacks callbacks) {
        int id = callbacks.hashCode();
        lifecycleSparse.append(id, callbacks);
        return id;
    }

    public static HostActivity.HostActivityLifecycleCallbacks getCallbacks(int id) {
        return lifecycleSparse.get(id);
    }

    public static void flushLifecycleCallbacks(int id) {
        lifecycleSparse.remove(id);
    }

    public static void flush() {
        lifecycleSparse.clear();
        System.gc();
    }

    //endregion
}
