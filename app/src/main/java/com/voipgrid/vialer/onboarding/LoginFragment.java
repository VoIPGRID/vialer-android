package com.voipgrid.vialer.onboarding;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.voipgrid.vialer.R;
import com.voipgrid.vialer.WebActivity;
import com.voipgrid.vialer.util.ConnectivityHelper;

import androidx.fragment.app.Fragment;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link SetupFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link LoginFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
//public class LoginFragment extends OnboardingFragment implements
//        View.OnClickListener,
//        TextWatcher {
//
//    private FragmentInteractionListener mListener;
//    private EditText mEmailEdittext;
//    private EditText mPasswordEdittext;
//    private Button mLoginButton;
//    private Button mForgotPasswordButton;
//    private Button mInfoButton;
//    private ConnectivityHelper mConnectivityHelper;
//
//    /**
//     * Use this factory method to create a new instance of
//     * this fragment using the provided parameters.
//     *
//     * @return A new instance of fragment LoginFragment.
//     */
//    public static LoginFragment newInstance() {
//        LoginFragment fragment = new LoginFragment();
//        return fragment;
//    }
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//        // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.fragment_login, container, false);
//    }
//
//
//    @Override
//    public void onAttach(Activity activity) {
//        super.onAttach(activity);
//        try {
//            mListener = (FragmentInteractionListener) activity;
//        } catch (ClassCastException e) {
////            throw new ClassCastException(activity.toString()
////                    + " must implement OnFragmentInteractionListener");
//        }
//    }
//
//    @Override
//    public void onDetach() {
//        super.onDetach();
//        mListener = null;
//    }
//
//    @Override
//    public void onClick(View v) {
//        switch(v.getId()) {
//
//
//            case R.id.button_info:
//
//                break;
//        }
//    }
//
//
//    public void onError(String error) {
//        reportErrorOrConnectivityError(R.string.onboarding_login_failed_title, R.string.onboarding_login_failed_message);
//    }
//
//    interface FragmentInteractionListener extends OnboardingFragment.FragmentInteractionListener {
//        void onLogin(Fragment fragment, String username, String password);
//    }
//
//}
