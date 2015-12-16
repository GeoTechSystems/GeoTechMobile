/**
 * Um die countFeature Abfrage zu trennen vom Abfragen der 
 * GetCapabilities
 * 
 * @author Torsten Hoch - tohoch@uos.de
 */

package de.geotech.systems.layerTables;

import de.geotech.systems.wfs.WFSLayer;

import android.os.AsyncTask;

public class CountFeaturesAsynctask extends AsyncTask<String, WFSLayer, Boolean> {

	/* (non-Javadoc)
	 * @see android.os.AsyncTask#doInBackground(Params[])
	 */
	@Override
	protected Boolean doInBackground(String... params) {
		return true;
	}

}
