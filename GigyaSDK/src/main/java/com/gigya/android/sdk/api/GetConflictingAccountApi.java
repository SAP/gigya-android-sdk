package com.gigya.android.sdk.api;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.GigyaRequest;
import com.gigya.android.sdk.network.GigyaRequestBuilder;
import com.gigya.android.sdk.network.GigyaResponse;
import com.gigya.android.sdk.network.adapter.INetworkCallbacks;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.gigya.android.sdk.network.GigyaResponse.OK;

public class GetConflictingAccountApi extends BaseApi {

    private static final String API = "accounts.getConflictingAccount";

    public void call(String regToken, final GigyaCallback<GigyaResponse> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("regToken", regToken);
        GigyaRequest request = new GigyaRequestBuilder(configuration).api(API).params(params).sessionManager(sessionManager).build();
        networkAdapter.send(request, new INetworkCallbacks() {
            @Override
            public void onResponse(String jsonResponse) {
                if (callback == null) {
                    return;
                }
                try {
                    final GigyaResponse response = new GigyaResponse(new JSONObject(jsonResponse));
                    final int statusCode = response.getStatusCode();
                    if (statusCode == OK) {
                        callback.onSuccess(response);
                        return;
                    }
                    final int errorCode = response.getErrorCode();
                    final String localizedMessage = response.getErrorDetails();
                    final String callId = response.getCallId();
                    callback.onError(new GigyaError(response.asJson(), errorCode, localizedMessage, callId));
                } catch (Exception ex) {
                    ex.printStackTrace();
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

    public static class ConflictingAccount {
        private List<String> loginProviders = new ArrayList<>();
        private String loginID;

        public List<String> getLoginProviders() {
            return loginProviders;
        }

        public String getLoginID() {
            return loginID;
        }
    }
}
