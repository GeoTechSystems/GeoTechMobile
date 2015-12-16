/**
 * Locks complete layers on the geoserver with lockfeature
 * 
 * @author Torsten Hoch
 */

package de.geotech.systems.locking;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Calendar;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.util.Log;

import de.geotech.systems.R;
import de.geotech.systems.projects.ProjectHandler;
import de.geotech.systems.projects.Server;
import de.geotech.systems.utilities.Alerts;
import de.geotech.systems.utilities.Functions;
import de.geotech.systems.utilities.StatusInfo;
import de.geotech.systems.utilities.XMLHandling;
import de.geotech.systems.wfs.WFSLayer;

public class LockWFSLayerWithTask {
	private static final String CLASSTAG = "LockWFSLayerWithTask";
	// der context
	private Context context;
	// laenge der zeit fuer den lockin minuten
	private int lockExpiryMinutes;
	// lockID
	private String lockID;
	
	private OnLockFinishedListener finished;
	
	/**
	 * Default constructor. Just initiates the object but doesn't 
	 * start the locking.
	 * 
	 * @param c
	 * @param newLockExpiry
	 */
	public LockWFSLayerWithTask(Context c, int newLockExpiry) {
		this.context = c;
		this.lockExpiryMinutes = newLockExpiry;
	}

	// initialisiert den Lock-Versuch
	public void lockLayer(WFSLayer currentWFSLayer) {
		ImportTask importTask = new ImportTask();
		importTask.execute(currentWFSLayer);
	}

	public String getLockID() {
		return lockID;
	}

	// asynchroner task zur serververbindung
	private class ImportTask extends AsyncTask<WFSLayer, StatusInfo, Boolean> {
		// dialog zur anzeige des fortschritts
		ProgressDialog dialog;

		// vor der serverkommunikation
		@Override
		protected void onPreExecute() {
			dialog = new ProgressDialog(context);
			// TODO UEBERSETUNGEN
			dialog.setTitle("Locking!");
			dialog.setMessage("Locking-Message!");
			dialog.setCancelable(true);
			dialog.show();
		}

		@Override
		protected Boolean doInBackground(WFSLayer... layers) {
			// zu lockender layer
			WFSLayer currentLayer = layers[0];
			// bisher kein fehler
			boolean noError = true;
			try {
				// datum merken, an dem lock vollzogen wird
				Calendar lockDate = Calendar.getInstance();
//				Log.i(CLASSTAG, "LockDate: " + lockDate.getTime().toString());
				// Name des Layers
				String layerName = currentLayer.getName();
				// Bereich des Layers				
				String workspace = currentLayer.getWorkspace();
//				Log.i(CLASSTAG,	"Layer ist momentan: (Name, Bereich, Lock-Status)");
//				Log.i(CLASSTAG,	 layerName + ", " + workspace + ", " + currentLayer.isLocked());
				// URL erstellen
				URL url = new URL(Functions.reviseWfsUrl(currentLayer.getUrl()) + "?SERVICE=WFS&VERSION=2.0.0&REQUEST=LockFeature");
//				Log.i(CLASSTAG,	"Sending WFS LockFeature request to following URL:\n" + url.toString());
				// Server-Kommunikation vorbereiten
				DefaultHttpClient client = new DefaultHttpClient();
				Server server = ProjectHandler.getServer(currentLayer.getUrl());
				/// wenn username gesetzt - authenticated abfrage
				if (!server.getUsername().equals("")) {
					client.getCredentialsProvider().setCredentials(AuthScope.ANY,
							new UsernamePasswordCredentials(server.getUsername(), server.getPassword()));
				} 
				HttpPost post = new HttpPost(url.toURI());
//				Log.i(CLASSTAG, "Sending Request with the following data: ");
//				Log.i(CLASSTAG, "Layer Name: " + workspace + ":" + layerName);
//				Log.i(CLASSTAG, "Lock Expiry: " + lockExpiry);
				StringEntity data = new StringEntity(lockFeatureXML(currentLayer, lockExpiryMinutes));
				data.setContentType("text/xml");
				post.setHeader("Content-Type", "application/xml;charset=UTF-8");
				post.setEntity(data);
				// Server anfragen und ergebnis speichern
				HttpResponse response = client.execute(post);
				String xml = EntityUtils.toString(response.getEntity());
				Log.i(CLASSTAG, "Received LOCKING transaction response: \n" + xml);
				// nach lockID suchen in der Antwort
				String newLockID = XMLHandling.getAttributeValue(xml, "lockId");
				lockID = newLockID;
				Log.i(CLASSTAG, "Lock ID ausgelesen ist: " + newLockID);
				// wenn lockID gegeben ist, dann hat es funktioniert
				if (newLockID == null) {
					noError = false;
				}
				else {
					// in unserer DB locken!!!
					currentLayer.lock(lockDate, lockExpiryMinutes, newLockID);
					Log.i(CLASSTAG, "New LockID: " + currentLayer.getLockID());
					Log.i(CLASSTAG, "Lock Expiry: " + currentLayer.getLockExpiry());
					Log.i(CLASSTAG, "LockDate: " + lockDate.getTime().toString());
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
		 * Erstellt einen String fuer eine LockFeature Anfrage fuer einen
		 * bestimmten Layer mkit einer bestimmten zeitlichen laenge
		 * 
		 * @author Paul Vincent Kuper (kuper@kit.edu)
		 * @author Tosretn Hoch (kuper@kit.edu)
		 * @param completeTypeName - Layer und workspace, die gelockt werden sollen
		 * @param newLockExpiry lock-zeit in minuten
		 * @return - das fertige XML Dokument
		 */
		private String lockFeatureXML(WFSLayer layer, int newLockExpiry) {
			
//			StringBuilder sb = new StringBuilder();
//			sb.append("<wfs:GetFeatureWithLock service='WFS' version='2.0.0' ");
//			sb.append("handle='GetFeatureWithLock-tc1' expiry='5' resultType='results' ");
//			sb.append("xmlns:topp='http://www.openplans.org/topp' ");
//			sb.append("xmlns:fes='http://www.opengis.net/fes/2.0' ");
//			sb.append("xmlns:wfs='http://www.opengis.net/wfs/2.0' ");
////			sb.append("valueReference='the_geom'> ");
//			sb.append("> \n");
//			sb.append("<wfs:Query typeNames='LGL:AD:dim'/> \n");
//			sb.append("</wfs:GetFeatureWithLock> ");
			
			
			String handleString = "GetFeatureWithLock-Test";
			String resultString = "Resultate";
			StringBuilder sb = new StringBuilder();
			sb.append("<?xml version=\"1.0\" ?> \n");
			sb.append("<wfs:" + "GetFeatureWithLock ");
			sb.append("service=\"WFS\" ");
			sb.append("version=\"1.1.0\" ");
			sb.append("expiry=\"" + lockExpiryMinutes + "\" ");
			
			
			sb.append("xmlns:topp=\"http://www.openplans.org/topp\" ");
			sb.append("xmlns:wfs=\"http://www.opengis.net/wfs\" ");
			sb.append("xmlns:ogc=\"http://www.opengis.net/ogc\" ");
			sb.append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ");
			sb.append("xsi:schemaLocation=\"http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd\"> \n");
			

//			String handleString = "GetFeatureWithLock-Test";
//			String resultString = "Resultate";
//			StringBuilder sb = new StringBuilder();
//			sb.append("<?xml version=\"1.0\" ?> \n");
//			sb.append("<wfs:" + "GetFeatureWithLock ");
//			sb.append("service=\"WFS\" ");
//			sb.append("version=\"2.0.0\" ");
////			sb.append("handle=\"" + handleString + "\" ");
//			// Expiry mitgeben:
//			sb.append("expiry=\"" + lockExpiryMinutes + "\" ");
////			sb.append("resultType=\"" + resultString + "\" ");
//			sb.append("xmlns=\"http://www.opengis.net/wfs/2.0\" ");
//			sb.append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ");
//			sb.append("xmlns:wfs=\"http://www.opengis.net/wfs/2.0\" ");
////			sb.append("xmlns:topp='http://www.openplans.org/topp' ");
//			sb.append("xsi:schemaLocation=\"http://www.opengis.net/wfs/2.0 ");
//			sb.append("http://schemas.opengis.net/wfs/2.0/wfs.xsd\"");
////			sb.append("valueReference=\"the_geom\"");
//			sb.append("> \n");
//			sb.append("ReleaseAction=\"ALL\" ");
//			sb.append("LockAction=\"ALL\" ");
			// Layername mitgeben
			sb.append("<wfs:Query typeNames=\"" + layer.getWorkspace() + ":" + layer.getName() + "\"/> \n");
//			sb.append("<wfs:Query typeNames=\"topp:" + layer.getName() + "\"/> \n");
			sb.append("</wfs:" + "GetFeatureWithLock>");
			Log.e(CLASSTAG, "XML-Locking Command sent: \n" + sb.toString());
			return sb.toString();
			
			
			
//			StringBuilder sb = new StringBuilder();
//			sb.append("<?xml version=\"1.0\" ?>");
//			sb.append("<LockFeature ");
//			sb.append("version=\"2.0.0\" ");
//			sb.append("service=\"WFS\" ");
//			// Expiry mitgeben:
//			sb.append("expiry=\""+ lockExpiry +"\" ");
//			sb.append("ReleaseAction=\"ALL\" ");
//			sb.append("LockAction=\"ALL\" ");
//			sb.append("xmlns=\"http://www.opengis.net/wfs/2.0\" ");
//			sb.append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ");
//			sb.append("xsi:schemaLocation=\"http://www.opengis.net/wfs/2.0 ");
//			sb.append("http://schemas.opengis.net/wfs/2.0/wfs.xsd\">");
//			// Layername mitgeben
//			sb.append("<Query typeNames=\"" + newLayerName + "\"/>");
//			sb.append("</LockFeature>");
//			Log.e(CLASSTAG, "XML-Locking Command sent: \n" + sb.toString());
//			return sb.toString();
		}

		// nach der serverkommunikation
		// Nachricht ausgeben, ob Lock ok oder nicht
		@Override
		protected void onPostExecute(Boolean result) {
			dialog.cancel();
			if (result) {
				Alerts.errorMessage(context, context.getString(R.string.lockfeaturewfs_alert_positive_feedback_title), context.getString(R.string.lockfeaturewfs_alert_positive_feedback_message)).show();
				finished.onLockFinished(true);
			}
			else {
				Alerts.errorMessage(context, context.getString(R.string.lockfeaturewfs_alert_negative_feedback_title), context.getString(R.string.lockfeaturewfs_alert_negative_feedback_message)).show();
				finished.onLockFinished(false);
			}
		}

		// Nachricht ausgeben , wenn error
		@Override
		protected void onProgressUpdate(StatusInfo... status) {
			StatusInfo info = status[0];
			if (info.getStatus() < 0) {
				// TODO Uebersetzungen
				Alerts.errorMessage(context, "Error!", info.getMessage()).show();
			}
		}
	}
	
	public interface OnLockFinishedListener {
		public void onLockFinished(boolean finished);
	}
	
	/**
	 * Sets the OnImportFinishedListener called after finished response parsing.
	 * 
	 * @param l
	 */
	public void setOnLockFinishedListener(OnLockFinishedListener l) {
		finished = l;
	}
}
