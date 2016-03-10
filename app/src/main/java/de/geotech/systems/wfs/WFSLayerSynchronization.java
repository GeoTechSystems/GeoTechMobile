/**
 * Class for synchronization of new data with WFS.
 * 
 * @author Karsten
 * @author Paul Vincent Kuper (kuper@kit.edu)
 * @author Sven Weisker 
 * @author Torsten Hoch - tohoch@uos.de
 */

package de.geotech.systems.wfs;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import de.geotech.systems.R;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.gml2.GMLWriter;

import de.geotech.systems.database.DBAdapter;
import de.geotech.systems.features.Feature;
import de.geotech.systems.projects.Project;
import de.geotech.systems.projects.ProjectHandler;
import de.geotech.systems.projects.Server;
import de.geotech.systems.utilities.Functions;

public class WFSLayerSynchronization {
	private static final String CLASSTAG = "WFSLayerSynchronization";
	// WFS-Versions
	private static final String WFS_VERSION_110 = "1.1.0";
	private static final String WFS_VERSION_100 = "1.0.0";
	// gml-EPSG-Adress for gml writer
	private static final String SRS_GML_URL = "http://www.opengis.net/gml/srs/epsg.xml#";
	// when no ID was given, this is the standard
	private static final String STANDARD_NO_ID = Feature.INVALIDGEOSERVERID;

	// the context
	private Context context;
	// the current project
	private Project project;
	// showing progress-dialog
	private boolean showProg;
	// the progress-dialog
	private ProgressDialog progress;
	// the gml-writer
	private GMLWriter writer;
	// count of updated/inserted features
	private int count;
	// step
	private int step;
	// if the transaction succeeds
	private boolean success;
	// the layer to be synchronized
	private WFSLayer wfsLayer;
	// Listener for start und finish
	private OnSyncStartListener syncStart;
	private OnSyncFinishedListener syncFinished;
	private String geoServerID;
	private String currentWFSVersion;
	private boolean geoServerIDChange;

	/**
	 * Instantiates a new WFS layer synchronization.
	 *
	 * @param newContect the new contect
	 * @param showProgress the show progress
	 * @param selectedLayer the selected layer
	 */
	public WFSLayerSynchronization(Context newContect, boolean showProgress, WFSLayer selectedLayer) {
		this.context = newContect;
		this.project = ProjectHandler.getCurrentProject();
		this.wfsLayer = selectedLayer;
		this.showProg = showProgress;
		this.progress = new ProgressDialog(context);
		// TODO uebersetzungen
		this.progress.setTitle(context.getString(R.string.transaction_progress_title));
		this.progress.setMessage(context.getString(R.string.transaction_progress_message));
		this.progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		this.progress.setProgress(0);
		this.writer = new GMLWriter();
		this.writer.setSrsName(SRS_GML_URL + project.getEpsgCode());
		this.count = 0;
		this.step = 0;
		this.success = false;
		this.syncStart = null;
		this.syncFinished = null;
		this.geoServerID = Feature.INVALIDGEOSERVERID;  
	}

	/**
	 * Instantiates a new WFS layer synchronization.
	 *
	 * @param newContext the new context
	 * @param showProgress the show progress
	 */
	public WFSLayerSynchronization(Context newContext, boolean showProgress) {
		this(newContext, showProgress, null);
	}
	/**
	 * Resets the feature counter.
	 */
	public void reset() {
		this.count = 0;
		this.step = 0;
		this.success = false;
	}

	/**
	 * Runs the synchronization task.
	 */
	public void execute() {
		SyncInBackground syncTask = new SyncInBackground();
		syncTask.execute();
	}

	/**
	 * Class for the background task.
	 * 
	 * @author Karsten
	 */
	private class SyncInBackground extends AsyncTask<Void, Integer, Boolean> {
		private DBAdapter dbAdapter = new DBAdapter(context);
		

		@Override
		protected void onPreExecute() {
			// Reset layer variables
			if (wfsLayer != null) {
				for (Feature feature : wfsLayer.getFeatureContainer()) {
					if (!feature.isSync()) {
						count++;
					}
				}
			}
			// Show Progress dialog
			if (showProg) {
				progress.show();
			} else if (syncStart != null) {
				syncStart.onSyncStart();
			}
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			// Looper.prepare();
			// looper stuerzt beim sony tablet ab!
			// Log.i(CLASSTAG, "Starting synchronization of all unsynchronized Features of Layer " + wfsLayer .getName() + "...");
			try {
				success = synchronizeLayer();
			} catch (Exception e) {
				e.printStackTrace();
				Log.e(CLASSTAG, "Error with Transaction in doInBackground : " + e.getMessage() + " - Transaction success status: " + success);
			}
			return success;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			// Reset progress counters
			if (showProg) {
				progress.cancel();
			}
			if (syncFinished != null) {
				syncFinished.onSyncFinished(result, wfsLayer);
			}
			reset();
		}

		@Override
		protected void onProgressUpdate(Integer... prog) {
			if (showProg) {
				progress.setProgress(prog[0]);
			}
		}

		@Override
		protected void onCancelled() {
			if (showProg) {
				progress.cancel();
			}
		}

		/**
		 * Exports new features of a certain layer.
		 * 
		 * @return booleean, false if it created an error
		 * @throws ParseException
		 */
		private boolean synchronizeLayer() throws Exception {
			boolean noError = true;
			String xmlStatement;
			URL url;
			DefaultHttpClient client;
			HttpPost post;
			StringEntity data;
			HttpResponse response;
			Server server = ProjectHandler.getServer(wfsLayer.getUrl());
			for (Feature feature : wfsLayer.getFeatureContainer()) {
				if (!feature.isSync()) {
					currentWFSVersion = WFS_VERSION_100;
					geoServerID = feature.getGeoServerID();
					// getting xml transaction statement
//					if (feature.getFeatureType() == WFSLayer.LAYER_TYPE_POLYGON) {
//						currentWFSVersion = WFS_VERSION_100;
//						xmlStatement = old(feature, wfsLayer);
//						Log.e(CLASSTAG, "Sent OLD XML Transaction Request: \n" + xmlStatement);
//					} else  {
//						currentWFSVersion = WFS_VERSION_100;
//						xmlStatement = transactionXMLString(feature, wfsLayer);
//						Log.e(CLASSTAG, "Sent XML Transaction Request: \n" + xmlStatement);
//					}
					xmlStatement = transactionXMLString(feature, wfsLayer);
					Log.i(CLASSTAG, "Sent XML Transaction Request: \n" + xmlStatement);
					// Sending HTTP request
					url = new URL(Functions.reviseWfsUrl(wfsLayer.getUrl()) + "?SERVICE=WFS&VERSION=" + currentWFSVersion + "&REQUEST=Transaction");
					Log.i(CLASSTAG, "Sending WFS Transaction Request to URL: \n" + url.toString());
					client = new DefaultHttpClient();
					/// wenn username gesetzt - authenticated abfrage
					if (!server.getUsername().equals("")) {
						client.getCredentialsProvider().setCredentials(AuthScope.ANY,
								new UsernamePasswordCredentials(server.getUsername(), server.getPassword()));
					} 
					post = new HttpPost(url.toURI());
					data = new StringEntity(xmlStatement);
					data.setContentType("text/xml");
					post.setHeader("Content-Type", "application/xml;charset=UTF-8");
					post.setEntity(data);
					response = client.execute(post);
					// Check for transaction success and update database
					if (isResponseOk(response)) {
						feature.setSync(true);
						feature.setGeoServerID(geoServerID);
						dbAdapter.updateFeatureInDB(feature);
						// Log.i(CLASSTAG, "Feature: " + feature.getFeatureID() + " syncronized");
					} else {
						noError = false;				
					}
					step++;
					publishProgress((int) (((float) step / (float) count) * 100f));

				}
			}
			if (noError) {
				wfsLayer.setSync(true);
				dbAdapter.updateWFSLayerInDB(wfsLayer);
			}
			return noError;
		}

		private String transactionXMLString(Feature feature, WFSLayer layer) {
			if (feature.getGeoServerID().equals(STANDARD_NO_ID)) {
				geoServerIDChange = true;
				return getInsertTransactionWFS110XML(feature, layer);
			} else {
				geoServerIDChange = false;
				return getUpdateTransactionWFS110XML(feature, layer);
			}
		}
		
		private String getInsertTransactionWFS110XML(Feature feature, WFSLayer layer) {
			String xmlHeaderPart1 = getTransactionHeader(feature, layer);
			String transactionName = "Insert";
			Log.i(CLASSTAG, "Transaction: " + transactionName);
			String xmlHeaderPart2 = "<wfs:" + transactionName + ">" + "\n"
					+ " <" + layer.getWorkspace() + ":" + layer.getName()	+ ">" + "\n"
					+ "	<" + layer.getWorkspace() + ":" + layer.getGeometryColumn() + ">" + "\n";
			String xmlInsertFooterPart1 = "</" + layer.getWorkspace() + ":" + layer.getGeometryColumn() + ">" + "\n";
			String gmlBody = getGML2Body(feature, layer);
			String xmlInsertKeyValueBody = getInsertKeyValueBody(feature, layer);
			String xmlFooterPart2 = "</" + layer.getWorkspace() + ":" + layer.getName() + ">" + "\n"; 
			String xmlFooterPart3 = "</wfs:" + transactionName + ">" + "</wfs:Transaction>" + "\n";
			return xmlHeaderPart1 + xmlHeaderPart2 + gmlBody + xmlInsertFooterPart1 + xmlInsertKeyValueBody + xmlFooterPart2 + xmlFooterPart3;
		}

		private String getUpdateTransactionWFS110XML(Feature feature, WFSLayer layer) {
			String xmlHeaderPart1 = getTransactionHeader(feature, layer);
			String transactionName = "Update";
			Log.i(CLASSTAG, "Transaction: " + transactionName);
			String ogcBody = "<ogc:Filter> <ogc:FeatureId fid=\"" + feature.getGeoServerID() + "\"/> </ogc:Filter>" + "\n";
			String xmlUpdateHeaderPart2 = "<wfs:" + transactionName +  " typeName=\"" + layer.getWorkspace() + ":" + layer.getName() 
					+ "\">" + "\n";
			String xmlUpdateBody = getUpdateKeyValueBody(feature, layer);
			String xmlFooterPart = "</wfs:" + transactionName + ">" + "</wfs:Transaction>" + "\n";
			return xmlHeaderPart1 + xmlUpdateHeaderPart2 + xmlUpdateBody + ogcBody + xmlFooterPart;
		}

		// TODO: GML 2, eigentlich unterstützt WFS 1.10 aber nur GML3 !!!
		private String getGML2Body(Feature feature, WFSLayer layer) {
			if (layer.isMultiGeom()) {
				return writer.write(toMultiGeom(feature.getGeom()));
			} else {
				return writer.write(feature.getGeom());
			}
		}

		private String getTransactionHeader(Feature feature, WFSLayer layer) {
			String xmlStart = ""; // "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
			String lockIdHeaderExtension = "";
			// Bei Layern mit Feature Lock: 
			if (layer.isLocked() && layer.getLockID() != null) {
				lockIdHeaderExtension = " lockId=\"" + layer.getLockID() + "\"";
			}
			String xmlHeader = "<wfs:Transaction" 
					+ " service=\"WFS\"" 
					+ " version=\"" + currentWFSVersion + "\""
					+ " xmlns=\"http://www.opengis.net/wfs\""
					+ " xmlns:"	+ layer.getWorkspace() + "=\"" + layer.getNamespace()	+ "\""
					+ " xmlns:ogc=\"http://www.opengis.net/ogc\""
					+ " xmlns:wfs=\"http://www.opengis.net/wfs\""
					+ " xmlns:gml=\"http://www.opengis.net/gml\""
					// + " xmlns=\"http://www.opengis.net/ogc\""
					// + " xsi:schemaLocation=\"http://www.opengis.net/wfs" 
					// + " " + Functions.reviseWfsUrl(layer.getUrl()) 
					// + "?Service=WFS&Request=DescribeFeatureType&version=" + VERSION
					// + "&typename=" + layer.getWorkspace() + ":" + layer.getName() + "\""
					+ lockIdHeaderExtension	+ ">" + "\n";
			return xmlStart + xmlHeader;
		}
		
		private String getInsertKeyValueBody(Feature feature, WFSLayer layer) {
			String xmlInsertKeyValueBody = "";
			ContentValues cv = feature.getAttributes();
			Set<String> stringSet = cv.keySet();
			Iterator<String> iterator = stringSet.iterator();
			String key;
			String value;
			String newInsertPair;
			while (iterator.hasNext()) {
				key = iterator.next();
				value = String.valueOf(cv.get(key));
				// Log.e(CLASSTAG, "Pair: Key \"" + key + "\" - Value: \"" + value + "\"");
				if (!value.equalsIgnoreCase("")) { 
					newInsertPair = "<" + layer.getWorkspace() + ":" + key + ">" + value + "</" + layer.getWorkspace() + ":" + key + ">";
					xmlInsertKeyValueBody = xmlInsertKeyValueBody + newInsertPair;
				}
			}
			return xmlInsertKeyValueBody;
		}

		private String getUpdateKeyValueBody(Feature feature, WFSLayer layer) {
			String gmlBody = getGML2Body(feature, layer);
			String gmlUpdate = "<" + layer.getWorkspace() + ":Name>" + layer.getGeometryColumn() + "</" + layer.getWorkspace() + ":Name>"
					+ "<" + layer.getWorkspace() + ":Value>" + gmlBody + "</" + layer.getWorkspace() + ":Value>";
			String xmlUpdatepropertyHeader = "<wfs:Property>"; 
			String xmlUpdatePropertyFooter = "</wfs:Property>" + "\n";
			ContentValues cv = feature.getAttributes();
			Set<String> stringSet = cv.keySet();
			Iterator<String> iterator = stringSet.iterator();
			String key;
			String value;
			String xmlUpdateKeyValueBody = "";
			String newUpdatePair;
			while (iterator.hasNext()) {
				key = iterator.next();
				value = String.valueOf(cv.get(key));
				// Log.e(CLASSTAG, "Pair: Key \"" + key + "\" - Value: \"" + value + "\"");
				xmlUpdateKeyValueBody = xmlUpdateKeyValueBody + xmlUpdatepropertyHeader;
				newUpdatePair = "<" + layer.getWorkspace() + ":Name>" + key + "</" + layer.getWorkspace() + ":Name>"
						+ "<" + layer.getWorkspace() + ":Value>" + value + "</" + layer.getWorkspace() + ":Value>";
				xmlUpdateKeyValueBody = xmlUpdateKeyValueBody + newUpdatePair; 
				xmlUpdateKeyValueBody = xmlUpdateKeyValueBody + xmlUpdatePropertyFooter;
			}
			return xmlUpdatepropertyHeader + gmlUpdate + xmlUpdatePropertyFooter + xmlUpdateKeyValueBody;
		}

	}

	/**
	 * Checks if WFS transaction was successful.
	 * 
	 * @param res
	 * @return
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	private boolean isResponseOk(HttpResponse res) throws IOException, SAXException, ParserConfigurationException {
		// Get response string
		String xml = EntityUtils.toString(res.getEntity());
		Log.i(CLASSTAG, "Received transaction response: \n" + xml);
		// Define xml reader and content handler
		SAXParserFactory spf = SAXParserFactory.newInstance();
		SAXParser parser = spf.newSAXParser();
		XMLReader reader = parser.getXMLReader();
		ResponseHandler handler = new ResponseHandler();
		reader.setContentHandler(handler);
		// Define input source
		InputSource input = new InputSource();
		input.setCharacterStream(new StringReader(xml));
		// Parse xml string
		reader.parse(input);
		// success for WFS 1.0.0
		if (handler.transactionSuccess()) {
			if (geoServerIDChange) {
				geoServerID = handler.getGeoServerID();
				Log.e(CLASSTAG, "New GeoServer-ID set: " + geoServerID);
			}
			// Log.i(CLASSTAG, "Transaction successful.");
			return true;
		} else {
			// success for WFS 1.1.0
			if (handler.totalInserted() > 0 || handler.totalUpdated() > 0) {
				if (handler.totalInserted == 1 && geoServerIDChange) {
					geoServerID = handler.getGeoServerID();
					Log.e(CLASSTAG, "New GeoServer-ID set: " + geoServerID);
				}
				// Log.i(CLASSTAG, "Transaction successful.");
				return true;
			} else {
				Log.i(CLASSTAG, "Transaction not successful.");
				return false;
			}
		}
	}

	/**
	 * ContentHandler for parsing the WFS transaction response.
	 * 
	 * @author Karsten
	 */
	public class ResponseHandler implements ContentHandler {

		private StringBuilder currentValue;
		private boolean inStatusTag;
		private boolean success;
		private int totalDeleted;
		private int totalUpdated;
		private int totalInserted;
		private String geoServerID;

		@Override
		public void characters(char[] ch, int start, int length)
				throws SAXException {
			currentValue.append(ch, start, length);
		}

		@Override
		public void endDocument() throws SAXException {
		}

		@Override
		public void endElement(String uri, String localName, String qName)
				throws SAXException {
			if (localName.equals("Status")){
				inStatusTag = false;
			} else if (localName.equals("totalInserted")) { 
				totalInserted = totalInserted + Integer.parseInt(currentValue.toString());
			} else if (localName.equals("totalUpdated")) { 
				totalUpdated = totalUpdated + Integer.parseInt(currentValue.toString());
			} else if (localName.equals("totalDeleted")) {
				totalDeleted = totalDeleted + Integer.parseInt(currentValue.toString());
			}
		}

		@Override
		public void endPrefixMapping(String prefix) throws SAXException {
		}

		@Override
		public void ignorableWhitespace(char[] ch, int start, int length)
				throws SAXException {
		}

		@Override
		public void processingInstruction(String target, String data)
				throws SAXException {
		}

		@Override
		public void setDocumentLocator(Locator locator) {
		}

		@Override
		public void skippedEntity(String name) throws SAXException {
		}

		@Override
		public void startDocument() throws SAXException {
			this.currentValue = new StringBuilder();
			this.inStatusTag = false;
			this.success = false;
			this.totalInserted = 0;
			this.totalUpdated = 0;
			this.totalDeleted = 0;
			this.geoServerID = STANDARD_NO_ID;
		}

		@Override
		public void startElement(String uri, String localName, String qName,
				Attributes atts) throws SAXException {
			currentValue.setLength(0);
			// success for WFS 1.0.0
			if (localName.equals("Status"))
				inStatusTag = true;
			else if (localName.equals("SUCCESS") && inStatusTag)
				success = true;
			if (qName.equalsIgnoreCase("ogc:FeatureID")) {
				geoServerID = atts.getValue("fid");
				// Log.e(CLASSTAG, "New GeoServer-ID given: " + geoServerID);
			}
		}

		public String getGeoServerID() {
			return geoServerID;
		}

		@Override
		public void startPrefixMapping(String prefix, String uri)
				throws SAXException {
		}

		/**
		 * Returns number of inserted features.
		 * 
		 * @return
		 */
		public int totalInserted() {
			return totalInserted;
		}

		/**
		 * Returns number of updated features.
		 * 
		 * @return
		 */
		public int totalUpdated() {
			return totalUpdated;
		}

		/**
		 * Returns number of deleted features.
		 * 
		 * @return
		 */
		public int totalDeleted() {
			return totalDeleted;
		}

		/**
		 * Returns number of features affected (inserted, updated, deleted) by
		 * the transaction.
		 * 
		 * @return
		 */
		public int totalAffected() {
			return totalInserted + totalUpdated + totalDeleted;
		}

		/**
		 * Returns whether the transaction was successful.
		 * 
		 * @return
		 */
		public boolean transactionSuccess() {
			return success;
		}
	}

	/**
	 * Interface for the OnSyncStartListener.
	 * 
	 * @author Karsten
	 */
	public interface OnSyncStartListener {
		public void onSyncStart();
	}

	/**
	 * Interface for the OnSyncFinishedListener.
	 * 
	 * @author Karsten
	 */
	public interface OnSyncFinishedListener {
		public void onSyncFinished(boolean result, WFSLayer selectedLayer);
	}

	/**
	 * Sets the OnSyncStartListener.
	 * 
	 * @param onSyncStartListener
	 */
	public void setOnSyncStartListener(OnSyncStartListener onSyncStartListener) {
		syncStart = onSyncStartListener;
	}

	/**
	 * Sets the OnSyncFinishedListener.
	 * 
	 * @param onSyncFinishedListener
	 */
	public void setOnSyncFinishedListener(
			OnSyncFinishedListener onSyncFinishedListener) {
		syncFinished = onSyncFinishedListener;
	}

	private Geometry toMultiGeom(Geometry input) {
		Geometry output = null;
		if (input.getGeometryType().equals("Point")) {
			output = new GeometryFactory().createMultiPoint(
					new Point[] {
							(Point) input 
					});
		}
		else if (input.getGeometryType().equals("LineString")) {
			output = new GeometryFactory().createMultiLineString(
					new LineString[] {
							(LineString) input 
					});
		}
		else if (input.getGeometryType().equals("Polygon")) {
			output = new GeometryFactory().createMultiPolygon(
					new Polygon[] {
							(Polygon) input
					});
		}
		if (output == null) {
			return input;
		} else {
			return output;
		}
	}

	private String old(Feature f, WFSLayer layer) {
		String gml;
		if (layer.isMultiGeom())
			gml = writer.write(toMultiGeom(f.getGeom()));
		else
			gml = writer.write(f.getGeom());
		String lockIdHeaderExtension = "";
		// Bei Layern mit Feature Lock: 
		if(layer.isLocked() && layer.getLockID() != null) {
			lockIdHeaderExtension = " lockId=\"" + layer.getLockID() + "\"";
		}
		String wfsInsertHeader = "<wfs:Transaction service=\"WFS\" version=\"" + currentWFSVersion + "\""
				+ " xmlns=\"http://www.opengis.net/ogc\""
				+ " xmlns:"
				+ layer.getWorkspace()
				+ "=\""
				+ layer.getNamespace()
				+ "\""
				+ " xmlns:ogc=\"http://www.opengis.net/ogc\""
				+ " xmlns:wfs=\"http://www.opengis.net/wfs\""
				+ " xmlns:gml=\"http://www.opengis.net/gml\""
				+ lockIdHeaderExtension
				+ ">"
				+ "<wfs:Insert>"
				+ "	<"
				+ layer.getWorkspace()
				+ ":"
				+ layer.getName()
				+ ">"
				+ "	<"
				+ layer.getWorkspace()
				+ ":"
				+ layer.getGeometryColumn() 
				+ ">";
		String wfsInsertAttrs = "</" + layer.getWorkspace()
				+ ":" 
				+ layer.getGeometryColumn() 
				+ ">";
		StringTokenizer token = new StringTokenizer(f
				.getAttributes().toString());
		while (token.hasMoreTokens()) {
			String[] att = token.nextToken().split("=");
			String key = att[0];
			String value;
			if (att.length > 1) {
				value = att[1];
			} else {
				value = "";
			}
			wfsInsertAttrs = wfsInsertAttrs 
					+ "<"
					+ layer.getWorkspace() 
					+ ":" 
					+ key 
					+ ">"
					+ value 
					+ "</" 
					+ layer.getWorkspace() 
					+ ":"
					+ key 
					+ ">";
		}
		String wfsInsertFooter = "</" 
				+ layer.getWorkspace()
				+ ":" 
				+ layer.getName() + ">" 
				+ "</wfs:Insert>"
				+ "</wfs:Transaction>";
		String wfsInsert = wfsInsertHeader + gml + wfsInsertAttrs
				+ wfsInsertFooter;
		// Log.e(CLASSTAG, "Returning OLD Statement: \n" + wfsInsert);
		return wfsInsert;
	}

}
