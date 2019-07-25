package com.voipgrid.vialer.t9;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.voipgrid.vialer.R;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.contacts.Contacts;
import com.voipgrid.vialer.permissions.ContactsPermission;

import javax.inject.Inject;

import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class T9Fragment extends Fragment implements AbsListView.OnScrollListener,
        AdapterView.OnItemClickListener,
        LoaderManager.LoaderCallbacks<Cursor> {

    @Inject T9ViewBinder viewBinder;
    @Inject Contacts mContacts;

    @BindView(R.id.list_view) ListView t9SearchResults;
    @BindView(R.id.no_contact_permission_warning) View mNoContactPermissionWarning;
    @BindView(R.id.permission_contact_description) TextView mPermissionContactDescription;
    @BindView(R.id.message) TextView mEmptyView;

    private T9HelperFragment helper;
    private SimpleCursorAdapter mContactsAdapter = null;
    private String t9Query;
    private Unbinder mUnbinder;
    private boolean mHasPermission;
    private boolean mAskForPermission;
    private Listener listener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_t9_search, container, false);

        mUnbinder = ButterKnife.bind(this, view);
        VialerApplication.get().component().inject(this);
        mPermissionContactDescription.setText(getString(R.string.permission_contact_description, getString(R.string.app_name)));
        mHasPermission = ContactsPermission.hasPermission(getActivity());
        mAskForPermission = true;
        helper = new T9HelperFragment();
        getChildFragmentManager().beginTransaction().add(R.id.helper, helper).commit();

        if (mHasPermission) {
            setupContactsListView();
        } else {
            mNoContactPermissionWarning.setVisibility(View.VISIBLE);
            helper.hide();
            t9SearchResults.setVisibility(View.GONE);
        }

        clearContactList();

        return view;
    }

    public void onResume() {
        // Permission changed since last accessing this Activity.
        super.onResume();
        if (mHasPermission != ContactsPermission.hasPermission(getActivity())) {
            // Force a recreate of the Activity to reflect the new permission.
            Intent intent = getActivity().getIntent();
            startActivity(intent);
            getActivity().finish();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
    }

    public void search(String query) {
        this.t9Query = query;

        if (query.length() == 0) {
            clearContactList();
        } else {
            helper.hide();
            getActivity().getSupportLoaderManager().restartLoader(1, null, this).forceLoad();
        }
    }

    /**
     * Function to clear the contact list in the dialer.
     */
    private void clearContactList() {
        if (!ContactsPermission.hasPermission(getActivity())) {
            mNoContactPermissionWarning.setVisibility(View.VISIBLE);
            return;
        }

        if (mContactsAdapter != null) {
            mContactsAdapter.swapCursor(null);
            mContactsAdapter.notifyDataSetChanged();
        }

        helper.show();
        t9SearchResults.setVisibility(View.GONE);
        mEmptyView.setVisibility(View.GONE);
        mNoContactPermissionWarning.setVisibility(View.GONE);
        mEmptyView.setText("");
    }

    /**
     * Function to setup the listview and cursor adapter.
     */
    private void setupContactsListView() {
        mContactsAdapter = new SimpleCursorAdapter(
                getActivity().getBaseContext(),
                R.layout.list_item_contact,
                null,
                new String[]{"name", "photo", "number", "type"},
                new int[]{
                        R.id.text_view_contact_name,
                        R.id.text_view_contact_icon,
                        R.id.text_view_contact_information,
                        R.id.text_view_contact_type
                },
                0
        );

        mContactsAdapter.setViewBinder(viewBinder);

        t9SearchResults.setOnScrollListener(this);
        t9SearchResults.setOnItemClickListener(this);
        t9SearchResults.setAdapter(mContactsAdapter);
        t9SearchResults.setEmptyView(mEmptyView);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        this.listener.onContactSelected(
                ((TextView) view.findViewById(R.id.text_view_contact_information)).getText().toString(),
                ((TextView) view.findViewById(R.id.text_view_contact_name)).getText().toString()
        );
    }

    @OnClick(R.id.give_contact_permission_button)
    void openAndroidApplicationDetails() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
        intent.setData(uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivityForResult(intent, 1);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (mContactsAdapter == null) {
            return;
        }

        // Set new data on adapter and notify data changed.
        mContactsAdapter.swapCursor(data);
        mContactsAdapter.notifyDataSetChanged();

        // Set empty to no contacts found when result is empty.
        if (data != null && data.getCount() == 0) {
            mEmptyView.setText(R.string.dialer_no_contacts_found_message);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        clearContactList();
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (scrollState == SCROLL_STATE_TOUCH_SCROLL) {
            this.listener.onExpandRequested();
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
            int totalItemCount) {

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Create loader and set t9Query to load for.
        ContactCursorLoader loader = new ContactCursorLoader(getActivity());
        loader.setT9Query(t9Query);
        return loader;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void hide() {
        getView().setVisibility(View.GONE);
    }

    public void show() {
        getView().setVisibility(View.VISIBLE);
    }

    public interface Listener {
        void onExpandRequested();

        void onContactSelected(String number, String name);
    }
}
