package com.gigya.android.sdk.providers.provider;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.gigya.android.sdk.GigyaContext;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.providers.LoginProvider;
import com.gigya.android.sdk.ui.HostActivity;
import com.gigya.android.sdk.utils.FileUtils;
import com.linecorp.linesdk.LineApiResponse;
import com.linecorp.linesdk.api.LineApiClient;
import com.linecorp.linesdk.api.LineApiClientBuilder;
import com.linecorp.linesdk.auth.LineLoginApi;
import com.linecorp.linesdk.auth.LineLoginResult;

import org.json.JSONObject;

import java.util.Map;

public class LineLoginProvider extends LoginProvider {

    private static final String LOG_TAG = "LineLoginProvider";

    private static final int REQUEST_CODE = 1;

    @Override
    public String getName() {
        return "line";
    }

    public LineLoginProvider(GigyaContext gigyaContext, GigyaLoginCallback callback) {
        super(gigyaContext, callback);
    }

    public static boolean isAvailable(Context context) {
        try {
            String lineChannelID = FileUtils.stringFromMetaData(context, "lineChannelID");
            Class.forName("com.linecorp.linesdk.auth.LineLoginApi");
            return lineChannelID != null;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void logout(Context context) {
        final String lineChannelID = FileUtils.stringFromMetaData(context, "lineChannelID");
        if (lineChannelID == null) {
            return;
        }
        LineApiClientBuilder builder = new LineApiClientBuilder(context, lineChannelID);
        LineApiClient client = builder.build();
        new LineLoginProvider.LogoutTask(client).execute();
    }

    @Override
    public String getProviderSessionsForRequest(String tokenOrCode, long expiration, String uid) {
        /* Only token is relevant */
        try {
            return new JSONObject()
                    .put(getName(), new JSONObject()
                            .put("authToken", tokenOrCode)).toString();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public void login(Context context, Map<String, Object> loginParams) {
        HostActivity.present(context, new HostActivity.HostActivityLifecycleCallbacks() {
            @Override
            public void onCreate(AppCompatActivity activity, @Nullable Bundle savedInstanceState) {
                /* Fetch channel Id from meta-data. */
                final String lineChannelID = FileUtils.stringFromMetaData(activity, "lineChannelID");
                if (lineChannelID == null) {
                    /* Fail login. */
                    _loginCallbacks.onProviderLoginFailed(getName(), "Channel Id not available");
                    activity.finish();
                    return;
                }
                final Intent loginIntent = LineLoginApi.getLoginIntent(activity, lineChannelID);
                activity.startActivityForResult(loginIntent, REQUEST_CODE);
            }

            @Override
            public void onActivityResult(AppCompatActivity activity, int requestCode, int resultCode, @Nullable Intent data) {
                if (requestCode == REQUEST_CODE) {
                    LineLoginResult result = LineLoginApi.getLoginResultFromIntent(data);
                    switch (result.getResponseCode()) {
                        case SUCCESS:
                            if (result.getLineCredential() == null) {
                                /* Fail login. */
                                return;
                            }
                            final String accessToken = result.getLineCredential().getAccessToken().getAccessToken();
                            _loginCallbacks.onProviderLoginSuccess(LineLoginProvider.this, getProviderSessionsForRequest(accessToken, -1, null));
                            break;
                        case CANCEL:
                            _loginCallbacks.onCanceled();
                            break;
                        default:
                            /* Any other is an error. */
                            _loginCallbacks.onProviderLoginFailed(getName(), result.getErrorData().getMessage());
                            break;
                    }

                    activity.finish();
                }
            }
        });
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
                GigyaLogger.debug(LOG_TAG, "Line logout success");
            } else {
                /* Logout error. */
                GigyaLogger.debug(LOG_TAG, "Line logout error");
            }
            _client = null;
        }
    }
}
