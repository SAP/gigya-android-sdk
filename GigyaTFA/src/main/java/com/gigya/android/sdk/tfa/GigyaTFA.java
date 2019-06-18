package com.gigya.android.sdk.tfa;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.containers.IoCContainer;
import com.gigya.android.sdk.tfa.api.ITFABusinessApiService;
import com.gigya.android.sdk.tfa.api.TFABusinessApiService;
import com.gigya.android.sdk.tfa.push.DeviceInfoBuilder;
import com.gigya.android.sdk.tfa.workers.ApproveTFAWorker;
import com.gigya.android.sdk.tfa.workers.TokenUpdateWorker;

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

    protected GigyaTFA() {
        generateDeviceInfo();
    }

    //region INTERFACING

    public void pushOptIn() {

    }

    public void pushOptOut() {

    }

    public void pushApprove() {
        OneTimeWorkRequest approveWorkRequest = new OneTimeWorkRequest.Builder(ApproveTFAWorker.class)
                .build();
        WorkManager.getInstance().enqueue(approveWorkRequest);
    }

    public void pushDeny() {

    }

    private void updatePushDeviceInfo(String fcmToken) {
        OneTimeWorkRequest.Builder updateWorkRequestBuilder = new OneTimeWorkRequest.Builder(TokenUpdateWorker.class);
        Data inputData = new Data.Builder().putString("token", fcmToken).build();
        updateWorkRequestBuilder.setInputData(inputData);
        WorkManager.getInstance().enqueue(updateWorkRequestBuilder.build());
    }

    private void generateDeviceInfo() {
        new DeviceInfoBuilder().setPushService(_pushService).buildAsync(new DeviceInfoBuilder.DeviceInfoCallback() {
            @Override
            public void onDeviceInfo(String deviceInfoJson) {
                _deviceInfo = deviceInfoJson;
            }
        });
    }

    //endregion
}
