package com.gigya.android.sample.screensets


import androidx.test.annotation.UiThreadTest
import com.gigya.android.sample.BaseAndroidTest

class SimpleSocialLoginUsingWebProviderTest : BaseAndroidTest() {

    @UiThreadTest
    fun simpleSocialLoginUsingWebProviderTest() {

        openDrawer()

        drawerSwipeDown()

        clickOnMenuItem(MENU_POSITION_SCREENSETS)
    }
}
