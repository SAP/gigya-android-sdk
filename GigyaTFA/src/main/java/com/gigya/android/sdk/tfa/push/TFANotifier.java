package com.gigya.android.sdk.tfa.push;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import java.util.Random;

import static com.gigya.android.sdk.tfa.GigyaDefinitions.TFA_CHANNEL_ID;

public class TFANotifier implements ITFANotifier {

    private Context _context;

    public TFANotifier(Context context) {
        _context = context;
    }

    @Override
    public void notifyWith(@NonNull String title, @NonNull String body) {
        // Build notification.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(_context, TFA_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        // Notify.
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(_context);
        notificationManager.notify(new Random().nextInt(), builder.build());
    }
}
