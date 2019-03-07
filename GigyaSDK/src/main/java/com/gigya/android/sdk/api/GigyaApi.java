package com.gigya.android.sdk.api;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaDefinitions;
import com.gigya.android.sdk.model.account.GigyaAccount;
import com.gigya.android.sdk.model.account.SessionInfo;
import com.gigya.android.sdk.network.GigyaApiRequest;
import com.gigya.android.sdk.network.GigyaApiRequestBuilder;
import com.gigya.android.sdk.network.GigyaApiResponse;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.adapter.INetworkCallbacks;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;
import com.gigya.android.sdk.services.AccountService;
import com.gigya.android.sdk.services.SessionService;

import java.util.Map;

/**
 * General Gigya API structure.
 *
 * @param <T> Generic Response structure (if provided)
 * @param <A> Generic account scheme structure. If not provided will be GigyaAccount.class scheme.
 */
public class GigyaApi<T, A extends GigyaAccount> implements IGigyaApi<T> {

    protected final NetworkAdapter _adapter;
    protected final SessionService _sessionService;
    protected final AccountService<A> _accountService;

    @NonNull
    final private Class<T> _responseClazz;

    public GigyaApi(NetworkAdapter networkAdapter, SessionService sessionService, AccountService<A> accountService,
                    @NonNull Class<T> responseClazz) {
        _adapter = networkAdapter;
        _sessionService = sessionService;
        _accountService = accountService;
        _responseClazz = responseClazz;
    }

    /**
     * Generic execution.
     *
     * @param api        Requested Gigya API.
     * @param httpMethod GET or POST currently supported.
     * @param params     Request parameters.
     * @param callback   Response callback.
     */
    public void execute(String api, NetworkAdapter.Method httpMethod, Map<String, Object> params, GigyaCallback<T> callback) {
        final GigyaApiRequest request = new GigyaApiRequestBuilder(_sessionService).api(api).params(params).httpMethod(httpMethod).build();
        send(request, callback, false);
    }

    /**
     * Send API request using NetworkAdapter provider.
     *
     * @param apiRequest GigyaApiRequest structure.
     * @param callback   Provided GigyaCallback or decedent.
     * @param isBlocking True if sending the request in blocking queue state,
     */
    protected void send(final GigyaApiRequest apiRequest, final GigyaCallback<T> callback, boolean isBlocking) {
        _adapter.send(apiRequest, new INetworkCallbacks() {
            @Override
            public void onResponse(String jsonResponse) {
                try {
                    final GigyaApiResponse apiResponse = new GigyaApiResponse(jsonResponse);
                    final int statusCode = apiResponse.getStatusCode();
                    if (statusCode == GigyaApiResponse.OK) {
                        onRequestSuccess(apiRequest.getApi(), apiResponse, callback);
                    } else {
                        onRequestError(apiRequest.getApi(), apiResponse, callback);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    if (callback != null)
                        callback.onError(GigyaError.generalError());
                }
            }

            @Override
            public void onError(GigyaError gigyaError) {
                if (callback != null) {
                    callback.onError(gigyaError);
                }
            }
        }, isBlocking);
    }

    /**
     * Success handler.
     * Override for custom implementation
     *
     * @param api         Requested API.
     * @param apiResponse GigyaApiResponse structure.
     * @param callback    Provided GigyaCallback or decedent.
     */
    @Override
    public void onRequestSuccess(@NonNull String api, GigyaApiResponse apiResponse, @Nullable GigyaCallback<T> callback) {
        if (apiResponse.contains("sessionSecret")) {
            final SessionInfo newSession = apiResponse.getField("sessionInfo", SessionInfo.class);
            _sessionService.setSession(newSession);
            _accountService.invalidateAccount();
        }
        // Patching due to ScreenSets error...
        if (api.equals(GigyaDefinitions.API.API_SET_ACCOUNT_INFO)) {
            _accountService.invalidateAccount();
        }
        // Parse to requested response scheme if available. Other wise forward unchecked GigyaApiResponse.
        T parsed = apiResponse.getGson().fromJson(apiResponse.asJson(), _responseClazz);
        if (callback != null) {
            callback.onSuccess(parsed);
        }
    }

    /**
     * Error handler.
     * By default will forward error to callback.
     * Override for custom implementation.
     *
     * @param api         Requested API.
     * @param apiResponse GigyaApiResponse structure.
     * @param callback    Provided GigyaCallback or decedent.
     */
    @Override
    public void onRequestError(String api, GigyaApiResponse apiResponse, @Nullable GigyaCallback<T> callback) {
        if (callback != null) {
            callback.onError(GigyaError.fromResponse(apiResponse));
        }
    }

}
