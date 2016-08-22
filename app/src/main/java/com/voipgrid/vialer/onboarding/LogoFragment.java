package com.voipgrid.vialer.onboarding;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.voipgrid.vialer.R;

/**
 * Class that shows a logo when starting the app and dismisses the logo after 1 second.
 * Use the {@link LogoFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LogoFragment extends OnboardingFragment {

    /**
     * Runnable property which is used as a timed task to proceed with the onboarding.
     */
    private Runnable mTouchTask = new Runnable() {
        @Override
        public void run() {
            ready();
        }
    };

    private void ready() {
        // In the onDetach of the super class the mListener is set to null. Because
        // this function is called by a handler in a separate thread the method is not
        // thread safe and null pointers can occur.
        if (mListener != null) {
            mListener.onNextStep(LoginFragment.newInstance());
        }
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment LogoFragment.
     */
    public static LogoFragment newInstance() {
        return new LogoFragment();
    }

    public LogoFragment() {
        // Required empty public constructor.
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment.
        return inflater.inflate(R.layout.fragment_logo, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // And an auto proceed for the one with peace of mind.
        Handler handler = new Handler();

        // Delay stored in onboarding.xml.
        handler.postDelayed(mTouchTask, view.getContext().getResources().getInteger(R.integer.logo_dismiss_delay_ms));
    }

    @Override
    public void onError(String error) {

    }
}
