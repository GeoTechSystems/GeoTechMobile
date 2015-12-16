/**
 * activity, die das Locken von Feature-layern ermoeglichen soll
 * 
 * @author Torsten Hoch
 * @author Paul Vincent Kuper (kuper@kit.edu)
 */

package de.geotech.systems.locking;

import de.geotech.systems.R;
import de.geotech.systems.layers.ServerSpinner;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;

public class LockWFSLayerActivity extends Activity {
	private ServerSpinner serverSpinner = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.addlayeractivity_lockload_screen);
		// create server and tables with lock feature
		serverSpinner = new ServerSpinner(this, false, R.id.getFeatureWithLockActivity_layer_table_layout);
		// Serverauswahlliste anzeigen
		serverSpinner.loadSpinner();
	}
	
	@Override
	public void onBackPressed()	{
		Intent returnToMain = new Intent();
		setResult(RESULT_CANCELED, returnToMain);
		finish();
	}
	
}
