package com.gigya.android.sdk.providers.provider;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.v7.app.AppCompatActivity;

import com.gigya.android.sdk.GigyaContext;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.providers.LoginProvider;
import com.gigya.android.sdk.ui.HostActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
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

    public GoogleLoginProvider(Context context, GigyaContext gigyaContext, GigyaLoginCallback callback) {
        super(gigyaContext, callback);
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
            if (providerClientId == null) {
                GigyaLogger.error("GoogleLoginProvider", "provider client id unavailable for logout");
                return;
            }
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestServerAuthCode(providerClientId)
                    .requestEmail()
                    .build();
            _googleClient = GoogleSignIn.getClient(context, gso);
        }
        _googleClient.signOut();
    }

    @Override
    public String getProviderSessionsForRequest(String tokenOrCode, long expiration, String uid) {
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
            _loginCallbacks.onProviderLoginFailed(getName(), "Missing server client id. Check manifest implementation");
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
            _loginCallbacks.onProviderLoginSuccess(this, getProviderSessionsForRequest(account.getServerAuthCode(), -1L, null));
            finish(null);
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
            if (account == null) {
                _loginCallbacks.onProviderLoginFailed(getName(), "Account unavailable");
            } else {
                /* Fetch server auth code */
                final String authCode = account.getServerAuthCode();
                if (authCode == null) {
                    _loginCallbacks.onProviderLoginFailed(getName(), "Id token no available");
                } else {
                    _loginCallbacks.onProviderLoginSuccess(this, getProviderSessionsForRequest(authCode, -1L, null));
                }
            }
            finish(activity);
        } catch (ApiException e) {
            final int exceptionStatusCode = e.getStatusCode();
            switch (exceptionStatusCode) {
                case GoogleSignInStatusCodes.SIGN_IN_CANCELLED:
                    _loginCallbacks.onCanceled();
                    break;
                case GoogleSignInStatusCodes.SIGN_IN_FAILED:
                default:
                    _loginCallbacks.onProviderLoginFailed(getName(), GoogleSignInStatusCodes.getStatusCodeString(exceptionStatusCode));
                    break;
            }
            finish(activity);
        }
    }

    private void finish(final Activity activity) {
        if (_googleClient != null) {
            _googleClient.signOut().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (activity != null) {
                        activity.finish();
                    }
                }
            });
        } else {
            if (activity != null) {
                activity.finish();
            }
        }
    }
}
