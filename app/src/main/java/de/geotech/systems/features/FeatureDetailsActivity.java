/**
 * Activity displays table of features of a selected locked layer 
 * to edit the features
 * 
 * @author Torsten Hoch
 */

package de.geotech.systems.features;

import de.geotech.systems.R;
import de.geotech.systems.database.DBAdapter;
import de.geotech.systems.database.SQLSatellite;
import de.geotech.systems.editor.PrecisionCon;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class FeatureDetailsActivity extends Activity {
	private static final String CLASSTAG = "FeatureDetailsActivity";

	private Context context;
	private int layerId;
	private int elementId;
	private DBAdapter db = new DBAdapter(context);
	//	private Boolean zparams;
	//	private StringBuffer content;

	@Override
	public void onCreate(Bundle savedInstanceState)	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.element_details);
		this.context = (Context) this;
		this.layerId = Integer.parseInt(getIntent().getStringExtra("de.geotech.systems.layerName").substring(5));
		this.elementId = Integer.parseInt(getIntent().getStringExtra("de.geotech.systems.elementId"));
		buildGUI();
	}

	private void buildGUI() {
		//      //Erzeuge Menü
		//      LinearLayout left_ll = (LinearLayout) findViewById(R.id.element_selector_menu);
		//      left_ll.setGravity(Gravity.CENTER_HORIZONTAL);
		//      //Füge Button "Zurück" ein
		//      left_ll.addView(MainActivity.createToolBarIcon(context, getString(R.string.back), Color.LTGRAY, R.drawable.ic_project_manager_load, new OnClickListener(){
		//			@Override
		//			public void onClick(View v) {
		//				// TODO Auto-generated method stub
		//				Intent returnToLayerManager = new Intent();
		//				returnToLayerManager.putExtra("de.geotech.systems.fly", false);
		//				setResult(RESULT_OK, returnToLayerManager);
		//				finish();
		//			}
		//		}));
		//    database=getIntent().getStringExtra("de.geotech.systems.database");

		PrecisionCon precision = new PrecisionCon(context, elementId, layerId);
		// Echo all details from TABLE_PRECISION ...
		TextView satCount = (TextView) findViewById(R.id.satCount) ;
		satCount.setText(precision.getSatCount());
		TextView satFixCount = (TextView) findViewById(R.id.satFixCount);
		satFixCount.setText(precision.getSatFixCount());
		TextView minElevation = (TextView) findViewById(R.id.minElevation);
		minElevation.setText(precision.getMinElevation());
		TextView averageElevation = (TextView) findViewById(R.id.averageElevation);
		averageElevation.setText(precision.getAverageElevation());
		TextView accuracy = (TextView) findViewById(R.id.accuracy);
		accuracy.setText(precision.getAccuracyString());
		TextView recordingTime = (TextView) findViewById(R.id.recording_time);
		recordingTime.setText(precision.getRecordingTime());
		//Echo all details for every single satellite from TABLE_SATELLITES ...
		//Create a layout-table
		TableLayout objTable = (TableLayout) findViewById(R.id.element_sat_table);
		objTable.removeAllViews();
		objTable = this.featureDetailsTable(objTable);
	}

	// Save changes and go back to main
	@Override
	public void onBackPressed() {
		Intent returnToMain = new Intent();
		setResult(RESULT_CANCELED, returnToMain);
		finish();
	}

	/**
	 * Feature details table.
	 *
	 * @param objTable the obj table
	 * @return the table layout
	 */
	public TableLayout featureDetailsTable(TableLayout objTable) {
		Cursor satCur = db.getSatellite(0);
		boolean zparams;
		TableRow tRow = new TableRow(context);
		tRow.setBackgroundColor(Color.BLACK);
		// Insert Headings
		for (int i = 0; i < satCur.getColumnCount(); i++) {
			zparams = true;
			if (i == satCur.getColumnCount() - 1) {
				zparams = false;
			}
			tRow.addView(FeatureSelectActivity.createZelle(context, satCur.getColumnName(i), zparams));
		}
		objTable.removeAllViews();
		objTable.addView(tRow);
		// Initialize content-String
		StringBuffer content = new StringBuffer();
		// Insert satellite-details
		while (satCur.moveToNext()) {
			Log.i(CLASSTAG + " verruecktesSatellitenIrgendwas", "Found Satellite!");
			TableRow row = new TableRow(context);
			row.setBackgroundColor(Color.BLACK);
			row.setPadding(0, 2, 0, 0);
			for (int i = 0; i < satCur.getColumnCount(); i++) {
				Log.i(CLASSTAG, satCur.getColumnName(i) + " -> " + satCur.getString(i));
				zparams = true;
				if (i == satCur.getColumnCount() - 1) {
					zparams = false;
				}
				content.setLength(0);
				content.append(satCur.getString(i));
				if (satCur.getColumnName(i).equals(SQLSatellite.ELEVATION)
						|| satCur.getColumnName(i).equals(SQLSatellite.AZIMUTH)) {
					content.append("°");
				}
				row.addView(FeatureSelectActivity.createZelle(context, content.toString(), zparams));
			}
			objTable.addView(row);
		}
		return objTable;
	}

}
