package com.voipgrid.vialer.screenshots;

import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.voipgrid.vialer.MainActivity;
import com.voipgrid.vialer.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.core.AllOf.allOf;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.locale.LocaleTestRule;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class ScreenshotTests {
    @ClassRule
    public static final LocaleTestRule localeTestRule = new LocaleTestRule();

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);

    @Rule
    public GrantPermissionRule mGrantPermissionRule =
            GrantPermissionRule.grant(
                    "android.permission.READ_CONTACTS",
                    "android.permission.GET_ACCOUNTS",
                    "android.permission.WRITE_CONTACTS",
                    "android.permission.READ_EXTERNAL_STORAGE",
                    "android.permission.READ_PHONE_STATE");

    @Test
    public void screenshotTests() {
        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(7000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Screengrab.screenshot("01-login");

        ViewInteraction appCompatEditText = onView(
            Matchers.allOf(
                ViewMatchers.withId(R.id.emailTextDialog),
                    childAtPosition(childAtPosition(withId(R.id.email_field), 0), 0),
                    isDisplayed()));
        appCompatEditText.perform(replaceText("android@vialerapp.com"), closeSoftKeyboard());

        ViewInteraction appCompatEditText2 = onView(
                allOf(withId(R.id.passwordTextDialog),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.password_field),
                                        0),
                                0),
                        isDisplayed()));
        appCompatEditText2.perform(replaceText("PW"), closeSoftKeyboard());

        ViewInteraction appCompatButton = onView(
                allOf(withId(R.id.button_login), withText("login"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.fragment_container),
                                        1),
                                4)));
        appCompatButton.perform(scrollTo(), click());

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(7000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

//        ViewInteraction appCompatEditText3 = onView(
//                allOf(withId(R.id.mobileNumberTextDialog), withText("+31612345678"),
//                        childAtPosition(
//                                childAtPosition(
//                                        withId(R.id.mobile_number_field),
//                                        0),
//                                0),
//                        isDisplayed()));
//        appCompatEditText3.perform(replaceText(""), closeSoftKeyboard());
//
//        ViewInteraction appCompatEditText6 = onView(
//                allOf(withId(R.id.mobileNumberTextDialog),
//                        childAtPosition(
//                                childAtPosition(
//                                        withId(R.id.mobile_number_field),
//                                        0),
//                                0),
//                        isDisplayed()));
//        appCompatEditText6.perform(replaceText("+31612345678"), closeSoftKeyboard());

        Screengrab.screenshot("02-configure");


        ViewInteraction appCompatButton2 = onView(
                allOf(withId(R.id.button_configure), withText("Configure"),
                        childAtPosition(
                                allOf(withId(R.id.fragment_account),
                                        childAtPosition(
                                                withId(R.id.fragment_container),
                                                1)),
                                4)));
        appCompatButton2.perform(scrollTo(), click());

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(7000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction appCompatButton3 = onView(
                allOf(withId(R.id.button_welcome), withText("Get started"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.fragment_container),
                                        1),
                                2)));
        appCompatButton3.perform(scrollTo(), click());



        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(7000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Screengrab.screenshot("03-contacts");
    }

    private static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {

        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }
}
