package com.gigya.android.sdk.tfa;

import android.support.annotation.NonNull;

import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.containers.IoCContainer;
import com.gigya.android.sdk.tfa.api.ITFABusinessApiService;
import com.gigya.android.sdk.tfa.api.TFABusinessApiService;
import com.gigya.android.sdk.tfa.push.DeviceInfoBuilder;

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

    private ITFABusinessApiService _tfaBusinessApiService;

    protected GigyaTFA(ITFABusinessApiService tfaBusinessApiService) {
        _tfaBusinessApiService = tfaBusinessApiService;
    }

    private void generateDeviceInfo(final Runnable completionHandler) {
        new DeviceInfoBuilder().setPushService(_pushService).buildAsync(new DeviceInfoBuilder.DeviceInfoCallback() {
            @Override
            public void onDeviceInfo(String deviceInfoJson) {
                _deviceInfo = deviceInfoJson;
                if (completionHandler != null) {
                    completionHandler.run();
                }
            }
        });
    }

    //region INTERFACING

    public void pushOptIn(@NonNull final GigyaCallback<GigyaApiResponse> gigyaCallback) {
        // Device info is required.
        generateDeviceInfo(new Runnable() {
            @Override
            public void run() {
                _tfaBusinessApiService.optIntoPush(_deviceInfo, gigyaCallback);
            }
        });
    }

    public void pushOptOut() {

    }

    public void verifyPush() {

    }

    public void pushApprove() {

    }

    public void pushDeny() {

    }

    private void updatePushDeviceInfo() {

    }

    //endregion
}
