/**
 * Class for WFS Feature Imports from Geoserver
 * 
 * @author sven weisker
 * @author Karsten
 * @author Paul Vincent Kuper (kuper@kit.edu)
 * @author Torsten Hoch
 */

package de.geotech.systems.wfs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.ListIterator;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

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
import com.vividsolutions.jts.io.WKTWriter;
import com.vividsolutions.jts.io.gml2.GMLReader;

import de.geotech.systems.R;
import de.geotech.systems.LGLSpecial.LGLValues;
import de.geotech.systems.database.DBAdapter;
import de.geotech.systems.features.Feature;
import de.geotech.systems.projects.Project;
import de.geotech.systems.projects.ProjectHandler;
import de.geotech.systems.projects.Server;
import de.geotech.systems.utilities.Alerts;
import de.geotech.systems.utilities.Functions;
import de.geotech.systems.utilities.StatusInfo;

public class WFSFeatureImport {
	private static final String CLASSTAG = "WFSFeatureImport";

	public static final String STANDARD_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
	private static final String STANDARD_SMALL_DATE_FORMAT = "yyyy-MM-dd";

	private Context context;
	private Project project;
	private OnImportFinishedListener finished;
	private WFSLayer layer;
	private DBAdapter dbAdapter;
	private WFSFeatureImportTask importTask;

	/**
	 * Default constructor.
	 * 
	 * @param context
	 */
	public WFSFeatureImport(Context context) {
		this.context = context;
		this.project = ProjectHandler.getCurrentProject();
		this.dbAdapter = new DBAdapter(context);
		this.importTask = new WFSFeatureImportTask();
		this.finished = null;
	}

	/**
	 * Imports features of the layer with given id using background task.
	 * 
	 * @param id The WFS-Layer
	 */
	public void importFeatures(WFSLayer layer, int sumFeaturesToImport) {
		this.layer = layer;
		// alle Feratures etc. aus der DB loeschen
		this.dbAdapter.deleteAllFeaturesOfALayerFromDB(layer);
		// alle Features aus dem RAM loeschen
		this.layer.getFeatureContainer().clear();
		// alle features aus Index loeschen
		this.layer.restartIndex();
		this.importTask.setSumFeaturesToImport(sumFeaturesToImport);
		this.importTask.execute(layer);
	}

	/**
	 * Sets the OnImportFinishedListener called after finished response parsing.
	 * 
	 * @param listener
	 */
	public void setOnImportFinishedListener(OnImportFinishedListener listener) {
		finished = listener;
	}

	public interface OnImportFinishedListener {
		public void onImportFinished(boolean finished);
	}

	/**
	 * The inner Class WFSFeatureImportTask.
	 * 
	 * @author Torsten Hoch
	 */
	private class WFSFeatureImportTask extends AsyncTask<WFSLayer, StatusInfo, Boolean> {
		private static final String CLASSTAG = "WFSFeatureImportTask";

		private Context taskContext;
		private ProgressDialog dialog;
		private WFSFeatureImportHandler handler;
		private int sumFeaturesToImport = 0;

		@Override
		protected void onPreExecute() {
			taskContext = context;
			handler = new WFSFeatureImportHandler();
			dialog = new ProgressDialog(taskContext);
			dialog.setTitle(context.getString(R.string.layerManager_feature_import_result_title));
			dialog.setMessage(context.getString(R.string.layerManager_feature_import_loading));
			dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			if (sumFeaturesToImport == 0) {
				sumFeaturesToImport = layer.getCountFeatures();
			}
			dialog.setMax(sumFeaturesToImport);
			dialog.show();
		}

		@Override
		protected Boolean doInBackground(WFSLayer... layers) {
			WFSLayer layer = layers[0];
			boolean noError = true;
			if (sumFeaturesToImport != layer.getCountFeatures()) {
				for (int layerIndex = project.getWFSContainer().size() - 1; layerIndex >= 0; layerIndex--) {
					layer = project.getWFSContainer().get(layerIndex);
					layer.getFeatureContainer().clear();
					// Log.e(CLASSTAG + " doInBackground", "Cleared all Features of Layer " + layer.getName() 
					// 		+ " - # of Features now: " + layer.getFeatureContainer().size());
					noError = manageImport(handler, layer);
					if (!noError) {
						break;
					}
				}
			} else {
				noError = manageImport(handler, layer);
			}
			return noError;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (result) {
				dialog.cancel();
			} else {
				dialog.cancel();
				Alerts.errorMessage(taskContext,
						context.getString(R.string.layerManager_feature_import_result_title),
						context.getString(R.string.layerManager_feature_import_result_error_message))
						.show();
			}
			finished.onImportFinished(result);
		}

		@Override
		protected void onProgressUpdate(StatusInfo... status) {
			int count = status[0].getStatus();
			dialog.setProgress(count);
			if (count < 0) {
				Alerts.errorMessage(taskContext, "Error!", status[0].getMessage()).show();
			}
		}

		private Boolean manageImport(WFSFeatureImportHandler handler, WFSLayer layer) {
			Boolean noError = true;
			try {
				URL url;
				// Set handler
				handler.setLayerAttrs(layer ,this);
				// set request and parse response
				if (layer.isLocked()) {
					// TODO:
					// Falls es sich um eine Lock Anfrage handelt muss eine
					// getFeatureWithLock Anfrage gestellt werden, ist aber hier noch
					// wie im else-Teil eine getFeature-Anfrage
					url = new URL(Functions.reviseWfsUrl(layer.getUrl())
							+ "?SERVICE=WFS&REQUEST=getFeature&TYPENAME=" + layer.getWorkspace() 
							+ ":" + layer.getName() + "&OUTPUTFORMAT=GML2&SRSNAME=epsg:" + project.getEpsgCode());
				} else {
					url = new URL(Functions.reviseWfsUrl(layer.getUrl())
							+ "?SERVICE=WFS&REQUEST=getFeature&TYPENAME=" + layer.getWorkspace() + ":" 
							+ layer.getName() + "&OUTPUTFORMAT=GML2&SRSNAME=epsg:"	+ project.getEpsgCode());
				}
				SAXParserFactory spf = SAXParserFactory.newInstance();
				SAXParser parser = spf.newSAXParser();
				XMLReader reader = parser.getXMLReader();
				reader.setContentHandler(handler);
				InputStreamReader xmlStream;
				final Server server = ProjectHandler.getServer(layer.getUrl());
				/// wenn username gesetzt authenticated abfrage
				if (server.getUsername() != null && !server.getUsername().equalsIgnoreCase("")) {
					Authenticator.setDefault(new Authenticator(){
						protected PasswordAuthentication getPasswordAuthentication() {
							return new PasswordAuthentication(server.getUsername(), server.getPassword().toCharArray());
						}});
					HttpURLConnection c = (HttpURLConnection) new URL(url.toString()).openConnection();
					c.setUseCaches(false);
					c.setConnectTimeout(5000);
					c.connect();
					Log.i(CLASSTAG, "Sending HTTP-AUTH getFeature request to: " + url.toString() + " with userName " + server.getUsername());
					xmlStream = new InputStreamReader(c.getInputStream());
				} else {
					// sonst nur normale abfrage
					URLConnection c = url.openConnection();
					c.setConnectTimeout(500000000);
					xmlStream = new InputStreamReader(c.getInputStream());
					Log.i(CLASSTAG, "Sending standard getFeature request to: " + url.toString());
				}
				BufferedReader buffered = new BufferedReader(xmlStream);
				reader.parse(new InputSource(buffered));			
			} catch (SAXException e) {
				// publishProgress(new StatusInfo(-1, "SAXException:\n" + e.getMessage()));
				Log.v(CLASSTAG, "SAXException:\n" + e.getMessage());
				noError = false;
			} catch (IOException e) {
				// publishProgress(new StatusInfo(-1, "IOException:\n" + e.getMessage()));
				Log.v(CLASSTAG, "IOException:\n" + e.getMessage());
				noError = false;
			} catch (ParserConfigurationException e) {
				// publishProgress(new StatusInfo(-1, "ParserConfigurationException:\n" + e.getMessage()));
				Log.v(CLASSTAG, "ParserConfigurationException:\n" + e.getMessage());
				noError = false;
			}
			return noError;
		}

		public void setSumFeaturesToImport(int sumFeaturesToImport) {
			this.sumFeaturesToImport = sumFeaturesToImport;
		}

	}

	/**
	 * Inner Handler for parsing the getfeature response.
	 * 
	 * @author Karsten
	 * @author Torsten Hoch
	 */
	private class WFSFeatureImportHandler implements ContentHandler {
		private static final String HANDLER_TAG = "WFSFeatureImportHandler";

		private WFSLayer currentLayer;
		private StringBuilder currentValue;
		private ContentValues values;
		private String geomGML = "";
		private long layerId;
		private String layerName;
		private String layerWorkspace;
		private String geometryColumn;
		private ArrayList<WFSLayerAttributeTypes> attributes;
		private boolean listen;
		private boolean geometry;
		private boolean bounds;
		private boolean noError;
		private WFSFeatureImportTask task;
		private ArrayList<Feature> featureList;
		private String geoServerID;
		private int parsedfeatureCount;
		private Boolean isDone;

		public void setLayerAttrs(WFSLayer l, WFSFeatureImportTask task) {
			this.currentLayer = l;
			this.layerId = l.getLayerID();
			this.layerName = l.getName();
			this.layerWorkspace = l.getWorkspace();
			this.geometryColumn = l.getGeometryColumn();
			this.attributes = l.getAttributeTypes();
			// Log.v(HANDLER_TAG, "Layer preferences set:" + " ID: " + layerId + ", Name: " + layerName + ", Workspace: " + layerWorkspace 
			// 		+ ", SRS: epsg:" + l.getSRSAsInt() + ", # of Attributes: " + attributes.size());
			this.task = task;
		}

		@Override
		public void startDocument() throws SAXException {
			listen = false;
			isDone = false;
			geometry = false;
			bounds = false;
			currentValue = new StringBuilder();
			featureList = new ArrayList<Feature>();
			parsedfeatureCount = 0;
		}

		@Override
		public void startElement(String uri, String localName, String qName,
				Attributes atts) throws SAXException {
			currentValue.setLength(0);
			if (listen) {
				if (localName.equalsIgnoreCase(geometryColumn)) {
					geometry = true;
					geomGML = "";
					// Log.v(CLASSTAG, "Start of geometry...");
				} else if (qName.equalsIgnoreCase("gml:boundedBy")) {
					bounds = true;
					// Log.v(CLASSTAG, "Start of bounds...");
				} else if (geometry) {
					geomGML = geomGML + "<" + qName;
					for (int i = 0; i < atts.getLength(); i++) {
						geomGML = geomGML + " " + atts.getLocalName(i) + "=\"" + atts.getValue(i) + "\"";
					}
					geomGML = geomGML + ">";
				}
			} else { 
				if (qName.equalsIgnoreCase(layerWorkspace + ":" + layerName)) {
					values = new ContentValues();
					values.clear();
					listen = true;
					noError = true;
					geoServerID = atts.getValue("fid");
					// Log.e(CLASSTAG, "New GeoServer-ID given: " + geoServerID);
				}
			}
		}

		@Override
		public void characters(char[] ch, int start, int length)
				throws SAXException {
			currentValue.append(ch, start, length);
		}

		@Override
		public void endDocument() throws SAXException {
			saveAllParsedFeatures();
			// Log.v(CLASSTAG, "Parsing of all imported Features finished.");
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			if (qName.equalsIgnoreCase(layerWorkspace + ":" + layerName)) {
				listen = false;
				parsedfeatureCount++;
				// Log.v(CLASSTAG, "Successfully imported Feature #" + index);
				if (noError) {
					saveFeatureInList();
				} else {
					Log.e(CLASSTAG, "Error in feature. Cannot save...");
				}
			}
			if (listen) {
				if (localName.equalsIgnoreCase(geometryColumn)) {
					geometry = false;
				} else if (qName.equalsIgnoreCase("gml:boundedBy")) {
					bounds = false;
				} else if (geometry) {
					if (localName.equalsIgnoreCase("coordinates")) {
						geomGML = geomGML + currentValue.toString();
					}
					geomGML = geomGML + "</" + qName + ">";
				} else if (!bounds) {
					int type = getAttributeType(localName);
					// Log.v(CLASSTAG, "Returned " + type + " as Type for " + localName);
					switch (type) {
					case WFSLayerAttributeTypes.INTEGER:
						// Log.v(HANDLER_TAG, "New integer attribute: "	+ localName + " -> " + currentValue.toString());
						values.put(localName, Integer.valueOf(currentValue.toString()));
						break;
					case WFSLayerAttributeTypes.MEASUREMENT:
						// Log.v(HANDLER_TAG, "New measurement attribute: "	+ localName + " -> " + currentValue.toString());
						values.put(localName, Double.valueOf(currentValue.toString()));
						break;
					case WFSLayerAttributeTypes.STRING:
						// Log.v(HANDLER_TAG, "New text attribute: " + localName + " -> " + currentValue.toString());
						values.put(localName, currentValue.toString());
						if (localName.equalsIgnoreCase(LGLValues.LGL_IS_DONE_FLAG_ATTRIBUTE)) {
							isDone = Integer.valueOf(currentValue.toString()) > 0;
							if (isDone) {
								// Log.e(HANDLER_TAG, "ISDONE is" + isDone.toString());
							}
						}
						break;
					case WFSLayerAttributeTypes.BOOLEAN:
						// Log.v(HANDLER_TAG, "New boolean attribute: "	+ localName + " -> " + currentValue.toString());
						values.put(localName, Boolean.valueOf(currentValue.toString()));
						break;
					case WFSLayerAttributeTypes.DOUBLE:
						// Log.v(HANDLER_TAG, "New double attribute: " + localName	+ " -> " + currentValue.toString());
						values.put(localName, Double.valueOf(currentValue.toString()));
						break;
					case WFSLayerAttributeTypes.DECIMAL:
						// Log.v(HANDLER_TAG, "New decimal attribute: "	+ localName + " -> " + currentValue.toString());
						values.put(localName, Double.valueOf(currentValue.toString()));
						break;
					case WFSLayerAttributeTypes.DATE:
						// Log.v(HANDLER_TAG, "New Date attribute: " + localName + " -> " + currentValue.toString());
						// doing nothing, just going into DATETIME, because of the missing BREAK:
					case WFSLayerAttributeTypes.DATETIME:
						// Log.v(HANDLER_TAG, "New DateTime attribute: " + localName + " -> " + currentValue.toString());
						Date date = null;
						String dateTimeString = "";
						SimpleDateFormat dateFormat;
						try {
							if (currentValue.toString().length() > 10) {
								dateFormat = new SimpleDateFormat(STANDARD_DATE_FORMAT);
							} else {
								dateFormat = new SimpleDateFormat(STANDARD_SMALL_DATE_FORMAT);
							}
							date = dateFormat.parse(currentValue.toString());
							dateFormat = new SimpleDateFormat(STANDARD_DATE_FORMAT);
							dateTimeString = dateFormat.format(date);
						} catch (ParseException e) {
							// publishProgress(new StatusInfo(-1, "SAXException:\n" + e.getMessage()));
							// Log.e(CLASSTAG, "Date ParseException: " + e.getMessage());
							// e.printStackTrace();
							// noError = false;
							date = new Date(0);
							dateFormat = new SimpleDateFormat(STANDARD_DATE_FORMAT);
							dateTimeString = dateFormat.format(date);
							// Log.e(CLASSTAG, "Setting date to: " + date.toString());
						}
						// Log.e(CLASSTAG, "Inserting date: " + dateTimeString);
						values.put(localName, dateTimeString);
						break;
					case WFSLayerAttributeTypes.UNKNOWN:
						Log.v(HANDLER_TAG, "New unknown attribute: " + localName + " -> " + currentValue.toString());
						values.put(localName, currentValue.toString());
						break;
					default:
						Log.v(HANDLER_TAG, "New superunknown attribute: " + localName + " -> " + currentValue.toString());
						values.put(localName, currentValue.toString());
						break;
					}
				}
			}
		}

		@Override
		public void endPrefixMapping(String arg0) throws SAXException {
		}

		@Override
		public void ignorableWhitespace(char[] ch, int start, int length)
				throws SAXException {
		}

		@Override
		public void processingInstruction(String arg0, String arg1)
				throws SAXException {
		}

		@Override
		public void setDocumentLocator(Locator arg0) {
		}

		@Override
		public void skippedEntity(String arg0) throws SAXException {
		}

		@Override
		public void startPrefixMapping(String prefix, String uri)
				throws SAXException {
		}

		/**
		 * Save all.
		 */
		private void saveAllParsedFeatures() {
			dbAdapter.insertAllFeatureIntoDBAndIndex(featureList, layer);
		}

		/**
		 * Save feature in db and index.
		 *
		 * @return true, if successful
		 */
		private boolean saveFeatureInList() {
			boolean noError = true;
			GMLReader reader = new GMLReader();
			WKTWriter writer = new WKTWriter();
			Geometry geom;
			try {
				geom = reader.read(geomGML, new GeometryFactory());
				String geomWKT = writer.write(geom);
				// Log.e(CLASSTAG, "Normal GML: " + geomGML);
				// Log.e(CLASSTAG, "Normal WKT: " + geomWKT);
				if (layer.isMultiGeom()) {
					geomWKT = writer.write(toMultiGeom(geom));
					// Log.e(CLASSTAG, "Multi-WKT: " + geomWKT);
				}
				if (geomWKT.contains("NaN")) {
					Log.e(CLASSTAG,	"Geometry contains non-number values. Tried to convert from " + geomGML + " into " + geomWKT);
				} else {
					Feature feature = new Feature(context, layerId, geomWKT, values,
							currentLayer.getColor(), currentLayer.getTypeInt(),
							true, currentLayer.isActive(), geoServerID, isDone);
					// Log.e(CLASSTAG, "New Feature " + geoServerID + ", Attributes: " + feature.getAttributes().toString());
					// feature in liste speichern
					featureList.add(feature);
				}
				task.dialog.incrementProgressBy(1);
			} catch (SAXException e) {
				Log.v(CLASSTAG, "SAXException:\n" + e.getMessage());
				noError = false;
			} catch (IOException e) {
				Log.v(CLASSTAG, "IOException:\n" + e.getMessage());
				noError = false;
			} catch (ParserConfigurationException e) {
				Log.v(CLASSTAG, "ParserConfigurationException:\n" + e.getMessage());
				noError = false;
			} catch (Exception e) {
				Log.v(CLASSTAG, "Maybe GML is damaged. Exception:\n" + e.getMessage());
				Log.v(CLASSTAG, "GML on the Server: " + geomGML);
				noError = false;
			}
			return noError;
		}

		private int getAttributeType(String name) {
			ListIterator<WFSLayerAttributeTypes> iter = attributes.listIterator();
			while (iter.hasNext()) {
				WFSLayerAttributeTypes current = iter.next();
				if (current.getName().equalsIgnoreCase(name)) {
					return current.getType();
				}
			}
			Log.e(CLASSTAG, "Unknown Type of Attribute " + name);
			return WFSLayerAttributeTypes.UNKNOWN;
		}
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
	
	
}