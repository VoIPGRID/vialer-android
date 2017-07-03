package com.voipgrid.vialer.onboarding;

import android.app.Activity;
import android.app.Fragment;

/**
 * Superclass used by fragments in the onboarding. A FragmentInteractionListener is implemented
 * To provide the flow in the onboarding.
 */
public abstract class OnboardingFragment extends Fragment {

    interface FragmentInteractionListener {
        void onNextStep(Fragment fragment);
        void onFinish(Fragment fragment);
        void onAlertDialog(String title, String message);
    }

    /**
     * Interaction listener for communication with the controlling activity.
     */
    protected FragmentInteractionListener mListener;

    public abstract void onError(String error);

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (FragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement ISetupFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}
