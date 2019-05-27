package com.gigya.android.sdk.ui.plugin;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.account.IAccountService;
import com.gigya.android.sdk.api.IBusinessApiService;
import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.session.SessionInfo;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.adapter.RestAdapter;
import com.gigya.android.sdk.session.ISessionService;
import com.gigya.android.sdk.utils.ObjectUtils;
import com.gigya.android.sdk.utils.UiUtils;
import com.gigya.android.sdk.utils.UrlUtils;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class WebBridge<T extends GigyaAccount> {

    private static final String LOG_TAG = "WebBridge";

    public enum AuthEvent {
        LOGIN, LOGOUT, ADD_CONNECTION, REMOVE_CONNECTION
    }

    private static final String CALLBACK_JS_PATH = "gigya._.apiAdapters.mobile.mobileCallbacks";

    // Dynamic.
    private WeakReference<WebView> _webViewRef;
    final private IWebBridge<T> _interactions;

    // Injections.
    final private Context _context;
    final private Config _config;
    final private boolean _shouldObfuscate;
    final private ISessionService _sessionService;
    final private IAccountService _accountService;
    final private IBusinessApiService<T> _bApiService;


    public WebBridge(Context context, Config config, ISessionService sessionService, IAccountService accountService, IBusinessApiService<T> bApiService,
                     boolean shouldObfuscate, IWebBridge<T> interactions) {
        _context = context;
        _config = config;
        _sessionService = sessionService;
        _accountService = accountService;
        _bApiService = bApiService;
        _shouldObfuscate = shouldObfuscate;
        _interactions = interactions;
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
                return _config.getApiKey();
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
            if (uri.getPath() == null) {
                return false;
            }
            return invoke(uri.getHost(), uri.getPath().replace("/", ""), uri.getEncodedQuery());
        }
        return false;
    }

    //region ACTIONS

    private boolean invoke(String actionString, String api, String queryStringParams) {
        if (actionString == null) {
            return false;
        }
        final Map<String, Object> data = new HashMap<>();
        UrlUtils.parseUrlParameters(data, queryStringParams);
        // Parse action.
        Actions action;
        try {
            action = Actions.valueOf(actionString.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            return true;
        }
        // Get callbackId.
        final String callbackId = (String) data.get("callbackID");
        // Get parameters map.
        final Map<String, Object> params = new HashMap<>();
        UrlUtils.parseUrlParameters(params, deobfuscate((String) data.get("params")));
        // Get settings map.
        final Map<String, Object> settings = new HashMap<>();
        UrlUtils.parseUrlParameters(settings, (String) data.get("settings"));
        // Act on action.
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

    private void getIds(String callbackId) {
        GigyaLogger.debug(LOG_TAG, "getIds: ");
        try {
            final String ids = new JSONObject()
                    .put("ucid", _config.getUcid()).put("gmid", _config.getGmid())
                    .toString();
            invokeCallback(callbackId, ids);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void isSessionValid(String callbackId) {
        final boolean isValid = _sessionService.isValid();
        GigyaLogger.debug(LOG_TAG, "isSessionValid: " + isValid);
        invokeCallback(callbackId, String.valueOf(isValid));
    }

    private void sendRequest(final String callbackId, final String api, Map<String, Object> params, Map<String, Object> settings) {
        GigyaLogger.debug(LOG_TAG, "sendRequest with api: " + api + " and params:\n" + params.toString());

        // Pre api related tasks.
        final boolean isValid = _sessionService.isValid();
        if (api.equals("accounts.getAccountInfo") && isValid) {
            params.remove("regToken");
        }

        // Define a general purpose response callback.
        final GigyaCallback<GigyaApiResponse> responseCallback = new GigyaCallback<GigyaApiResponse>() {
            @Override
            public void onSuccess(GigyaApiResponse response) {
                GigyaLogger.debug(LOG_TAG, "onSuccess for api = " + api + " with data:\n" + response.asJson());
                if (response.getErrorCode() == 0) {
                    handleAuthRequests(api, response);
                    invokeCallback(callbackId, response.asJson());
                } else {
                    onError(GigyaError.fromResponse(response));
                }
            }

            @Override
            public void onError(GigyaError error) {
                GigyaLogger.error(LOG_TAG, "onError for api = " + api + " with error:\n" + error.toString());
                invokeCallback(callbackId, error.getData());
            }
        };

        // Map known apis.
        switch (api) {
            case "socialize.logout":
            case "accounts.logout":
                _bApiService.logout(responseCallback);
                break;
            case "socialize.addConnection":
            case "accounts.addConnection":
                String providerToAdd = (String) params.get("provider");
                // Add connection requires a login callback.
                GigyaLogger.debug(LOG_TAG, "Add Connection to provider: " + providerToAdd);
                _bApiService.addConnection(providerToAdd, new GigyaLoginCallback<T>() {
                    @Override
                    public void onSuccess(T response) {
                        GigyaLogger.debug(LOG_TAG, "onSuccess for api = " + api);
                        _interactions.onAuthEvent(AuthEvent.ADD_CONNECTION, response);
                        invokeCallback(callbackId, new Gson().toJson(response));
                    }

                    @Override
                    public void onError(GigyaError error) {
                        // Direct to general response callback error method.
                        responseCallback.onError(error);
                    }
                });
                break;
            case "socialize.removeConnection":
                final String providerToRemove = (String) params.get("provider");
                GigyaLogger.debug(LOG_TAG, "Remove Connection to provider: " + providerToRemove);
                _bApiService.removeConnection(providerToRemove, responseCallback);
                break;
            default:
                _bApiService.send(api, params, RestAdapter.POST, GigyaApiResponse.class, responseCallback);
                break;
        }
    }

    private void handleAuthRequests(String api, GigyaApiResponse response) {
        switch (api) {
            case "socialize.logout":
            case "accounts.logout":
                _interactions.onAuthEvent(AuthEvent.LOGOUT, null);
                break;
            case "socialize.removeConnection":
                _interactions.onAuthEvent(AuthEvent.REMOVE_CONNECTION, null);
                break;
            case "accounts.register":
            case "accounts.login":
                T parsed = (T) response.parseTo(_accountService.getAccountSchema());
                final SessionInfo newSession = response.getField("sessionInfo", SessionInfo.class);
                _sessionService.setSession(newSession);
                _accountService.setAccount(response.asJson());
                _interactions.onAuthEvent(AuthEvent.LOGIN, parsed);
                break;
            default:
                break;
        }
    }

    private void sendOAuthRequest(final String callbackId, String api, Map<String, Object> params, Map<String, Object> settings) {
        final String providerName = ObjectUtils.firstNonNull((String) params.get("provider"), "");
        _bApiService.login(providerName, params, new GigyaLoginCallback<T>() {
            @Override
            public void onSuccess(T obj) {
                GigyaLogger.debug(LOG_TAG, "sendOAuthRequest: onSuccess with:\n" + obj.toString());
                String invocation = null;
                try {
                    invocation = new JSONObject().put("errorCode", obj.getErrorCode()).put("userInfo", new Gson().toJson(obj)).toString();
                } catch (Exception ex) {
                    GigyaLogger.error(LOG_TAG, "Error in sendOauthRequest bridge -> invocation creation -> " + ex.getMessage());
                    ex.printStackTrace();
                }
                invokeCallback(callbackId, invocation);
                _interactions.onAuthEvent(AuthEvent.LOGIN, obj);
            }

            @Override
            public void onError(GigyaError error) {
                GigyaLogger.error(LOG_TAG, "sendOAuthRequest: onError with:\n" + error.getLocalizedMessage());
                invokeCallback(callbackId, error.getData());
                _interactions.onError(error);
            }

            @Override
            public void onOperationCanceled() {
                GigyaLogger.debug(LOG_TAG, "sendOAuthRequest: onOperationCanceled");
                invokeCallback(callbackId, GigyaError.cancelledOperation().getData());
                _interactions.onCancel();
            }
        });

        final Activity activity = UiUtils.findActivity(_webViewRef.get().getContext());
        if (activity != null && !activity.isFinishing()) {
            activity.finish();
        }
    }

    private void onPluginEvent(Map<String, Object> params) {
        final String containerId = (String) params.get("sourceContainerID");

        if (containerId != null) {
            _interactions.onPluginEvent(new GigyaPluginEvent(params), containerId);
        }
    }

    private void registerForNamespaceEvents(Map<String, Object> params) {
        final String namespace = ObjectUtils.firstNonNull((String) params.get("namespace"), "");
        GigyaLogger.debug(LOG_TAG, "registerForNamespaceEvents: with namespace = " + namespace);
        /* This method is no longer in use. */
    }

    //endregion

    //region OBFUSCATION

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

