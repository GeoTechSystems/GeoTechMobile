/**
 * Global GeoTechMobile Settings
 * 
 * @author tubatubsen
 */

package de.geotech.systems.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;

import de.geotech.systems.R;
import de.geotech.systems.layers.ServerSpinner;
import de.geotech.systems.projects.Project;
import de.geotech.systems.projects.ProjectHandler;

public class GeoTechMobileSettingsActivity extends Activity implements
OnClickListener, OnCheckedChangeListener {
	private static final String CLASSTAG = "GeoTechMobileSettingsActivity";

	private boolean anyChange = false;
	private Project project;
	private Switch osmSwitch;
	private Switch autosyncSwitch;
	private Switch markerSwitch;
	private Switch notSyncCyanSwitch;
	private Button resetServer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.geotechmobilesettingsactivity_layout);
		// settings einladen
		project = ProjectHandler.getCurrentProject();
		// switch einladen, ob die osm map eingeladen werden soll
		osmSwitch = (Switch) findViewById(R.id.geotechmobilesettingsactivity_switch_osm);
		osmSwitch.setOnCheckedChangeListener(this);
		osmSwitch.setChecked(project.isOSM());
		// autosync an oder aus
		autosyncSwitch = (Switch) findViewById(R.id.geotechmobilesettingsactivity_switch_auto_sync);
		autosyncSwitch.setOnCheckedChangeListener(this);
		autosyncSwitch.setChecked(project.getAutoSync() > 0);
		// marker an oder aus
		markerSwitch = (Switch) findViewById(R.id.geotechmobilesettingsactivity_switch_markers);
		markerSwitch.setOnCheckedChangeListener(this);
		markerSwitch.setChecked(project.isShowMarker());
		// not synchronized features as cyan
		notSyncCyanSwitch = (Switch) findViewById(R.id.geotechmobilesettingsactivity_switch_notSyncCyan);
		notSyncCyanSwitch.setOnCheckedChangeListener(this);
		notSyncCyanSwitch.setChecked(project.isUnsyncAsCyan());
		// serverliste auf standard
		resetServer = (Button) findViewById(R.id.geotechmobilesettingsactivity_button_resetServer);
		resetServer.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.geotechmobilesettingsactivity_button_resetServer:
			AlertDialog.Builder alert = new AlertDialog.Builder(v.getContext());
			// erzeugt einen EditText Feld zur Eingabe der Serveradresse
			alert.setTitle(R.string.geotechmobilesettingsactivity_button_resetserver_alert_title);
			alert.setMessage(R.string.geotechmobilesettingsactivity_button_resetserver_alert_message);
			// OK Button (im Dialog)
			alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					resetServerListen();
				}
			});
			// Cancel - Button (im Dialog)
			alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					dialog.cancel();
				}
			});
			// Erzeugt die Anzeige des Dialogs
			AlertDialog dialog = alert.create();
			dialog.show();
			break;
		}
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		switch (buttonView.getId()) {
		case R.id.geotechmobilesettingsactivity_switch_osm:
			if (isChecked != project.isOSM()) {
				anyChange = true;
			}
			project.setOSM(isChecked);
			break;
		case R.id.geotechmobilesettingsactivity_switch_auto_sync:
			if (isChecked != (project.getAutoSync() > 0)) {
				anyChange = true;
			}
			if (isChecked) {
				project.setAutoSync(1);
			} else {
				project.setAutoSync(0);
			}
			break;
		case R.id.geotechmobilesettingsactivity_switch_markers:
			if (isChecked != (project.isShowMarker())) {
				anyChange = true;
			}
			if (isChecked) {
				project.setShowMarker(true);
			} else {
				project.setShowMarker(false);
			}
			break;
		case R.id.geotechmobilesettingsactivity_switch_notSyncCyan:
			if (isChecked != (project.isUnsyncAsCyan())) {
				anyChange = true;
			}
			if (isChecked) {
				project.setUnsyncAsCyan(true);
			} else {
				project.setUnsyncAsCyan(false);
			}
			break;
		}
	}

	@Override
	public void onBackPressed(){
		Intent returnToMain = new Intent();
		setResult(Activity.RESULT_CANCELED, returnToMain);
		returnToMain.putExtra("de.geotech.systems.anychange", anyChange);
		finish();
	}

	private void resetServerListen() {
		new ServerSpinner(this, true);
		Toast.makeText(this, R.string.geotechmobilesettingsactivity_button_resetserver_toast_message, Toast.LENGTH_SHORT).show();
	}

}
