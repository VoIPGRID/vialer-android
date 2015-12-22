package com.voipgrid.vialer;

import android.support.test.InstrumentationRegistry;
import android.test.ActivityInstrumentationTestCase2;

import com.voipgrid.vialer.onboarding.SetupActivity;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.AllOf.allOf;

public class OnBoardingLoginTest extends ActivityInstrumentationTestCase2<SetupActivity> {

    private SetupActivity mActivity;
    private TestActions mActions;

    public static final String EMPTY_VALUE = "";
    // Valid but unknown to backend!
    public static final String VALID_BUT_UNKNOWN_EMAIL_VALUE = "me@example.com";
    public static final String INVALID_EMAIL_VALUE = "notanemail";
    public static final String VALID_AND_KNOWN_EMAIL_VALUE = "test7@peperzaken.nl";
    // Valid but unknown to backend!
    public static final String VALID_BUT_UNKNOWN_PASSWORD_VALUE = "somepassword";
    public static final String VALID_AND_KNOWN_PASSWORD_VALUE = "Peperzaken7";

    public OnBoardingLoginTest() {
        super(SetupActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());
        mActivity = getActivity();

        mActions = new TestActions();
        mActions.logoWaitProceed();
    }

    /**
     * Dialog check and dismiss helper.
     */
    private void checkForLoginDialogAndDismiss() {
        // verify pop up dialog with title/message from Strings file.
        mActions.checkIsDisplayed(withText(R.string.login_missing_field_title));
        mActions.checkIsDisplayed(withText(R.string.login_missing_field_message));

        // Close dialog
        onView(withText(mActivity.getString(R.string.ok)))
                .perform(click());
    }

    /**
     * Verify that we are on the configure page!
     */
    private void verifyProceedToConfigure() {
        mActions.checkIsDisplayed(allOf(withId(R.id.title_label), withText(R.string.configure_header_label_text)));
        mActions.checkIsDisplayed(allOf(withId(R.id.subtitle_label), withText(R.string.configure_header_label_subtext)));
    }

    /**
     * Login all empty.
     */
    public void testLoginEmptyValues_sameActivity() {
        // empty email field.
        mActions.enterEditTextValueValue(R.id.emailTextDialog, EMPTY_VALUE);
        // empty password field.
        mActions.enterEditTextValueValue(R.id.passwordTextDialog, EMPTY_VALUE);

        // click login.
        mActions.performClick(R.id.button_login);

        // verify pop up dialog with title/message from Strings file.
        checkForLoginDialogAndDismiss();
    }

    /**
     * Login password empty.
     */
    public void testLoginEmptyPassword_sameActivity() {
        // enter valid email value.
        mActions.enterEditTextValueValue(R.id.emailTextDialog, VALID_BUT_UNKNOWN_EMAIL_VALUE);
        // empty password field.
        mActions.enterEditTextValueValue(R.id.passwordTextDialog, EMPTY_VALUE);

        // click login.
        mActions.scrollToId(R.id.button_login);
        mActions.performClick(R.id.button_login);

        // verify pop up dialog with title/message from Strings file.
        checkForLoginDialogAndDismiss();
    }

    /**
     * Login e-mail empty.
     */
    public void testLoginEmptyEmail_sameActivity() {
        // enter incorrect email value.
        mActions.enterEditTextValueValue(R.id.emailTextDialog, EMPTY_VALUE);
        // enter correct password value.
        mActions.enterEditTextValueValue(R.id.passwordTextDialog, VALID_BUT_UNKNOWN_PASSWORD_VALUE);

        // click login.
        mActions.performClick(R.id.button_login);

        // verify pop up dialog with title/message from Strings file.
        checkForLoginDialogAndDismiss();
    }

    /**
     * Login all filled but invalid e-mail.
     */
    public void testLoginInvalidEmail_sameActivity() {
        // enter incorrect email value.
        mActions.enterEditTextValueValue(R.id.emailTextDialog, INVALID_EMAIL_VALUE);
        // enter correct password value.
        mActions.enterEditTextValueValue(R.id.passwordTextDialog, VALID_BUT_UNKNOWN_PASSWORD_VALUE);

        // click login.
        mActions.performClick(R.id.button_login);

        // verify pop up dialog with title/message from Strings file.
        checkForLoginDialogAndDismiss();
    }

    /**
     * Login both filled but incorrect.
     */
    public void testLoginIncorrect_sameActivity() {
        // enter valid email value (but unknown to backend).
        mActions.enterEditTextValueValue(R.id.emailTextDialog, VALID_BUT_UNKNOWN_EMAIL_VALUE);
        // enter valid password value (but unknown to backend).
        mActions.enterEditTextValueValue(R.id.passwordTextDialog, VALID_BUT_UNKNOWN_PASSWORD_VALUE);
        // click login.
        mActions.performClick(R.id.button_login);

        // Verify movement to AccountFragment
        verifyProceedToConfigure();

    }

    /**
     * Login both filled and correct.
     */
    public void testLoginCorrect_SameActivity() {
        mActions.correctLogin();

        // Verify movement to AccountFragment
        verifyProceedToConfigure();
    }
}
