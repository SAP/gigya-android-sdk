package com.gigya.android.sample.screensets

import androidx.test.annotation.UiThreadTest
import com.gigya.android.sample.BaseAndroidTest

class InterruptedSocialLoginUsingWebProviderTest : BaseAndroidTest() {

    @UiThreadTest
    fun interruptedSocialLoginUsingWebProviderTest() {

        openDrawer()

        drawerSwipeDown()

        clickOnMenuItem(MENU_POSITION_SCREENSETS)
    }
}