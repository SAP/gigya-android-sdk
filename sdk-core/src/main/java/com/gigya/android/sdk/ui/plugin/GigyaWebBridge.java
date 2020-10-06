package com.gigya.android.sdk.ui.plugin;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.util.Base64;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.GigyaPluginCallback;
import com.gigya.android.sdk.account.IAccountService;
import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.api.IBusinessApiService;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.adapter.RestAdapter;
import com.gigya.android.sdk.providers.IProviderFactory;
import com.gigya.android.sdk.session.ISessionService;
import com.gigya.android.sdk.session.ISessionVerificationService;
import com.gigya.android.sdk.session.SessionInfo;
import com.gigya.android.sdk.utils.ObjectUtils;
import com.gigya.android.sdk.utils.UrlUtils;
import com.google.gson.Gson;

import org.json.JSONArray;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.gigya.android.sdk.ui.plugin.PluginAuthEventDef.ADD_CONNECTION;
import static com.gigya.android.sdk.ui.plugin.PluginAuthEventDef.CANCELED;
import static com.gigya.android.sdk.ui.plugin.PluginAuthEventDef.LOGIN;
import static com.gigya.android.sdk.ui.plugin.PluginAuthEventDef.LOGIN_STARTED;
import static com.gigya.android.sdk.ui.plugin.PluginAuthEventDef.LOGOUT;
import static com.gigya.android.sdk.ui.plugin.PluginAuthEventDef.REMOVE_CONNECTION;
import static com.gigya.android.sdk.ui.plugin.PluginEventDef.AFTER_SCREEN_LOAD;
import static com.gigya.android.sdk.ui.plugin.PluginEventDef.AFTER_SUBMIT;
import static com.gigya.android.sdk.ui.plugin.PluginEventDef.AFTER_VALIDATION;
import static com.gigya.android.sdk.ui.plugin.PluginEventDef.BEFORE_SCREEN_LOAD;
import static com.gigya.android.sdk.ui.plugin.PluginEventDef.BEFORE_SUBMIT;
import static com.gigya.android.sdk.ui.plugin.PluginEventDef.BEFORE_VALIDATION;
import static com.gigya.android.sdk.ui.plugin.PluginEventDef.ERROR;
import static com.gigya.android.sdk.ui.plugin.PluginEventDef.FIELD_CHANGED;
import static com.gigya.android.sdk.ui.plugin.PluginEventDef.HIDE;
import static com.gigya.android.sdk.ui.plugin.PluginEventDef.LOAD;
import static com.gigya.android.sdk.ui.plugin.PluginEventDef.SUBMIT;

public class GigyaWebBridge<A extends GigyaAccount> implements IGigyaWebBridge<A> {

    private static final String LOG_TAG = "GigyaWebBridge";

    private static final String EVALUATE_JS_PATH = "gigya._.apiAdapters.mobile.mobileCallbacks";

    public enum Feature {
        IS_SESSION_VALID("IS_SESSION_VALID"), SEND_REQUEST("SEND_REQUEST"), SEND_OAUTH_REQUEST("SEND_OAUTH_REQUEST"),
        GET_IDS("GET_IDS"), ON_PLUGIN_EVENT("ON_PLUGIN_EVENT"), ON_CUSTOM_EVENT("ON_CUSTOM_EVENT"),
        REGISTER_FOR_NAMESPACE_EVENTS("REGISTER_FOR_NAMESPACE_EVENTS"), ON_JS_EXCEPTION("ON_JS_EXCEPTION");

        private String value;

        Feature(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @NonNull
        @Override
        public String toString() {
            return this.getValue();
        }
    }

    final private Context _context;
    final private Config _config;
    final private ISessionService _sessionService;
    final private IBusinessApiService<A> _businessApiService;
    final private IAccountService<A> _accountService;
    final private ISessionVerificationService _sessionVerificationService;
    final private IProviderFactory _providerFactory;

    private GigyaPluginFragment.IBridgeCallbacks<A> _invocationCallback;
    private boolean _obfuscation = false;

    public GigyaWebBridge(Context context,
                          Config config,
                          ISessionService sessionService,
                          IBusinessApiService<A> businessApiService,
                          IAccountService<A> accountService,
                          ISessionVerificationService sessionVerificationService,
                          IProviderFactory providerFactory) {
        _context = context;
        _config = config;
        _sessionService = sessionService;
        _businessApiService = businessApiService;
        _accountService = accountService;
        _sessionVerificationService = sessionVerificationService;
        _providerFactory = providerFactory;
    }

    @Override
    public IGigyaWebBridge<A> withObfuscation(boolean obfuscation) {
        _obfuscation = obfuscation;
        return this;
    }

    @Override
    public void setInvocationCallback(@NonNull GigyaPluginFragment.IBridgeCallbacks<A> invocationCallback) {
        _invocationCallback = invocationCallback;
    }

    //region ACTIONS

    @Override
    public boolean invoke(String action, String method, String queryStringParams) {
        if (action == null) {
            return false;
        }

        // Parse data map.
        final Map<String, Object> data = new HashMap<>();
        UrlUtils.parseUrlParameters(data, queryStringParams);

        // Get parameters map.
        final Map<String, Object> params = new HashMap<>();
        UrlUtils.parseUrlParameters(params, deobfuscate((String) data.get("params")));

        // Get settings map.
        final Map<String, Object> settings = new HashMap<>();
        UrlUtils.parseUrlParameters(settings, (String) data.get("settings"));

        final Feature feature = Feature.valueOf(action.toUpperCase(Locale.ENGLISH));
        final String callbackId = (String) data.get("callbackID");

        switch (feature) {
            case GET_IDS:
                getIds(callbackId);
                break;
            case IS_SESSION_VALID:
                isSessionValid(callbackId);
                break;
            case SEND_REQUEST:
            case SEND_OAUTH_REQUEST:
                mapApisToRequests(feature, callbackId, method, params);
                break;
            case ON_PLUGIN_EVENT:
                onPluginEvent(params);
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    public boolean invoke(String url) {
        Uri uri = Uri.parse(url);
        if (uri == null || !UrlUtils.isGigyaScheme(uri.getScheme())) {
            return false;
        }
        if (uri.getPath() == null) {
            return false;
        }
        return invoke(uri.getHost(), uri.getPath().replace("/", ""), uri.getEncodedQuery());
    }

    @Override
    public void invokeWebViewCallback(String id, String baseInvocation) {
        GigyaLogger.debug(LOG_TAG, "evaluateJS: " + baseInvocation);
        String value = obfuscate(baseInvocation, true);
        final String invocation = String.format("javascript:%s['%s'](%s);", EVALUATE_JS_PATH, id, value);
        if (_invocationCallback != null) {
            _invocationCallback.invokeCallback(invocation);
        }
    }

    @Override
    public void getIds(String id) {
        String ids = "{\"ucid\":\"" + _config.getUcid() + "\", \"gmid\":\"" + _config.getGmid() + "\"}";
        GigyaLogger.debug(LOG_TAG, "getIds: " + ids);
        invokeWebViewCallback(id, ids);
    }

    @Override
    public void isSessionValid(String id) {
        final boolean isValid = _sessionService.isValid();
        GigyaLogger.debug(LOG_TAG, "isSessionValid: " + isValid);
        invokeWebViewCallback(id, String.valueOf(isValid));
    }

    /*
    Mapping requests directed from web sdk mobile adapter.
    This is required because certain requests require specific flows.
     */
    private void mapApisToRequests(Feature feature,
                                   String callbackId,
                                   String api,
                                   Map<String, Object> params) {
        GigyaLogger.debug(LOG_TAG, "mapApisToRequests with api: " + api + " and params:\n<<<<" + params.toString() + "\n>>>>");
        switch (api) {
            case "socialize.logout":
            case "accounts.logout":
                logout(callbackId);
                break;
            case "socialize.addConnection":
            case "accounts.addConnection":
                final String providerToAdd = (String) params.get("provider");
                addConnection(callbackId, providerToAdd);
                break;
            case "socialize.removeConnection":
                final String providerToRemove = (String) params.get("provider");
                removeConnection(callbackId, providerToRemove);
                break;
            default:
                if (feature.equals(Feature.SEND_REQUEST)) {
                    sendRequest(callbackId, api, params);
                } else if (feature.equals(Feature.SEND_OAUTH_REQUEST)) {
                    sendOAuthRequest(callbackId, api, params);
                }
                break;
        }
    }

    @Override
    public void sendOAuthRequest(final String callbackId, String api, Map<String, Object> params) {
        GigyaLogger.debug(LOG_TAG, "sendOAuthRequest with api: " + api + " and params:\n<<<<" + params.toString() + "\n>>>>");
        final String providerName = ObjectUtils.firstNonNull((String) params.get("provider"), "");
        if (providerName.isEmpty()) {
            return;
        }

        // Invoking login started (custom event) in order to show the web view progress bar.
        if (_invocationCallback != null) {
            _invocationCallback.onPluginAuthEvent(PluginAuthEventDef.LOGIN_STARTED, null);
        }

        _businessApiService.login(
                providerName,
                params,
                new GigyaLoginCallback<A>() {
                    @Override
                    public void onSuccess(A account) {
                        GigyaLogger.debug(LOG_TAG, "sendOAuthRequest: onSuccess with:\n" + account.toString());
                        String invocation = "{\"errorCode\":" + account.getErrorCode() + ",\"userInfo\":" + new Gson().toJson(account) + "}";
                        invokeWebViewCallback(callbackId, invocation);
                        if (_invocationCallback != null) {
                            _invocationCallback.onPluginAuthEvent(PluginAuthEventDef.LOGIN, account);
                        }
                    }

                    @Override
                    public void onError(GigyaError error) {
                        invokeWebViewCallback(callbackId, error.getData());
                    }

                    @Override
                    public void onOperationCanceled() {
                        invokeWebViewCallback(callbackId, GigyaError.cancelledOperation().getData());
                        if (_invocationCallback != null) {
                            _invocationCallback.onPluginAuthEvent(PluginAuthEventDef.CANCELED, null);
                        }
                    }
                });
    }

    @Override
    public void onPluginEvent(Map<String, Object> params) {
        final String containerId = (String) params.get("sourceContainerID");
        if (containerId != null && _invocationCallback != null) {
            _invocationCallback.onPluginEvent(new GigyaPluginEvent(params), containerId);
        }
    }

    //endregion

    //region APIS

    @Override
    public void sendRequest(final String callbackId, final String api, Map<String, Object> params) {
        _businessApiService.send(
                api,
                params,
                RestAdapter.POST,
                GigyaApiResponse.class,
                new GigyaCallback<GigyaApiResponse>() {
                    @Override
                    public void onSuccess(GigyaApiResponse response) {
                        if (response.getErrorCode() == 0) {
                            // Check if generic send was a login/register request.
                            if (response.containsNested("sessionInfo.sessionSecret")) {
                                A parsed = response.parseTo(_accountService.getAccountSchema());
                                final SessionInfo newSession = response.getField("sessionInfo", SessionInfo.class);
                                _sessionService.setSession(newSession);
                                _accountService.setAccount(response.asJson());
                                if (_invocationCallback != null) {
                                    _invocationCallback.onPluginAuthEvent(PluginAuthEventDef.LOGIN, parsed);
                                }
                            }
                            invokeWebViewCallback(callbackId, response.asJson());
                        } else {
                            onError(GigyaError.fromResponse(response));
                        }
                    }

                    @Override
                    public void onError(GigyaError error) {
                        invokeWebViewCallback(callbackId, error.getData());
                    }
                });
    }

    /*
    Send logout request via business API service.
     */
    private void logout(final String callbackId) {
        GigyaLogger.debug(LOG_TAG, "Sending logout request");
        _businessApiService.logout(
                new GigyaCallback<GigyaApiResponse>() {
                    @Override
                    public void onSuccess(GigyaApiResponse response) {
                        if (response.getErrorCode() == 0) {
                            invokeWebViewCallback(callbackId, response.asJson());
                            if (_invocationCallback != null) {
                                _invocationCallback.onPluginAuthEvent(PluginAuthEventDef.LOGOUT, null);
                            }

                            // Cleaning up.
                            _sessionService.cancelSessionCountdownTimer();
                            _sessionService.clear(true);
                            _providerFactory.logoutFromUsedSocialProviders();
                            _sessionVerificationService.stop();
                            clearCookies();

                        } else {
                            onError(GigyaError.fromResponse(response));
                        }
                    }

                    @Override
                    public void onError(GigyaError error) {
                        invokeWebViewCallback(callbackId, error.getData());
                    }
                });
    }

    private void clearCookies() {
        CookieManager cookieManager = CookieManager.getInstance();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.flush();
        } else {
            CookieSyncManager.createInstance(_context);
            cookieManager.removeAllCookie();
        }
    }

    /*
    Send removeConnection request via business API service.
     */
    private void removeConnection(final String callbackId, String provider) {
        GigyaLogger.debug(LOG_TAG, "Sending removeConnection api request with provider: " + provider);
        _businessApiService.removeConnection(
                provider,
                new GigyaCallback<GigyaApiResponse>() {
                    @Override
                    public void onSuccess(GigyaApiResponse response) {
                        if (response.getErrorCode() == 0) {
                            invokeWebViewCallback(callbackId, response.asJson());
                            if (_invocationCallback != null) {
                                _invocationCallback.onPluginAuthEvent(PluginAuthEventDef.REMOVE_CONNECTION, null);
                            }
                        } else {
                            onError(GigyaError.fromResponse(response));
                        }
                    }

                    @Override
                    public void onError(GigyaError error) {
                        invokeWebViewCallback(callbackId, error.getData());
                    }
                });
    }

    /*
    Send addConnection request via business API service.
    On successful response request userInfo in order to invoke the web sdk callback.
     */
    private void addConnection(final String callbackId, String provider) {
        GigyaLogger.debug(LOG_TAG, "Sending addConnection api request with provider: " + provider);
        _businessApiService.addConnection(
                provider,
                new GigyaLoginCallback<A>() {
                    @Override
                    public void onSuccess(A response) {
                        getUserInfoAndInvoke(callbackId);
                        if (_invocationCallback != null) {
                            _invocationCallback.onPluginAuthEvent(PluginAuthEventDef.ADD_CONNECTION, response);
                        }
                    }

                    @Override
                    public void onError(GigyaError error) {
                        invokeWebViewCallback(callbackId, error.getData());
                    }
                });
    }

    /*
    Add connection requests requires us to invoke a "socialize.getUserInfo" json response in order
    To correctly refresh the screenset.
     */
    private void getUserInfoAndInvoke(final String callbackId) {
        _businessApiService.send(
                "socialize.getUserInfo",
                null,
                RestAdapter.POST,
                GigyaApiResponse.class,
                new GigyaCallback<GigyaApiResponse>() {
                    @Override
                    public void onSuccess(GigyaApiResponse obj) {
                        invokeWebViewCallback(callbackId, obj.asJson());
                    }

                    @Override
                    public void onError(GigyaError error) {
                        invokeWebViewCallback(callbackId, error.getData());
                    }
                });
    }


    //endregion

    //region OBFUSCATION

    @SuppressWarnings("CharsetObjectCanBeUsed")
    private String obfuscate(String string, boolean quote) {
        if (_obfuscation && string != null) {
            // by default, using obfuscation strategy of base64
            try {
                byte[] data = string.getBytes("UTF-8");
                String base64 = Base64.encodeToString(data, Base64.NO_WRAP);
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
        if (_obfuscation && base64String != null) {
            try {
                byte[] data = Base64.decode(base64String, Base64.NO_WRAP);
                return new String(data, "UTF-8");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return base64String;
    }

    //endregion

    //region ATTACH

    /**
     * Attach a WebView instance to WebBridge.
     * Allows external use of the Gigya web bridge.
     *
     * @param webView        WebView instance.
     * @param pluginCallback Plugin callback used for JS and event interactions.
     * @param progressView   Optional progress view that will be triggered (VISIBLE/GONE) according to event life cycle.
     */
    @SuppressLint("AddJavascriptInterface")
    @Override
    public void attachTo(
            @NonNull final WebView webView,
            @NonNull final GigyaPluginCallback<A> pluginCallback,
            @Nullable final View progressView) {

        if (android.os.Build.VERSION.SDK_INT < 17) {
            GigyaLogger.error(LOG_TAG, "WebBridge invocation is only available for Android >= 17");
            return;
        }
        webView.addJavascriptInterface(new Object() {

            private static final String ADAPTER_NAME = "mobile";

            @JavascriptInterface
            public String getAPIKey() {
                return _config.getApiKey();
            }

            @JavascriptInterface
            public String getAdapterName() {
                return ADAPTER_NAME;
            }

            @JavascriptInterface
            public String getObfuscationStrategy() {
                return _obfuscation ? "base64" : "";
            }

            @JavascriptInterface
            public String getFeatures() {
                JSONArray features = new JSONArray();
                for (GigyaWebBridge.Feature feature : GigyaWebBridge.Feature.values()) {
                    features.put(feature.toString().toLowerCase(Locale.ENGLISH));
                }
                return features.toString();
            }

            @JavascriptInterface
            public boolean sendToMobile(String action, String method, String queryStringParams) {
                return invoke(action, method, queryStringParams);
            }
        }, "__gigAPIAdapterSettings");


        _invocationCallback = new GigyaPluginFragment.IBridgeCallbacks<A>() {
            @Override
            public void invokeCallback(final String invocation) {
                webView.post(new Runnable() {
                    @Override
                    public void run() {
                        if (android.os.Build.VERSION.SDK_INT > 18) {
                            webView.evaluateJavascript(invocation, new ValueCallback<String>() {
                                @Override
                                public void onReceiveValue(String value) {
                                    GigyaLogger.debug("evaluateJavascript Callback", value);
                                }
                            });
                        } else {
                            webView.loadUrl(invocation);
                        }
                    }
                });
            }

            @Override
            public void onPluginEvent(final GigyaPluginEvent event, final String containerID) {
                final @PluginEventDef.PluginEvent String eventName = event.getEvent();
                if (eventName == null) {
                    return;
                }
                webView.post(new Runnable() {
                    @Override
                    public void run() {
                        switch (eventName) {
                            case BEFORE_SCREEN_LOAD:
                                if (progressView != null) {
                                    progressView.setVisibility(View.VISIBLE);
                                }
                                pluginCallback.onBeforeScreenLoad(event);
                                break;
                            case LOAD:
                                if (progressView != null) {
                                    progressView.setVisibility(View.INVISIBLE);
                                }
                                break;
                            case AFTER_SCREEN_LOAD:
                                if (progressView != null) {
                                    progressView.setVisibility(View.INVISIBLE);
                                }
                                pluginCallback.onAfterScreenLoad(event);
                                break;
                            case FIELD_CHANGED:
                                pluginCallback.onFieldChanged(event);
                                break;
                            case BEFORE_VALIDATION:
                                pluginCallback.onBeforeValidation(event);
                                break;
                            case AFTER_VALIDATION:
                                pluginCallback.onAfterValidation(event);
                                break;
                            case BEFORE_SUBMIT:
                                pluginCallback.onBeforeSubmit(event);
                                break;
                            case SUBMIT:
                                pluginCallback.onSubmit(event);
                                break;
                            case AFTER_SUBMIT:
                                pluginCallback.onAfterSubmit(event);
                                break;
                            case HIDE:
                                final String reason = (String) event.getEventMap().get("reason");
                                pluginCallback.onHide(event, reason);
                                break;
                            case ERROR:
                                pluginCallback.onError(event);
                                break;
                            default:
                                break;
                        }
                    }
                });
            }

            @Override
            public void onPluginAuthEvent(final String method, final @Nullable A accountObj) {
                webView.post(new Runnable() {
                    @Override
                    public void run() {
                        switch (method) {
                            case LOGIN_STARTED:
                                if (progressView != null) {
                                    progressView.setVisibility(View.VISIBLE);
                                }
                                break;
                            case LOGIN:
                                if (progressView != null) {
                                    progressView.setVisibility(View.INVISIBLE);
                                }
                                if (accountObj != null) {
                                    pluginCallback.onLogin(accountObj);
                                }
                                break;
                            case LOGOUT:
                                pluginCallback.onLogout();
                                break;
                            case ADD_CONNECTION:
                                pluginCallback.onConnectionAdded();
                                break;
                            case REMOVE_CONNECTION:
                                pluginCallback.onConnectionRemoved();
                                break;
                            case CANCELED:
                                if (progressView != null) {
                                    progressView.setVisibility(View.INVISIBLE);
                                }
                                pluginCallback.onCanceled();
                                break;
                            default:
                                break;
                        }
                    }
                });

            }
        };
    }

    /**
     * Detach a WebView instance from this web bridge instance.
     * Use to avoid leaking the enclosing context.
     *
     * @param webView Current attached WebView instance.
     */
    @Override
    public void detachFrom(@NonNull final WebView webView) {
        webView.loadUrl("about:blank");
        webView.setWebViewClient(null);
        webView.setWebChromeClient(null);
        if (_invocationCallback != null) {
            _invocationCallback = null;
        }
    }

    //endregion
}
