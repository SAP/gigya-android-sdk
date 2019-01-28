package com.gigya.android.sdk.ui;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import com.gigya.android.sdk.DependencyRegistry;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.log.GigyaLogger;
import com.gigya.android.sdk.model.Configuration;
import com.gigya.android.sdk.utils.UrlUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class WebBridge {

    private static final String CALLBACK_JS_PATH = "gigya._.apiAdapters.mobile.mobileCallbacks";

    private static final String LOG_TAG = "WebBridge";

    private WeakReference<WebView> _webViewRef;

    private Configuration _configuration;
    private SessionManager _sessionManager;
    private boolean _shouldObfuscate;

    public WebBridge(boolean shouldObfuscate) {
        _shouldObfuscate = shouldObfuscate;
        DependencyRegistry.getInstance().inject(this);
    }

    public void inject(Configuration configuration, SessionManager sessionManager) {
        _configuration = configuration;
        _sessionManager = sessionManager;
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
        GigyaLogger.debug(LOG_TAG, "invokeCallback: " + baseInvocationString);
        String value = obfuscate(baseInvocationString, true);
        final String invocation = String.format("javascript:%s['%s'](%s);", CALLBACK_JS_PATH, callbackId, value);
        final WebView webView = _webViewRef.get();
        if (webView == null) {
            return;
        }
        webView.post(new Runnable() {
            @Override
            public void run() {
                if (android.os.Build.VERSION.SDK_INT > 18) {
                    webView.evaluateJavascript(invocation, new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String value) {
                            GigyaLogger.debug("Callback", value);
                        }
                    });
                } else {
                    webView.loadUrl(invocation);
                }
            }
        });
    }

    @SuppressLint("AddJavascriptInterface")
    public void attach(WebView webView) {
        _webViewRef = new WeakReference<>(webView);
        webView.addJavascriptInterface(new Object() {

            @JavascriptInterface
            public String getAPIKey() {
                return _configuration.getApiKey();
            }

            @JavascriptInterface
            public String getAdapterName() {
                return "mobile";
            }

            @JavascriptInterface
            public String getObfuscationStrategy() {
                if (_shouldObfuscate) {
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

    public boolean handleUrl(String url) {
        if (url.startsWith("gsapi://")) {
            Uri uri = Uri.parse(url);
            return invoke(uri.getHost(), uri.getPath().replace("/", ""), uri.getEncodedQuery());
        }
        return false;
    }

    private boolean invoke(String actionString, String method, String queryStringParams) {
        if (actionString == null) {
            return false;
        }

        GigyaLogger.debug(LOG_TAG, "invoke: " + actionString);

        final Map<String, Object> data = new HashMap<>();
        UrlUtils.parseUrlParameters(data, queryStringParams);
        GigyaLogger.debug(LOG_TAG, "invoke: data:\n" + data.toString());

        Actions action;
        try {
            action = Actions.valueOf(actionString.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            return true;
        }

        final String callbackId = (String) data.get("callbackID");

        final Map<String, Object> params = new HashMap<>();
        UrlUtils.parseUrlParameters(params, deobfuscate((String) data.get("params")));
        GigyaLogger.debug(LOG_TAG, "invoke: params:\n" + params.toString());

        final Map<String, Object> settings = new HashMap<>();
        UrlUtils.parseUrlParameters(settings, (String) data.get("settings"));
        GigyaLogger.debug(LOG_TAG, "invoke: settings:\n" + settings.toString());

        switch (action) {
            case GET_IDS:
                getIds(callbackId);
                break;
            case REGISTER_FOR_NAMESPACE_EVENTS:
                registerForNamespaceEvents();
                break;
            case SEND_REQUEST:
                sendRequest(callbackId, method, params, settings);
                break;
            case IS_SESSION_VALID:
                isSessionValid(callbackId);
                break;
            case SEND_OAUTH_REQUEST:
                break;
            case ON_PLUGIN_EVENT:
                onPluginEvent(params);
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

    //region Actions

    private void sendRequest(final String callbackId, String method, Map<String, Object> params, Map<String, Object> settings) {

    }

    private void isSessionValid(String callbackId) {
        invokeCallback(callbackId, String.valueOf(_sessionManager.isValidSession()));
    }

    private void sendOAuthRequest(final String callbackId, String method, Map<String, Object> params, Map<String, Object> settings) {
        // TODO: 28/01/2019 Should perform login.
    }

    private void onPluginEvent(Map<String, Object> params) {
        String containerId = (String) params.get("sourceContainerID");
        if (containerId != null) {
            // TODO: 28/01/2019 Throttle event.
        }
    }

    private void registerForNamespaceEvents() {
        // TODO: 28/01/2019 Not how to implement it yet.
    }

    //endregion

    //region Obfuscation

    @SuppressWarnings("CharsetObjectCanBeUsed")
    private String obfuscate(String string, boolean quote) {
        if (_shouldObfuscate) {
            try {
                byte[] data = string.getBytes("UTF-8");
                String base64 = Base64.encodeToString(data, Base64.DEFAULT);
                if (quote) {
                    return "\"" + base64 + "\"";
                } else {
                    return base64;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return string;
    }

    @SuppressWarnings("CharsetObjectCanBeUsed")
    private String deobfuscate(String base64String) {
        if (_shouldObfuscate) {
            try {
                byte[] data = Base64.decode(base64String, Base64.DEFAULT);
                return new String(data, "UTF-8");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return base64String;
    }

    //endregion
}

