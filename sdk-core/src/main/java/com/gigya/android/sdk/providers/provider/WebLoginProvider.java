package com.gigya.android.sdk.providers.provider;

import android.app.Activity;
import android.content.Context;
import android.util.Pair;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.account.IAccountService;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.network.adapter.RestAdapter;
import com.gigya.android.sdk.persistence.IPersistenceService;
import com.gigya.android.sdk.session.ISessionService;
import com.gigya.android.sdk.session.SessionInfo;
import com.gigya.android.sdk.ui.WebLoginActivity;
import com.gigya.android.sdk.utils.AuthUtils;
import com.gigya.android.sdk.utils.UrlUtils;

import org.json.JSONObject;

import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

public class WebLoginProvider extends Provider {

    private static final String LOG_TAG = "WebLoginProvider";

    final private ISessionService _sessionService;
    final private IAccountService _accountService;
    final private Config _config;

    public WebLoginProvider(Context context,
                            Config config,
                            ISessionService sessionService,
                            IAccountService accountService,
                            IPersistenceService persistenceService,
                            ProviderCallback providerCallback) {
        super(context, persistenceService, providerCallback);
        _config = config;
        _sessionService = sessionService;
        _accountService = accountService;
    }

    @Override
    public String getName() {
        return "web";
    }

    @Override
    public void login(Map<String, Object> loginParams, final String loginMode) {
        if (_connecting) {
            return;
        }
        _connecting = true;
        _loginMode = loginMode;
        final String providerName = (String) loginParams.get("provider");
        final String loginUrl = getRequest(_context, loginParams, loginMode);
        WebLoginActivity.present(_context, loginUrl, new WebLoginActivity.WebLoginActivityCallback() {

            @Override
            public void onResult(Activity activity, Map<String, Object> parsed) {
                GigyaLogger.debug(LOG_TAG, "onResult: " + parsed.toString());

                final String status = (String) parsed.get("status");
                if (status != null && status.equals("ok")) {

                    final SessionInfo sessionInfo = parseSessionInfo(parsed);
                    // Notify successful sign in.
                    onLoginSuccess(providerName, sessionInfo);
                    
                } else {

                    JSONObject jsonObject = new JSONObject();

                    try {
                        // An error result appears in a different format.
                        // Need to parse it correctly in order for the flow to continue as expected.

                        if (parsed.containsKey("error_description")) {

                            final String errorDescription = (String) parsed.get("error_description");
                            if (errorDescription != null) {
                                String[] parts = errorDescription.replace("+", "").split("-");
                                if (parts.length > 1) {
                                    final int errorCode = Integer.parseInt(parts[0].trim());
                                    final String errorMessage = parts[1].trim();

                                    jsonObject.put("errorCode", errorCode);
                                    jsonObject.put("errorMessage", errorMessage);
                                }
                            }

                            if (parsed.containsKey("x_regToken")) {
                                final String regToken = (String) parsed.get("x_regToken");
                                jsonObject.put("regToken", regToken); // Nullification is not relevant here.
                            }
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    GigyaLogger.debug(LOG_TAG, "onResult: with error = " + jsonObject.toString());

                    onLoginFailed(new GigyaApiResponse(jsonObject.toString()));
                }

                if (activity != null) {
                    activity.finish();
                }
            }

            @Override
            public void onCancelled() {
                if (_providerCallback != null) {
                    _providerCallback.onCanceled();
                }
            }
        });
    }

    @Override
    public void logout() {
        // Stub.
    }

    @Override
    public String getProviderSessions(String tokenOrCode, long expiration, String uid) {
        return null;
    }

    private SessionInfo parseSessionInfo(Map<String, Object> result) {
        final String accessToken = (String) result.get("access_token");
        final String expiresInString = (String) result.get("expires_in");
        final long expiresIn = Long.parseLong(expiresInString != null ? expiresInString : "0");
        final String secret = (String) result.get("x_access_token_secret");
        return new SessionInfo(secret, accessToken, expiresIn);
    }

    private String getRequest(Context context, Map<String, Object> loginParams, String loginMode) {
        final TreeMap<String, Object> serverParams = new TreeMap<>();
        final String provider = ((String) loginParams.get("provider"));
        if (provider != null) {
            final String xperm = (String) loginParams.get(provider + "ExtraPermissions");
            if (xperm != null) {
                loginParams.remove(provider + "ExtraPermissions");
                serverParams.put("x_extraPermissions", xperm);
            }
        }
        // Deep link schematics -> Scheme = gigya, host = gsapi, pathPrefix = package name + "/login_result.
        final String redirectUri = "gigya://gsapi/" + context.getPackageName() + "/login_result";
        serverParams.put("redirect_uri", redirectUri);
        serverParams.put("response_type", "token");
        serverParams.put("client_id", _config.getApiKey());
        serverParams.put("gmid", _config.getGmid());
        serverParams.put("ucid", _config.getUcid());
        serverParams.put("x_secret_type", "oauth1");
        serverParams.put("x_sdk", Gigya.VERSION);
        // x_params additions.
        for (Map.Entry entry : loginParams.entrySet()) {
            final String key = (String) entry.getKey();
            Object value = loginParams.get(key);
            if (value != null) {
                if (key.startsWith("x_"))
                    serverParams.put(key, value);
                else
                    serverParams.put("x_" + key, value);
            }
        }

        String api = "socialize.login";
        if (loginMode.equals("connect")) {
            api = "socialize.addConnection";
        }

        // Add nonce.
        final Random random = new Random();
        String nonce = System.currentTimeMillis() + "_" + random.nextInt();
        serverParams.put("nonce", nonce);

        if (_sessionService.isValid()) {
            // Add signature parameters if needed.
            @SuppressWarnings("ConstantConditions") final String sessionToken = _sessionService.getSession().getSessionToken();
            serverParams.put("oauth_token", sessionToken);
            final String sessionSecret = _sessionService.getSession().getSessionSecret();
            AuthUtils.addAuthenticationParameters(sessionSecret,
                    RestAdapter.GET,
                    UrlUtils.getBaseUrl(api, _config.getApiDomain()),
                    serverParams,
                    _config.getServerOffset());
        }

        // Build final URL.
        return String.format("%s://%s.%s/%s?%s", "https", "socialize", _config.getApiDomain(),
                api, UrlUtils.buildEncodedQuery(serverParams));
    }

    private Pair<String, String> getPostRequest(Map<String, Object> loginParams) {
        /* Remove if added. */
        if (!loginParams.isEmpty())
            loginParams.remove(FacebookProvider.LOGIN_BEHAVIOUR);

        final String url = "https://socialize." + _config.getApiDomain() + "/socialize.login";
        final TreeMap<String, Object> urlParams = new TreeMap<>();
        urlParams.put("redirect_uri", "gsapi://login_result");
        urlParams.put("response_type", "token");
        urlParams.put("client_id", _config.getApiKey());
        urlParams.put("gmid", _config.getGmid());
        urlParams.put("ucid", _config.getUcid());

        // x_params additions.
        for (Map.Entry entry : loginParams.entrySet()) {
            final String key = (String) entry.getKey();
            Object value = loginParams.get(key);
            if (value != null) {
                if (key.startsWith("x_"))
                    urlParams.put(key, value);
                else
                    urlParams.put("x_" + key, value);
            }
        }
        urlParams.put("x_secret_type", "oauth1");
        urlParams.put("x_endPoint", "socialize.login");
        urlParams.put("x_sdk", Gigya.VERSION);
        final String provider = (String) loginParams.get("provider");
        if (provider != null) {
            urlParams.put("x_provider", provider);
        }

        // Encode post body.
        final String encodedParams = UrlUtils.buildEncodedQuery(urlParams);
        return new Pair<>(url, encodedParams);
    }
}
