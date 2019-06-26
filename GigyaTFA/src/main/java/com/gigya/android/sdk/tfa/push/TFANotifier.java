package com.gigya.android.sdk.tfa.push;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.gigya.android.sdk.GigyaLogger;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.gigya.android.sdk.tfa.GigyaDefinitions.TFA_CHANNEL_ID;

/**
 * Local notifications helper class.
 */
public class TFANotifier implements ITFANotifier {

    private static final String LOG_TAG = "TFANotifier";

    private Context _context;

    public TFANotifier(Context context) {
        _context = context;
    }

    @Override
    public void notifyWith(@NonNull String title, @NonNull String body) {
        GigyaLogger.debug(LOG_TAG, "notifyWith: title = " + title + ", body = " + body);
        // Build notification.
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(_context, TFA_CHANNEL_ID)
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
        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(_context);
        notificationManager.notify(new Random().nextInt(), builder.build());
    }
}
