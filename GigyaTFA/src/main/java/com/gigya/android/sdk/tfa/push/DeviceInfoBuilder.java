package com.gigya.android.sdk.tfa.push;

import com.gigya.android.sdk.tfa.GigyaTFA;
import com.gigya.android.sdk.tfa.push.firebase.FirebasePushTokenFetcher;
import com.gigya.android.sdk.utils.DeviceUtils;

public class DeviceInfoBuilder {

    public interface DeviceInfoCallback {

        void onDeviceInfo(String deviceInfoJson);
    }

    GigyaTFA.PushService pushService;

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
                        final String man = DeviceUtils.getManufacturer();
                        final String os = DeviceUtils.getOsVersion();
                        final String json = "{ \"platform\": \"android\", \"os\": \"" + os + "\", \"man\": \"" + man + "\", \"pushToken\": \"" + pushToken + "\" }";
                        callback.onDeviceInfo(json);
                    }
                });
        }
    }
}
