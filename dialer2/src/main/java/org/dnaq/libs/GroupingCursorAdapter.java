package org.dnaq.libs;

import android.content.Context;
import android.database.Cursor;
import android.widget.CursorAdapter;

public abstract class GroupingCursorAdapter extends CursorAdapter {

    public static final int ORDINARY_VIEW = 0;
    public static final int GROUPED_VIEW = 1;
    public static final int VIEW_TYPE_COUNT = 2;

    private int mGroupColumnId;
    private String mGroupColumnName;
    
    public GroupingCursorAdapter(Context context, Cursor c, String groupColumnName) {
        super(context, c);
        mGroupColumnId = 0; // not used in this case
        mGroupColumnName = groupColumnName;
    }
    
    public GroupingCursorAdapter(Context context, Cursor c, int groupColumnId) {
        super(context, c);
        mGroupColumnId = groupColumnId;
        mGroupColumnName = null;
    }
    
    public GroupingCursorAdapter(Context context, Cursor c, boolean autoRequery, String groupColumnName) {
        super(context, c, autoRequery);
        mGroupColumnId = 0; // not used in this case
        mGroupColumnName = groupColumnName;
    }
    public GroupingCursorAdapter(Context context, Cursor c, boolean autoRequery, int groupColumnId) {
        super(context, c, autoRequery);
        mGroupColumnId = groupColumnId;
        mGroupColumnName = null;
    }

    @Override
    public int getItemViewType(int position) {
        Cursor c = getCursor();
        c.moveToPosition(position);
        if (shouldBeGrouped(c)) {
            return GROUPED_VIEW;
        } else {
            return ORDINARY_VIEW;
        }
    }

    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }

    
    public boolean shouldBeGrouped(Cursor cursor) {
	    if (cursor.isFirst()) {
	        return false;
	    }
	    
	    int columnId = mGroupColumnName != null ? cursor.getColumnIndex(mGroupColumnName) : mGroupColumnId;
	    
	    cursor.moveToPrevious();
	    String prevGroupColumn = cursor.getString(columnId);
	    cursor.moveToNext();
	    String lookupKey = cursor.getString(columnId);
	    
	    if (prevGroupColumn != null && prevGroupColumn.equals(lookupKey)) {
	        return true;
	    }
	    
	    return false;
	}
}
