package com.gigya.android.sample.gigya.providers;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.gigya.android.sample.R;
import com.gigya.android.sdk.providers.external.IProviderWrapper;
import com.gigya.android.sdk.providers.external.IProviderWrapperCallback;
import com.gigya.android.sdk.providers.external.ProviderWrapper;
import com.gigya.android.sdk.ui.HostActivity;
import com.linecorp.linesdk.LineApiResponse;
import com.linecorp.linesdk.Scope;
import com.linecorp.linesdk.api.LineApiClient;
import com.linecorp.linesdk.api.LineApiClientBuilder;
import com.linecorp.linesdk.auth.LineAuthenticationParams;
import com.linecorp.linesdk.auth.LineLoginApi;
import com.linecorp.linesdk.auth.LineLoginResult;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * LINE sign in wrapper class.
 * Add the following class to your application ../gigya.providers package.
 * Make sure to define the application id in your strings resources file.
 */
public class LineProviderWrapper extends ProviderWrapper implements IProviderWrapper {

    private static final int REQUEST_CODE = 1;

    IProviderWrapperCallback providerWrapperCallback;
    final Context context;

    LineProviderWrapper(Context context) {
        super(context, R.string.line_channel_id);
        this.context = context;
    }

    @Override
    public void login(Context context, Map<String, Object> params, IProviderWrapperCallback callback) {
        providerWrapperCallback = callback;
        HostActivity.present(context, new HostActivity.HostActivityLifecycleCallbacks() {
            @Override
            public void onCreate(AppCompatActivity activity, @Nullable Bundle savedInstanceState) {
                if (pId == null) {
                    // Fail login.
                    callback.onFailed("Channel Id not available");
                    activity.finish();
                    return;
                }

                Intent loginIntent = LineLoginApi.getLoginIntent(
                        activity,
                        pId,
                        new LineAuthenticationParams.Builder()
                                .scopes(Arrays.asList(Scope.PROFILE))
                                .build());
                activity.startActivityForResult(loginIntent, REQUEST_CODE);
            }

            @Override
            public void onActivityResult(AppCompatActivity activity, int requestCode, int resultCode, @Nullable Intent data) {
                if (providerWrapperCallback == null) {
                    return;
                }
                if (requestCode == REQUEST_CODE) {
                    LineLoginResult result = LineLoginApi.getLoginResultFromIntent(data);
                    switch (result.getResponseCode()) {
                        case SUCCESS:
                            if (result.getLineCredential() == null) {
                                // Fail login.
                                return;
                            }
                            final String accessToken = result.getLineCredential().getAccessToken().getTokenString();
                            final Map<String, Object> loginMap = new HashMap<>();
                            loginMap.put("token", accessToken);
                            providerWrapperCallback.onLogin(loginMap);
                            break;
                        case CANCEL:
                            providerWrapperCallback.onCanceled();
                            break;
                        default:
                            // Any other is an error.
                            providerWrapperCallback.onFailed(result.getErrorData().getMessage());
                            break;
                    }
                    activity.finish();
                }
            }
        });
    }

    @Override
    public void logout() {
        LineApiClientBuilder builder = new LineApiClientBuilder(context, pId);
        LineApiClient client = builder.build();
        new LogoutTask(client).execute();
    }

    private static class LogoutTask extends AsyncTask<Void, Void, LineApiResponse> {

        private LineApiClient _client;

        LogoutTask(LineApiClient client) {
            _client = client;
        }

        @Override
        protected LineApiResponse doInBackground(Void... voids) {
            return _client.logout();
        }

        @Override
        protected void onPostExecute(LineApiResponse lineApiResponse) {
            if (lineApiResponse.isSuccess()) {
                /* Logout success. */
            } else {
                /* Logout error. */
            }
            _client = null;
        }
    }
}
