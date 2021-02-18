package com.gigya.android.sdk.auth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.auth.api.AuthBusinessApiService;
import com.gigya.android.sdk.auth.api.IAuthBusinessApiService;
import com.gigya.android.sdk.auth.push.AuthRemoteMessageHandler;
import com.gigya.android.sdk.auth.resolvers.IGigyaOtpResult;
import com.gigya.android.sdk.auth.resolvers.OTPRegistrationResolver;
import com.gigya.android.sdk.auth.ui.PushAuthActivity;
import com.gigya.android.sdk.containers.IoCContainer;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.persistence.IPersistenceService;
import com.gigya.android.sdk.push.GigyaFirebaseMessagingService;
import com.gigya.android.sdk.push.IGigyaNotificationManager;
import com.gigya.android.sdk.push.IGigyaPushCustomizer;
import com.gigya.android.sdk.push.IRemoteMessageHandler;
import com.gigya.android.sdk.push.RemoteMessageLocalReceiver;

import java.util.HashMap;
import java.util.Map;

import static com.gigya.android.sdk.auth.GigyaDefinitions.AUTH_CHANNEL_ID;

public class GigyaAuth {

    private static final String VERSION = "2.0.0";

    private static final String LOG_TAG = "GigyaAuth";

    @SuppressLint("StaticFieldLeak")
    private static GigyaAuth _sharedInstance;

    public static synchronized GigyaAuth getInstance() {
        if (_sharedInstance == null) {
            IoCContainer container = Gigya.getContainer();

            container.bind(GigyaAuth.class, GigyaAuth.class, true);
            container.bind(IAuthBusinessApiService.class, AuthBusinessApiService.class, true);
            container.bind(IRemoteMessageHandler.class, AuthRemoteMessageHandler.class, true);

            try {
                _sharedInstance = container.get(GigyaAuth.class);
                GigyaLogger.debug(LOG_TAG, "Instantiation version: " + VERSION);
            } catch (Exception e) {
                GigyaLogger.error(LOG_TAG, "Error creating Gigya Auth library (did you forget to Gigya.setApplication?");
                e.printStackTrace();
                throw new RuntimeException("Error instantiating Gigya Auth library (did you forget to Gigya.setApplication?");
            }
        }
        return _sharedInstance;
    }

    private final Context _context;
    private final IGigyaNotificationManager _gigyaNotificationManager;
    private final IAuthBusinessApiService _authBusinessApiService;
    private final IRemoteMessageHandler _remoteMessageHandler;
    private final IPersistenceService _persistenceService;

    protected GigyaAuth(Context context,
                        IAuthBusinessApiService authBusinessApiService,
                        IRemoteMessageHandler remoteMessageHandler,
                        IPersistenceService persistenceService,
                        IGigyaNotificationManager gigyaNotificationManager) {
        _context = context;
        _gigyaNotificationManager = gigyaNotificationManager;
        _authBusinessApiService = authBusinessApiService;
        _persistenceService = persistenceService;
        _remoteMessageHandler = remoteMessageHandler;

        // Set default customization.
        _remoteMessageHandler.setPushCustomizer(new IGigyaPushCustomizer() {
            @Override
            public int getSmallIcon() {
                return android.R.drawable.ic_dialog_info;
            }

            @Override
            public int getApproveActionIcon() {
                return 0;
            }

            @Override
            public int getDenyActionIcon() {
                return 0;
            }

            @Override
            public Class getCustomActionActivity() {
                return PushAuthActivity.class;
            }
        });

        /*
        Register remote message receiver to handle TFA push messages.
         */
        LocalBroadcastManager.getInstance(context).registerReceiver(
                new RemoteMessageLocalReceiver(remoteMessageHandler),
                new IntentFilter(com.gigya.android.sdk.GigyaDefinitions.Broadcasts.INTENT_ACTION_REMOTE_MESSAGE)
        );
    }

    /*
    Device info JSON representation.
    */
    private String _deviceInfo;

    /**
     * Generate required device information asynchronously.
     */
    private void generateDeviceInfo(@NonNull final Runnable completionHandler, @NonNull final Runnable errorHandler) {
        final String currentPushToken = _persistenceService.getPushToken();
        if (currentPushToken == null) {
            GigyaFirebaseMessagingService.requestTokenAsync(new GigyaFirebaseMessagingService.IFcmTokenResponse() {
                @Override
                public void onAvailable(@Nullable String token) {
                    if (token == null) {
                        // All else fails.
                        errorHandler.run();
                        return;
                    }

                    // Updating push token in prefs.
                    _persistenceService.setPushToken(token);

                    _deviceInfo = _gigyaNotificationManager.getDeviceInfo(token);

                    GigyaLogger.debug(LOG_TAG, "generateDeviceInfo: " + _deviceInfo);

                    completionHandler.run();
                }
            });
            return;
        }

        _deviceInfo = _gigyaNotificationManager.getDeviceInfo(currentPushToken);
        completionHandler.run();
    }

    /**
     * Optional setter for TFA push notification customization.
     *
     * @param customizer custom IGigyaPushCustomizer interface for available notification customization options.
     */
    public void setPushCustomizer(IGigyaPushCustomizer customizer) {
        _remoteMessageHandler.setPushCustomizer(customizer);
    }

    /**
     * Register device to receive push authentication messages.
     */
    public void registerForAuthPush(final GigyaCallback<GigyaApiResponse> gigyaCallback) {
        if (_deviceInfo == null) {
            generateDeviceInfo(new Runnable() {
                @Override
                public void run() {
                    _authBusinessApiService.registerDevice(_deviceInfo, gigyaCallback);
                }
            }, new Runnable() { /* Error handler */
                @Override
                public void run() {
                    GigyaLogger.error(LOG_TAG, "registerForAuthPush: Failed to register auth push due to missing device info.");

                    gigyaCallback.onError(GigyaError.unauthorizedUser());
                }
            });
        } else {
            _authBusinessApiService.registerDevice(_deviceInfo, gigyaCallback);
        }
    }

    /**
     * Verify authentication push message.
     *
     * @param vToken String verification token.
     */
    public void verifyAuthPush(@NonNull final String vToken) {
        _authBusinessApiService.verifyPush(vToken, new GigyaCallback<GigyaApiResponse>() {
            @Override
            public void onSuccess(GigyaApiResponse obj) {
                GigyaLogger.debug(LOG_TAG, "verifyAuthPush: Successfully verified push");

                _gigyaNotificationManager.notifyWith(
                        _context,
                        _context.getString(R.string.gig_auth_login_approval_success_title),
                        _context.getString(R.string.gig_auth_login_approval_success_body),
                        AUTH_CHANNEL_ID);
            }

            @Override
            public void onError(GigyaError error) {
                GigyaLogger.error(LOG_TAG, "verifyAuthPush: Failed to verify push");

                // Notify error.
                _gigyaNotificationManager.notifyWith(
                        _context,
                        _context.getString(R.string.gig_auth_login_approval_success_title),
                        _context.getString(R.string.gig_auth_login_approval_error_body),
                        AUTH_CHANNEL_ID);
            }
        });
    }

    /**
     * Check if push notifications are enabled for application.
     * <p>
     * For Android >= Oreo check if push TFA notification channel is enabled.
     *
     * @return True if enabled.
     */
    private boolean pushAuthEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) _context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = manager.getNotificationChannel(AUTH_CHANNEL_ID);
            if (channel != null) {
                return channel.getImportance() != NotificationManager.IMPORTANCE_NONE;
            }
        }
        return NotificationManagerCompat.from(_context).areNotificationsEnabled();
    }

    /**
     * Check if device is registered for push TFA and notifications permission is available.
     * If not. Will display a information dialog allowing the user to open the notifications application settings in order
     * to enable them.
     * Note: It is recommended to call this method when the activity context is attached.
     *
     * @param activity Current activity. Activity context must be provided.
     */
    public void registerForPushNotifications(final Activity activity) {
        if (activity.isFinishing()) {
            return;
        }
        if (!pushAuthEnabled()) {

            GigyaLogger.debug(LOG_TAG, "registerForPushNotifications: Push permission is required but not enabled. notify client");

            // Show dialog informing the user that he needs to enable push notifications.
            AlertDialog alert = new AlertDialog.Builder(activity)
                    .setTitle(R.string.gig_auth_push_notifications_alert_title)
                    .setMessage(R.string.gig_auth_push_notifications_alert_message)
                    .setCancelable(false)
                    .setPositiveButton(R.string.gig_auth_approve, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            GigyaLogger.debug(LOG_TAG, "approve clicked");

                            Intent intent = new Intent();
                            intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
                            //for Android 5-7
                            intent.putExtra("app_package", activity.getPackageName());
                            intent.putExtra("app_uid", activity.getApplicationInfo().uid);
                            // for Android 8 and above
                            intent.putExtra("android.provider.extra.APP_PACKAGE", activity.getPackageName());
                            activity.startActivity(intent);
                            dialog.dismiss();
                        }
                    }).setNegativeButton(R.string.gig_auth_no_thanks, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            GigyaLogger.debug(LOG_TAG, "registerForPushNotifications: deny clicked.");
                            dialog.dismiss();
                        }
                    }).create();
            alert.show();
        }
    }

    //region OTP

    /**
     * Authentication OTP specific interface.
     */
    public final IOTP otp = new IOTP() {

        /**
         * This method is used to trigger a Phone Number OTP Login flow.
         * Returns a vToken, and sends an SMS message containing the authentication code to the user.
         *
         * @param phoneNumber User's phone number.
         */
        @Override
        public <A extends GigyaAccount> void phoneLogin(@NonNull String phoneNumber, @NonNull GigyaOTPCallback<A> gigyaCallback) {
            final Map<String, Object> params = new HashMap<>();
            _authBusinessApiService.otpPhoneLogin(phoneNumber, params, gigyaCallback);
        }

        /**
         * This method is used to trigger a Phone Number OTP Login flow.
         * Returns a vToken, and sends an SMS message containing the authentication code to the user.
         *
         * @param phoneNumber User's phone number.
         * @param params      Parameter map.
         */
        @Override
        public <A extends GigyaAccount> void phoneLogin(@NonNull String phoneNumber, @NonNull Map<String, Object> params, @NonNull GigyaOTPCallback<A> gigyaCallback) {
            _authBusinessApiService.otpPhoneLogin(phoneNumber, params, gigyaCallback);
        }

        /**
         * This method is used to trigger a Phone Number OTP update flow.
         * Returns a vToken, and send an SMS containing the authentication code to the user.
         *
         * @param phoneNumber User's phone number.
         */
        @Override
        public <A extends GigyaAccount> void phoneUpdate(@NonNull String phoneNumber, @NonNull GigyaOTPCallback<A> gigyaCallback) {
            final Map<String, Object> params = new HashMap<>();
            _authBusinessApiService.otpPhoneUpdate(phoneNumber, params, gigyaCallback);
        }

        /**
         * This method is used to trigger a Phone Number OTP update flow.
         * Returns a vToken, and send an SMS containing the authentication code to the user.
         *
         * @param phoneNumber User's phone number.
         * @param params      Parameter map.
         */
        @Override
        public <A extends GigyaAccount> void phoneUpdate(@NonNull String phoneNumber, @NonNull Map<String, Object> params, @NonNull GigyaOTPCallback<A> gigyaCallback) {
            _authBusinessApiService.otpPhoneUpdate(phoneNumber, params, gigyaCallback);
        }

    };

    //endregion
}
