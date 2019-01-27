package com.gigya.android.sdk.ui.plugin;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.gigya.android.sdk.ui.WebViewFragment;
import com.gigya.android.sdk.utils.UrlUtils;

import org.json.JSONArray;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PluginFragment extends WebViewFragment {

    private static final String LOG_TAG = "PluginFragment";

    /* Arguments. */
    public static final String ARG_API_KEY = "arg_api_key";
    public static final String ARG_API_DOMAIN = "arg_api_domain";
    public static final String ARG_OBFUSCATE = "arg_obfuscate";
    public static final String ARG_PLUGIN = "arg_plugin";
    public static final String ARG_PARAMS = "arg_params";

    public static void present(AppCompatActivity activity, Bundle args) {
        PluginFragment fragment = new PluginFragment();
        fragment.setArguments(args);
        FragmentTransaction fragmentTransaction = activity.getSupportFragmentManager().beginTransaction();
        fragmentTransaction.add(fragment, LOG_TAG);
        fragmentTransaction.commitAllowingStateLoss();
    }

    private String _apiKey, _apiDomain, _plugin;
    private HashMap<String, Object> _params;

    private boolean _obfuscate;

    @SuppressWarnings("unchecked")
    @Override
    protected void parseArguments() {
        if (getArguments() == null) {
            return;
        }
        Bundle args = getArguments();
        _apiKey = args.getString(ARG_API_KEY);
        _obfuscate = args.getBoolean(ARG_OBFUSCATE);
        _plugin = args.getString(ARG_PLUGIN);
        _params = (HashMap<String, Object>) args.getSerializable(ARG_PARAMS);

        if (_apiKey == null || _plugin == null) {
            /* Implementation error. */
            dismiss();
        }
    }

    @Override
    protected void setUpWebView() {
        super.setUpWebView();

        WebBridge webBridge = new WebBridge();
        webBridge.attach(_webView, _apiKey, _obfuscate);

        _webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return super.shouldOverrideUrlLoading(view, request);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
            }
        });

        _webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                return super.onShowFileChooser(webView, filePathCallback, fileChooserParams);
            }
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        final String html = getHTML("", "");
        _webView.loadDataWithBaseURL("http://www.gigya.com", html, "text/html", "utf-8", null);
    }

    private String getHTML(final String apiKey, final String apiDomain) {
        String template =
                "<head>" +
                        "<meta name='viewport' content='initial-scale=1,maximum-scale=1,user-scalable=no' />" +
                        "<script>" +
                        "function onJSException(ex) {" +
                        "document.location.href = '%s://%s?ex=' + encodeURIComponent(ex);" +
                        "}" +
                        "function onJSLoad() {" +
                        "if (gigya && gigya.isGigya)" +
                        "window.__wasSocializeLoaded = true;" +
                        "}" +
                        "setTimeout(function() {" +
                        "if (!window.__wasSocializeLoaded)" +
                        "document.location.href = '%s://%s';" +
                        "}, %s);" +
                        "</script>" +
                        "<script src='https://cdns." + apiDomain + "/JS/gigya.js?apikey=%s' type='text/javascript' onLoad='onJSLoad();'>" +
                        "{" +
                        "deviceType: 'mobile'" +
                        "}" +
                        "</script>" +
                        "</head>" +
                        "<body>" +
                        "<div id='%s'></div>" +
                        "<script>" +
                        "%s" +
                        "try {" +
                        "gigya._.apiAdapters.mobile.showPlugin('%s', %s);" +
                        "} catch (ex) { onJSException(ex); }" +
                        "</script>" +
                        "</body>";
        return null;
    }

    private static class WebBridge {

        private static final String LOG_TAG = "WebBridge";

        private enum Actions {
            IS_SESSION_VALID,
            SEND_REQUEST,
            SEND_OAUTH_REQUEST,
            GET_IDS,
            ON_PLUGIN_EVENT,
            ON_CUSTOM_EVENT,
            REGISTER_FOR_NAMESPACE_EVENTS,
            ON_JS_EXCEPTION
        }

        @SuppressLint("AddJavascriptInterface")
        void attach(WebView webView, final String apiKey, final boolean obfuscate) {
            webView.addJavascriptInterface(new Object() {

                @JavascriptInterface
                public String getAPIKey() {
                    return apiKey;
                }

                @JavascriptInterface
                public String getAdapterName() {
                    return "mobile";
                }

                @JavascriptInterface
                public String getObfuscationStrategy() {
                    if (obfuscate) {
                        return "base64";
                    } else {
                        return "";
                    }
                }

                @JavascriptInterface
                public String getFeatures() {
                    JSONArray features = new JSONArray();
                    for (Actions feature : Actions.values()) {
                        features.put(feature.toString().toLowerCase(Locale.ROOT));
                    }
                    return features.toString();
                }

                @JavascriptInterface
                public boolean sendToMobile(String action, String method, String queryStringParams) {
                    return invoke(action, method, queryStringParams);
                }

            }, "__gigAPIAdapterSettings");
        }

        public boolean invoke(String actionString, String method, String queryStringParams) {
            final Map<String, Object> params = UrlUtils.parseUrlParameters(queryStringParams);
            Actions action;
            try {
                action = Actions.valueOf(actionString.toLowerCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                ex.printStackTrace();
                return true;
            }

            switch (action) {
                case IS_SESSION_VALID:
                    break;
                case SEND_REQUEST:
                    break;
                case GET_IDS:
                    break;
                case SEND_OAUTH_REQUEST:
                    break;
                case ON_PLUGIN_EVENT:
                    break;
                case REGISTER_FOR_NAMESPACE_EVENTS:
                    break;
            }

            return true;
        }
    }
}
