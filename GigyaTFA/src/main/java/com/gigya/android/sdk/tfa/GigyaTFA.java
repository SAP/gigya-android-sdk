package com.gigya.android.sdk.tfa;

import android.support.annotation.NonNull;

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

public class GigyaTFA {

    public static final String VERSION = "1.0.0";

    private static final String LOG_TAG = "GigyaTFA";

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

    private ITFABusinessApiService _businessApiService;
    private ITFAPersistenceService _persistenceService;
    private ITFANotifier _tfaNotifier;

    protected GigyaTFA(ITFABusinessApiService businessApiService,
                       ITFAPersistenceService persistenceService,
                       ITFANotifier tfaNotifier) {
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
     * Request to Opt-In to push Two Factor Authentication.
     * This is the first of two stages of the Opt-In process.
     *
     * @param gigyaCallback Request callback.
     */
    public void pushOptIn(@NonNull final GigyaCallback<GigyaApiResponse> gigyaCallback) {
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

    }

    /**
     * Verify Opt-In process push notifications.
     * This is the second and final stage of the Opt-In process.
     *
     * @param gigyaAssertion    Provided gigya assertion token.
     * @param verificationToken Provided verification token.
     */
    public void verifyPushOptIn(@NonNull String gigyaAssertion, @NonNull String verificationToken) {
        _businessApiService.finalizePushOptIn(gigyaAssertion, verificationToken, new GigyaCallback<GigyaApiResponse>() {
            @Override
            public void onSuccess(GigyaApiResponse obj) {
                GigyaLogger.error(LOG_TAG, "Opt-In verification flow completed");
                _tfaNotifier.notifyWith("Opt-In for push TFA", "This device is registered for push two factor authentication");
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
    public void pushApprove(@NonNull String gigyaAssertion, @NonNull String verificationToken) {
        _businessApiService.verifyPush(gigyaAssertion, verificationToken, new GigyaCallback<GigyaApiResponse>() {
            @Override
            public void onSuccess(GigyaApiResponse obj) {
                GigyaLogger.error(LOG_TAG, "Successfully verified push");
                _tfaNotifier.notifyWith("Verify push TFA", "Successfully authenticated login");
            }

            @Override
            public void onError(GigyaError error) {
                GigyaLogger.error(LOG_TAG, "Failed to verify push");
            }
        });
    }

    /**
     * Not implemented in version 1.0.0
     */
    private void pushDeny() {

    }

    /**
     * Update device information in server.
     * Device information includes: platform, manufacturer, os & push token.
     *
     * @param newPushToken New provided push token.
     */
    public void updateDeviceInfo(@NonNull final String newPushToken) {
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

    //endregion
}
