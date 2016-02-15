package com.voipgrid.vialer.twostepcall;

import android.content.Context;

import com.voipgrid.vialer.R;
import com.voipgrid.vialer.api.models.TwoStepCallStatus;

/**
 * Class for util functions needed for the two step process.
 */
public class TwoStepCallUtils {
    public static final String STATE_INITIAL = "initial";
    public static final String STATE_CALLING_A = "calling-a";
    public static final String STATE_CALLING_B = "calling-b";
    public static final String STATE_FAILED_A = "failed-a";
    public static final String STATE_FAILED_B = "failed-b";
    public static final String STATE_CONNECTED = "connected";
    public static final String STATE_DISCONNECTED = "disconnected";
    public static final String STATE_CANCELLING = "cancelling";
    public static final String STATE_CANCELLED = "cancelled";

    /**
     * Function to get the state based on the state returned in API.
     * @param apiState
     * @return The internal two step state.
     */
    public static String getViewState(String apiState) {
        if(apiState != null) {
            switch (apiState) {
                case TwoStepCallStatus.STATE_CALLING_A:
                    return TwoStepCallUtils.STATE_CALLING_A;
                case TwoStepCallStatus.STATE_CALLING_B:
                    return TwoStepCallUtils.STATE_CALLING_B;
                case TwoStepCallStatus.STATE_FAILED_A:
                    return TwoStepCallUtils.STATE_FAILED_A;
                case TwoStepCallStatus.STATE_FAILED_B:
                    return TwoStepCallUtils.STATE_FAILED_B;
                case TwoStepCallStatus.STATE_CONNECTED:
                    return TwoStepCallUtils.STATE_CONNECTED;
                case TwoStepCallStatus.STATE_DISCONNECTED:
                    return TwoStepCallUtils.STATE_DISCONNECTED;
            }
        }
        return TwoStepCallUtils.STATE_INITIAL;
    }

    /**
     * Function to get the user text for a certain two step state.
     * @param context Needed for getResources.
     * @param state State we want the text for.
     * @return Text for given state.
     */
    public static String getStateText(Context context, String state) {
        if(state != null) {
            int resource = 0;
            switch (state) {
                case TwoStepCallUtils.STATE_CALLING_A:
                    resource = R.string.two_step_call_state_dialing_a; break;
                case TwoStepCallUtils.STATE_CALLING_B:
                    resource =  R.string.two_step_call_state_dialing_b; break;
                case TwoStepCallUtils.STATE_FAILED_A:
                    resource =  R.string.two_step_call_state_failed_a; break;
                case TwoStepCallUtils.STATE_FAILED_B:
                    resource =  R.string.two_step_call_state_failed_b; break;
                case TwoStepCallUtils.STATE_CONNECTED:
                    resource =  R.string.two_step_call_state_connected; break;
                case TwoStepCallUtils.STATE_DISCONNECTED:
                    resource =  R.string.two_step_call_state_disconnected; break;
                case TwoStepCallUtils.STATE_CANCELLING:
                    resource = R.string.two_step_call_state_cancelling; break;
                case TwoStepCallUtils.STATE_CANCELLED:
                    resource = R.string.two_step_call_state_cancelled; break;
            }

            if(resource > 0) {
                return context.getResources().getString(resource);
            }
        }
        return null;
    }
}
