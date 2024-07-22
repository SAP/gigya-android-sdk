package com.gigya.android.sample.providers;

import android.content.Context;
import android.os.Bundle;
import android.os.CancellationSignal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.NoCredentialException;

import com.gigya.android.sample.R;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.providers.external.IProviderWrapper;
import com.gigya.android.sdk.providers.external.IProviderWrapperCallback;
import com.gigya.android.sdk.providers.external.ProviderWrapper;
import com.gigya.android.sdk.ui.HostActivity;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Google sign in wrapper class.
 * Add the following class to your application ../gigya.providers package.
 * Make sure to define the application id in your strings resources file.
 */
public class GoogleProviderWrapper extends ProviderWrapper implements IProviderWrapper {

    private static final int RC_SIGN_IN = 0;

    private final CredentialManager _credentialsManager;

    private final Executor _executor;

    final Context context;

    public GoogleProviderWrapper(Context context) {
        super(context, R.string.google_client_id);
        this.context = context;
        _credentialsManager = CredentialManager.create(context);
        _executor = ContextCompat.getMainExecutor(context);
    }

    @Override
    public void login(Context context, final Map<String, Object> params, final IProviderWrapperCallback callback) {
        if (pId == null) {
            callback.onFailed("Missing server client id. Check manifest implementation");
            return;
        }
        // Not using cached account. Server auth code can be used only once.
        authenticate(params, callback, true);
    }

    private void handleSignIn(GetCredentialResponse getCredentialResponse, final Map<String, Object> params, final IProviderWrapperCallback callback) {
        Credential credential = getCredentialResponse.getCredential();
        if (credential.getType().equals(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
            GoogleIdTokenCredential googleIdTokenCredential =
                    GoogleIdTokenCredential.createFrom(credential.getData());
            params.put("idToken", googleIdTokenCredential.getIdToken());
            callback.onLogin(params);
        } else {
            GigyaLogger.error("GoogleProviderWrapper", "Unexpected type of credential");
            // ERROR.
            callback.onFailed("Unexpected type of credential");
        }
    }

    private void authenticate(final Map<String, Object> params,
                              final IProviderWrapperCallback callback,
                              boolean setFilterByAuthorizedAccounts) {
        HostActivity.present(context, new HostActivity.HostActivityLifecycleCallbacks() {

            @Override
            public void onCreate(AppCompatActivity activity, @Nullable Bundle savedInstanceState) {

                GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(setFilterByAuthorizedAccounts)
                        .setAutoSelectEnabled(true)
                        .setServerClientId(pId)
                        .build();

                GetCredentialRequest request = new GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build();

                credentialsSignIn(activity, params, request, setFilterByAuthorizedAccounts, callback);
            }
        });
    }

    private void credentialsSignIn(AppCompatActivity activity,
                                   final Map<String, Object> params,
                                   GetCredentialRequest request,
                                   boolean setFilterByAuthorizedAccounts,
                                   final IProviderWrapperCallback callback) {
        _credentialsManager.getCredentialAsync(activity, request,
                new CancellationSignal(),
                _executor,
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse getCredentialResponse) {
                        handleSignIn(getCredentialResponse, params, callback);
                        activity.finish();
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        GigyaLogger.debug("GoogleProviderWrapper", "login exception: " + e);
                        if (e instanceof NoCredentialException && setFilterByAuthorizedAccounts) {
                            authenticate(params, callback, false);
                        } else {
                            callback.onFailed(e.getLocalizedMessage());
                            activity.finish();
                        }
                    }
                }
        );
    }

    @Override
    public void logout() {
        ClearCredentialStateRequest request = new ClearCredentialStateRequest();
        _credentialsManager.clearCredentialStateAsync(request, new CancellationSignal(),
                _executor,
                new CredentialManagerCallback<Void, ClearCredentialException>() {
                    @Override
                    public void onResult(Void unused) {
                        GigyaLogger.debug("GoogleProviderWrapper", "logout success");
                    }

                    @Override
                    public void onError(@NonNull ClearCredentialException e) {
                        GigyaLogger.debug("GoogleProviderWrapper", "logout exception: " + e);
                    }
                }
        );
    }

}
