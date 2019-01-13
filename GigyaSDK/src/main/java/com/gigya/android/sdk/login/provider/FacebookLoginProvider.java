package com.gigya.android.sdk.login.provider;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.LoggingBehavior;
import com.facebook.login.DefaultAudience;
import com.facebook.login.LoginBehavior;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.gigya.android.sdk.BuildConfig;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.login.LoginProvider;
import com.gigya.android.sdk.ui.HostActivity;
import com.gigya.android.sdk.utils.ObjectUtils;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FacebookLoginProvider extends LoginProvider {

    public static final String NAME = "facebook";

    public static final String LOGIN_BEHAVIOUR = "facebookLoginBehavior";
    public static final String READ_PERMISSIONS = "facebookReadPermissions";
    public static final String PUBLISH_PERMISSIONS = "facebookPublishPermissions";

    private static final String[] DEFAULT_READ_PERMISSIONS = {"email"};

    private final CallbackManager _callbackManager = CallbackManager.Factory.create();

    private AccessTokenTracker _tokenTracker;

    public FacebookLoginProvider(LoginProviderCallbacks loginCallbacks, LoginProviderTrackerCallback trackerCallback) {
        super(loginCallbacks, trackerCallback);
        if (BuildConfig.DEBUG) {
            FacebookSdk.setIsDebugEnabled(true);
            FacebookSdk.addLoggingBehavior(LoggingBehavior.INCLUDE_ACCESS_TOKENS);
        }
    }

    @Override
    public void trackTokenChanges(@Nullable final SessionManager sessionManager) {
        if (sessionManager == null) {
            return;
        }
        /* Tracking access token changes. */
        _tokenTracker = new AccessTokenTracker() {

            @Override
            protected void onCurrentAccessTokenChanged(AccessToken oldAccessToken, AccessToken currentAccessToken) {
                if (!sessionManager.isValidSession()) {
                    return;
                }
                // Send api request.
                if (loginCallbacks != null) {
                    final String newAuthToken = currentAccessToken.getToken();
                    final long expiresInSeconds = currentAccessToken.getExpires().getTime() / 1000;
                    trackerCallback.onProviderTrackingTokenChanges(NAME, getProviderSessions(newAuthToken, expiresInSeconds, null), null);
                }
            }
        };
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
    public void logout(Context context) {
        if (_tokenTracker != null) {
            _tokenTracker.stopTracking();
        }
        if (AccessToken.getCurrentAccessToken() != null) {
            LoginManager.getInstance().logOut();
        }
    }

    @Override
    public String getProviderSessions(String tokenOrCode, long expiration, String uid) {
        /* token & expiration is relevant */
        try {
            return new JSONObject()
                    .put("facebook", new JSONObject()
                            .put("authToken", tokenOrCode).put("tokenExpiration", expiration)).toString();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public void login(final Context context, final Map<String, Object> loginParams) {
        /* Get login permissions. */
        final List<String> readPermissions = getReadPermissions(loginParams);

        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        boolean isLoggedIn = accessToken != null && !accessToken.isExpired() && permissionsGranted(readPermissions);
        if (isLoggedIn) {
            return;
        }
        HostActivity.present(context, new HostActivity.HostActivityLifecycleCallbacks() {
            @Override
            public void onCreate(final AppCompatActivity activity, @Nullable Bundle savedInstanceState) {
                final LoginManager loginManager = LoginManager.getInstance();
                /* Set login behaviour. */
                LoginBehavior loginBehaviour = LoginBehavior.NATIVE_WITH_FALLBACK;
                if (loginParams != null && loginParams.containsKey(LOGIN_BEHAVIOUR)) {
                    loginBehaviour = (LoginBehavior) loginParams.get(LOGIN_BEHAVIOUR);
                }

                loginManager.setLoginBehavior(loginBehaviour);
                loginManager.registerCallback(_callbackManager, new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        loginManager.unregisterCallback(_callbackManager);
                        AccessToken accessToken = AccessToken.getCurrentAccessToken();
                        loginCallbacks.onProviderLoginSuccess(NAME, getProviderSessions(accessToken.getToken(), accessToken.getExpires().getTime() / 1000, null));

                        activity.finish();
                    }

                    @Override
                    public void onCancel() {
                        loginManager.unregisterCallback(_callbackManager);
                        loginCallbacks.onProviderLoginFailed("facebook", "Operation cancelled");

                        activity.finish();
                    }

                    @Override
                    public void onError(FacebookException error) {
                        loginManager.unregisterCallback(_callbackManager);
                        loginCallbacks.onProviderLoginFailed("facebook", error.getLocalizedMessage());

                        activity.finish();
                    }
                });

                /* Request login. */
                loginManager.logInWithReadPermissions(activity, readPermissions);
            }

            @Override
            public void onActivityResult(AppCompatActivity activity, int requestCode, int resultCode, @Nullable Intent data) {
                _callbackManager.onActivityResult(requestCode, resultCode, data);
            }
        });
    }

    /**
     * Request an updated session with additional permissions. Publish or Read.
     */
    public void requestPermissionsUpdate(Context context, final String permissionsType, List<String> permissionList,
                                         final LoginPermissionCallbacks permissionCallbacks) {
        _tokenTracker.stopTracking();
        if (AccessToken.getCurrentAccessToken() == null) {
            permissionCallbacks.noAccess();
            return;
        }
        if (permissionsGranted(permissionList)) {
            permissionCallbacks.granted();
            return;
        }

        final List<String> requestPermissions = ObjectUtils.mergeRemovingDuplicates(
                permissionsType.equals(READ_PERMISSIONS) ? new ArrayList<>(AccessToken.getCurrentAccessToken().getPermissions()) : new ArrayList<String>(),
                permissionList);

        HostActivity.present(context, new HostActivity.HostActivityLifecycleCallbacks() {

            @Override
            public void onCreate(final AppCompatActivity activity, @Nullable Bundle savedInstanceState) {

                final LoginManager loginManager = LoginManager.getInstance();
                loginManager.setDefaultAudience(DefaultAudience.FRIENDS); // default.
                loginManager.registerCallback(_callbackManager, new FacebookCallback<LoginResult>() {

                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        loginManager.unregisterCallback(_callbackManager);
                        AccessToken accessToken = AccessToken.getCurrentAccessToken();

                        /* Check if all permissions were granted. */
                        if (accessToken.getDeclinedPermissions().isEmpty()) {
                            final long expiresInSeconds = accessToken.getExpires().getTime() / 1000;
                            trackerCallback.onProviderTrackingTokenChanges(NAME, getProviderSessions(accessToken.getToken(), expiresInSeconds, null), permissionCallbacks);
                            _tokenTracker.startTracking();
                        } else {
                            permissionCallbacks.declined(new ArrayList<>(accessToken.getDeclinedPermissions()));
                        }

                        activity.finish();
                    }

                    @Override
                    public void onCancel() {
                        loginManager.unregisterCallback(_callbackManager);
                        permissionCallbacks.cancelled();

                        activity.finish();
                    }

                    @Override
                    public void onError(FacebookException error) {
                        loginManager.unregisterCallback(_callbackManager);
                        permissionCallbacks.failed(error.getLocalizedMessage());

                        activity.finish();
                    }
                });

                if (permissionsType.equals(READ_PERMISSIONS)) {
                    loginManager.logInWithReadPermissions(activity, requestPermissions);
                } else if (permissionsType.equals(PUBLISH_PERMISSIONS)) {
                    loginManager.logInWithPublishPermissions(activity, requestPermissions);
                }
            }

            @Override
            public void onActivityResult(AppCompatActivity activity, int requestCode, int resultCode, @Nullable Intent data) {
                _callbackManager.onActivityResult(requestCode, resultCode, data);
            }
        });
    }

    private List<String> getReadPermissions(Map<String, Object> loginParams) {
        List<String> readPermissions = Arrays.asList(DEFAULT_READ_PERMISSIONS);
        if (loginParams != null && loginParams.containsKey(READ_PERMISSIONS)) {
            String userDefinedReadPermissions = (String) loginParams.get(READ_PERMISSIONS);
            if (userDefinedReadPermissions != null) {
                final String[] split = userDefinedReadPermissions.split(",");
                readPermissions = ObjectUtils.mergeRemovingDuplicates(readPermissions, Arrays.asList(split));
            }
        }
        return readPermissions;
    }

    private boolean permissionsGranted(List<String> permissions) {
        AccessToken fbAccessToken = AccessToken.getCurrentAccessToken();
        Set<String> grantedPermissions = fbAccessToken.getPermissions();
        for (String permission : permissions) {
            if (!grantedPermissions.contains(permission))
                return false;
        }
        return true;
    }
}
