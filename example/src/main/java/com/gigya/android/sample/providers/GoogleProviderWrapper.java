package com.gigya.android.sample.providers;

import android.content.Context;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Log;

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

import com.gigya.android.sample.R;
import com.gigya.android.sdk.providers.external.IProviderWrapper;
import com.gigya.android.sdk.providers.external.IProviderWrapperCallback;
import com.gigya.android.sdk.providers.external.ProviderWrapper;
import com.gigya.android.sdk.ui.HostActivity;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
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
        authenticate(params, callback);
    }

    private void handleSignIn(GetCredentialResponse getCredentialResponse, final Map<String, Object> params, final IProviderWrapperCallback callback) {
        Credential credential = getCredentialResponse.getCredential();
        if (credential.getType().equals(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
            GoogleIdTokenCredential googleIdTokenCredential =
                    GoogleIdTokenCredential.createFrom(credential.getData());
            params.put("idToken", googleIdTokenCredential.getIdToken());
            callback.onLogin(params);
        } else {
            Log.e("GoogleProviderWrapper", "Unexpected type of credential");
            // ERROR.
            callback.onFailed("Unexpected type of credential");
        }
    }

    private void authenticate(final Map<String, Object> params, final IProviderWrapperCallback callback) {
        HostActivity.present(context, new HostActivity.HostActivityLifecycleCallbacks() {

            @Override
            public void onCreate(AppCompatActivity activity, @Nullable Bundle savedInstanceState) {
                GetSignInWithGoogleOption ss = new GetSignInWithGoogleOption.Builder(pId)
                        .build();

                GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(pId)
                        .build();

                GetCredentialRequest request = new GetCredentialRequest.Builder()
                        .addCredentialOption(ss)
                        .build();

                credentialsSignIn(activity, params, request, callback);
            }
        });
    }

    private void credentialsSignIn(AppCompatActivity activity, final Map<String, Object> params, GetCredentialRequest request, final IProviderWrapperCallback callback) {
        _credentialsManager.getCredentialAsync(activity, request,
                new CancellationSignal(),
                _executor,
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse getCredentialResponse) {
                        handleSignIn(getCredentialResponse, params, callback);
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        Log.d("GoogleProviderWrapper", "login exception: " + e);
                        callback.onFailed(e.getLocalizedMessage());
                    }
                }
        );
    }

    @Override
    public void logout() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ClearCredentialStateRequest request = new ClearCredentialStateRequest();
            _credentialsManager.clearCredentialStateAsync(request, new CancellationSignal(),
                    _executor,
                    new CredentialManagerCallback<Void, ClearCredentialException>() {
                        @Override
                        public void onResult(Void unused) {
                            Log.d("GoogleProviderWrapper", "logout success");
                        }

                        @Override
                        public void onError(@NonNull ClearCredentialException e) {
                            Log.d("GoogleProviderWrapper", "logout exception: " + e);
                        }
                    }
            );
        }
    }
}
