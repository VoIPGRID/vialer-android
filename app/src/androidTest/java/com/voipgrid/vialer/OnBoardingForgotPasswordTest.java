package com.voipgrid.vialer;

import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import com.voipgrid.vialer.onboarding.SetupActivity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class OnBoardingForgotPasswordTest {

    @Rule
    public ActivityTestRule<SetupActivity> mActivityTestRule = new ActivityTestRule<>(SetupActivity.class);

    private SetupActivity mActivity;
    private TestActions mActions;

    @Before
    public void setUp() {
        mActivity = mActivityTestRule.getActivity();

        mActions = new TestActions();
        mActions.logoWaitProceed();

        mActions.performClick(R.id.button_forgot_password);
    }

    /**
     * Dialog check and dismiss helper.
     */
    private void checkForDialogAndDismiss() {
        // verify pop up dialog with title/message from Strings file.
        mActions.checkIsDisplayed(withText(R.string.forgot_password_missing_field_title));
        mActions.checkIsDisplayed(withText(R.string.forgot_password_missing_field_message));

        // Close dialog
        onView(withText(mActivity.getString(R.string.ok)))
                .perform(click());
    }

    public void testClickSendNoEmail_sameActicity() {
        mActions.enterEditTextValueValue(R.id.forgotPasswordEmailTextDialog, "");
        mActions.performClick(R.id.button_send_password_email);

        checkForDialogAndDismiss();
    }

    public void testClickSendIncorrectEmail_sameActicity() {
        mActions.enterEditTextValueValue(R.id.forgotPasswordEmailTextDialog, "some random text");
        mActions.performClick(R.id.button_send_password_email);

        checkForDialogAndDismiss();
    }

    public void testClickSendCorrectEmail_sameActicity() {
        mActions.enterEditTextValueValue(R.id.forgotPasswordEmailTextDialog, "me@example.com");
        mActions.performClick(R.id.button_send_password_email);
    }
}
