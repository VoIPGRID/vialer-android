package com.voipgrid.vialer.contacts;

import java.util.ArrayList;
import java.util.List;

/**
 * Class for storing all information needed for the contact to be synced.
 */
public class SyncContact {
    private long mContactId;
    private String mLookupKey;
    private String mDisplayName;
    private String mThumbnailUri;

    private List<SyncContactNumber> mNumbers;


    public SyncContact(long contactId, String lookupKey, String displayName, String thumbnailUri) {
        setContactId(contactId);
        setLookupKey(lookupKey);
        setDisplayName(displayName);
        setThumbnailUri(thumbnailUri);
        mNumbers = new ArrayList<>();
    }

    public long getContactId() {
        return mContactId;
    }

    private void setContactId(long contactId) {
        this.mContactId = contactId;
    }

    public String getLookupKey() {
        return mLookupKey;
    }

    private void setLookupKey(String lookupKey) {
        // Some lookup keys are to large and can cause memory allocation problems.
        if (lookupKey.length() < 1000) {
            this.mLookupKey = lookupKey;
        } else {
            this.mLookupKey = null;
        }
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    private void setDisplayName(String displayName) {
        this.mDisplayName = displayName;
    }

    public String getThumbnailUri() {
        return this.mThumbnailUri;
    }

    private void setThumbnailUri(String thumbnailUri) {
        this.mThumbnailUri = thumbnailUri;
    }

    public void addNumber(SyncContactNumber syncContactNumber) {
        // We do not want duplicates.
        if (!mNumbers.contains(syncContactNumber)) {
            mNumbers.add(syncContactNumber);
        }
    }

    public List<SyncContactNumber> getNumbers() {
        return mNumbers;
    }
}
