package com.voipgrid.vialer;

import android.support.test.InstrumentationRegistry;
import android.test.ActivityInstrumentationTestCase2;

import com.voipgrid.vialer.onboarding.SetupActivity;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

public class OnBoardingForgotPasswordTest extends ActivityInstrumentationTestCase2<SetupActivity> {

    private SetupActivity mActivity;
    private TestActions mActions;

    public OnBoardingForgotPasswordTest() {
        super(SetupActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());
        mActivity = getActivity();

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
