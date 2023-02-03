package com.gigya.android.sample.gigya.providers;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.gigya.android.sample.R;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.providers.external.IProviderWrapper;
import com.gigya.android.sdk.providers.external.IProviderWrapperCallback;
import com.gigya.android.sdk.providers.external.ProviderWrapper;
import com.gigya.android.sdk.ui.HostActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import java.util.HashMap;
import java.util.Map;

/**
 * Google sign in wrapper class.
 * Add the following class to your application ../gigya.providers package.
 * Make sure to define the application id in your strings resources file.
 */
public class GoogleProviderWrapper extends ProviderWrapper implements IProviderWrapper {

    private static final int RC_SIGN_IN = 0;
    private GoogleSignInClient _googleClient;
    final Context context;

    GoogleProviderWrapper(Context context) {
        super(context, R.string.google_client_id);
        this.context = context;
    }

    @Override
    public void login(Context context, final Map<String, Object> params, final IProviderWrapperCallback callback) {
        if (pId == null) {
            callback.onFailed("Missing server client id. Check manifest implementation");
            return;
        }
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestServerAuthCode(pId)
                .requestEmail()
                .build();
        _googleClient = GoogleSignIn.getClient(context, gso);

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (account != null) {
            // This option should not happen theoretically because we logout out explicitly.
            final Map<String, Object> loginMap = new HashMap<>();
            final String code = account.getServerAuthCode();
            if (code == null) {
                GigyaLogger.error("GoogleProviderWrapper", "Server auth code null");
                callback.onFailed("Error acquiring server auth code");
            }
            loginMap.put("code", account.getServerAuthCode());
            callback.onLogin(loginMap);
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
                    handleSignInResult(params, activity, task, callback);
                }
            }
        });
    }

    private void handleSignInResult(final Map<String, Object> loginParams, AppCompatActivity activity, Task<GoogleSignInAccount> completedTask, final IProviderWrapperCallback callback) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account == null) {
                callback.onFailed("Account unavailable");
            } else {
                /* Fetch server auth code */
                final String authCode = account.getServerAuthCode();
                if (authCode == null) {
                    callback.onFailed("Id token no available");
                } else {
                    final Map<String, Object> loginMap = new HashMap<>();
                    loginMap.put("code", authCode);
                    callback.onLogin(loginMap);
                }
            }
            activity.finish();
        } catch (ApiException e) {
            final int exceptionStatusCode = e.getStatusCode();
            switch (exceptionStatusCode) {
                case GoogleSignInStatusCodes.SIGN_IN_CANCELLED:
                    callback.onCanceled();
                    break;
                case GoogleSignInStatusCodes.SIGN_IN_FAILED:
                default:
                    callback.onFailed(GoogleSignInStatusCodes.getStatusCodeString(exceptionStatusCode));
                    break;
            }
            activity.finish();
        }
    }

    @Override
    public void logout() {
        if (_googleClient == null) {
            if (pId == null) {
                GigyaLogger.error("GoogleLoginProvider", "provider client id unavailable for logout");
                return;
            }
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestServerAuthCode(pId)
                    .requestEmail()
                    .build();
            _googleClient = GoogleSignIn.getClient(context, gso);
        }
        _googleClient.signOut();
    }
}
