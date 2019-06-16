package com.gigya.android.sdk.tfa;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.containers.IoCContainer;
import com.gigya.android.sdk.tfa.workers.ApproveTFAWorker;

public class GigyaTFA {

    public static final String VERSION = "1.0.0";

    private static final String LOG_TAG = "GigyaTFA";

    private static GigyaTFA _sharedInstance;

    public static synchronized GigyaTFA getInstance() {
        if (_sharedInstance == null) {
            IoCContainer container = Gigya.getContainer();

            container.bind(GigyaTFA.class, GigyaTFA.class, true);

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

    }

    public void optInForPushTFA() {

    }

    public void optOutOfPushTFA() {

    }

    public void onApprovePushTFA() {
        OneTimeWorkRequest approveWorkRequest = new OneTimeWorkRequest.Builder(ApproveTFAWorker.class)
                .build();
        WorkManager.getInstance().enqueue(approveWorkRequest);
    }

    public void onDenyPushTFA() {

    }
}
