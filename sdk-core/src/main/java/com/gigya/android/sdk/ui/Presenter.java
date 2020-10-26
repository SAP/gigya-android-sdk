package com.gigya.android.sdk.ui;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.SparseArray;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.GigyaPluginCallback;
import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.api.IBusinessApiService;
import com.gigya.android.sdk.ui.plugin.IWebViewFragmentFactory;
import com.gigya.android.sdk.ui.provider.ProviderFragment;
import com.gigya.android.sdk.utils.UrlUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Presenter<A extends GigyaAccount> implements IPresenter<A> {

    public static class Consts {

        public static final String REDIRECT_URL_SCHEME = "gsapi";
        public static final String ON_JS_LOAD_ERROR = "on_js_load_error";
        public static final String ON_JS_EXCEPTION = "on_js_exception";
        public static final String CONTAINER_ID = "pluginContainer";
        public static final int JS_TIMEOUT = 10000;
    }

    public static final String ARG_STYLE_SHOW_FULL_SCREEN = "arg_style_show_full_screen";
    public static final String ARG_OBFUSCATE = "arg_obfuscate";

    final private Context _context;
    final private Config _config;
    final private IWebViewFragmentFactory<A> _pfgFactory;

    private static final String REDIRECT_URI = "gsapi://result/";

    public Presenter(Context context, Config config, IWebViewFragmentFactory<A> pfgFactory) {
        _context = context;
        _config = config;
        _pfgFactory = pfgFactory;
    }

    @Override
    public void showPlugin(final boolean obfuscate,
                           final String plugin,
                           final boolean fullScreen,
                           final Map<String, Object> params,
                           final GigyaPluginCallback<A> gigyaPluginCallback) {
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
                args.putBoolean(ARG_STYLE_SHOW_FULL_SCREEN, fullScreen);
                args.putBoolean(ARG_OBFUSCATE, obfuscate);
                _pfgFactory.showPluginFragment(activity, plugin, params, args, gigyaPluginCallback);
            }
        });
    }

    @Override
    public void showNativeLoginProviders(List<String> providers,
                                         final IBusinessApiService businessApiService,
                                         final Map<String, Object> params,
                                         final GigyaLoginCallback<A> gigyaLoginCallback) {
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
                _pfgFactory.showProviderFragment(activity, businessApiService, params, args, gigyaLoginCallback);
            }
        });
    }

    @Override
    public void clearOnLogout() {
        // Clearing cached cookies.
        CookieManager cookieManager = CookieManager.getInstance();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.flush();
        } else {
            CookieSyncManager.createInstance(_context);
            cookieManager.removeAllCookie();
        }
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


    //region HOST ACTIVITY LIFECYCLE CALLBACKS TRACKING

    private static SparseArray<HostActivity.HostActivityLifecycleCallbacks> lifecycleSparse = new SparseArray<>();
    private static SparseArray<WebLoginActivity.WebLoginActivityCallback> webLoginLifecycleSparse = new SparseArray<>();

    public static int addLifecycleCallbacks(HostActivity.HostActivityLifecycleCallbacks callbacks) {
        int id = callbacks.hashCode();
        lifecycleSparse.append(id, callbacks);
        return id;
    }

    public static int addWebLoginLifecycleCallback(WebLoginActivity.WebLoginActivityCallback callback) {
        int id = callback.hashCode();
        webLoginLifecycleSparse.append(id, callback);
        return id;
    }

    public static HostActivity.HostActivityLifecycleCallbacks getCallbacks(int id) {
        return lifecycleSparse.get(id);
    }

    public static WebLoginActivity.WebLoginActivityCallback getWebLoginCallback(int id) {
        return webLoginLifecycleSparse.get(id);
    }

    public static void flushLifecycleCallbacks(int id) {
        lifecycleSparse.remove(id);
    }

    public static void flushWebLoginLifecycleCallback(int id) {
        webLoginLifecycleSparse.remove(id);
    }

    public static void flush() {
        lifecycleSparse.clear();
        webLoginLifecycleSparse.clear();
        System.gc();
    }

    //endregion
}
