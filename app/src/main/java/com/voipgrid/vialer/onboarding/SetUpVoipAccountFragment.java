package com.voipgrid.vialer.onboarding;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.voipgrid.vialer.R;
import com.voipgrid.vialer.api.Api;
import com.voipgrid.vialer.api.ServiceGenerator;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.api.models.SystemUser;
import com.voipgrid.vialer.util.JsonStorage;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * A simple {@link Fragment} subclass.
 */
public class SetUpVoipAccountFragment extends OnboardingFragment implements
        View.OnClickListener, Callback {

    private Api mApi;
    private Context mContext;
    private FragmentInteractionListener mListener;
    private JsonStorage mJsonStorage;
    private SystemUser mSystemUser;

    private Button mVoipAccountButton;
    private Button mCancelButton;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment SetUpVoipAccountFragment.
     */
    public static SetUpVoipAccountFragment newInstance() {
        SetUpVoipAccountFragment fragment = new SetUpVoipAccountFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_voip_account_missing, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mContext = getActivity().getApplicationContext();

        mJsonStorage = new JsonStorage(mContext);

        mSystemUser = (SystemUser) mJsonStorage.get(SystemUser.class);
        mApi = ServiceGenerator.createService(
                getActivity(),
                Api.class,
                getString(R.string.api_url),
                mSystemUser.getEmail(),
                mSystemUser.getPassword()
        );

        mVoipAccountButton = (Button) view.findViewById(R.id.set_voip_account_button);
        mVoipAccountButton.setOnClickListener(this);

        mCancelButton = (Button) view.findViewById(R.id.cancel_set_voip_account_button);
        mCancelButton.setOnClickListener(this);
    }

    @Override
    public void onError(String error) {

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (FragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.set_voip_account_button:
                mListener.onSetVoipAccount(this);
                break;
            case R.id.cancel_set_voip_account_button:
                mListener.onFinish(this);
                break;
        }
    }

    @Override
    public void onResume() {
        refreshObjects();
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResponse(Call call, Response response) {
        if (response.body() instanceof SystemUser) {
            SystemUser systemUser = ((SystemUser) response.body());

            // Update existing systemuser to avoid overriding values like password.
            SystemUser currentSystemuser = mSystemUser;
            currentSystemuser.setOutgoingCli(systemUser.getOutgoingCli());
            currentSystemuser.setMobileNumber(systemUser.getMobileNumber());
            currentSystemuser.setClient(systemUser.getClient());
            currentSystemuser.setAppAccountUri(systemUser.getAppAccountUri());

            mJsonStorage.save(currentSystemuser);
            mSystemUser = currentSystemuser;
            // Check if a account has been set.
            String phoneAccountId = mSystemUser.getPhoneAccountId();
            if (phoneAccountId != null) {
                // Request new phoneaccount object.
                Call<PhoneAccount> apicall = mApi.phoneAccount(phoneAccountId);
                apicall.enqueue(this);
            } else {
                // User has no phone account linked so remove it from the local storage.
                mJsonStorage.remove(PhoneAccount.class);
            }
        } else if (response.body() instanceof PhoneAccount) {
            mJsonStorage.save(response.body());
            mListener.onFinish(this);
        }
    }

    @Override
    public void onFailure(Call call, Throwable t) {
    }

    private void requestSystemUser() {
        mApi = ServiceGenerator.createService(
                mContext,
                Api.class,
                getString(R.string.api_url),
                mSystemUser.getEmail(),
                mSystemUser.getPassword()
        );
        Call<SystemUser> call = mApi.systemUser();
        call.enqueue(this);
    }

    private void refreshObjects() {
        // Request new systemUser object because it
        // might have been changed in the webactivity.
        requestSystemUser();
    }

    public interface FragmentInteractionListener extends OnboardingFragment.FragmentInteractionListener {
        void onSetVoipAccount(Fragment fragment);
    }

}
