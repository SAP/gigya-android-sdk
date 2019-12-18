package com.gigya.android.sdk.auth.ui;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.auth.GigyaAuth;
import com.gigya.android.sdk.auth.R;

/**
 * Activity used in conjunction with the GigyaMessagingService in order to apply custom message actions.
 * It is optional to use your own custom activity class instead of this one. In order to do so you will need
 * to extend the GigyaMessagingService and provide your own by overriding the getCustomActionActivity method.
 */
public class PushAuthActivity extends AppCompatActivity {

    private final static String LOG_TAG = "PushAuthActivity";

    protected final GigyaAuth _authLib = GigyaAuth.getInstance();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_push_auth);

        if (alertOnCreate()) {
            showActionAlert();
        }
    }

    protected boolean alertOnCreate() {
        return true;
    }

    /**
     * Show TFA actions alert.
     */
    protected void showActionAlert() {
        final Bundle extras = getIntent().getExtras();
        if (extras == null) {
            finish();
            return;
        }
        // Show alert fragment.
        AlertDialog alert = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.gig_auth_push_activity_alert_title))
                .setMessage(R.string.gig_auth_push_activity_alert_message)
                .setCancelable(false)
                .setPositiveButton(R.string.gig_auth_approve, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        GigyaLogger.debug(LOG_TAG, "approve clicked");
                        onApprove(extras);
                        dialog.dismiss();
                    }
                }).setNegativeButton(R.string.gig_auth_deny, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        GigyaLogger.debug(LOG_TAG, "deny clicked");
                        onDeny();
                        dialog.dismiss();
                    }
                }).create();
        alert.show();
    }

    /**
     * User chose to approve the push action.
     * Approval logic is dependant on included mode parameter.
     *
     * @param extras Parameters passed in push intent.
     */
    protected void onApprove(Bundle extras) {
        final String pushMode = extras.getString("mode");
        final String verificationToken = extras.getString("vToken");

        // Restrictions apply.
        if (pushMode != null && verificationToken != null) {

            GigyaLogger.debug(LOG_TAG, "Action for vToken: " + verificationToken);

            // Continue flow.
            _authLib.verifyAuthPush(verificationToken);
        } else {
            GigyaLogger.error(LOG_TAG, "Error fetching mandatory fields (gigyaAssertion, verificationToken, pushMode) from intent extras");
        }
        finish();
    }

    /**
     * User chose to deny the push action.
     */
    protected void onDeny() {
        //_tfaLib.getInstance().pushDeny(); /* Not implemented on version 1.0.0 */
        finish();
    }


    @Override
    public void finish() {
        super.finish();
        // Disable exit animation. Will cause a smoother dismissal experience.
        overridePendingTransition(0, 0);
    }
}


