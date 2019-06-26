package com.gigya.android.sdk.tfa.push;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.tfa.GigyaTFA;
import com.gigya.android.sdk.tfa.persistence.ITFAPersistenceService;
import com.gigya.android.sdk.tfa.push.firebase.FirebasePushTokenFetcher;
import com.gigya.android.sdk.utils.DeviceUtils;

public class DeviceInfoBuilder {

    private static final String LOG_TAG = "DeviceInfoBuilder";

    final private ITFAPersistenceService _persistenceService;

    public DeviceInfoBuilder(ITFAPersistenceService persistenceService) {
        _persistenceService = persistenceService;
    }

    public interface DeviceInfoCallback {

        void onDeviceInfo(String deviceInfoJson);

        void unavailableToken();
    }

    private GigyaTFA.PushService pushService;

    public DeviceInfoBuilder setPushService(GigyaTFA.PushService pushService) {
        this.pushService = pushService;
        return this;
    }

    public void buildAsync(final DeviceInfoCallback callback) {
        switch (pushService) {
            case FIREBASE:
                new FirebasePushTokenFetcher().getToken(new IPushTokenAvailability() {
                    @Override
                    public void onToken(String pushToken) {
                        if (pushToken == null) {
                            callback.unavailableToken();
                            return;
                        }

                        // Persist updated push token.
                        _persistenceService.setPushToken(pushToken);

                        final String man = DeviceUtils.getManufacturer();
                        final String os = DeviceUtils.getOsVersion();
                        final String json = "{ \"platform\": \"android\", \"os\": \"" + os + "\", \"man\": \"" + man + "\", \"pushToken\": \"" + pushToken + "\" }";

                        GigyaLogger.debug(LOG_TAG, "Device info: " + json);

                        callback.onDeviceInfo(json);
                    }
                });
        }
    }


    public String buildWith(String pushToken) {
        final String man = DeviceUtils.getManufacturer();
        final String os = DeviceUtils.getOsVersion();
        final String json = "{ \"platform\": \"android\", \"os\": \"" + os + "\", \"man\": \"" + man + "\", \"pushToken\": \"" + pushToken + "\" }";

        GigyaLogger.debug(LOG_TAG, "Device info: " + json);

        return json;
    }
}
