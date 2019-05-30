package com.gigya.android.sdk.ui.new_plugin_impl;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Base64;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.session.ISessionService;
import com.gigya.android.sdk.ui.plugin.GigyaPluginEvent;
import com.gigya.android.sdk.utils.UrlUtils;

import java.util.HashMap;
import java.util.Map;

public class GigyaWebBridge implements IGigyaWebBridge {

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

        @Override
        public String toString() {
            return this.getValue();
        }
    }

    final private Config _config;
    final private ISessionService _sessionService;

    private GigyaPluginFragment.IBridgeCallbacks _invocationCallback;
    private boolean _obfuscation = false;

    public GigyaWebBridge(Config config,
                          ISessionService sessionService) {
        _config = config;
        _sessionService = sessionService;
    }

    public void withObfuscation(boolean obfuscation) {
        _obfuscation = obfuscation;
    }

    public void setInvocationCallback(@NonNull GigyaPluginFragment.IBridgeCallbacks invocationCallback) {
        _invocationCallback = invocationCallback;
    }

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

        final Feature feature = Feature.valueOf(action);
        final String callbackId = (String) data.get("callbackID");

        switch (feature) {
            case GET_IDS:
                getIds(callbackId);
                break;
            case IS_SESSION_VALID:
                isSessionValid(callbackId);
                break;
            case SEND_REQUEST:
                sendRequest(callbackId, method, params, settings);
                break;
            case SEND_OAUTH_REQUEST:
                sendOAuthRequest(callbackId, method, params, settings);
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
        if (url.startsWith("gsapi://")) {
            Uri uri = Uri.parse(url);
            if (uri.getPath() == null) {
                return false;
            }
            return invoke(uri.getHost(), uri.getPath().replace("/", ""), uri.getEncodedQuery());
        }
        return false;
    }

    @Override
    public void invokeCallback(String id, String baseInvocation) {
        GigyaLogger.debug(LOG_TAG, "evaluateJS: " + baseInvocation);
        String value = obfuscate(baseInvocation, true);
        final String invocation = String.format("javascript:%s['%s'](%s);", EVALUATE_JS_PATH, id, value);
        _invocationCallback.invokeCallback(invocation);
    }

    @Override
    public void getIds(String id) {
        String ids = "{\"ucid\":" + _config.getUcid() + ", \"gmid\":" + _config.getGmid() + "}";
        GigyaLogger.debug(LOG_TAG, "getIds: " + ids);
        invokeCallback(id, ids);
    }

    @Override
    public void isSessionValid(String id) {
        final boolean isValid = _sessionService.isValid();
        GigyaLogger.debug(LOG_TAG, "isSessionValid: " + isValid);
        invokeCallback(id, String.valueOf(isValid));
    }

    @Override
    public void sendRequest(String callbackId, String api, Map<String, Object> params, Map<String, Object> settings) {

    }

    @Override
    public void sendOAuthRequest(String callbackId, String api, Map<String, Object> params, Map<String, Object> settings) {

    }

    @Override
    public void onPluginEvent(Map<String, Object> params) {
        final String containerId = (String) params.get("sourceContainerID");
        if (containerId != null) {
            _invocationCallback.onPluginEvent(new GigyaPluginEvent(params), containerId);
        }
    }

    //region OBFUSCATION

    @SuppressWarnings("CharsetObjectCanBeUsed")
    private String obfuscate(String string, boolean quote) {
        if (_obfuscation) {
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
        if (_obfuscation) {
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
