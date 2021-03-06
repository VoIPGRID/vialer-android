package com.voipgrid.vialer.t9;

import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.tamir7.contacts.Contact;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.util.ColorHelper;
import com.voipgrid.vialer.util.HtmlHelper;

import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class T9HelperFragment extends Fragment  {

    @Inject HtmlHelper htmlHelper;
    @Inject ColorHelper colorHelper;

    private Unbinder mUnbinder;

    @BindView(R.id.help_text) TextView mainText;

    @BindViews({ R.id.digit_1, R.id.digit_2, R.id.digit_3 })
    List<TextView> digitViews;

    @BindViews({ R.id.digit_1_letters, R.id.digit_2_letters, R.id.digit_3_letters })
    List<TextView> digitLetterViews;

    /**
     * Will not attempt to use any name with more than this many
     * characters.
     */
    private static final int NAME_MAX_LENGTH = 15;

    /**
     * Uses this name if there are no suitable entries in the contacts
     * table.
     */
    private static final String DEFAULT_NAME = "Felix";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_t9_helper, container, false);

        mUnbinder = ButterKnife.bind(this, view);
        VialerApplication.get().component().inject(this);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initialize();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
    }

    /**
     * Initialize the fragment with an example name.
     *
     */
    public void initialize() {
        String contactName = getExampleFirstNameToDemonstrateT9();

        initializeMainText(contactName);

        initializeHelperDigitsForName(contactName);
    }

    /**
     * Initialize the main text, inserting in the contact name being used and highlighting the first 3 characters of it.
     *
     * @param name The name to initialize the main text for.
     */
    private void initializeMainText(String name) {
        String highlightedName = htmlHelper.colorSubstring(
                name,
                getHighlightColor(),
                0,
                digitViews.size()
        );

        String helperText = getString(R.string.t9helper_text, highlightedName);

        mainText.setText(Html.fromHtml(helperText));
    }

    /**
     * Initialize the helper digits, displaying the appropriate t9 digit and the corresponding letters
     * with the relevant letter highlighted.
     *
     * @param name The name to initialize the helper digits for.
     */
    private void initializeHelperDigitsForName(String name) {
        String t9QueryForName = T9.INSTANCE.convertResultBackIntoT9Query(name);

        for (int i = 0; i < digitViews.size(); i++) {
            TextView digitView = digitViews.get(i);
            TextView digitLetterView = digitLetterViews.get(i);
            char digit = t9QueryForName.charAt(i);

            digitView.setText(String.valueOf(digit));
            digitLetterView.setText(highlightRelevantLetter(digit, name.charAt(i)));
        }
    }

    /**
     * Formats the digit letter text, coloring the correct letters based on the digit being displayed.
     */
    private Spanned highlightRelevantLetter(char digitBeingDisplayed, char letterToHighlight) {
        String letters = T9.INSTANCE.getLettersThatMapToKey(digitBeingDisplayed);

        int positionOfLetterToHighlight = letters.indexOf(String.valueOf(letterToHighlight).toLowerCase());

        letters = htmlHelper.colorSubstring(
                letters.toUpperCase(),
                getHighlightColor(),
                positionOfLetterToHighlight,
                positionOfLetterToHighlight + 1
        );

        return Html.fromHtml(letters);
    }

    /**
     * Chooses a random name from the contacts database, if no appropriate name can be found, the {@link #DEFAULT_NAME}
     * is used. An appropriate name is a name with at least 3 characters where the first 3 characters appear on the dialpad (i.e. a-z).
     *
     * @return An example first name to use in the helper
     */
    private String getExampleFirstNameToDemonstrateT9() {
        List<Contact> contacts = ContactsSearcher.Companion.getContacts();

        if (contacts.isEmpty()) {
            return DEFAULT_NAME;
        }

        String contactName = contacts.get(new Random().nextInt(contacts.size())).getDisplayName();

        String[] split = contactName.split(" ");

        if (split.length != 1) contactName = split[0];

        if (contactName.length() < 3 ) {
            return DEFAULT_NAME;
        }

        Pattern p = Pattern.compile("[^a-zA-Z]");

        if (p.matcher(contactName).find()) {
            return DEFAULT_NAME;
        }

        if (contactName.length() > NAME_MAX_LENGTH) {
            return DEFAULT_NAME;
        }

        return contactName;
    }

    public void hide() {
        if (getView() == null) return;

        getView().setVisibility(View.GONE);
    }

    public void show() {
        if (getView() == null) return;

        getView().setVisibility(View.VISIBLE);
    }

    private String getHighlightColor() {
        return colorHelper.getColorResourceAsHexCode(R.color.color_primary);
    }
}
