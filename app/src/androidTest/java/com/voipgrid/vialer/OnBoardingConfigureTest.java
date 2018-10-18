package com.voipgrid.vialer;

import androidx.test.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import com.voipgrid.vialer.onboarding.SetupActivity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.AllOf.allOf;

@RunWith(AndroidJUnit4.class)
public class OnBoardingConfigureTest {

    @Rule
    public ActivityTestRule<SetupActivity> mActivityTestRule = new ActivityTestRule<>(SetupActivity.class);

    private  SetupActivity mActivity;
    private TestActions mActions;

    @Before
    public void setUp() {
        mActivity = mActivityTestRule.getActivity();

        mActions = new TestActions();
        mActions.logoWaitProceed();
        mActions.correctLogin();
    }

    @Test
    private void checkForDialogAndDismiss() {
        // verify pop up dialog with title/message from Strings file.
        mActions.checkIsDisplayed(withText(R.string.login_missing_field_title));
        mActions.checkIsDisplayed(withText(R.string.login_missing_field_message));

        // Close dialog
        onView(withText(mActivity.getString(R.string.ok)))
                .perform(click());
    }

    @Test
    public void testConfigureEmptyMobileNumber_sameActivity() {
        mActions.enterEditTextValueValue(R.id.mobileNumberTextDialog, "");
        mActions.performClick(R.id.button_configure);

        checkForDialogAndDismiss();
    }

    @Test
    public void testConfigureInvalidPhonenumber_sameActivity() {
        mActions.enterEditTextValueValue(R.id.mobileNumberTextDialog, "notaphone");
        mActions.performClick(R.id.button_configure);

        checkForDialogAndDismiss();
    }

    @Test
    public void testConfigureSucces_sameActivity() {
        mActions.enterEditTextValueValue(R.id.mobileNumberTextDialog, "0612345678");
        mActions.performClick(R.id.button_configure);

        mActions.checkIsDisplayed(allOf(withId(R.id.title_label), withText(R.string.welcome_text)));
    }
}
