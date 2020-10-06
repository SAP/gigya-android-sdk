package com.gigya.android.sdk.push;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.utils.DeviceUtils;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class GigyaNotificationManager implements IGigyaNotificationManager {

    private static final String LOG_TAG = "GigyaNotificationManager";

    @Override
    public void createNotificationChannelIfNeeded(@NonNull Context context,
                                                  @NonNull String name,
                                                  @NonNull String description,
                                                  @NonNull String channelId) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final int importance = NotificationManager.IMPORTANCE_HIGH;
            final NotificationChannel channel = new NotificationChannel(channelId, name, importance);
            channel.setDescription(description);
            final NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            /*
            Concurrent calls will update channel info.
             */
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void notifyWith(@NonNull Context context,
                           @NonNull String title,
                           @NonNull String body,
                           @NonNull String channelId) {

        GigyaLogger.debug(LOG_TAG, "notifyWith: title = " + title + ", body = " + body + ", channelId = " + channelId);

        // Build notification.
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        // Set a 3 second timeout for the notification display.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setTimeoutAfter(TimeUnit.SECONDS.toMillis(3));
        }

        // Notify.
        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(new Random().nextInt(), builder.build());
    }


    @Override
    public String getDeviceInfo(@NonNull String pushToken) {

        final String man = DeviceUtils.getManufacturer();
        final String os = DeviceUtils.getOsVersion();
        final String deviceInfoJson = "{ \"platform\": \"android\", \"os\": \"" + os + "\", \"man\": \"" + man + "\", \"pushToken\": \"" + pushToken + "\" }";

        GigyaLogger.debug(LOG_TAG, "getDeviceInfo: " + deviceInfoJson);

        return deviceInfoJson;
    }
}
