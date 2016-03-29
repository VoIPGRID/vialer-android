package com.voipgrid.vialer.t9;

/**
 * Class that contains information about the match found for the t9 search.
 */
public class T9Match {
    private long mContactId;
    private String mLookupKey;
    private String mDisplayName;
    private String mThumbnailUri;
    private String mNumber;
    private int mType;
    private String mLabel;

    private String mT9Query;

    public T9Match(long contactId, String lookupKey, String displayName, String thumbnailUri,
                   String number, int type, String label, String t9Query) {
        setContactId(contactId);
        setLookupKey(lookupKey);
        setDisplayName(displayName);
        setThumbnailUri(thumbnailUri);
        setNumber(number);
        setType(type);
        setLabel(label);
        setT9Query(t9Query);
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
        this.mLookupKey = lookupKey;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    private void setDisplayName(String displayName) {
        this.mDisplayName = displayName;
    }

    public String getNumber() {
        return mNumber;
    }

    private void setNumber(String number) {
        this.mNumber = number;
    }

    public String getThumbnailUri() {
        return this.mThumbnailUri;
    }

    private void setThumbnailUri(String thumbnailUri) {
        this.mThumbnailUri = thumbnailUri;
    }

    public String getT9Query() {
        return mT9Query;
    }

    private void setT9Query(String t9Query) {
        this.mT9Query = t9Query;
    }

    public int getType() {
        return mType;
    }

    private void setType(int type) {
        this.mType = type;
    }

    public String getLabel() {
        return mLabel;
    }

    private void setLabel(String label) {
        this.mLabel = label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        T9Match t9Match = (T9Match) o;

        if (mContactId != t9Match.mContactId) return false;
        if (!mDisplayName.equals(t9Match.mDisplayName)) return false;
        return mNumber.equals(t9Match.mNumber);

    }

    @Override
    public int hashCode() {
        int result = (int) (mContactId ^ (mContactId >>> 32));
        result = 31 * result + mDisplayName.hashCode();
        result = 31 * result + mNumber.hashCode();
        return result;
    }
}
