package com.gigya.android.sdk.providers.provider;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.account.IAccountService;
import com.gigya.android.sdk.api.IApiService;
import com.gigya.android.sdk.persistence.IPersistenceService;
import com.gigya.android.sdk.session.ISessionService;
import com.gigya.android.sdk.ui.HostActivity;
import com.gigya.android.sdk.utils.FileUtils;
import com.linecorp.linesdk.LineApiResponse;
import com.linecorp.linesdk.api.LineApiClient;
import com.linecorp.linesdk.api.LineApiClientBuilder;
import com.linecorp.linesdk.auth.LineLoginApi;
import com.linecorp.linesdk.auth.LineLoginResult;

import org.json.JSONObject;

import java.util.Map;

import static com.gigya.android.sdk.GigyaDefinitions.Providers.LINE;

public class LineProvider extends Provider {

    private static final String LOG_TAG = "LineLoginProvider";

    private static final int REQUEST_CODE = 1;

    public LineProvider(Config config, ISessionService sessionService, IAccountService accountService,
                        IApiService apiService, IPersistenceService persistenceService, GigyaLoginCallback gigyaLoginCallback) {
        super(config, sessionService, accountService, apiService, persistenceService, gigyaLoginCallback);
    }

    @Override
    public String getName() {
        return LINE;
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
    public void login(Context context, Map<String, Object> loginParams, final String loginMode) {
        _loginMode = loginMode;
        HostActivity.present(context, new HostActivity.HostActivityLifecycleCallbacks() {
            @Override
            public void onCreate(AppCompatActivity activity, @Nullable Bundle savedInstanceState) {
                // Fetch channel Id from meta-data.
                final String lineChannelID = FileUtils.stringFromMetaData(activity, "lineChannelID");
                if (lineChannelID == null) {
                    // Fail login.
                    onLoginFailed("Channel Id not available");
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
                                // Fail login.
                                return;
                            }
                            final String accessToken = result.getLineCredential().getAccessToken().getAccessToken();
                            onLoginSuccess(getProviderSessions(accessToken, -1, null), loginMode);
                            break;
                        case CANCEL:
                            onCanceled();
                            break;
                        default:
                            // Any other is an error.
                            onLoginFailed(result.getErrorData().getMessage());
                            break;
                    }
                    activity.finish();
                }
            }
        });
    }

    @Override
    public void logout(Context context) {
        final String lineChannelID = FileUtils.stringFromMetaData(context, "lineChannelID");
        if (lineChannelID == null) {
            return;
        }
        LineApiClientBuilder builder = new LineApiClientBuilder(context, lineChannelID);
        LineApiClient client = builder.build();
        new LogoutTask(client).execute();
    }

    @Override
    public String getProviderSessions(String tokenOrCode, long expiration, String uid) {
        // Only token is relevant.
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
    public boolean supportsTokenTracking() {
        return false;
    }

    @Override
    public void trackTokenChange() {
        // Stub.
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
                GigyaLogger.error(LOG_TAG, "Line logout error");
            }
            _client = null;
        }
    }
}
