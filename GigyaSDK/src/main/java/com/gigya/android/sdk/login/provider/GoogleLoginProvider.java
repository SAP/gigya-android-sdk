package com.gigya.android.sdk.login.provider;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.v7.app.AppCompatActivity;

import com.gigya.android.sdk.login.LoginProvider;
import com.gigya.android.sdk.ui.HostActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import org.json.JSONObject;

import java.util.Map;

public class GoogleLoginProvider extends LoginProvider {

    public static final String NAME = "googleplus";

    public static final String GOOGLE_SERVER_CLIENT_ID = "google_server_client_id";

    private static final int RC_SIGN_IN = 0;
    private GoogleSignInClient _googleClient;

    public GoogleLoginProvider(LoginProviderCallbacks loginCallbacks) {
        super(loginCallbacks);
    }

    @UiThread
    public static boolean isAvailable(Context context) {
        try {
            Class.forName("com.google.android.gms.auth.api.signin.GoogleSignInClient");
            return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void logout() {
        if (_googleClient != null) {
            _googleClient.signOut();
        }
    }

    @Override
    public String getProviderSessions(String tokenOrCode, long expiration, String uid) {
        /* code is relevant */
        try {
            return new JSONObject()
                    .put(NAME, new JSONObject()
                            .put("code", tokenOrCode)).toString();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public void login(Context context, Map<String, Object> loginParams) {
        final String serverClientId = (String) loginParams.get(GOOGLE_SERVER_CLIENT_ID);
        if (serverClientId == null) {
            loginCallbacks.onProviderLoginFailed(NAME, "Missing server client id");
            return;
        }
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestServerAuthCode(serverClientId)
                .requestEmail()
                .build();
        _googleClient = GoogleSignIn.getClient(context, gso);
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (account != null) {
            /* This option should not happen theoretically because we logout out explicitly. */
            this.loginCallbacks.onProviderLoginSuccess(NAME, getProviderSessions(account.getServerAuthCode(), -1L, null));
            _googleClient.signOut();
            return;
        }
        HostActivity.present(context, new HostActivity.HostActivityLifecycleCallbacks() {

            @Override
            public void onCreate(AppCompatActivity activity, @Nullable Bundle savedInstanceState) {
                Intent signInIntent = _googleClient.getSignInIntent();
                activity.startActivityForResult(signInIntent, RC_SIGN_IN);
            }

            @Override
            public void onActivityResult(AppCompatActivity activity, int requestCode, int resultCode, @Nullable Intent data) {
                if (requestCode == RC_SIGN_IN) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                    handleSignInResult(activity, task);
                }
            }
        });
    }

    private void handleSignInResult(AppCompatActivity activity, Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) {
                /* Fetch server auth code */
                final String authCode = account.getServerAuthCode();
                if (authCode == null) {
                    loginCallbacks.onProviderLoginFailed(NAME, "Id token no available");
                    return;
                }
                this.loginCallbacks.onProviderLoginSuccess(NAME, getProviderSessions(authCode, -1L, null));
                if (_googleClient != null) {
                    _googleClient.signOut();
                }
                activity.finish();
            }
        } catch (ApiException e) {
            loginCallbacks.onProviderLoginFailed(NAME, e.getLocalizedMessage());
            if (_googleClient != null) {
                _googleClient.signOut();
            }
            activity.finish();
        }
    }
}
