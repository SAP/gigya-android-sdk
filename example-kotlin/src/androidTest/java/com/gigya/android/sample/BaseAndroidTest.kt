package com.gigya.android.sample

import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.web.sugar.Web
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.DriverAtoms.webClick
import androidx.test.espresso.web.webdriver.Locator
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.gigya.android.sample.ui.MainActivity
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
abstract class BaseAndroidTest {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(MainActivity::class.java)

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

    /**
     * Click on "hamburger" icon to open navigation drawer.
     */
    fun openDrawer() {
        val appCompatImageButton = onView(
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

    /**
     * Perform click on a DOM element within the WebView element.
     */
    fun clickOnWebElement(locator: Locator, element: String) {
        Web.onWebView()
                .withElement(findElement(locator, element))
                .perform(webClick())
    }

    /**
     * Perform click on a scoped DOM element within the WebView
     */
    fun clickOnContextualWebElement(parentLocator: Locator, parentElement: String, locator: Locator, element: String) {
        Web.onWebView()
                .withElement(findElement(parentLocator, parentElement))
                .withContextualElement(findElement(locator, element))
                .perform(webClick())
    }

    /**
     * Find click & return the relevant navigation menu item.
     */
    fun clickOnMenuItem(item: Int): ViewInteraction {
        val navigationMenuItemView = onView(
                Matchers.allOf(childAtPosition(
                        Matchers.allOf(ViewMatchers.withId(R.id.design_navigation_view),
                                childAtPosition(
                                        ViewMatchers.withId(R.id.nav_view),
                                        0)),
                        item),
                        ViewMatchers.isDisplayed()))
        navigationMenuItemView.perform(ViewActions.click())
        return navigationMenuItemView
    }

    //region Menu positions

    companion object Positions {

        const val MENU_POSITION_SEND_REQUEST = 1
        const val MENU_POSITION_LOGIN = 2
        const val MENU_POSITION_LOGIN_WITH_PROVIDER = 3
        const val MENU_POSITION_ADD_CONNECTION = 4
        const val MENU_POSITION_REMOVE_CONECTION = 5
        const val MENU_POSITION_REGISTER = 6
        const val MENU_POSITION_GET_ACCOUNT_INFO = 7
        const val MENU_POSITION_GET_ACCOUNT_INFO_EXTRA = 8
        const val MENU_POSITION_SET_ACCOUNT_INFO = 9
        const val MENU_POSITION_VERIFY_LOGIN = 10
        const val MENU_POSITION_FORGOT_PASSWORD = 11
        const val MENU_POSITION_PUSH_TFA_OPT_IN = 12
        const val MENU_POSITION_NATIVE_LOGIN = 15
        const val MENU_POSITION_SCREENSETS = 16
    }

    //endregion
}