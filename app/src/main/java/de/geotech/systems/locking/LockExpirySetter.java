/**
 * 
 * @author Torsten Hoch
 */

package de.geotech.systems.locking;

import java.util.Calendar;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TableLayout;
import android.widget.TimePicker;

import de.geotech.systems.R;
import de.geotech.systems.database.DBAdapter;
import de.geotech.systems.layerTables.LayerTable;
import de.geotech.systems.locking.LockWFSLayerWithTask.OnLockFinishedListener;
import de.geotech.systems.projects.ProjectHandler;
import de.geotech.systems.wfs.WFSCheckedListener;
import de.geotech.systems.wfs.WFSLayer;

public class LockExpirySetter {
	final static String CLASSTAG = "LockExpirySetter";
	// maximum of lock expiry as one month (30 days)
	final static long maxMillies = 2592000000l;
	// current date variables
	private int mYear;
	private int mMonth;
	private int mDay;
	private int mHour;
	private int mMinute;
	private long mMillies;
	private Calendar pressedDate;
	private Calendar unLockDate;
	// lock expiry
	private int lockExpiry;
	private Context context;
	private WFSLayer wfsLayer2Lock;
	private LayerTable table;
	private DBAdapter db;

	private LockWFSLayerWithTask lockTask;
	private AlertDialog.Builder builder;

	/**
	 * Instantiates a new lock get expiry.
	 *
	 * @param newContext the new context
	 */
	public LockExpirySetter(Context newContext, LayerTable newTable) {
		this.context = newContext;
		this.db = new DBAdapter(context);
		this.table = newTable;
	}

	/**
	 * Start lock.
	 *
	 * @param newLayer the new layer
	 */
	public void startLock(WFSLayer newLayer) {	
		this.wfsLayer2Lock = newLayer;
		// get the current date
		this.pressedDate = Calendar.getInstance();
		this.mYear = pressedDate.get(Calendar.YEAR);
		this.mMonth = pressedDate.get(Calendar.MONTH);
		this.mDay = pressedDate.get(Calendar.DAY_OF_MONTH);
		this.mHour = pressedDate.get(Calendar.HOUR);
		this.mMinute = pressedDate.get(Calendar.MINUTE);
		this.mMillies = pressedDate.getTimeInMillis();
		// lockdate auf in 5 minuten setzen
		this.unLockDate = Calendar.getInstance();
		this.unLockDate.setTimeInMillis(mMillies);
		this.unLockDate.add(Calendar.MINUTE, 5);
		this.lockExpiry = 300;
		// alertdialog erstellen 
		this.builder = new AlertDialog.Builder(context);
		this.builder.setTitle(context.getString(R.string.expiry_picker_selectedDate) + " " + unLockDate.getTime().toLocaleString());
		// layout fuer den builder
		TableLayout layout = new TableLayout(context);
		Button changeDate = new Button(context);
		changeDate.setText(R.string.lockexpiriysetter_lockingtime_dialog_datebutton);
		Button changeTime = new Button(context);
		changeTime.setText(R.string.lockexpiriysetter_lockingtime_dialog_timebutton);
		Log.e(CLASSTAG + " setLockDate", "Before: LockDate is " + unLockDate.getTime().toLocaleString());
		// bei gedruecktem time-button				
		changeTime.setOnClickListener(new View.OnClickListener() {
			// Perform action on click
			public void onClick(View v) {
				final TimePickerDialog.OnTimeSetListener mTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
					@Override
					public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
						unLockDate.set(Calendar.HOUR, hourOfDay);
						unLockDate.set(Calendar.MINUTE, minute);
						Log.e(CLASSTAG + " OnTimeSetListener()", "Date set to " + unLockDate.getTime().toLocaleString());
						builder.setTitle(context.getString(R.string.expiry_picker_selectedDate) + " " + unLockDate.getTime().toLocaleString());
					}
				};
				AlertDialog.Builder builderTime = new AlertDialog.Builder(context);
				final TimePicker timePicker = new TimePicker(context);
				timePicker.setIs24HourView(true);
				timePicker.setCurrentHour(mHour);
				timePicker.setCurrentMinute(mMinute);
				builderTime.setView(timePicker);
				builderTime.setPositiveButton(R.string.ok, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if(null != mTimeSetListener) {
							mTimeSetListener.onTimeSet(timePicker, timePicker.getCurrentHour(), timePicker.getCurrentMinute());
						}
					}
				});
				builderTime.show();
			}
		});
		// bei gedruecktem date-button				
		changeDate.setOnClickListener(new View.OnClickListener() {
			// Perform action on click
			public void onClick(View v) {
				final DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {
					@Override
					public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
						unLockDate.set(year, monthOfYear, dayOfMonth);
						Log.e(CLASSTAG + " OnDateSetListener()", "Date set to " + unLockDate.getTime().toLocaleString());
						builder.setMessage(context.getString(R.string.expiry_picker_selectedDate) + " " + unLockDate.getTime().toLocaleString());
					}
				};
				AlertDialog.Builder builderDate = new AlertDialog.Builder(context);
				final DatePicker DatePicker = new DatePicker(context);
				DatePicker.setMinDate(mMillies);
				DatePicker.setMaxDate(mMillies + maxMillies);
				DatePicker.updateDate(mYear, mMonth, mDay);
				builderDate.setView(DatePicker);
				builderDate.setPositiveButton(R.string.ok, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if(null != mDateSetListener) {
							mDateSetListener.onDateSet(DatePicker, DatePicker.getYear(), DatePicker.getMonth(), DatePicker.getDayOfMonth());
						}
					}
				});
				builderDate.show();
			}
		});
		layout.addView(changeDate);
		layout.addView(changeTime);
		this.builder.setView(layout);
		// bei ok-button
		this.builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				// User clicked OK button
				// locken mit standardexpiry
				int test = (int) ((unLockDate.getTimeInMillis() - Calendar.getInstance().getTimeInMillis()) / 1000 / 60);
				Log.i(CLASSTAG, "Lock-Date: " + unLockDate.getTime().toLocaleString() + " - Seconds: " + test);
				if (test > 0) {
					lockExpiry = test;
					lockNow();
				} else {
					lockExpiry = 0;	
				}
				Log.i(CLASSTAG + " OK-Button()", "LockExpiry is " + lockExpiry + " seconds!");
			}
		});
		// bei cancel button
		this.builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			// User cancelled the dialog
			public void onClick(DialogInterface dialog, int id) {
				lockExpiry = 0;

			}
		});
		this.builder.show();
	}

	/**
	 * Lock now.
	 */
	private void lockNow() {
		lockTask = new LockWFSLayerWithTask(context, lockExpiry);
		lockTask.lockLayer(wfsLayer2Lock);
		lockTask.setOnLockFinishedListener(new OnLockFinishedListener() {
			@Override
			public void onLockFinished(boolean finished) {
				if (finished) {
					// Date from now
					Calendar now = Calendar.getInstance();
					// wenn der projekte-container initialisiert ist
					// wenn der layer bereits im projekt-container enthalten ist
					if (ProjectHandler.getCurrentProject().getWFSContainer() != null && ProjectHandler.getCurrentProject().getWFSContainer().contains(wfsLayer2Lock)) {
						// den richtigen layer finden und locked setzen
						for (int i = 0; i < ProjectHandler.getCurrentProject().getWFSContainer().size(); i++) {
							if (ProjectHandler.getCurrentProject().getWFSContainer().get(i).equals(wfsLayer2Lock)) {
								// layer auf lock ueberpruefen
								if (ProjectHandler.getCurrentProject().getWFSContainer().get(i).lock(now, lockExpiry, lockTask.getLockID())) {
									// Nachricht, Layer gelockt worden ist
									// ProjectHandler.getCurrentProject().updateDatabase();
									db.updateWFSLayerInDB(ProjectHandler.getCurrentProject().getWFSContainer().get(i));
									Log.i(CLASSTAG + " in onClick", ProjectHandler.getCurrentProject().getWFSContainer().get(i).getName() + " wurde gelockt!");
									// abbruch der for-schleife, da gefunden
									i = ProjectHandler.getCurrentProject().getWFSContainer().size();
								} else {
									// fehlermeldung, lock net geklappt
								}
							}
						}
					} else {
						// Layer ins projekt einfügen und locked setzen
						if (wfsLayer2Lock.lock(now, lockExpiry, lockTask.getLockID())) {
							ProjectHandler.getCurrentProject().addWFSLayerInContainerAndDB(wfsLayer2Lock);
//							WFSCheckedListener.getWFSCheckedList().add(wfsLayer2Lock);
						} else {
							Log.e(CLASSTAG + " in onClick if 2", "HAT NICHT GEKLAPPT fuers if 2!");
						}
					}
				} else {
					Log.e(CLASSTAG + " in onClick", "HAT NICHT GEKLAPPT fuers if 1!");
				}
				table.activateLayerTableBuilding(table.getWFSHandler());
			}
		});
	}

}
