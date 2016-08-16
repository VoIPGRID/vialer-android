package com.voipgrid.vialer.onboarding;

import android.app.Fragment;
import android.content.Context;

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
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (FragmentInteractionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement ISetupFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}
