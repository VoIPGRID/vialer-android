package com.voipgrid.vialer;

import android.support.test.espresso.PerformException;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.util.HumanReadables;
import android.support.test.espresso.util.TreeIterables;
import android.view.View;

import org.hamcrest.Matcher;

import java.util.concurrent.TimeoutException;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isRoot;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

public class TestActions {

    void logoClickProceed() {
        onView(isRoot()) // See if click for impatient people succeeds!
                .perform(click())
                .perform(waitId(R.id.email_field, 1000));
    }

    void logoWaitProceed() {
        onView(isRoot()) // Wait for one second for a email edittext
                .perform(waitId(R.id.email_field, 1000));
    }

    void correctLogin() {
        // enter valid email field
        enterEditTextValueValue(R.id.emailTextDialog, OnBoardingLoginTest.VALID_AND_KNOWN_EMAIL_VALUE);
        // enter valid password field
        enterEditTextValueValue(R.id.passwordTextDialog, OnBoardingLoginTest.VALID_AND_KNOWN_PASSWORD_VALUE);
        // click login.
        performClick(R.id.button_login);
    }

    public void enterEditTextValueValue(int resourceId, String value) {
        scrollToId(resourceId);
        onView(withId(resourceId))
                .perform(typeText(value));
    }

    public void performClick(int resourceId) {
        scrollToId(resourceId);
        onView(withId(resourceId))
                .perform(click());
    }

    public void checkIsDisplayed(Matcher<View> matcher) {
        onView(matcher)
                .check(matches(isDisplayed()));
    }

    void scrollToId(int resourceId) {
        onView(withId(resourceId)).perform(scrollTo());
    }

    /** Perform action of waiting for a specific view id. */
    public static ViewAction waitId(final int viewId, final long millis) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isRoot();
            }

            @Override
            public String getDescription() {
                return "wait for a specific view with id <" + viewId + "> during " + millis + " millis.";
            }

            @Override
            public void perform(final UiController uiController, final View view) {
                uiController.loopMainThreadUntilIdle();
                final long startTime = System.currentTimeMillis();
                final long endTime = startTime + millis;
                final Matcher<View> viewMatcher = withId(viewId);

                do {
                    for (View child : TreeIterables.breadthFirstViewTraversal(view)) {
                        // found view with required ID
                        if (viewMatcher.matches(child)) {
                            return;
                        }
                    }

                    uiController.loopMainThreadForAtLeast(50);
                }
                while (System.currentTimeMillis() < endTime);

                // timeout happens
                throw new PerformException.Builder()
                        .withActionDescription(this.getDescription())
                        .withViewDescription(HumanReadables.describe(view))
                        .withCause(new TimeoutException())
                        .build();
            }
        };
    }

}
