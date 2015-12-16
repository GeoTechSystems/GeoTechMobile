/**
 * Handles Date and Time.
 *  
 * @author Torsten Hoch
 */

package de.geotech.systems.utilities;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.text.InputType;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DateTime {
	private static final String CLASSTAG = "DateTime";

	private Context context;
	private boolean withTime;
	private EditText year;
	private EditText month;
	private EditText day;
	private EditText hour;
	private EditText minute;
	private EditText second;
	private String attributeTag;

	public DateTime(Context c, boolean time, Date ourUsedDate) {
		// Log.v(CLASSTAG, "Creating new DateTimeView...");
		context 	= c;
		withTime	= time;
		year		= new EditText(context);
		month		= new EditText(context);
		day			= new EditText(context);
		hour		= new EditText(context);
		minute		= new EditText(context);
		second		= new EditText(context);
		if (ourUsedDate.getTime() < 24*60*60*1000) {

		} else {
			SimpleDateFormat sdf 	= new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			String[] 	dateAndTime = (sdf.format(ourUsedDate)).split("T");
			String[]	dateString 	= dateAndTime[0].split("-");
			String[]	timeString	= dateAndTime[1].split(":");
			year.setText(dateString[0]);
			month.setText(dateString[1]);
			day.setText(dateString[2]);
			hour.setText(timeString[0]);
			minute.setText(timeString[1]);
			second.setText(timeString[2]);
		}
		int inputType = InputType.TYPE_CLASS_NUMBER;
		year.setEms(4);
		year.setInputType(inputType);
		month.setEms(2);
		month.setInputType(inputType);
		day.setEms(2);
		day.setInputType(inputType);
		hour.setEms(2);
		hour.setInputType(inputType);
		minute.setEms(2);
		minute.setInputType(inputType);
		second.setEms(2);
		second.setInputType(inputType);
		attributeTag = "";
	}

	/**
	 * Default constructor.
	 * @param c
	 * @param time
	 */
	public DateTime(Context c, boolean time) {
		this(c, time, new Date(System.currentTimeMillis()));
	}

	public LinearLayout getView() {
		// Log.v(CLASSTAG, "Returning view...");
		LinearLayout wrapper = new LinearLayout(context);
		wrapper.setOrientation(LinearLayout.HORIZONTAL);
		wrapper.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		wrapper.addView(year);
		wrapper.addView(getSeperator("-"));
		wrapper.addView(month);
		wrapper.addView(getSeperator("-"));
		wrapper.addView(day);
		if (withTime) {
			wrapper.addView(getSeperator("   "));
			wrapper.addView(hour);
			wrapper.addView(getSeperator(":"));
			wrapper.addView(minute);
			wrapper.addView(getSeperator(":"));
			wrapper.addView(second);
		}
		return wrapper;
	}

	private TextView getSeperator(String sep) {
		TextView seperator = new TextView(context);
		seperator.setText(sep);
		return seperator;
	}

	@Override
	public String toString() {
		return createCurrentTag();
	}
	
	
//	@Override
//	public String toString() {
//		String result = year.getEditableText().toString() + "-" +
//				month.getEditableText().toString() + "-" +
//				day.getEditableText().toString();
//		if (withTime)
//			result = result + "T" +
//					hour.getEditableText().toString() + ":" +
//					minute.getEditableText().toString() + ":" +
//					second.getEditableText().toString();
//		return result;
//	}

	public void setTag(String tag) {
		attributeTag = tag;
	}

	public String getTag() {
		return attributeTag;
	}

	public String getDateAsString() {
		return createCurrentTag();
	}
	
	private String createCurrentTag() {
		boolean noError = true;
		String dateString = null;
		try {
			if (!(Integer.valueOf(year.getText().toString()) < 1970 || Integer.valueOf(year.getText().toString()) > 3000)) {
				noError = false;
			} else if (!(Integer.valueOf(month.getText().toString()) > 1 || Integer.valueOf(month.getText().toString()) < 12)) {
				noError = false;
			} else  if (!(Integer.valueOf(day.getText().toString()) > 1 || Integer.valueOf(day.getText().toString()) < 31)) {
				noError = false;
			} else if (!(Integer.valueOf(hour.getText().toString()) > 0 || Integer.valueOf(hour.getText().toString()) < 23)) {
				noError = false;
			} else if (!(Integer.valueOf(minute.getText().toString()) > 0 || Integer.valueOf(minute.getText().toString()) < 59)) {
				noError = false;
			} else if (!(Integer.valueOf(second.getText().toString()) > 0 || Integer.valueOf(second.getText().toString()) < 59)) {
				noError = false;
			}
		} catch (NumberFormatException e) {
			Log.e(CLASSTAG, "NumberFormatException: Numbers in Date somehow incorrect.");
			noError = false;
		}
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		if (!noError) {
			return sdf.format(new Date(System.currentTimeMillis()));
		} else {
			dateString = String.format("%4s", year) + "-" + String.format("%2s", month) + "-" + String.format("%2s", day);
			dateString = dateString + "T";
			dateString = dateString + String.format("%2s", hour) + "-" + String.format("%2s", minute) + "-" + String.format("%2s", second);
//			SimpleDateFormat ft = new SimpleDateFormat(WFSFeatureImport.STANDARD_DATE_FORMAT); 
//			Date t = new Date(); 
//			try { 
//				//parse the date based on the format ft defined above
//				t = ft.parse(dateString); 
//			} catch (ParseException e) { 
//				Log.e(CLASSTAG, "ParseException: Date " + dateString + " somehow incorrect: " + e.getMessage());
//				e.printStackTrace();
//				return sdf.format(new Date(System.currentTimeMillis()));
//			}
			Log.e(CLASSTAG, "Returning " + dateString);
			return dateString;
		}
	}

}
