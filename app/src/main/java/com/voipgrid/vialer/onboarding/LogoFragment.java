package com.voipgrid.vialer.onboarding;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.voipgrid.vialer.R;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link SetupFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link LogoFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LogoFragment extends OnboardingFragment implements View.OnTouchListener {

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
        mListener.onNextStep(LoginFragment.newInstance());
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
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_logo, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // And an auto proceed for the one with peace of mind.
        Handler handler = new Handler();

        // delay stored in onboarding.xml
        handler.postDelayed(mTouchTask, view.getContext().getResources().getInteger(R.integer.logo_dismiss_delay_ms));

        // set OnTouchListener
        view.findViewById(R.id.fragment_logo_root).setOnTouchListener(this);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
            ready();
            return true;
        }
        return false;
    }

    @Override
    public void onError(String error) {

    }
}
