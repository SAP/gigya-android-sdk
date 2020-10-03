package com.gigya.android.sdk.push;

import android.content.Context;
import androidx.annotation.NonNull;

public interface IGigyaNotificationManager {

    void createNotificationChannelIfNeeded(@NonNull Context context,
                                           @NonNull String name,
                                           @NonNull String description,
                                           @NonNull String channelId);

    void notifyWith(@NonNull Context context,
                    @NonNull String title,
                    @NonNull String body,
                    @NonNull String channelId);

    String getDeviceInfo(@NonNull String pushToken);

}
