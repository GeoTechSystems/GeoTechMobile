/**
 * Class for synchronization of updated features of a WFS-Layer 
 * with a WFS-T Server.
 * 
 * @author Torsten Hoch - tohoch@uos.de
 */

package unused;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.StringTokenizer;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
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

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Looper;
import android.util.Log;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.gml2.GMLWriter;

import de.geotech.systems.features.Feature;
import de.geotech.systems.projects.Project;
import de.geotech.systems.projects.ProjectHandler;
import de.geotech.systems.utilities.Functions;
import de.geotech.systems.wfs.WFSLayer;

/**
 * The Class WFSFeatureUpdateCon.
 */
public class WFSFeatureUpdateCon {
	/** The Constant CLASS_TAG. */
	private static final String CLASSTAG = "WFSFeatureUpdateCon";

	/** The context. */
	private Context context;
	/** The settings. */
	private Project project;
	/** The progress. */
	private ProgressDialog progress;
	/** The writer. */
	private GMLWriter writer;
	/** The success. */
	private boolean success;
	//	/** The layer is multi geom. */
	//	private boolean layerIsMultiGeom;
	/** The wfs layer. */
	private Feature editedFeature;
	/** The sync start. */
	private OnSyncStartListener syncStart;
	/** The sync finished. */
	private OnSyncFinishedListener syncFinished;
	// the WFS-Layer of the Feature
	private WFSLayer wfsLayer;

	/**
	 * Default constructor.
	 *
	 * @param newContext the context
	 * @param showProgress Decide whether to show progress or not.
	 * @param selectedLayer the selected wfs-layer
	 */
	public WFSFeatureUpdateCon(Context newContext, Feature newEditedFeature) {
		this.context = newContext;
		this.project = ProjectHandler.getCurrentProject();
		this.editedFeature = newEditedFeature;
		for (WFSLayer layer : ProjectHandler.getCurrentProject().getWFSContainer()) {
			if (layer.getLayerID() == editedFeature.getWFSlayerID()) {
				this.wfsLayer = layer;
			}
		}
		this.progress = new ProgressDialog(context);
		this.progress.setTitle("Synchronization");
		this.progress.setMessage("Updating features on WFS-T Server...");
		this.progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		this.progress.setProgress(0);
		this.writer = new GMLWriter();
		this.writer.setSrsName("http://www.opengis.net/gml/srs/epsg.xml#" + project.getEpsgCode());
		this.success = false;
		this.syncStart = null;
		this.syncFinished = null;
	}

	/**
	 * Runs the synchronization task.
	 */
	public void execute() {
		UpdateTask syncTask = new UpdateTask();
		syncTask.execute();
	}

	/**
	 * Class for the background task. Ein String wird uebergeben beim Aufruf von 
	 * execute, ein integer wird uebergeben, um statusanzeigen machen zu keonnen 
	 * und ein boolean wird intern uebergeben zum check, ob alles ok.
	 */
	private class UpdateTask extends AsyncTask<String, Integer, Boolean> {

		@Override
		protected void onPreExecute() {
			// layerIsMultiGeom = false;
			if (editedFeature != null) {

			}
			// Show Progress dialog
			progress.show();
			if (syncStart != null) {
				syncStart.onSyncStart();
			}
		}

		@Override
		protected Boolean doInBackground(String... params) {
			// Looper.prepare();
			// looper stuerzt beim sony tablet ab!
			Log.v(CLASSTAG, "Starting Update...");
			try {
				success = updateFeature();
			} catch (Exception e) {
				e.printStackTrace();
				Log.e(CLASSTAG, "ERROR in doInBackground : " + e.getMessage());
				Log.e(CLASSTAG, "Success: " + success);
			}
			return success;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			// Reset progress counters
			progress.cancel();
			if (syncFinished != null) {
				syncFinished.onSyncFinished(result, editedFeature);
			}
		}

		@Override
		protected void onProgressUpdate(Integer... prog) {
				progress.setProgress(prog[0]);
		}

		@Override
		protected void onCancelled() {
				progress.cancel();
		}

		/**
		 * Exports new features of a certain layer.
		 *
		 * @return booleean, false if it created an error
		 * @throws Exception the exception
		 */
		private boolean updateFeature() throws Exception {
			boolean noError = true;
			String gml;

			publishProgress(10);
			if (wfsLayer.isMultiGeom()) {
				gml = writer.write(toMultiGeom(editedFeature.getGeom()));
			} else {
				gml = writer.write(editedFeature.getGeom());
			}
			String lockIdHeaderExtension = "";
			// Bei Layern mit Feature Lock: 
			if (wfsLayer.isLocked() && wfsLayer.getLockID() != null) {
				lockIdHeaderExtension = " lockId=\"" + wfsLayer.getLayerID() + "\"";
			}
			String wfsInsertHeader = "<wfs:Transaction service=\"WFS\" version=\"1.0.0\""
					+ " xmlns=\"http://www.opengis.net/ogc\""
					+ " xmlns:"
					+ wfsLayer.getWorkspace()
					+ "=\""
					+ wfsLayer.getNamespace()
					+ "\""
					+ " xmlns:ogc=\"http://www.opengis.net/ogc\""
					+ " xmlns:wfs=\"http://www.opengis.net/wfs\""
					+ " xmlns:gml=\"http://www.opengis.net/gml\""
					+ lockIdHeaderExtension
					+ ">"
					+ "<wfs:Insert>"
					+ "	<"
					+ wfsLayer.getWorkspace()
					+ ":"
					+ wfsLayer.getName()
					+ ">"
					+ "	<"
					+ wfsLayer.getWorkspace()
					+ ":"
					+ wfsLayer.getGeometryColumn() 
					+ ">";
			String wfsInsertAttrs = "</" + wfsLayer.getWorkspace()
					+ ":" 
					+ wfsLayer.getGeometryColumn() 
					+ ">";
			StringTokenizer token = new StringTokenizer(editedFeature.getAttributes().toString());
			while (token.hasMoreTokens()) {
				String[] att = token.nextToken().split("=");
				String key = att[0];
				String value = att[1];
				wfsInsertAttrs = wfsInsertAttrs 
						+ "<"
						+ wfsLayer.getWorkspace() 
						+ ":" 
						+ key 
						+ ">"
						+ value 
						+ "</" 
						+ wfsLayer.getWorkspace() 
						+ ":"
						+ key 
						+ ">";
			}
			String wfsInsertFooter = "</" 
					+ wfsLayer.getWorkspace()
					+ ":" 
					+ wfsLayer.getName() + ">" 
					+ "</wfs:Insert>"
					+ "</wfs:Transaction>";
			String wfsInsert = wfsInsertHeader + gml + wfsInsertAttrs + wfsInsertFooter;
			Log.v(CLASSTAG, "New feature: " + wfsInsert);
			// Sending HTTP request
			URL url = new URL(Functions.reviseWfsUrl(wfsLayer.getUrl())
					+ "?SERVICE=WFS&VERSION=1.0.0&REQUEST=Transaction");
			Log.v(CLASSTAG, "Sending WFS insert to following URL: " + url.toString());
			HttpClient client = new DefaultHttpClient();
			HttpPost post = new HttpPost(url.toURI());
			StringEntity data = new StringEntity(wfsInsert);
			data.setContentType("text/xml");
			post.setHeader("Content-Type", "application/xml;charset=UTF-8");
			post.setEntity(data);
			HttpResponse response = client.execute(post);
			// Check for transaction success and update database
			if (responseOk(response)) {
				editedFeature.setSync(true);
				Log.v(CLASSTAG, "Feature: " + editedFeature.getGeom() + " syncronized");
			} else {
				return false;				
			}
			publishProgress(100);
			return noError;
		}
	}

	/**
	 * Checks if WFS transaction was successful.
	 *
	 * @param res the res
	 * @return true, if successful
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws SAXException the SAX exception
	 * @throws ParserConfigurationException the parser configuration exception
	 */
	private boolean responseOk(HttpResponse res) throws IOException,
	SAXException, ParserConfigurationException {
		// Get response string
		String xml = EntityUtils.toString(res.getEntity());
		Log.v(CLASSTAG, "Received transaction response:\n" + xml);
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
		if (handler.transactionSuccess())
			return true;
		else {
			if (handler.totalInserted() == 1)
				return true;
			else
				return false;
		}
	}

	/**
	 * ContentHandler for parsing the WFS transaction response.
	 */
	public class ResponseHandler implements ContentHandler {
		/** The current value. */
		private StringBuilder currentValue;
		/** The total inserted. */
		private int totalInserted;
		/** The total updated. */
		private int totalUpdated;
		/** The total deleted. */
		private int totalDeleted;
		/** The in status tag. */
		private boolean inStatusTag;
		/** The success. */
		private boolean success;

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
			if (localName.equals("Status"))
				inStatusTag = false;
			else if (localName.equals("totalInserted"))
				totalInserted = Integer.parseInt(currentValue.toString());
			else if (localName.equals("totalUpdated"))
				totalUpdated = Integer.parseInt(currentValue.toString());
			else if (localName.equals("totalDeleted"))
				totalDeleted = Integer.parseInt(currentValue.toString());
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
			currentValue = new StringBuilder();
			totalInserted = 0;
			totalUpdated = 0;
			totalDeleted = 0;
			inStatusTag = false;
			success = false;
		}

		@Override
		public void startElement(String uri, String localName, String qName,
				Attributes atts) throws SAXException {
			currentValue.setLength(0);
			if (localName.equals("Status"))
				inStatusTag = true;
			else if (localName.equals("SUCCESS") && inStatusTag)
				success = true;
		}

		@Override
		public void startPrefixMapping(String prefix, String uri)
				throws SAXException {
		}

		/**
		 * Returns number of inserted features.
		 *
		 * @return the int
		 */
		public int totalInserted() {
			return totalInserted;
		}

		/**
		 * Returns number of updated features.
		 *
		 * @return the int
		 */
		public int totalUpdated() {
			return totalUpdated;
		}

		/**
		 * Returns number of deleted features.
		 *
		 * @return the int
		 */
		public int totalDeleted() {
			return totalDeleted;
		}

		/**
		 * Returns number of features affected (inserted, updated, deleted) by
		 * the transaction.
		 *
		 * @return the int
		 */
		public int totalAffected() {
			return totalInserted + totalUpdated + totalDeleted;
		}

		/**
		 * Returns whether the transaction was successful.
		 *
		 * @return true, if successful
		 */
		public boolean transactionSuccess() {
			return success;
		}
	}

	/**
	 * Interface for the OnSyncStartListener.
	 */
	public interface OnSyncStartListener {

		/**
		 * On sync start.
		 */
		public void onSyncStart();
	}

	/**
	 * Interface for the OnSyncFinishedListener.
	 */
	public interface OnSyncFinishedListener {

		/**
		 * On sync finished.
		 *
		 * @param result the result
		 * @param selectedLayer the selected layer
		 */
		public void onSyncFinished(boolean result, Feature feature);
	}

	/**
	 * Sets the OnSyncStartListener.
	 *
	 * @param onSyncStartListener the new on sync start listener
	 */
	public void setOnSyncStartListener(OnSyncStartListener onSyncStartListener) {
		syncStart = onSyncStartListener;
	}

	/**
	 * Sets the OnSyncFinishedListener.
	 *
	 * @param onSyncFinishedListener the new on sync finished listener
	 */
	public void setOnSyncFinishedListener(
			OnSyncFinishedListener onSyncFinishedListener) {
		syncFinished = onSyncFinishedListener;
	}

	/**
	 * To multi geom.
	 *
	 * @param input the input
	 * @return the geometry
	 */
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

}
