package com.gigya.android.sdk.tfa.push;

import com.gigya.android.sdk.push.IGigyaPushCustomizer;
import com.gigya.android.sdk.tfa.ui.PushTFAActivity;

public class TFAPushCustomizer implements IGigyaPushCustomizer {

    /**
     * Optional override
     * Define the notification small icon.
     *
     * @return Icon reference.
     */
    @Override
    public int getSmallIcon() {
        return android.R.drawable.ic_dialog_info;
    }

    /**
     * Optional override.
     * Define the notification approve action icon.
     *
     * @return Icon reference.
     */
    @Override
    public int getApproveActionIcon() {
        return 0;
    }

    /**
     * Optional override.
     * Define the notification deny action icon.
     *
     * @return Icon reference.
     */
    @Override
    public int getDenyActionIcon() {
        return 0;
    }

    /**
     * Optional override.
     * Allows to define the activity class used by the the notification's content intent.
     * default class GigyaPushTfaActivity.class.
     *
     * @return Activity class reference.
     */
    @Override
    public Class getCustomActionActivity() {
        return PushTFAActivity.class;
    }
}
