package com.gigya.android.sdk.login.provider;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginBehavior;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.gigya.android.sdk.login.LoginProvider;
import com.gigya.android.sdk.ui.HostActivity;
import com.gigya.android.sdk.utils.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class FacebookLoginProvider extends LoginProvider {

    public static final String LOGIN_BEHAVIOUR = "facebookLoginBehavior";
    public static final String READ_PERMISSIONS = "facebookReadPermissions";

    private static final String[] DEFAULT_READ_PERMISSIONS = {"email"};

    private final CallbackManager _callbackManager = CallbackManager.Factory.create();

    public FacebookLoginProvider(LoginProviderCallbacks loginCallbacks) {
        super(loginCallbacks);
    }

    public static boolean isAvailable(Context context) {
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            String fbAppId = (String) appInfo.metaData.get("com.facebook.sdk.ApplicationId");
            Class.forName("com.facebook.login.LoginManager");
            return fbAppId != null;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void login(final Context context, final Map<String, Object> loginParams) {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        boolean isLoggedIn = accessToken != null && !accessToken.isExpired();
        if (isLoggedIn) {
            return;
        }
        HostActivity.present(context, new HostActivity.HostActivityLifecycleCallbacks() {
            @Override
            public void onCreate(final AppCompatActivity activity, @Nullable Bundle savedInstanceState) {
                LoginManager loginManager = LoginManager.getInstance();
                /* Set login behaviour. */
                LoginBehavior loginBehaviour = LoginBehavior.NATIVE_WITH_FALLBACK;
                if (loginParams != null && loginParams.containsKey(LOGIN_BEHAVIOUR)) {
                    loginBehaviour = (LoginBehavior) loginParams.get(LOGIN_BEHAVIOUR);
                }
                loginManager.setLoginBehavior(loginBehaviour);
                
                loginManager.registerCallback(_callbackManager, new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        AccessToken accessToken = AccessToken.getCurrentAccessToken();
                        loginCallbacks.onSuccess(accessToken.getToken(), accessToken.getExpires().getTime());
                        activity.finish();
                    }

                    @Override
                    public void onCancel() {
                        loginCallbacks.onError("Operation cancelled");
                        activity.finish();
                    }

                    @Override
                    public void onError(FacebookException error) {
                        loginCallbacks.onError(error.getLocalizedMessage());
                        activity.finish();
                    }
                });

                /* Set login permissions. */
                List<String> readPermissions = Arrays.asList(DEFAULT_READ_PERMISSIONS);
                if (loginParams != null && loginParams.containsKey(READ_PERMISSIONS)) {
                    String userDefinedReadPermissions = (String) loginParams.get(READ_PERMISSIONS);
                    if (userDefinedReadPermissions != null) {
                        final String[] split = userDefinedReadPermissions.split(",");
                        readPermissions = ObjectUtils.mergeRemovingDuplicates(readPermissions, Arrays.asList(split));
                    }
                }
                /* Request login. */
                loginManager.logInWithReadPermissions(activity, readPermissions);
            }

            @Override
            public void onStart(AppCompatActivity activity) {
                // Stub.
            }

            @Override
            public void onResume(AppCompatActivity activity) {
                // Stub.
            }

            @Override
            public void onActivityResult(AppCompatActivity activity, int requestCode, int resultCode, @Nullable Intent data) {
                _callbackManager.onActivityResult(requestCode, resultCode, data);
            }
        });
    }

    @Override
    public void logout() {
        if (AccessToken.getCurrentAccessToken() != null) {
            LoginManager.getInstance().logOut();
        }
    }
}
