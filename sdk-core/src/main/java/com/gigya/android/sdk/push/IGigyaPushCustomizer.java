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
     * Note: These icons will only display until Android Nougat.
     *
     * @return Icon reference.
     * @see {https://android-developers.googleblog.com/2016/06/notifications-in-android-n.html} for more information.
     */
    int getApproveActionIcon();

    /**
     * Optional override.
     * Define the notification deny action icon.
     * Note: These icons will only display until Android Nougat.
     *
     * @return Icon reference.
     * @see {https://android-developers.googleblog.com/2016/06/notifications-in-android-n.html} for more information.
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
