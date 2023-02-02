package com.gigya.android.sample.gigya.providers;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginBehavior;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.gigya.android.sample.R;
import com.gigya.android.sdk.providers.external.IProviderWrapper;
import com.gigya.android.sdk.providers.external.IProviderWrapperCallback;
import com.gigya.android.sdk.providers.external.ProviderWrapper;
import com.gigya.android.sdk.ui.HostActivity;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FacebookProviderWrapper extends ProviderWrapper implements IProviderWrapper {

    private static final String[] DEFAULT_READ_PERMISSIONS = {"email"};

    private final CallbackManager callbackManager = CallbackManager.Factory.create();

    public FacebookProviderWrapper(Context context) {
        super(context, R.string.facebook_app_id);
    }

    @Override
    public void login(Context context, final Map<String, Object> params, final IProviderWrapperCallback wrapperCallback) {

        final List<String> readPermissions = Arrays.asList(DEFAULT_READ_PERMISSIONS);

        // Check login state.
        final AccessToken accessToken = AccessToken.getCurrentAccessToken();
        boolean isLoggedIn = accessToken != null && !accessToken.isExpired() && permissionsGranted(readPermissions);
        if (isLoggedIn) {
            // Notify login success.
            final Map<String, Object> map = new HashMap<>();
            map.put("token", accessToken.getToken());
            map.put("expiration", accessToken.getExpires().getTime() / 1000);
            wrapperCallback.onLogin(map);
            return;
        }

        // Start new login flow.
        HostActivity.present(context, new HostActivity.HostActivityLifecycleCallbacks() {
            @Override
            public void onCreate(final AppCompatActivity activity, @Nullable Bundle savedInstanceState) {
                final LoginManager loginManager = LoginManager.getInstance();
                // Set login behaviour. Override if provided in login parameters.
                LoginBehavior loginBehaviour = LoginBehavior.NATIVE_WITH_FALLBACK;
                if (params != null && params.containsKey("facebookLoginBehavior")) {
                    Object behaviour = params.get("facebookLoginBehavior");
                    if (behaviour instanceof LoginBehavior) {
                        loginBehaviour = (LoginBehavior) behaviour;
                    }
                }
                loginManager.setLoginBehavior(loginBehaviour);
                loginManager.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        loginManager.unregisterCallback(callbackManager);
                        AccessToken accessToken = AccessToken.getCurrentAccessToken();

                        final Map<String, Object> map = new HashMap<>();
                        map.put("token", accessToken.getToken());
                        map.put("expiration", accessToken.getExpires().getTime() / 1000);
                        // Notify login success. Provider a map of required values.
                        wrapperCallback.onLogin(map);
                        activity.finish();
                    }

                    @Override
                    public void onCancel() {
                        loginManager.unregisterCallback(callbackManager);

                        // Notify login canceled.
                        wrapperCallback.onCanceled();

                        activity.finish();
                    }

                    @Override
                    public void onError(FacebookException error) {
                        loginManager.unregisterCallback(callbackManager);

                        // Notify login failed.
                        wrapperCallback.onFailed(error.getLocalizedMessage());

                        activity.finish();
                    }
                });
                // Request login.
                loginManager.logInWithReadPermissions(activity, readPermissions);
            }

            @Override
            public void onActivityResult(AppCompatActivity activity, int requestCode, int resultCode, @Nullable Intent data) {
                callbackManager.onActivityResult(requestCode, resultCode, data);
            }
        });
    }

    @Override
    public void logout() {
        if (AccessToken.getCurrentAccessToken() != null) {
            LoginManager.getInstance().logOut();
        }
    }

    private boolean permissionsGranted(List<String> permissions) {
        AccessToken token = AccessToken.getCurrentAccessToken();
        Set<String> grantedPermissions = token.getPermissions();
        for (String permission : permissions) {
            if (!grantedPermissions.contains(permission))
                return false;
        }
        return true;
    }

}
