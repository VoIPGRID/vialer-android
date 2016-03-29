package com.voipgrid.vialer.contacts;

/**
 * Class for storing number information for a contact.
 */
public class SyncContactNumber {

    private long mDataId;
    private String mNumber;
    private int mType;
    private String mLabel;

    public SyncContactNumber(long dataId, String number, int type, String label) {
        setDataId(dataId);
        setNumber(number);
        setType(type);
        setLabel(label);
    }

    public long getDataId() {
        return mDataId;
    }

    public void setDataId(long dataId) {
        this.mDataId = dataId;
    }

    public String getLabel() {
        return mLabel;
    }

    public void setLabel(String label) {
        this.mLabel = label;
    }

    public String getNumber() {
        return mNumber;
    }

    public void setNumber(String number) {
        this.mNumber = number;
    }

    public int getType() {
        return mType;
    }

    public void setType(int type) {
        this.mType = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SyncContactNumber that = (SyncContactNumber) o;

        return mNumber.equals(that.mNumber);
    }

    @Override
    public int hashCode() {
        return mNumber.hashCode();
    }
}
