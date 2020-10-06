package com.gigya.android.sdk.providers.provider;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginBehavior;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.gigya.android.sdk.api.IBusinessApiService;
import com.gigya.android.sdk.persistence.IPersistenceService;
import com.gigya.android.sdk.ui.HostActivity;
import com.gigya.android.sdk.utils.FileUtils;
import com.gigya.android.sdk.utils.ObjectUtils;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.gigya.android.sdk.GigyaDefinitions.Providers.FACEBOOK;

public class FacebookProvider extends Provider {

    public FacebookProvider(Context context,
                            IPersistenceService persistenceService,
                            IBusinessApiService businessApiService,
                            ProviderCallback providerCallback) {
        super(context, persistenceService, businessApiService, providerCallback);
    }

    private static final String[] DEFAULT_READ_PERMISSIONS = {"email"};

    private final CallbackManager _callbackManager = CallbackManager.Factory.create();
    private AccessTokenTracker _tokenTracker;

    @Override
    public String getName() {
        return FACEBOOK;
    }

    public static boolean isAvailable(FileUtils fileUtils) {
        try {
            String fbAppId = fileUtils.stringFromMetaData("com.facebook.sdk.ApplicationId");
            Class.forName("com.facebook.login.LoginManager");
            return fbAppId != null;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void login(final Map<String, Object> loginParams, final String loginMode) {
        if (_connecting) {
            return;
        }
        _connecting = true;
        _loginMode = loginMode;
        // Get login permissions.
        final List<String> readPermissions = getReadPermissions(loginParams);

        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        // Check login state.
        boolean isLoggedIn = accessToken != null && !accessToken.isExpired() && permissionsGranted(readPermissions);
        if (isLoggedIn) {
            onLoginSuccess(loginParams, getProviderSessions(accessToken.getToken(), accessToken.getExpires().getTime() / 1000, null), loginMode);
            return;
        }
        // Start new login flow.
        HostActivity.present(_context, new HostActivity.HostActivityLifecycleCallbacks() {
            @Override
            public void onCreate(final AppCompatActivity activity, @Nullable Bundle savedInstanceState) {
                final LoginManager loginManager = LoginManager.getInstance();
                // Set login behaviour.
                LoginBehavior loginBehaviour = LoginBehavior.NATIVE_WITH_FALLBACK;
                if (loginParams != null && loginParams.containsKey(LOGIN_BEHAVIOUR)) {
                    Object behaviour = loginParams.get(LOGIN_BEHAVIOUR);
                    if (behaviour instanceof LoginBehavior) {
                        loginBehaviour = (LoginBehavior) behaviour;
                    }
                }
                loginManager.setLoginBehavior(loginBehaviour);
                loginManager.registerCallback(_callbackManager, new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        loginManager.unregisterCallback(_callbackManager);
                        AccessToken accessToken = AccessToken.getCurrentAccessToken();
                        onLoginSuccess(loginParams, getProviderSessions(accessToken.getToken(), accessToken.getExpires().getTime() / 1000, null), loginMode);
                        activity.finish();
                    }

                    @Override
                    public void onCancel() {
                        loginManager.unregisterCallback(_callbackManager);
                        onCanceled();
                        activity.finish();
                    }

                    @Override
                    public void onError(FacebookException error) {
                        loginManager.unregisterCallback(_callbackManager);
                        onLoginFailed(error.getLocalizedMessage());
                        activity.finish();
                    }
                });
                // Request login.
                loginManager.logInWithReadPermissions(activity, readPermissions);
            }

            @Override
            public void onActivityResult(AppCompatActivity activity, int requestCode, int resultCode, @Nullable Intent data) {
                _callbackManager.onActivityResult(requestCode, resultCode, data);
            }

        });
    }

    @Override
    public void logout() {
        super.logout();
        if (_tokenTracker != null) {
            _tokenTracker.stopTracking();
        }
        if (AccessToken.getCurrentAccessToken() != null) {
            LoginManager.getInstance().logOut();
        }
    }

    @Override
    public String getProviderSessions(String tokenOrCode, long expiration, String uid) {
        // token & expiration is relevant
        try {
            return new JSONObject()
                    .put("facebook", new JSONObject()
                            .put("authToken", tokenOrCode).put("tokenExpiration", expiration)).toString();
        } catch (Exception ex) {
            _connecting = false;
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean supportsTokenTracking() {
        return true;
    }

    @Override
    public void trackTokenChange() {
        _tokenTracker = new AccessTokenTracker() {

            @Override
            protected void onCurrentAccessTokenChanged(AccessToken oldAccessToken, AccessToken currentAccessToken) {
                // Send api request.
                final String newAuthToken = currentAccessToken.getToken();
                final long expiresInSeconds = currentAccessToken.getExpires().getTime() / 1000;
                _tokenTrackingListener.onTokenChange(getName(), getProviderSessions(newAuthToken, expiresInSeconds, null), null);
            }
        };
    }

    //region SPECIFIC PROVIDER LOGIC

    public static final String LOGIN_BEHAVIOUR = "facebookLoginBehavior";
    public static final String READ_PERMISSIONS = "facebookReadPermissions";
    public static final String PUBLISH_PERMISSIONS = "facebookPublishPermissions";

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

    //endregion
}
