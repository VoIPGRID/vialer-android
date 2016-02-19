package com.voipgrid.vialer.callrecord;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.voipgrid.vialer.R;
import com.voipgrid.vialer.api.models.CallRecord;
import com.voipgrid.vialer.util.IconHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Adapter to display the call records
 */
public class CallRecordAdapter extends BaseAdapter {

    private List<CallRecord> mCallRecords;

    private Context mContext;

    /**
     * Construct a new CallRecordAdapter
     * @param context
     * @param callRecords
     */
    public CallRecordAdapter(Context context, List<CallRecord> callRecords) {
        mContext = context;
        mCallRecords = callRecords;
    }

    /**
     * Set call records to the adapter
     * @param callRecords
     */
    public void setCallRecords(List<CallRecord> callRecords) {
        mCallRecords = callRecords;
        notifyDataSetChanged();
    }

    /**
     * Add call records to the adapter
     * @param callRecords
     */
    public void addCallRecords(List<CallRecord> callRecords) {
        mCallRecords.addAll(callRecords);
        notifyDataSetChanged();
    }


    @Override
    public int getCount() {
        return mCallRecords.size();
    }

    @Override
    public CallRecord getItem(int position) {
        return mCallRecords.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;

        if(convertView == null) {

            // inflate the layout
            LayoutInflater inflater = ((Activity)mContext).getLayoutInflater();
            convertView = inflater.inflate(R.layout.list_item_call_record, parent, false);

            Bitmap bitmapImage = IconHelper.getCallerIconBitmap("", Color.BLUE);

            View photoView = convertView.findViewById(R.id.text_view_contact_icon);

            ((CircleImageView) photoView).setImageBitmap(bitmapImage);
            // well set up the ViewHolder
            viewHolder = new ViewHolder();
            viewHolder.title = (TextView) convertView.findViewById(R.id.text_view_contact_name);
            viewHolder.information = (TextView) convertView.findViewById(
                    R.id.text_view_contact_information);

            // store the holder with the view.
            convertView.setTag(viewHolder);
        } else {
            // we've just avoided calling findViewById() on resource everytime
            // just use the viewHolder
            viewHolder = (ViewHolder) convertView.getTag();
        }

        // get the call record
        CallRecord callRecord = getItem(position);

        if(callRecord != null) {
            // default resource for direction.
            int resource = 0;

            // get the direction from the call record
            String direction = callRecord.getDirection();

            // set the drawable resource
            if(direction.equals(CallRecord.DIRECTION_OUTBOUND)) {
                viewHolder.title.setText(callRecord.getDialedNumber());
                resource = R.drawable.ic_outgoing;
            } else if(direction.equals(CallRecord.DIRECTION_INBOUND)) {
                viewHolder.title.setText(callRecord.getCaller());
                if(callRecord.getDuration() == 0) {
                    resource = R.drawable.ic_incoming_missed;
                } else {
                    resource = R.drawable.ic_incoming;
                }
            }

            // set the compound drawable to the view
            viewHolder.information.setCompoundDrawablesWithIntrinsicBounds(resource, 0, 0, 0);

            // format the date
            SimpleDateFormat dateFormat = new SimpleDateFormat(CallRecord.DATE_FORMAT);
            Date date = null;
            try {
                date = dateFormat.parse(callRecord.getCallDate());
            } catch (ParseException e) {
                e.printStackTrace();
            }

            // set the call record date information to the view
            viewHolder.information.setText(DateUtils.getRelativeDateTimeString(
                    mContext, date.getTime(), DateUtils.SECOND_IN_MILLIS,
                    DateUtils.YEAR_IN_MILLIS, DateUtils.FORMAT_ABBREV_TIME));
        }

        return convertView;
    }

    static class ViewHolder {
        TextView title;
        TextView information;
    }
}
