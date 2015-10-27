package org.dnaq.dialer2;

public class Contact {
	
	public final String lookupKey;
	public final String displayName;
	public final String status;
	public final int timesContacted;
	public final long lastTimeContacted;
	public final boolean starred;
	
	public Contact(String lookupKey, String displayName, String status, int timesContacted, long lastTimeContacted, boolean starred) {
		this.lookupKey = lookupKey;
		this.displayName = displayName != null ? displayName : "";
		this.status = status != null ? status : "";
		this.timesContacted = timesContacted;
		this.lastTimeContacted = lastTimeContacted;
		this.starred = starred;
	}
	
//	public static Contact fromCursor(Cursor c) {
//		String lookupKey = c.getString(c.getColumnIndex(Contacts.LOOKUP_KEY));
//		String displayName = c.getString(c.getColumnIndex(Contacts.DISPLAY_NAME));
//		String status = c.getString(c.getColumnIndex(Contacts.CONTACT_STATUS));
//		int timesContacted = c.getInt(c.getColumnIndex(Contacts.TIMES_CONTACTED));
//		long lastTimeContacted = c.getLong(c.getColumnIndex(Contacts.LAST_TIME_CONTACTED));
//		boolean starred = c.getInt(c.getColumnIndex(Contacts.STARRED)) == 1 ? true : false;
//		return new Contact(lookupKey, displayName != null ? displayName : "", status, timesContacted, lastTimeContacted, starred);
//	}
}
