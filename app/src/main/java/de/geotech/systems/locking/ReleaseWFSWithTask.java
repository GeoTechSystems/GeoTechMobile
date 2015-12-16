/**
 * Unlocks layers on the geoserver with lockfeature
 * 
 * DOESN'T WoRK YET!
 * 
 * @author Torsten Hoch
 */

package de.geotech.systems.locking;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.xml.sax.InputSource;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.util.Log;

import de.geotech.systems.utilities.Alerts;
import de.geotech.systems.utilities.Functions;
import de.geotech.systems.utilities.StatusInfo;
import de.geotech.systems.utilities.XMLHandling;
import de.geotech.systems.wfs.WFSLayer;

public class ReleaseWFSWithTask {
	private static final String CLASSTAG = "ReleaseWFSWithTask";
	// der context
	private Context context;
	// zu unlockende ID
	private String lockID;

	private OnUnlockFinishedListener finished;


	/**
	 * Default constructor. Just initiates the object but doesn't 
	 * start the unlocking.
	 * 
	 * @param c
	 * @param newLockID
	 */
	public ReleaseWFSWithTask(Context c, String newLockID) {
		this.context = c;
		this.lockID = newLockID;
	}

	// initialisiert den Unlock-Versuch
	public void unLockLayer(WFSLayer currentWFSLayer) {
		ReleaseTask releaser = new ReleaseTask();
		releaser.execute(currentWFSLayer);
	}

	// asynchroner task zur serververbindung
	private class ReleaseTask extends AsyncTask<WFSLayer, StatusInfo, Boolean> {
		// dialog zur anzeige des fortschritts
		ProgressDialog dialog;

		// vor der serverkommunikation
		@Override
		protected void onPreExecute() {
			dialog = new ProgressDialog(context);
			// TODO Messages uebersetzen
			dialog.setTitle("Unlocking!");
			dialog.setMessage("Unlocking-Message!");
			dialog.setCancelable(false);
			dialog.show();
		}

		@Override
		protected Boolean doInBackground(WFSLayer... layers) {
			// get( "wfs?request=ReleaseLock&lockId=" + lockId );


			// zu lockender layer
			WFSLayer currentLayer = layers[0];
			// bisher kein fehler
			boolean noError = true;
			try {
				// URL erstellen
				// URL url = new URL(Functions.reviseWfsUrl(currentLayer.getUrl()) + "?SERVICE=WFS&VERSION=2.0.0&REQUEST=ReleaseLock&lockId=" + lockID);
				// URL url = new URL(Functions.reviseWfsUrl(currentLayer.getUrl()) + "?request=LockFeature&releaseAction=ALL&lockId=" + lockID);
				// URL url = new URL(Functions.reviseWfsUrl(currentLayer.getUrl()) + "?request=LockFeature&lockId=" + lockID);
				URL url = new URL(Functions.reviseWfsUrl(currentLayer.getUrl()) + "?SERVICE=WFS&VERSION=2.0.0&REQUEST=LockFeature");







				Log.i(CLASSTAG,	"Sending WFS UnlockFeature request to following URL:\n" + url.toString());
				
				
				// Server-Kommunikation vorbereiten
				HttpClient client = new DefaultHttpClient();
				HttpPost post = new HttpPost(url.toURI());
//				Log.i(CLASSTAG, "Sending Request with the following data: ");
//				Log.i(CLASSTAG, "Layer Name: " + workspace + ":" + layerName);
//				Log.i(CLASSTAG, "Lock Expiry: " + lockExpiry);
				StringEntity data = new StringEntity(unLockFeatureXML(lockID));
				data.setContentType("text/xml");
				post.setHeader("Content-Type", "application/xml;charset=UTF-8");
				post.setEntity(data);
				// Server anfragen und ergebnis speichern
				HttpResponse response = client.execute(post);
				String xml = EntityUtils.toString(response.getEntity());
				
				
				
				Log.i(CLASSTAG, "Received UNLOCKING transaction response:\n" + xml);
				// nach irgendwas suchen in der Antwort
				String responsePart = XMLHandling.getAttributeValue(xml, "irgendwas");
				// wenn responsepart irgendwie falsch
				if (responsePart == null) {
					noError = false;
				}
				else {
					// unlocken hat geklappt
					noError = true;
				}
			} catch (SQLiteException e) {
				publishProgress(new StatusInfo(-1, "SQLiteException:\n"	+ e.getMessage()));
				Log.e(CLASSTAG, "SQLiteException:\n" + e.getMessage());
				noError = false;
			} catch (IOException e) {
				publishProgress(new StatusInfo(-1, "IOException:\n"	+ e.getMessage()));
				Log.e(CLASSTAG, "IOException:\n" + e.getMessage());
				noError = false;
			} catch (URISyntaxException e) {
				publishProgress(new StatusInfo(-1, "URISyntaxException:\n"	+ e.getMessage()));
				Log.e(CLASSTAG, "URISyntaxException:\n" + e.getMessage());
				noError = false;
			}
			// ueberprufen ob locking gefunzt hat, dann zurueckgeben
			return noError;
		}

		/**
		 * Erstellt (noch k)einen String fuer eine UnlockFeature Anfrage 
		 */
		private String unLockFeatureXML(String newLockID) {
//			StringBuilder sb = new StringBuilder();
//			sb.append("<?xml version=\"1.0\" ?>");
//			sb.append("<LockFeature ");
//			sb.append("version=\"2.0.0\" ");
//			sb.append("service=\"WFS\" ");
//			// Expiry mitgeben:
//			sb.append("lockid=\""+ newLockID +"\" ");
//			sb.append("releaseAction=\"ALL\"");
//			
//			sb.append("xmlns=\"http://www.opengis.net/wfs/2.0\" ");
//			sb.append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ");
//			sb.append("xsi:schemaLocation=\"http://www.opengis.net/wfs/2.0 ");
//			sb.append("http://schemas.opengis.net/wfs/2.0/wfs.xsd\">");
//			// Layername mitgeben
//			// sb.append("<Query typeNames=\"" + newLayerName + "\"/>");
//			sb.append("</LockFeature>");
//			Log.e(CLASSTAG, "XML sent: \n" + sb.toString());
//			return sb.toString();
			
			
			
			StringBuilder sb = new StringBuilder();
			sb.append("<wfs:Transaction service=\"WFS\" version=\"1.0.0\"");
			sb.append(" xmlns:topp=\"http://www.openplans.org/topp\"");
			sb.append(" xmlns:ogc=\"http://www.opengis.net/ogc\"");
			sb.append(" xmlns:wfs=\"http://www.opengis.net/wfs\"");
			sb.append(" releaseAction=\"ALL\">");
			sb.append("<wfs:LockId>" + newLockID + "</wfs:LockId>");
			sb.append("</wfs:Transaction>");
			
			Log.e(CLASSTAG, "XML sent: \n" + sb.toString());
			return sb.toString();
			
		}

		// nach der serverkommunikation
		// Nachricht ausgeben, ob Lock ok oder nicht
		@Override
		protected void onPostExecute(Boolean result) {
			dialog.cancel();
			if (result) {
				// TODO
				Alerts.errorMessage(context, "JUHU!!", "Lock RELEASED!").show();
				//				finished.onUnlockFinished(result);
			}
			else {
				// TODO
				Alerts.errorMessage(context, "OH-OH-OH!!!", "Didn't work out with releasing Lock....").show();
			}
		}

		// Nachricht ausgeben , wenn error
		@Override
		protected void onProgressUpdate(StatusInfo... status) {
			StatusInfo info = status[0];
			if (info.getStatus() < 0) {
				// TODO
				Alerts.errorMessage(context, "Error!", info.getMessage()).show();
			}
		}
	}

	public interface OnUnlockFinishedListener {
		public void onUnlockFinished(boolean finished);
	}

	/**
	 * Sets the OnImportFinishedListener called after finished response parsing.
	 * 
	 * @param l
	 */
	public void setOnLockFinishedListener(OnUnlockFinishedListener l) {
		finished = l;
	}
}
