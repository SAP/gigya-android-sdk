package com.gigya.android.sdk.tfa.push.firebase;

import android.support.annotation.NonNull;

import com.gigya.android.sdk.tfa.push.IPushTokenAvailability;
import com.gigya.android.sdk.tfa.push.IPushTokenFetcher;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

public class FirebasePushTokenFetcher implements IPushTokenFetcher {

    private static final String LOG_TAG = "FirebasePushTokenFetcher";

    @Override
    public void getToken(@NonNull final IPushTokenAvailability availabilityCallback) {
        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
            @Override
            public void onComplete(@NonNull Task<InstanceIdResult> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    // Get new Instance ID token
                    final String fcmToken = task.getResult().getToken();
                    availabilityCallback.onToken(fcmToken);
                    return;
                }

                availabilityCallback.onToken(null);
            }
        });
    }
}
