package com.gigya.android.sdk.tfa.ui;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.tfa.GigyaDefinitions;
import com.gigya.android.sdk.tfa.GigyaTFA;
import com.gigya.android.sdk.tfa.R;

/**
 * Activity used in conjunction with the GigyaMessagingService in order to apply custom message actions.
 * It is optional to use your own custom activity class instead of this one. In order to do so you will need
 * to extend the GigyaMessagingService and provide your own by overriding the getCustomActionActivity method.
 */
public class PushTFAActivity extends AppCompatActivity {

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
                .setPositiveButton(R.string.tfa_approve, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        GigyaLogger.debug(LOG_TAG, "approve clicked");
                        onApprove(getIntent().getExtras());
                        dialog.dismiss();
                    }
                }).setNegativeButton(R.string.tfa_deny, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        GigyaLogger.debug(LOG_TAG, "deny clicked");
                        onDeny();
                        dialog.dismiss();
                    }
                }).create();
        alert.show();
    }

    private void onApprove(Bundle extras) {
        final String pushMode = extras.getString("mode");
        final String gigyaAssertion = extras.getString("gigyaAssertion");
        final String verificationToken = extras.getString("verificationToken");

        // Restrictions apply.
        if (pushMode != null && gigyaAssertion != null && verificationToken != null) {

            GigyaLogger.debug(LOG_TAG, "Action for vt: " + verificationToken);

            // Continue flow.
            if (pushMode.equals(GigyaDefinitions.PushMode.OPT_IN)) {
                GigyaTFA.getInstance().verifyOptInForPushTFA(gigyaAssertion, verificationToken);
            } else {
                GigyaTFA.getInstance().approveLoginForPushTFA(gigyaAssertion, verificationToken);
            }
        } else {
            GigyaLogger.error(LOG_TAG, "Error fetching mandatory fields (gigyaAssertion, verificationToken, pushMode) from intent extras");
        }
        finish();
    }

    private void onDeny() {
        //GigyaTFA.getInstance().pushDeny(); /* Not implemented on version 1.0.0 */
        finish();
    }

    @Override
    public void finish() {
        super.finish();
        // Disable exit animation.
        overridePendingTransition(0, 0);
    }
}


