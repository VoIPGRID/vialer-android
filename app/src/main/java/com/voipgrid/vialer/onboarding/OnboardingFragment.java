package com.voipgrid.vialer.onboarding;

import android.app.Activity;
import android.app.Fragment;

import com.voipgrid.vialer.R;
import com.voipgrid.vialer.util.ConnectivityHelper;

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

    /**
     * Report the error provided unless there is currently no internet connectivity, then report
     * a no connectivity message.
     *
     * @param title The id of the string resource to use as the dialog title
     * @param error The id of the string resource to use as the dialog message
     */
    public void reportErrorOrConnectivityError(int title, int error) {
        ConnectivityHelper connectivityHelper = ConnectivityHelper.get(getActivity());
        String errorMessage;
        if(!connectivityHelper.hasNetworkConnection()) {
            errorMessage = getString(R.string.onboarding_no_internet_message);
        } else {
            errorMessage = getString(error);
        }
        mListener.onAlertDialog(getString(title), errorMessage);
    }
}
