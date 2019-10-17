package com.gigya.android.sdk.tfa;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AlertDialog;

import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.containers.IoCContainer;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.tfa.api.ITFABusinessApiService;
import com.gigya.android.sdk.tfa.api.TFABusinessApiService;
import com.gigya.android.sdk.tfa.persistence.ITFAPersistenceService;
import com.gigya.android.sdk.tfa.persistence.TFAPersistenceService;
import com.gigya.android.sdk.tfa.push.DeviceInfoBuilder;
import com.gigya.android.sdk.tfa.push.ITFANotifier;
import com.gigya.android.sdk.tfa.push.TFANotifier;

import static com.gigya.android.sdk.tfa.GigyaDefinitions.TFA_CHANNEL_ID;

public class GigyaTFA {

    private static final String VERSION = "1.0.3";

    private static final String LOG_TAG = "GigyaTFA";

    @SuppressLint("StaticFieldLeak")
    private static GigyaTFA _sharedInstance;

    public enum PushService {
        FIREBASE
    }

    private PushService _pushService = PushService.FIREBASE;

    /*
    Device info JSON representation.
     */
    private String _deviceInfo;

    /**
     * Optional method to allow changing the push service provider.
     * Currently not supported. Thus private.
     *
     * @param pushService Selected push service.
     */
    private void setPushService(PushService pushService) {
        _pushService = pushService;
    }

    public static synchronized GigyaTFA getInstance() {
        if (_sharedInstance == null) {
            IoCContainer container = Gigya.getContainer();

            container.bind(GigyaTFA.class, GigyaTFA.class, true);
            container.bind(ITFABusinessApiService.class, TFABusinessApiService.class, true);
            container.bind(ITFAPersistenceService.class, TFAPersistenceService.class, true);
            container.bind(ITFANotifier.class, TFANotifier.class, true);

            try {
                _sharedInstance = container.get(GigyaTFA.class);
                GigyaLogger.debug(LOG_TAG, "Instantiation version: " + VERSION);
            } catch (Exception e) {
                GigyaLogger.error(LOG_TAG, "Error creating Gigya TFA library (did you forget to Gigya.setApplication?");
                e.printStackTrace();
                throw new RuntimeException("Error creating Gigya TFA library (did you forget to Gigya.setApplication?");
            }
        }
        return _sharedInstance;
    }

    private final ITFABusinessApiService _businessApiService;
    private final ITFAPersistenceService _persistenceService;
    private final ITFANotifier _tfaNotifier;
    private final Context _context;

    protected GigyaTFA(Context context,
                       ITFABusinessApiService businessApiService,
                       ITFAPersistenceService persistenceService,
                       ITFANotifier tfaNotifier) {
        _context = context;
        _businessApiService = businessApiService;
        _persistenceService = persistenceService;
        _tfaNotifier = tfaNotifier;
    }

    /*
    Will generate required device information asynchronously.
     */
    private void generateDeviceInfo(@NonNull final Runnable completionHandler, @NonNull final Runnable errorHandler) {
        final DeviceInfoBuilder builder = new DeviceInfoBuilder(_persistenceService).setPushService(_pushService);
        builder.buildAsync(new DeviceInfoBuilder.DeviceInfoCallback() {
            @Override
            public void onDeviceInfo(String deviceInfoJson) {
                _deviceInfo = deviceInfoJson;
                completionHandler.run();
            }

            @Override
            public void unavailableToken() {
                GigyaLogger.error(LOG_TAG, "Push token fetch unsuccessful");

                // Try to fetch the token from persistence.
                final String persistentToken = _persistenceService.getPushToken();
                if (persistentToken != null) {
                    _deviceInfo = builder.buildWith(persistentToken);
                    completionHandler.run();

                    return;
                }

                // All else failed.
                errorHandler.run();
            }
        });
    }

    //region INTERFACING

    /**
     * Check if push notifications are enabled for application.
     * For Android >= 0 check if push TFA notification channel is enabled.
     *
     * @return True if enabled.
     */
    public boolean pushTFAEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) _context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = manager.getNotificationChannel(TFA_CHANNEL_ID);
            if (channel != null) {
                return channel.getImportance() != NotificationManager.IMPORTANCE_NONE;
            }
        }
        return NotificationManagerCompat.from(_context).areNotificationsEnabled();
    }

    /**
     * Check if device is registered for push TFA & notifications permission is available.
     * If not. Will display a information dialog allowing the user to open the notificaitons application settings in order
     * to enable them.
     *
     * @param activity Current activity. Activity context must be provided.
     */
    public void checkNotificationsPermissionsRequired(final Activity activity) {
        final boolean deviceRegisteredForPushTFA = _persistenceService.isOptInForPushTFA();
        if (!pushTFAEnabled() && deviceRegisteredForPushTFA) {

            // Show dialog informing the user that he needs to enable push notifications.
            AlertDialog alert = new AlertDialog.Builder(activity)
                    .setTitle(R.string.tfa_push_notifications_alert_title)
                    .setMessage(R.string.tfa_push_notifications_alert_message)
                    .setCancelable(false)
                    .setPositiveButton(R.string.tfa_approve, new DialogInterface.OnClickListener() {
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
                    }).setNegativeButton(R.string.tfa_no_thanks, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            GigyaLogger.debug(LOG_TAG, "deny clicked");
                            dialog.dismiss();
                        }
                    }).create();
            alert.show();
        }
    }

    /**
     * Request to Opt-In to push Two Factor Authentication.
     * This is the first of two stages of the Opt-In process.
     *
     * @param gigyaCallback Request callback.
     */
    public void optInForPushTFA(@NonNull final GigyaCallback<GigyaApiResponse> gigyaCallback) {
        // Device info is required.
        if (_deviceInfo == null) {
            generateDeviceInfo(new Runnable() {
                @Override
                public void run() {
                    _businessApiService.optIntoPush(_deviceInfo, gigyaCallback);
                }
            }, new Runnable() {
                @Override
                public void run() {
                    gigyaCallback.onError(GigyaError.unauthorizedUser());
                }
            });
        } else {
            _businessApiService.optIntoPush(_deviceInfo, gigyaCallback);
        }
    }

    /**
     * Not implemented in version 1.0.0.
     */
    private void pushOptOut() {
        // For security reasons opt-out must be performed when logging out of the account.
    }

    /**
     * Verify Opt-In process push notifications.
     * This is the second and final stage of the Opt-In process.
     *
     * @param gigyaAssertion    Provided gigya assertion token.
     * @param verificationToken Provided verification token.
     */
    public void verifyOptInForPushTFA(@NonNull String gigyaAssertion, @NonNull String verificationToken) {
        _businessApiService.finalizePushOptIn(gigyaAssertion, verificationToken, new GigyaCallback<GigyaApiResponse>() {
            @Override
            public void onSuccess(GigyaApiResponse obj) {
                GigyaLogger.error(LOG_TAG, "Opt-In verification flow completed");

                // Update shared preferences.
                _persistenceService.updateOptInState(true);

                // Notify success.
                _tfaNotifier.notifyWith(
                        _context.getString(R.string.tfa_opt_in_approval_success_title),
                        _context.getString(R.string.tfa_opt_in_approval_success_body));
            }

            @Override
            public void onError(GigyaError error) {
                GigyaLogger.error(LOG_TAG, "Failed to complete TFA opt in verification");
            }
        });
    }

    /**
     * Approve push TFA.
     *
     * @param gigyaAssertion    Provided gigya assertion token.
     * @param verificationToken Provided verification token to identify device.
     */
    public void approveLoginForPushTFA(@NonNull String gigyaAssertion, @NonNull String verificationToken) {
        _businessApiService.verifyPush(gigyaAssertion, verificationToken, new GigyaCallback<GigyaApiResponse>() {
            @Override
            public void onSuccess(GigyaApiResponse obj) {
                GigyaLogger.error(LOG_TAG, "Successfully verified push");
                _tfaNotifier.notifyWith(
                        _context.getString(R.string.tfa_login_approval_success_title),
                        _context.getString(R.string.tfa_login_approval_success_body));
            }

            @Override
            public void onError(GigyaError error) {
                GigyaLogger.error(LOG_TAG, "Failed to verify push");
            }
        });
    }

    /*
     * Not implemented in version 1.0.0
     */
    private void denyLoginForPushTFA() {
        // Stub.
    }

    /**
     * Update device information in server.
     * Device information includes: platform, manufacturer, os & push token.
     *
     * @param newPushToken New provided push token.
     */
    public void updateDeviceInfoForPushTFA(@NonNull final String newPushToken) {
        _businessApiService.updateDevice(newPushToken, new GigyaCallback<GigyaApiResponse>() {
            @Override
            public void onSuccess(GigyaApiResponse obj) {
                GigyaLogger.debug(LOG_TAG, "Successfully update push token. Persisting new token");

                // Persist new token. This will allow to correctly monitor any token changes.
                _persistenceService.setPushToken(newPushToken);
            }

            @Override
            public void onError(GigyaError error) {
                GigyaLogger.debug(LOG_TAG, "Failed to update device info.");
            }
        });
    }

    /**
     * Return the current session encryption type.
     */
    public String getSessionEncryption() {
        return _persistenceService.getSessionEncryptionType();
    }


    //endregion
}
