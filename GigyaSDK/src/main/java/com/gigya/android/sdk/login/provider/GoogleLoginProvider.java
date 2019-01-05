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

import java.util.Map;

public class GoogleLoginProvider extends LoginProvider {

    private static final int RC_SIGN_IN = 0;
    private GoogleSignInClient _googleClient;

    public GoogleLoginProvider(LoginProviderCallbacks loginCallbacks) {
        super(loginCallbacks);
    }

    @UiThread
    public static boolean isAvailable(Context context) {
        try {
            Class.forName("com.google.android.gms.common.GoogleApiAvailability");
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
    public void login(Context context, Map<String, Object> loginParams) {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        _googleClient = GoogleSignIn.getClient(context, gso);
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (account != null) {
            loginCallbacks.onProviderLoginSuccess("googlePlus", account.getIdToken(), 0);
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
                final String idToken = account.getIdToken();
                loginCallbacks.onProviderLoginSuccess("googlePlus", idToken, 0);
                activity.finish();
            }
        } catch (ApiException e) {
            loginCallbacks.onProviderLoginFailed("googlePlus", e.getLocalizedMessage());
            activity.finish();
        }
    }
}
