package com.gigya.android.sdk.push;

public interface IGigyaPushCustomizer {

    /**
     * Optional override
     * Define the notification small icon.
     *
     * @return Icon reference.
     */
    int getSmallIcon();

    /**
     * Optional override.
     * Define the notification approve action icon.
     *
     * @return Icon reference.
     */
    int getApproveActionIcon();

    /**
     * Optional override.
     * Define the notification deny action icon.
     *
     * @return Icon reference.
     */
    int getDenyActionIcon();

    /**
     * Optional override.
     * Allows to define the activity class used by the the notification's content intent.
     * default class GigyaPushTfaActivity.class.
     *
     * @return Activity class reference.
     */
    Class getCustomActionActivity();
}
