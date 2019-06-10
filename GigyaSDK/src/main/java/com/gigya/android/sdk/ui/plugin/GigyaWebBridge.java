package com.gigya.android.sdk.ui.plugin;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Base64;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.account.IAccountService;
import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.api.IBusinessApiService;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.adapter.RestAdapter;
import com.gigya.android.sdk.session.ISessionService;
import com.gigya.android.sdk.session.SessionInfo;
import com.gigya.android.sdk.utils.ObjectUtils;
import com.gigya.android.sdk.utils.UrlUtils;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

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

    final private Config _config;
    final private ISessionService _sessionService;
    final private IBusinessApiService<A> _businessApiService;
    final private IAccountService<A> _accountService;

    private GigyaPluginFragment.IBridgeCallbacks<A> _invocationCallback;
    private boolean _obfuscation = false;

    public GigyaWebBridge(Config config,
                          ISessionService sessionService,
                          IBusinessApiService<A> businessApiService,
                          IAccountService<A> accountService) {
        _config = config;
        _sessionService = sessionService;
        _businessApiService = businessApiService;
        _accountService = accountService;
    }

    @Override
    public void withObfuscation(boolean obfuscation) {
        _obfuscation = obfuscation;
    }

    @Override
    public void setInvocationCallback(@NonNull GigyaPluginFragment.IBridgeCallbacks<A> invocationCallback) {
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

        final Feature feature = Feature.valueOf(action.toUpperCase());
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
        String ids = "{\"ucid\":\"" + _config.getUcid() + "\", \"gmid\":\"" + _config.getGmid() + "\"}";
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
        GigyaLogger.debug(LOG_TAG, "sendRequest with api: " + api + " and params:\n<<<<" + params.toString() + "\n>>>>");
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
                sendRequest(callbackId, api, params);
                break;
        }
    }

    @Override
    public void sendOAuthRequest(final String callbackId, String api, Map<String, Object> params, Map<String, Object> settings) {
        GigyaLogger.debug(LOG_TAG, "sendOAuthRequest with api: " + api + " and params:\n<<<<" + params.toString() + "\n>>>>");
        final String providerName = ObjectUtils.firstNonNull((String) params.get("provider"), "");
        if (providerName.isEmpty()) {
            return;
        }

        // Invoking login started (custom event) in order to show the web view progress bar.
        _invocationCallback.onPluginAuthEvent(PluginAuthEventDef.LOGIN_STARTED, null);

        _businessApiService.login(providerName, params, new GigyaLoginCallback<A>() {
            @Override
            public void onSuccess(A account) {
                GigyaLogger.debug(LOG_TAG, "sendOAuthRequest: onSuccess with:\n" + account.toString());
                String invocation = "{\"errorCode\":" + account.getErrorCode() + ",\"userInfo\":" + new Gson().toJson(account) + "}";
                invokeCallback(callbackId, invocation);
                _invocationCallback.onPluginAuthEvent(PluginAuthEventDef.LOGIN, account);
            }

            @Override
            public void onError(GigyaError error) {
                invokeCallback(callbackId, error.getData());
            }

            @Override
            public void onOperationCanceled() {
                invokeCallback(callbackId, GigyaError.cancelledOperation().getData());
            }
        });
    }

    @Override
    public void onPluginEvent(Map<String, Object> params) {
        final String containerId = (String) params.get("sourceContainerID");
        if (containerId != null) {
            _invocationCallback.onPluginEvent(new GigyaPluginEvent(params), containerId);
        }
    }

    //region APIS

    private void sendRequest(final String callbackId, final String api, Map<String, Object> params) {
        _businessApiService.send(api, params, RestAdapter.POST, GigyaApiResponse.class, new GigyaCallback<GigyaApiResponse>() {
            @Override
            public void onSuccess(GigyaApiResponse response) {
                if (response.getErrorCode() == 0) {
                    // Check if generic send was a login/register request.
                    if (response.containsNested("sessionInfo.sessionSecret")) {
                        A parsed = response.parseTo(_accountService.getAccountSchema());
                        final SessionInfo newSession = response.getField("sessionInfo", SessionInfo.class);
                        _sessionService.setSession(newSession);
                        _accountService.setAccount(response.asJson());
                        _invocationCallback.onPluginAuthEvent(PluginAuthEventDef.LOGIN, parsed);
                    }
                    invokeCallback(callbackId, response.asJson());
                } else {
                    onError(GigyaError.fromResponse(response));
                }
            }

            @Override
            public void onError(GigyaError error) {
                invokeCallback(callbackId, error.getData());
            }
        });
    }

    private void logout(final String callbackId) {
        GigyaLogger.debug(LOG_TAG, "Sending logout request");
        _businessApiService.logout(new GigyaCallback<GigyaApiResponse>() {
            @Override
            public void onSuccess(GigyaApiResponse response) {
                if (response.getErrorCode() == 0) {
                    invokeCallback(callbackId, response.asJson());
                    _invocationCallback.onPluginAuthEvent(PluginAuthEventDef.LOGOUT, null);
                } else {
                    onError(GigyaError.fromResponse(response));
                }
            }

            @Override
            public void onError(GigyaError error) {
                invokeCallback(callbackId, error.getData());
            }
        });
    }

    private void removeConnection(final String callbackId, String provider) {
        GigyaLogger.debug(LOG_TAG, "Sending removeConnection api request with provider: " + provider);
        _businessApiService.removeConnection(provider, new GigyaCallback<GigyaApiResponse>() {
            @Override
            public void onSuccess(GigyaApiResponse response) {
                if (response.getErrorCode() == 0) {
                    invokeCallback(callbackId, response.asJson());
                    _invocationCallback.onPluginAuthEvent(PluginAuthEventDef.REMOVE_CONNECTION, null);
                } else {
                    onError(GigyaError.fromResponse(response));
                }
            }

            @Override
            public void onError(GigyaError error) {
                invokeCallback(callbackId, error.getData());
            }
        });
    }

    private void addConnection(final String callbackId, String provider) {
        GigyaLogger.debug(LOG_TAG, "Sending addConnection api request with provider: " + provider);
        _businessApiService.addConnection(provider, new GigyaLoginCallback<A>() {
            @Override
            public void onSuccess(A response) {
                invokeCallback(callbackId, new Gson().toJson(response));
                _invocationCallback.onPluginAuthEvent(PluginAuthEventDef.ADD_CONNECTION, response);
            }

            @Override
            public void onError(GigyaError error) {
                invokeCallback(callbackId, error.getData());
            }
        });
    }


    //endregion

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
