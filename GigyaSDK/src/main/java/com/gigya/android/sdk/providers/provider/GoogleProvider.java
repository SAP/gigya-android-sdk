package com.gigya.android.sdk.providers.provider;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.account.IAccountService;
import com.gigya.android.sdk.api.IApiObservable;
import com.gigya.android.sdk.persistence.IPersistenceService;
import com.gigya.android.sdk.session.ISessionService;
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

import static com.gigya.android.sdk.GigyaDefinitions.Providers.GOOGLE;

public class GoogleProvider extends Provider {

    private static final int RC_SIGN_IN = 0;
    private GoogleSignInClient _googleClient;
    private String _clientId;

    public GoogleProvider(Context context, Config config, ISessionService sessionService, IAccountService accountService, IPersistenceService persistenceService,
                          IApiObservable observable, GigyaLoginCallback gigyaLoginCallback) {
        super(context, config, sessionService, accountService, persistenceService, observable, gigyaLoginCallback);
    }

    @Override
    public String getName() {
        return GOOGLE;
    }

    public static boolean isAvailable(Context context) {
        try {
            Class.forName("com.google.android.gms.auth.api.signin.GoogleSignInClient");
            return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void login(final Map<String, Object> loginParams, final String loginMode) {
        _loginMode = loginMode;
        try {
            final ApplicationInfo appInfo = _context.getPackageManager().getApplicationInfo(_context.getPackageName(), PackageManager.GET_META_DATA);
            _clientId = (String) appInfo.metaData.get("googleClientId");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (_clientId == null) {
            onLoginFailed("Missing server client id. Check manifest implementation");
            return;
        }
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestServerAuthCode(_clientId)
                .requestEmail()
                .build();
        _googleClient = GoogleSignIn.getClient(_context, gso);

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(_context);
        if (account != null) {
            // This option should not happen theoretically because we logout out explicitly.
            onLoginSuccess(loginParams, getProviderSessions(account.getServerAuthCode(), -1L, null), loginMode);
            finish(null);
            return;
        }

        HostActivity.present(_context, new HostActivity.HostActivityLifecycleCallbacks() {

            @Override
            public void onCreate(AppCompatActivity activity, @Nullable Bundle savedInstanceState) {
                Intent signInIntent = _googleClient.getSignInIntent();
                activity.startActivityForResult(signInIntent, RC_SIGN_IN);
            }

            @Override
            public void onActivityResult(AppCompatActivity activity, int requestCode, int resultCode, @Nullable Intent data) {
                if (requestCode == RC_SIGN_IN) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                    handleSignInResult(loginParams, activity, task);
                }
            }
        });
    }

    private void handleSignInResult(final Map<String, Object> loginParams, AppCompatActivity activity, Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account == null) {
                onLoginFailed("Account unavailable");
            } else {
                /* Fetch server auth code */
                final String authCode = account.getServerAuthCode();
                if (authCode == null) {
                    onLoginFailed("Id token no available");
                } else {
                    onLoginSuccess(loginParams, getProviderSessions(authCode, -1L, null), _loginMode);
                }
            }
            finish(activity);
        } catch (ApiException e) {
            final int exceptionStatusCode = e.getStatusCode();
            switch (exceptionStatusCode) {
                case GoogleSignInStatusCodes.SIGN_IN_CANCELLED:
                    onCanceled();
                    break;
                case GoogleSignInStatusCodes.SIGN_IN_FAILED:
                default:
                    onLoginFailed(GoogleSignInStatusCodes.getStatusCodeString(exceptionStatusCode));
                    break;
            }
            finish(activity);
        }
    }

    @Override
    public void logout() {
        super.logout();
        if (_googleClient == null) {
            if (_clientId == null) {
                GigyaLogger.error("GoogleLoginProvider", "provider client id unavailable for logout");
                return;
            }
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestServerAuthCode(_clientId)
                    .requestEmail()
                    .build();
            _googleClient = GoogleSignIn.getClient(_context, gso);
        }
        _googleClient.signOut();
    }

    @Override
    public String getProviderSessions(String tokenOrCode, long expiration, String uid) {
        // code is relevant.
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
    public boolean supportsTokenTracking() {
        return false;
    }

    @Override
    public void trackTokenChange() {
        // Stub.
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
