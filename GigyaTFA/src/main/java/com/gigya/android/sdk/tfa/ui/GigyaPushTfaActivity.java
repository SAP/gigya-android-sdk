package com.gigya.android.sdk.tfa.ui;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.tfa.R;
import com.gigya.android.sdk.tfa.worker.ApproveTFAWorker;

/**
 * Activity used in conjunction with the GigyaMessagingService in order to apply custom message actions.
 */
public class GigyaPushTfaActivity extends AppCompatActivity {

    private final static String LOG_TAG = "GigyaPushTfaActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_push_tfa);

        // Show alert fragment.
        AlertDialog alert = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.tfa_title))
                .setMessage(R.string.tfa_message)
                .setCancelable(false)
                .setPositiveButton(R.string.approve, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        GigyaLogger.debug(LOG_TAG, "approve clicked");
                        onApprove();
                        dialog.dismiss();
                    }
                }).setNegativeButton(R.string.deny, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        GigyaLogger.debug(LOG_TAG, "deny clicked");
                        onDeny();
                        dialog.dismiss();
                    }
                }).create();
        alert.show();
    }

    private void onApprove() {
        OneTimeWorkRequest approveWorkRequest = new OneTimeWorkRequest.Builder(ApproveTFAWorker.class)
                .build();
        WorkManager.getInstance().enqueue(approveWorkRequest);
        finish();
    }

    private void onDeny() {
        // TODO: 2019-06-12 Should we do anything here?
        finish();
    }
}


