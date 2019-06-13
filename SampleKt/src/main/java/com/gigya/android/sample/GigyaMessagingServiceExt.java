package com.gigya.android.sample;

import com.gigya.android.sdk.tfa.service.GigyaMessagingService;

public class GigyaMessagingServiceExt extends GigyaMessagingService {

    @Override
    protected int getNotificationIcon() {
        return R.mipmap.ic_launcher_round;
    }
}
