package com.gigya.android.sdk.login.provider;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
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

    @Override
    public String getName() {
        return "googleplus";
    }

    private static final int RC_SIGN_IN = 0;
    private GoogleSignInClient _googleClient;

    public GoogleLoginProvider(Context context, LoginProviderCallbacks loginCallbacks) {
        // TODO: 14/01/2019 Do we still need this fallback?
        super(loginCallbacks, null);
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            providerClientId = (String) appInfo.metaData.get("googleClientId");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public boolean clientIdRequired() {
        return true;
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
    public void logout(Context context) {
        if (_googleClient == null) {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestServerAuthCode(providerClientId)
                    .requestEmail()
                    .build();
            _googleClient = GoogleSignIn.getClient(context, gso);
        }
        _googleClient.signOut();
    }

    @Override
    public String getProviderSessions(String tokenOrCode, long expiration, String uid) {
        /* code is relevant */
        try {
            return new JSONObject()
                    .put(getName(), new JSONObject()
                            .put("code", tokenOrCode)).toString();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public void login(Context context, Map<String, Object> loginParams) {
        if (providerClientId == null) {
            loginCallbacks.onProviderLoginFailed(getName(), "Missing server client id. Check manifest implementation");
            return;
        }
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestServerAuthCode(providerClientId)
                .requestEmail()
                .build();
        _googleClient = GoogleSignIn.getClient(context, gso);

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (account != null) {
            /* This option should not happen theoretically because we logout out explicitly. */
            this.loginCallbacks.onProviderLoginSuccess(getName(), getProviderSessions(account.getServerAuthCode(), -1L, null));
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
                    loginCallbacks.onProviderLoginFailed(getName(), "Id token no available");
                    return;
                }
                this.loginCallbacks.onProviderLoginSuccess(getName(), getProviderSessions(authCode, -1L, null));
                if (_googleClient != null) {
                    _googleClient.signOut();
                }
                activity.finish();
            }
        } catch (ApiException e) {
            loginCallbacks.onProviderLoginFailed(getName(), e.getLocalizedMessage());
            if (_googleClient != null) {
                _googleClient.signOut();
            }
            activity.finish();
        }
    }
}
