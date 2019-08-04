package com.gigya.android.sample.screensets


import androidx.test.annotation.UiThreadTest
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.rule.ActivityTestRule
import com.gigya.android.sample.BaseAndroidTest
import com.gigya.android.sample.R
import com.gigya.android.sample.ui.MainActivity
import org.hamcrest.Matchers.allOf
import org.junit.Rule

class SimpleSocialLoginUsingWebProviderTest : BaseAndroidTest() {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(MainActivity::class.java)

    @UiThreadTest
    fun simpleSocialLoginUsingWebProviderTest() {

        openDrawer()
        drawerSwipeDown()

        // Find screensets navigation menu item.
        val navigationMenuItemView = onView(
                allOf(childAtPosition(
                        allOf(withId(R.id.design_navigation_view),
                                childAtPosition(
                                        withId(R.id.nav_view),
                                        0)),
                        16),
                        isDisplayed()))

        // Click on screensets navigation menu item.
        navigationMenuItemView.perform(click())


    }
}
