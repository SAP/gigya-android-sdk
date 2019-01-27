package com.gigya.android.sdk.ui;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.gigya.android.sdk.DependencyRegistry;
import com.gigya.android.sdk.model.Configuration;
import com.gigya.android.sdk.utils.UrlUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;
import java.util.Map;

public class WebBridge {

    private static final String LOG_TAG = "WebBridge";

    private Configuration _configuration;

    public WebBridge() {
        DependencyRegistry.getInstance().inject(this);
    }

    public void inject(Configuration configuration) {
        _configuration = configuration;
    }

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

    private void invokeCallback(String callbackId, String baseInvocationString) {

    }

    @SuppressLint("AddJavascriptInterface")
    public void attach(WebView webView, final String apiKey, final boolean obfuscate) {
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

    public boolean handleUrl(WebView webView, String url) {
        if (url.startsWith("gsapi://")) {
            Uri uri = Uri.parse(url);
            return invoke(uri.getHost(), uri.getPath().replace("/", ""), uri.getEncodedQuery());
        }
        return false;
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

        final String callbackId = (String) params.get("callbackID");

        switch (action) {
            case GET_IDS:
                getIds(callbackId);
                break;
            case REGISTER_FOR_NAMESPACE_EVENTS:
                break;
            case SEND_REQUEST:
                break;
            case IS_SESSION_VALID:
                break;
            case SEND_OAUTH_REQUEST:
                break;
            case ON_PLUGIN_EVENT:
                break;
            case ON_CUSTOM_EVENT:
                break;
            case ON_JS_EXCEPTION:
                break;
            default:
                break;
        }

        return true;
    }

    private void getIds(String callbackId) {
        try {
            final String ids = new JSONObject()
                    .put("ucid", _configuration.getUCID()).put("gmid", _configuration.getGMID())
                    .toString();
            invokeCallback(callbackId, ids);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void sendReqeust() {

    }

    private void isSessionValid() {

    }

    private void sendOAuthRequest() {

    }

    private void onPluginEvent() {

    }

    private void registerForNamespaceEvents() {

    }
}

