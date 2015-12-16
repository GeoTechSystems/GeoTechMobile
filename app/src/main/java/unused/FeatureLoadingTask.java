/**
 * Klasse soll im Hintergrund Features laden, damit die Anzeige der 
 * MainActivity mit dem Panel nicht mehr ausfaellt. 
 * 
 * @author Torsten Hoch
 */

package unused;

import de.geotech.systems.features.Feature;
import android.os.AsyncTask;

public class FeatureLoadingTask extends AsyncTask<String, Feature, Boolean> {

	@Override
	protected Boolean doInBackground(String... params) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void onCancelled() {
		// TODO Auto-generated method stub
		super.onCancelled();
	}

	@Override
	protected void onCancelled(Boolean result) {
		// TODO Auto-generated method stub
		super.onCancelled(result);
	}

	@Override
	protected void onPostExecute(Boolean result) {
		// TODO Auto-generated method stub
		super.onPostExecute(result);
	}

	@Override
	protected void onPreExecute() {
		// TODO Auto-generated method stub
		super.onPreExecute();
	}

	@Override
	protected void onProgressUpdate(Feature... values) {
		// TODO Auto-generated method stub
		super.onProgressUpdate(values);
	}

}
