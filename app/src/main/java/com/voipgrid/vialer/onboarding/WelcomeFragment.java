package com.voipgrid.vialer.onboarding;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.voipgrid.vialer.R;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link SetupFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link WelcomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WelcomeFragment extends OnboardingFragment implements View.OnClickListener {

    public static final String ARG_NAME = "caller-name";

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.

     * @return A new instance of fragment WelcomeFragment.
     * @param name the full_name of the user which comes from its account.
     */
    public static WelcomeFragment newInstance(String name) {
        WelcomeFragment fragment = new WelcomeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_NAME, name);
        fragment.setArguments(args);
        return fragment;
    }

    public WelcomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_welcome, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle arguments = getArguments();
        if (arguments != null) {
            ((TextView) view.findViewById(R.id.subtitle_label))
                    .setText(arguments.getString(ARG_NAME));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        View view = getView();
        if (view != null) {
            View startButton = view.findViewById(R.id.button_welcome);
            startButton.setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View view) {
        mListener.onFinish(this);
    }

    @Override
    public void onError(String error) {

    }
}
