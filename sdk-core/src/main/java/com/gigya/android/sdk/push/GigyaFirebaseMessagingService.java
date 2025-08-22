package com.gigya.android.sdk.push;

import static com.gigya.android.sdk.persistence.PersistenceService.PREFS_FILE_KEY;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.gigya.android.sdk.GigyaDefinitions;
import com.gigya.android.sdk.GigyaLogger;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;

public class GigyaFirebaseMessagingService extends FirebaseMessagingService {

    final private static String LOG_TAG = "GigyaMessagingService";

    public static final String EXTRA_REMOTE_MESSAGE_DATA = "extra_remote_message_data";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        // Redirect the message to all available local receivers.
        GigyaLogger.debug(LOG_TAG, "onMessageReceived: " + remoteMessage.getData().toString());

        final Intent routingIntent = new Intent(GigyaDefinitions.Broadcasts.INTENT_ACTION_REMOTE_MESSAGE)
                .putExtra(
                        EXTRA_REMOTE_MESSAGE_DATA,
                        /* Serializable */ new HashMap<>(remoteMessage.getData())
                );
        LocalBroadcastManager.getInstance(this).sendBroadcast(routingIntent);
    }

    @Override
    public void onNewToken(String newToken) {

        GigyaLogger.debug(LOG_TAG, "onNewToken: " + newToken);

        final SharedPreferences sp = getSharedPreferences(PREFS_FILE_KEY, Context.MODE_PRIVATE);
        sp.edit().putString("GS_PUSH_TOKEN", newToken).apply();
    }


    //region Token fetched async.

    public interface IFcmTokenResponse {

        void onAvailable(@Nullable String token);
    }

    /**
     * Requesting the Firebase push token dynamically.
     *
     * @param response IFcmTokenResponse interface instance to route the token response.
     *                 Note: response can yield a nullable token.
     */
    public static void requestTokenAsync(final IFcmTokenResponse response) {

        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    // Get new FCM registration token
                    final String fcmToken = task.getResult();

                    GigyaLogger.debug(LOG_TAG, "requestTokenAsync: " + fcmToken);

                    response.onAvailable(fcmToken);
                    return;
                }
                response.onAvailable(null);
            }
        });
    }

    //endregion
}
