// DERZEIT OHNE FUNKTION!!!

package unused;

import java.util.Calendar;

import de.geotech.systems.R;
import de.geotech.systems.R.id;
import de.geotech.systems.R.layout;
import de.geotech.systems.R.menu;
import de.geotech.systems.R.string;

import android.os.Bundle;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;

public class ExpiryPickerActivity extends Activity {

	private TextView mDateText;
	private TextView mDateDisplay;
	private Button mPickDate;
	private Button mPickTime;

	private int mYear;
	private int mMonth;
	private int mDay;
	private int mHour;
	private int mMinute;
	private long mMillies;
	private Calendar pressedTime;
	private Calendar lockDate;
	
	static final int DATE_DIALOG_ID = 0;
	static final int TIME_DIALOG_ID = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_expiry_picker);

		// get the current date
		pressedTime = Calendar.getInstance();
		mYear = pressedTime.get(Calendar.YEAR);
		mMonth = pressedTime.get(Calendar.MONTH);
		mDay = pressedTime.get(Calendar.DAY_OF_MONTH);
		mHour = pressedTime.get(Calendar.HOUR);
		mMinute = pressedTime.get(Calendar.MINUTE);
		mMillies = pressedTime.getTimeInMillis();

		mDateText = (TextView) findViewById(R.id.expiry_picker_dateDisplay);
		mDateDisplay = (TextView) findViewById(R.id.expiry_picker_dateDisplay2);
		mDateText.setText(getString(R.string.expiry_picker_selectedDate));
		
		mPickDate = (Button) findViewById(R.id.expiry_picker_pickDate);
		mPickTime = (Button) findViewById(R.id.expiry_picker_pickTime);
		
		mPickDate.setText("Change Date");
		mPickTime.setText("Change Time");

		// add click listeners to the buttons
		mPickDate.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showDialog(DATE_DIALOG_ID);
			}
		});
		mPickTime.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showDialog(TIME_DIALOG_ID);
			}
		});
		// display the current date (this method is below)
		updateDisplay();
	}

	// updates the date in the TextView
	private void updateDisplay() {
		mDateDisplay.setText(setDate().getTime().toLocaleString());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.expiry_picker, menu);
		return true;
	}

	// the callback received when the user "sets" the date in the dialog
	private DatePickerDialog.OnDateSetListener mDateSetListener =
			new DatePickerDialog.OnDateSetListener() {
		@Override
		public void onDateSet(DatePicker view, int year,
				int monthOfYear, int dayOfMonth) {
			mYear = year;
			mMonth = monthOfYear;
			mDay = dayOfMonth;
			updateDisplay();
		}
	};

	// the callback received when the user "sets" the time in the dialog
	private TimePickerDialog.OnTimeSetListener mTimeSetListener =
			new TimePickerDialog.OnTimeSetListener() {
		@Override
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			mHour = hourOfDay;
			mMinute = minute;
			updateDisplay();
		}
	};

	private Calendar setDate(){
		lockDate = Calendar.getInstance();
		lockDate.set(mYear, mMonth, mDay, mHour, mMinute, 0);
		return lockDate;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DATE_DIALOG_ID:
			return new DatePickerDialog(this, mDateSetListener, mYear, mMonth, mDay);
		case TIME_DIALOG_ID:
			return new TimePickerDialog(this, mTimeSetListener, mHour, mMinute, true);
		}
		return null;
	}

}
