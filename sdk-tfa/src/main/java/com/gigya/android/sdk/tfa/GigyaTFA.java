package com.gigya.android.sdk.tfa;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;

import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.containers.IoCContainer;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.persistence.IPersistenceService;
import com.gigya.android.sdk.push.GigyaFirebaseMessagingService;
import com.gigya.android.sdk.push.IGigyaNotificationManager;
import com.gigya.android.sdk.push.IGigyaPushCustomizer;
import com.gigya.android.sdk.push.IRemoteMessageHandler;
import com.gigya.android.sdk.push.RemoteMessageLocalReceiver;
import com.gigya.android.sdk.tfa.api.ITFABusinessApiService;
import com.gigya.android.sdk.tfa.api.TFABusinessApiService;
import com.gigya.android.sdk.tfa.push.TFARemoteMessageHandler;
import com.gigya.android.sdk.tfa.ui.PushTFAActivity;

import static com.gigya.android.sdk.tfa.GigyaDefinitions.TFA_CHANNEL_ID;

@SuppressWarnings("Convert2Lambda")
public class GigyaTFA {

    private static final String VERSION = "1.0.3";

    private static final String LOG_TAG = "GigyaTFA";

    @SuppressLint("StaticFieldLeak")
    private static GigyaTFA _sharedInstance;

    public static synchronized GigyaTFA getInstance() {
        if (_sharedInstance == null) {
            IoCContainer container = Gigya.getContainer();

            container.bind(GigyaTFA.class, GigyaTFA.class, true);
            container.bind(ITFABusinessApiService.class, TFABusinessApiService.class, true);
            container.bind(IRemoteMessageHandler.class, TFARemoteMessageHandler.class, true);

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

    private final Context _context;
    private final ITFABusinessApiService _businessApiService;
    private final IPersistenceService _persistenceService;
    private final IGigyaNotificationManager _gigyaNotificationManager;
    private final IRemoteMessageHandler _remoteMessageHandler;

    protected GigyaTFA(Context context,
                       ITFABusinessApiService businessApiService,
                       IPersistenceService persistenceService,
                       IRemoteMessageHandler remoteMessageHandler,
                       IGigyaNotificationManager gigyaNotificationManager) {
        _context = context;
        _businessApiService = businessApiService;
        _persistenceService = persistenceService;
        _remoteMessageHandler = remoteMessageHandler;
        _gigyaNotificationManager = gigyaNotificationManager;

        /*
        Set default customization.
         */
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
                return PushTFAActivity.class;
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

    /**
     * Optional setter for TFA push notification customization.
     *
     * @param customizer custom IGigyaPushCustomizer interface for available notification customization options.
     */
    public void setPushCustomizer(IGigyaPushCustomizer customizer) {
        _remoteMessageHandler.setPushCustomizer(customizer);
    }

    /*
    Device info JSON representation.
    */
    private String _deviceInfo;

    /*
    Will generate required device information asynchronously.
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
     * Request to Opt-In to push Two Factor Authentication.
     * This is the first of two stages of the Opt-In process.
     *
     * @param gigyaCallback Request callback.
     */
    public void optInForPushTFA(@NonNull final GigyaCallback<GigyaApiResponse> gigyaCallback) {
        GigyaLogger.debug(LOG_TAG, "optInForPushTFA: action selected");

        // Device info is required.
        if (_deviceInfo == null) {
            GigyaLogger.debug(LOG_TAG, "optInForPushTFA: device info unavailable - generate.");

            generateDeviceInfo(new Runnable() {
                @Override
                public void run() {
                    GigyaLogger.debug(LOG_TAG, "optInForPushTFA: device info generated = " + _deviceInfo);
                    _businessApiService.optIntoPush(_deviceInfo, gigyaCallback);
                }
            }, new Runnable() {
                @Override
                public void run() {
                    GigyaLogger.error(LOG_TAG, "optInForPushTFA: Failed to generate device info.");
                    gigyaCallback.onError(GigyaError.unauthorizedUser());
                }
            });
        } else {
            GigyaLogger.debug(LOG_TAG, "optInForPushTFA: with device info = " + _deviceInfo);
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
        GigyaLogger.debug(LOG_TAG, "verifyOptInForPushTFA: with gigyaAssertion = " + gigyaAssertion + ", verificationToken = " + verificationToken);

        _businessApiService.finalizePushOptIn(gigyaAssertion, verificationToken, new GigyaCallback<GigyaApiResponse>() {
            @Override
            public void onSuccess(GigyaApiResponse obj) {
                GigyaLogger.debug(LOG_TAG, "verifyOptInForPushTFA: Opt-In verification flow completed");

                // Notify success.
                _gigyaNotificationManager.notifyWith(
                        _context,
                        _context.getString(R.string.gig_tfa_opt_in_approval_success_title),
                        _context.getString(R.string.gig_tfa_opt_in_approval_success_body),
                        TFA_CHANNEL_ID);
            }

            @Override
            public void onError(GigyaError error) {
                GigyaLogger.error(LOG_TAG, "verifyOptInForPushTFA: Failed to complete TFA opt in verification");

                // Notify error.
                _gigyaNotificationManager.notifyWith(
                        _context,
                        _context.getString(R.string.gig_tfa_opt_in_approval_success_title),
                        _context.getString(R.string.gig_tfa_opt_in_approval_error_body),
                        TFA_CHANNEL_ID);
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
        GigyaLogger.debug(LOG_TAG, "approveLoginForPushTFA: with gigyaAssertion = " + gigyaAssertion + ", verificationToken = " + verificationToken);

        _businessApiService.verifyPush(gigyaAssertion, verificationToken, new GigyaCallback<GigyaApiResponse>() {
            @Override
            public void onSuccess(GigyaApiResponse obj) {
                GigyaLogger.debug(LOG_TAG, "approveLoginForPushTFA: Successfully verified push");

                // Notify success.
                _gigyaNotificationManager.notifyWith(
                        _context,
                        _context.getString(R.string.gig_tfa_login_approval_success_title),
                        _context.getString(R.string.gig_tfa_login_approval_success_body),
                        TFA_CHANNEL_ID);
            }

            @Override
            public void onError(GigyaError error) {
                GigyaLogger.error(LOG_TAG, "approveLoginForPushTFA: Failed to verify push");

                // Notify error.
                _gigyaNotificationManager.notifyWith(
                        _context,
                        _context.getString(R.string.gig_tfa_login_approval_success_title),
                        _context.getString(R.string.gig_login_approval_error_body),
                        TFA_CHANNEL_ID);
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

    /**
     * Check if device is registered for push TFA & notifications permission is available.
     * If not. Will display a information dialog allowing the user to open the notifications application settings in order
     * to enable them.
     * Note: It is recommended to call this method when the activity context is attached.
     *
     * @param activity Current activity. Activity context must be provided.
     */
    public void registerForRemoteNotifications(final Activity activity) {
        if (activity.isFinishing()) {
            return;
        }
        if (!pushTFAEnabled()) {

            // Show dialog informing the user that he needs to enable push notifications.
            AlertDialog alert = new AlertDialog.Builder(activity)
                    .setTitle(R.string.gig_tfa_push_notifications_alert_title)
                    .setMessage(R.string.gig_tfa_push_notifications_alert_message)
                    .setCancelable(false)
                    .setPositiveButton(R.string.gig_tfa_approve, new DialogInterface.OnClickListener() {
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
                    }).setNegativeButton(R.string.gig_tfa_no_thanks, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            GigyaLogger.debug(LOG_TAG, "deny clicked");
                            dialog.dismiss();
                        }
                    }).create();
            alert.show();
        }
    }
}
