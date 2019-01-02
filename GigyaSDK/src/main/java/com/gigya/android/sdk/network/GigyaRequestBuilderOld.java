package com.gigya.android.sdk.network;

import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.log.GigyaLogger;
import com.gigya.android.sdk.model.Configuration;
import com.gigya.android.sdk.utils.AuthUtils;
import com.gigya.android.sdk.utils.UrlUtils;

import org.json.JSONObject;

import java.util.Map;
import java.util.TreeMap;

/*
Request builder for all SDK communication.
 */
public class GigyaRequestBuilderOld<T> {

    private static final String LOG_TAG = "GigyaRequestBuilderOld";
    private final TreeMap<String, Object> serverParams = new TreeMap<>();
    private Configuration configuration;
    private Map<String, Object> params;
    private Class<T> clazz;
    private int httpMethod = Request.Method.POST;
    private String api;
    private GigyaCallback<T> callback;
    private Request.Priority priority;
    private SessionManager sessionManager;
    private GigyaInterceptionCallback interceptor;

    //region Builder pattern

    public GigyaRequestBuilderOld(Configuration configuration) {
        this.configuration = configuration;
    }

    public GigyaRequestBuilderOld api(String api) {
        this.api = api;
        return this;
    }

    public GigyaRequestBuilderOld httpMethod(int httpMethod) {
        this.httpMethod = httpMethod;
        return this;
    }

    public GigyaRequestBuilderOld params(Map<String, Object> params) {
        this.params = params;
        return this;
    }

    public GigyaRequestBuilderOld priority(Request.Priority priority) {
        this.priority = priority;
        return this;
    }

    public GigyaRequestBuilderOld output(Class<T> clazz) {
        this.clazz = clazz;
        return this;
    }

    public GigyaRequestBuilderOld sessionManager(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
        return this;
    }

    public GigyaRequestBuilderOld callback(GigyaCallback<T> callback) {
        this.callback = callback;
        return this;
    }

    public GigyaRequestBuilderOld interceptor(GigyaInterceptionCallback interceptor) {
        this.interceptor = interceptor;
        return this;
    }

    //endregion

    // TODO: 31/12/2018 Unit test build()
    @SuppressWarnings("unchecked")
    public GigyaRequestOld build() {
        if (params != null) {
            // Convert supplied params to tree map in order to keep the item order which is needed to generate the signature.
            serverParams.putAll(params);
        }

        addEnvironmentParameters();
        addConfigurationParameters();

        /*
        Instantiate a Volley error listener. Receiving an error here means we are not reaching the Gigya backend.
         */

        final Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // Generate GigyaError instance from VolleyError.
                int errorCode = 0;
                if (error.networkResponse != null) {
                    errorCode = error.networkResponse.statusCode;
                }
                final String localizedMessage = error.getLocalizedMessage() == null ? "" : error.getLocalizedMessage();
                final GigyaError gigyaError = new GigyaError(errorCode, localizedMessage, null);
                GigyaLogger.debug("GigyaResponse", "GigyaResponse: Error " + gigyaError.toString());
                callback.onError(gigyaError);
            }
        };

        /*
        Instantiate a Volley response listener. Handle JSON.
         */

        final Response.Listener<String> responseListener = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject responseJSON = new JSONObject(response);
                    // Log response (PrettyPrint)
                    GigyaLogger.debug("GigyaResponse", api + ": HTTPS Success" + "\n" + responseJSON.toString(2));
                    GigyaResponseHandler<T> responseHandler = new GigyaResponseHandler(callback, clazz, sessionManager, interceptor);
                    responseHandler.verifyWith(responseJSON);
                } catch (Exception e) {
                    GigyaLogger.debug("GigyaResponse", api + ": HTTPS Success" + "\n" + response);
                    errorListener.onErrorResponse(new ParseError(e));
                }
            }
        };

        if (this.sessionManager != null) {
            addSessionParameters();
        }

        final String encodedParams = UrlUtils.buildEncodedQuery(serverParams);
        GigyaLogger.debug(LOG_TAG, api + ": " + encodedParams);
        final String url = getUrl(encodedParams);
        GigyaLogger.debug(LOG_TAG, api + ": " + url);
        // Build request.
        if (httpMethod == Request.Method.POST) {
            return new GigyaRequestOld(url, encodedParams, responseListener, errorListener, this.priority, this.api);
        }
        return new GigyaRequestOld(url, responseListener, errorListener, this.priority, this.api);
    }

    private String getBaseUrl() {
        final StringBuilder sb = new StringBuilder();
        final String[] split = this.api.split("\\.");
        return sb.append("https://")
                .append(split[0]).append(".")
                .append(configuration.getApiDomain())
                .append("/")
                .append(api)
                .toString();
    }

    @SuppressWarnings("StringBufferReplaceableByString")
    private String getUrl(String encodedParams) {
        final StringBuilder sb = new StringBuilder();
        return sb.append(getBaseUrl())
                // Concatenate query params with URL encoding for GET requests.
                .append(this.httpMethod == Request.Method.GET ? "?" + encodedParams : "")
                .toString();
    }

    private void addEnvironmentParameters() {
        // Add sdk version & target environment
        serverParams.put("sdk", Gigya.VERSION);
        serverParams.put("targetEnv", "mobile");
        serverParams.put("httpStatusCodes", false);
        serverParams.put("format", "json");
    }

    private void addConfigurationParameters() {
        final String gmid = configuration.getGMID();
        if (gmid != null) {
            serverParams.put("gmid", gmid);
        }
        final String ucid = configuration.getUCID();
        if (ucid != null) {
            serverParams.put("ucid", ucid);
        }
    }

    // TODO: 06/12/2018 Check if Api-key causes interference. if not keep it in the params map.

    private void addSessionParameters() {
        if (serverParams.containsKey("ApiKey")) {
            // Apis that contain "ApiKey" parameter explicitly.
            return;
        }
        if (sessionManager.isValidSession()) {
            @SuppressWarnings("ConstantConditions") final String sessionToken = sessionManager.getSession().getSessionToken();
            serverParams.put("oauth_token", sessionToken);
        } else {
            serverParams.put("ApiKey", configuration.getApiKey());
        }
        addAuthenticationParameters();
    }

    private void addAuthenticationParameters() {
        if (sessionManager.getSession() != null) {
            // Add timestamp.
            final String sessionSecret = sessionManager.getSession().getSessionSecret();
            AuthUtils.addAuthenticationParameters(sessionSecret,
                    httpMethod,
                    getBaseUrl(),
                    serverParams);
        }
    }

}
