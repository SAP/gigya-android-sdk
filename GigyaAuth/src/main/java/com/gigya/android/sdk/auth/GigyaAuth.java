package com.gigya.android.sdk.auth;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.auth.api.AuthBusinessApiService;
import com.gigya.android.sdk.auth.api.IAuthBusinessApiService;
import com.gigya.android.sdk.auth.push.AuthRemoteMessageHandler;
import com.gigya.android.sdk.containers.IoCContainer;
import com.gigya.android.sdk.push.IGigyaNotificationManager;
import com.gigya.android.sdk.push.IGigyaPushCustomizer;
import com.gigya.android.sdk.push.IRemoteMessageHandler;
import com.gigya.android.sdk.push.RemoteMessageLocalReceiver;

public class GigyaAuth {

    private static final String VERSION = "1.0.0";

    private static final String LOG_TAG = "GigyaAuth";

    @SuppressLint("StaticFieldLeak")
    private static GigyaAuth _sharedInstance;

    public static synchronized GigyaAuth getInstance() {
        if (_sharedInstance == null) {
            IoCContainer container = Gigya.getContainer();

            container.bind(GigyaAuth.class, GigyaAuth.class, true);
            container.bind(IAuthBusinessApiService.class, AuthBusinessApiService.class, true);
            container.bind(IRemoteMessageHandler.class, AuthRemoteMessageHandler.class, true);

            try {
                _sharedInstance = container.get(GigyaAuth.class);
                GigyaLogger.debug(LOG_TAG, "Instantiation version: " + VERSION);
            } catch (Exception e) {
                GigyaLogger.error(LOG_TAG, "Error creating Gigya Auth library (did you forget to Gigya.setApplication?");
                e.printStackTrace();
                throw new RuntimeException("Error creating Gigya Auth library (did you forget to Gigya.setApplication?");
            }
        }
        return _sharedInstance;
    }

    private final Context _context;
    private final IGigyaNotificationManager _gigyaNotificationManager;
    private final IAuthBusinessApiService _authBusinessApiService;

    protected GigyaAuth(Context context,
                        IAuthBusinessApiService authBusinessApiService,
                        IRemoteMessageHandler remoteMessageHandler,
                        IGigyaNotificationManager gigyaNotificationManager) {
        _context = context;
        _gigyaNotificationManager = gigyaNotificationManager;
        _authBusinessApiService = authBusinessApiService;

        /*
        Update push notification customization options.
         */
        remoteMessageHandler.setPushCustomizer(_authPushCustomizer);

        /*
        Register remote message receiver to handle TFA push messages.
         */
        LocalBroadcastManager.getInstance(context).registerReceiver(
                new RemoteMessageLocalReceiver(remoteMessageHandler),
                new IntentFilter(com.gigya.android.sdk.GigyaDefinitions.Broadcasts.INTENT_ACTION_REMOTE_MESSAGE)
        );
    }

    /**
     * default push customizer instance.
     */
    private IGigyaPushCustomizer _authPushCustomizer = new IGigyaPushCustomizer() {
        @Override
        public int getSmallIcon() {
            return android.R.drawable.ic_dialog_info;
        }

        @Override
        public int getApproveActionIcon() {
            return 0;
        }

        @Override
        public int getDenyActionIcon() {
            return 0;
        }

        @Override
        public Class getCustomActionActivity() {
            return null;
        }
    };

    /**
     * Optional setter for TFA push notification customization.
     *
     * @param customizer custom IGigyaPushCustomizer interface for available notification customization options.
     */
    public void setPushCustomizer(IGigyaPushCustomizer customizer) {
        _authPushCustomizer = customizer;
    }

}
