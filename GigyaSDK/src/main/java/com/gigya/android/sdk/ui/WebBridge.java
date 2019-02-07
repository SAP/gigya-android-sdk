package com.gigya.android.sdk.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import com.gigya.android.sdk.AccountManager;
import com.gigya.android.sdk.ApiManager;
import com.gigya.android.sdk.DependencyRegistry;
import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.log.GigyaLogger;
import com.gigya.android.sdk.login.LoginProvider;
import com.gigya.android.sdk.login.LoginProviderFactory;
import com.gigya.android.sdk.login.provider.WebViewLoginProvider;
import com.gigya.android.sdk.model.Configuration;
import com.gigya.android.sdk.model.GigyaAccount;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.GigyaResponse;
import com.gigya.android.sdk.utils.ObjectUtils;
import com.gigya.android.sdk.utils.UrlUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class WebBridge<T extends GigyaAccount> {

    private static final String LOG_TAG = "WebBridge";

    public enum AuthEvent {
        LOGIN, LOGOUT, ADD_CONNECTION
    }

    public interface WebBridgeInteractions<T> {
        void onPluginEvent(Map<String, Object> event, String containerID);

        void onAuthEvent(AuthEvent authEvent, T obj);

        void onCancel();

        void onError(GigyaError error);
    }

    private static final String CALLBACK_JS_PATH = "gigya._.apiAdapters.mobile.mobileCallbacks";

    private WeakReference<WebView> _webViewRef;
    private Configuration _configuration;
    private SessionManager _sessionManager;
    private ApiManager _apiManager;
    private AccountManager _accountManager;
    private boolean _shouldObfuscate;

    @NonNull
    private WebBridgeInteractions<GigyaAccount> _interactions;

    public WebBridge(boolean shouldObfuscate, @NonNull WebBridgeInteractions interactions) {
        _shouldObfuscate = shouldObfuscate;
        _interactions = interactions;
        DependencyRegistry.getInstance().inject(this);
    }

    public void inject(Configuration configuration, SessionManager sessionManager, ApiManager apiManager, AccountManager accountManager) {
        _configuration = configuration;
        _sessionManager = sessionManager;
        _apiManager = apiManager;
        _accountManager = accountManager;
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

    private boolean invoke(String actionString, String api, String queryStringParams) {
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

        final Map<String, Object> settings = new HashMap<>();
        UrlUtils.parseUrlParameters(settings, (String) data.get("settings"));

        switch (action) {
            case GET_IDS:
                getIds(callbackId);
                break;
            case REGISTER_FOR_NAMESPACE_EVENTS:
                registerForNamespaceEvents(params);
                break;
            case IS_SESSION_VALID:
                isSessionValid(callbackId);
                break;
            case SEND_REQUEST:
                sendRequest(callbackId, api, params, settings);
                break;
            case SEND_OAUTH_REQUEST:
                sendOAuthRequest(callbackId, api, params, settings);
                break;
            case ON_PLUGIN_EVENT:
                onPluginEvent(params);
                break;
            case ON_CUSTOM_EVENT:
            case ON_JS_EXCEPTION:
                break;
            default:
                break;
        }

        return true;
    }

    //region Actions

    private void getIds(String callbackId) {
        GigyaLogger.debug(LOG_TAG, "getIds: ");
        try {
            final String ids = new JSONObject()
                    .put("ucid", _configuration.getUCID()).put("gmid", _configuration.getGMID())
                    .toString();
            invokeCallback(callbackId, ids);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void isSessionValid(String callbackId) {
        GigyaLogger.debug(LOG_TAG, "isSessionValid: ");
        invokeCallback(callbackId, String.valueOf(_sessionManager.isValidSession()));
    }

    private void sendRequest(final String callbackId, final String api, Map<String, Object> params, Map<String, Object> settings) {
        GigyaLogger.debug(LOG_TAG, "sendRequest: with params:\n" + params.toString());

        // TODO: 29/01/2019 Should add support for non Https in GigyaRequest.
        final boolean forceHttps = Boolean.parseBoolean(ObjectUtils.firstNonNull((String) settings.get("forceHttps"), "false"));
        final boolean requiresSession = Boolean.parseBoolean(ObjectUtils.firstNonNull((String) settings.get("requiresSession"), "false"));

        if (api.equals("accounts.getAccountInfo") && _sessionManager.isValidSession()) {
            params.remove("regToken");
        }

        _apiManager.sendAnonymous(api, params, new GigyaCallback<GigyaResponse>() {
            @Override
            public void onSuccess(GigyaResponse obj) {
                GigyaLogger.debug(LOG_TAG, obj.asJson());
                handleAuthRequests(api, obj);
                invokeCallback(callbackId, obj.asJson());
            }

            @Override
            public void onError(GigyaError error) {
                GigyaLogger.error(LOG_TAG, error.toString());
            }
        });
    }

    private void handleAuthRequests(String api, GigyaResponse response) {
        switch (api) {
            case "socialize.logout":
            case "accounts.logout":
                _interactions.onAuthEvent(AuthEvent.LOGOUT, null);
                break;
            case "socialize.addConnection":
                // TODO: 07/02/2019 Will be handled in Business apis stories.
                _interactions.onAuthEvent(AuthEvent.ADD_CONNECTION, null);
                break;
            case "accounts.register":
                _interactions.onAuthEvent(AuthEvent.LOGIN, (T) response.parseTo(_accountManager.getAccountClazz()));
                break;
            default:
                break;
        }
    }

    private void sendOAuthRequest(final String callbackId, String api, Map<String, Object> params, Map<String, Object> settings) {
        GigyaLogger.debug(LOG_TAG, "sendOAuthRequest: with params:\n" + params.toString());

        final String provider = ObjectUtils.firstNonNull((String) params.get("provider"), "");
        final LoginProvider loginProvider = LoginProviderFactory.providerFor(_webViewRef.get().getContext(), _configuration, provider,
                new GigyaLoginCallback<T>() {
                    @Override
                    public void onCancelledOperation() {
                        GigyaLogger.debug(LOG_TAG, "sendOAuthRequest: onCancelledOperation");
                        _interactions.onCancel();
                    }

                    @Override
                    public void onSuccess(T obj) {
                        GigyaLogger.debug(LOG_TAG, "sendOAuthRequest: onSuccess with:\n" + obj.toString());
                        _interactions.onAuthEvent(AuthEvent.LOGIN, obj);
                    }

                    @Override
                    public void onError(GigyaError error) {
                        GigyaLogger.error(LOG_TAG, "sendOAuthRequest: onError with:\n" + error.getLocalizedMessage());
                        _interactions.onError(error);
                    }
                });

        final Activity activity = (Activity) _webViewRef.get().getContext();

        if (loginProvider instanceof WebViewLoginProvider && !_configuration.hasGMID()) {
            /* WebView Provider must have basic config fields. */
            loginProvider.configurationRequired(activity, params);
            return;
        }
        if (loginProvider.clientIdRequired() && !_configuration.isSynced()) {
            loginProvider.configurationRequired(activity, params);
            return;
        }

        if (_configuration.isSynced()) {
            /* Update provider client id if available */
            final String providerClientId = _configuration.getAppIds().get(provider);
            if (providerClientId != null) {
                loginProvider.updateProviderClientId(providerClientId);
            }
        }

        if (activity != null) {
            activity.finish();
        }

        loginProvider.login(_webViewRef.get().getContext(), params);
    }

    private void onPluginEvent(Map<String, Object> params) {
        GigyaLogger.debug(LOG_TAG, "onPluginEvent: with params:\n" + params.toString());

        String containerId = (String) params.get("sourceContainerID");
        if (containerId != null) {
            _interactions.onPluginEvent(params, containerId);
        }
    }

    private void registerForNamespaceEvents(Map<String, Object> params) {
        final String namespace = ObjectUtils.firstNonNull((String) params.get("namespace"), "");
        GigyaLogger.debug(LOG_TAG, "registerForNamespaceEvents: with namespace = " + namespace);
        /* This method is no longer in use. */
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

