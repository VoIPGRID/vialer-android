package com.voipgrid.vialer;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.TextView;

import org.dnaq.dialer2.AsyncContactImageLoader;
import org.dnaq.libs.GroupingCursorAdapter;

/**
 * ContactsAdater used to show T9 Dialpad search results
 */
public class ContactsAdapter extends GroupingCursorAdapter {

    public static final String[] PROJECTION = new String[] { Phone._ID, Phone.LOOKUP_KEY, Phone.DISPLAY_NAME, Phone.LAST_TIME_CONTACTED, Phone.NUMBER };

    private AsyncContactImageLoader mAsyncContactImageLoader;

    public ContactsAdapter(Context context, Cursor cursor, AsyncContactImageLoader asyncContactImageLoader) {
        super(context, cursor, Phone.LOOKUP_KEY);
        mAsyncContactImageLoader = asyncContactImageLoader;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        if (shouldBeGrouped(cursor)) { // we just hide the grouped views
            View view = new View(context);
            /**
             * Hackz0r: when cursor should group, give view width, height 0. Hence make invisible.
             * TODO: improve this to a chooser dialog of some sort to choose between available numbers.
             * See: VIALA-225
             */
            view.setLayoutParams(new AbsListView.LayoutParams(0, 0));
            return view;
        }
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_contact, null);
        ViewHolder viewCache = new ViewHolder(view);
        view.setTag(viewCache);
        return view;
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        final ViewHolder viewHolder = (ViewHolder) view.getTag();
        if (viewHolder == null) { // empty view has no viewcache, and we do
            // nothing with it
            return;
        }
        String displayName = cursor.getString(cursor.getColumnIndex(Phone.DISPLAY_NAME));

        //set contact name
        viewHolder.nameTextView.setText(displayName);

        //set contact information
        long lastTimeContacted = cursor.getLong(cursor.getColumnIndex(Phone.LAST_TIME_CONTACTED));
        if (lastTimeContacted == 0) {
            viewHolder.informationTextView.setText(null);
        } else {
            viewHolder.informationTextView.setText(DateUtils.getRelativeTimeSpanString(lastTimeContacted));
        }

        String number = cursor.getString(cursor.getColumnIndex(Phone.NUMBER));
        viewHolder.informationTextView.setText(number);

        //set default contact image
        viewHolder.imageView.setImageDrawable(context.getResources().getDrawable(R.drawable.bg_contact_item_icon));

        //load contact image
        String lookupKey = cursor.getString(cursor.getColumnIndex(Phone.LOOKUP_KEY));
        if (lookupKey != null) {
            viewHolder.imageView.setTag(lookupKey);
            mAsyncContactImageLoader.loadDrawableForContact(lookupKey, new AsyncContactImageLoader.ImageCallback() {
                @Override
                public void imageLoaded(Drawable imageDrawable, String lookupKey) {
                    if (lookupKey.equals(viewHolder.imageView.getTag())) {
                        if (imageDrawable instanceof RoundedBitmapDrawable) {
                            ((RoundedBitmapDrawable) imageDrawable).setCornerRadius(context.getResources().getDimensionPixelSize(R.dimen.contact_icon_width));
                        }
                        viewHolder.imageView.setImageDrawable(imageDrawable);
                    }
                }
            });
        }
    }

    private class ViewHolder {
        public final TextView nameTextView;
        public final TextView informationTextView;
        public final ImageView imageView;

        public ViewHolder(View view) {
            nameTextView = (TextView) view.findViewById(R.id.text_view_contact_name);
            informationTextView = (TextView) view.findViewById(R.id.text_view_contact_information);
            imageView = (ImageView) view.findViewById(R.id.text_view_contact_icon);
        }
    }
}
