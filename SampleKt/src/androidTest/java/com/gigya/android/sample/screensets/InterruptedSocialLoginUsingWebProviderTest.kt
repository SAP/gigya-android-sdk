package com.gigya.android.sample.screensets

import androidx.test.annotation.UiThreadTest
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.rule.ActivityTestRule
import com.gigya.android.sample.BaseAndroidTest
import com.gigya.android.sample.R
import com.gigya.android.sample.ui.MainActivity
import org.hamcrest.Matchers
import org.junit.Rule

class InterruptedSocialLoginUsingWebProviderTest : BaseAndroidTest() {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(MainActivity::class.java)

    @UiThreadTest
    fun interruptedSocialLoginUsingWebProviderTest() {

        openDrawer()
        drawerSwipeDown()

        // Find screensets navigation menu item.
        val navigationMenuItemView = Espresso.onView(
                Matchers.allOf(childAtPosition(
                        Matchers.allOf(ViewMatchers.withId(R.id.design_navigation_view),
                                childAtPosition(
                                        ViewMatchers.withId(R.id.nav_view),
                                        0)),
                        16),
                        ViewMatchers.isDisplayed()))

        // Click on screensets navigation menu item.
        navigationMenuItemView.perform(ViewActions.click())


    }
}