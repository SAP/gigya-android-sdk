package com.gigya.android.sample

import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.TypeSafeMatcher
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
abstract class BaseAndroidTest {

    fun childAtPosition(
            parentMatcher: Matcher<View>, position: Int): Matcher<View> {

        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent)
                        && view == parent.getChildAt(position)
            }
        }
    }

    /**
     * Click on "hamburger" icon to open navigation drawer.
     */
    fun openDrawer() {
        val appCompatImageButton = Espresso.onView(
                Matchers.allOf(ViewMatchers.withContentDescription("Open navigation drawer"),
                        childAtPosition(
                                Matchers.allOf(ViewMatchers.withId(R.id.toolbar),
                                        childAtPosition(
                                                ViewMatchers.withId(R.id.app_bar_layout),
                                                0)),
                                1),
                        ViewMatchers.isDisplayed()))
        appCompatImageButton.perform(ViewActions.click())
    }

    /**
     * Swipe down on navigation drawer.
     */
    fun drawerSwipeDown() {
        onView(ViewMatchers.withId(R.id.nav_view)).perform(ViewActions.swipeUp())
    }
}