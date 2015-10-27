package com.voipgrid.vialer;

import android.support.test.InstrumentationRegistry;
import android.test.ActivityInstrumentationTestCase2;

import com.voipgrid.vialer.onboarding.SetupActivity;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.AllOf.allOf;

public class OnBoardingConfigureTest extends ActivityInstrumentationTestCase2<SetupActivity> {

    private  SetupActivity mActivity;
    private TestActions mActions;

    public OnBoardingConfigureTest() {
        super(SetupActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());
        mActivity = getActivity();

        mActions = new TestActions();
        mActions.logoWaitProceed();
        mActions.correctLogin();
    }

    private void checkForDialogAndDismiss() {
        // verify pop up dialog with title/message from Strings file.
        mActions.checkIsDisplayed(withText(R.string.login_missing_field_title));
        mActions.checkIsDisplayed(withText(R.string.login_missing_field_message));

        // Close dialog
        onView(withText(mActivity.getString(R.string.ok)))
                .perform(click());
    }

    public void testConfigureEmptyMobileNumber_sameActivity() {
        mActions.enterEditTextValueValue(R.id.mobileNumberTextDialog, "");
        mActions.performClick(R.id.button_configure);

        checkForDialogAndDismiss();
    }

    public void testConfigureInvalidPhonenumber_sameActivity() {
        mActions.enterEditTextValueValue(R.id.mobileNumberTextDialog, "notaphone");
        mActions.performClick(R.id.button_configure);

        checkForDialogAndDismiss();
    }

    public void testConfigureSucces_sameActivity() {
        mActions.enterEditTextValueValue(R.id.mobileNumberTextDialog, "0612345678");
        mActions.performClick(R.id.button_configure);

        mActions.checkIsDisplayed(allOf(withId(R.id.title_label), withText(R.string.welcome_text)));
    }
}
