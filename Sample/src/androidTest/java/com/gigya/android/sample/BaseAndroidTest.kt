package com.gigya.android.sample

import android.app.Activity
import android.support.annotation.Nullable
import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.TypeSafeMatcher

open class BaseAndroidTest {

    companion object {
        const val NAV_POSITION_NATIVE_LOGIN = 9
    }

    @Nullable
    protected fun getActivity(): Activity? {
        var currentActivity: Activity? = null
        getInstrumentation().runOnMainSync {
            val resumedActivities = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
            if (resumedActivities.iterator().hasNext()) {
                currentActivity = resumedActivities.iterator().next() as Activity
            }
        }
        return currentActivity
    }

    private fun childAtPosition(
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

    protected fun openDrawer() {
        Espresso.onView(
                Matchers.allOf(ViewMatchers.withContentDescription("Open navigation drawer"),
                        childAtPosition(
                                Matchers.allOf(ViewMatchers.withId(R.id.toolbar),
                                        childAtPosition(
                                                ViewMatchers.withClassName(Matchers.`is`("android.support.design.widget.AppBarLayout")),
                                                0)),
                                1),
                        ViewMatchers.isDisplayed())).perform(ViewActions.click())
    }

    protected fun selectNavigationItemAt(position: Int) {
        Espresso.onView(
                Matchers.allOf(childAtPosition(
                        Matchers.allOf(ViewMatchers.withId(R.id.design_navigation_view),
                                childAtPosition(
                                        ViewMatchers.withId(R.id.nav_view),
                                        0)),
                        position),
                        ViewMatchers.isDisplayed())).perform(ViewActions.click())
    }
}