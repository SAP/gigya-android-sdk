package com.gigya.android.sdk.api;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.GigyaRequest;
import com.gigya.android.sdk.network.GigyaResponse;
import com.gigya.android.sdk.network.adapter.INetworkCallbacks;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;

import org.json.JSONObject;

import static com.gigya.android.sdk.network.GigyaResponse.OK;

public abstract class BaseApi<T> {

    protected final NetworkAdapter networkAdapter;
    protected final SessionManager sessionManager;

    //region Convenience constructors

    public BaseApi(NetworkAdapter networkAdapter, SessionManager sessionManager) {
        this.networkAdapter = networkAdapter;
        this.sessionManager = sessionManager;
    }

    //endregion

    protected void sendRequest(GigyaRequest request, final String api, final GigyaCallback<T> callback) {
        networkAdapter.send(request, new INetworkCallbacks() {
            @Override
            public void onResponse(String jsonResponse) {
                try {
                    final GigyaResponse response = new GigyaResponse(new JSONObject(jsonResponse));
                    final int statusCode = response.getStatusCode();
                    if (statusCode == OK) {
                        onRequestSuccess(api, response, callback);
                    } else {
                        onRequestError(api, response, callback);
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
        });
    }

    protected void onRequestSuccess(String api, GigyaResponse response, GigyaCallback<T> callback) {
        // Stub.
    }

    /* Override if needed. */
    protected void onRequestError(String api, GigyaResponse response, GigyaCallback<T> callback) {
        callback.onError(GigyaError.fromResponse(response));
    }


}
