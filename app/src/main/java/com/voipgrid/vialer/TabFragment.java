package com.voipgrid.vialer;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * TabFragment for use in MainActivities ViewPager
 */
public class TabFragment extends Fragment {

    private static final String TAB_TITLE = "arg-tab-title";

    private String mTitle;

    public TabFragment() {

    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param title to display as placeholder text in the Fragment
     * @return TabFragment
     */
    public static TabFragment newInstance(String title) {
        TabFragment tabFragment = new TabFragment();
        Bundle arguments = new Bundle();
        arguments.putString(TAB_TITLE, title);
        tabFragment.setArguments(arguments);
        return tabFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTitle = getArguments().getString(TAB_TITLE);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tab, null);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        TextView textView = (TextView) view.findViewById(R.id.text_view);
        textView.setText(mTitle);

        super.onViewCreated(view, savedInstanceState);
    }
}
