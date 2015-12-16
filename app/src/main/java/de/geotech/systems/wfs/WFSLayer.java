/**
 * This class is used so store a WFS layer with its properties
 *  
 * @author Torsten Hoch
 */

package de.geotech.systems.wfs;

import gnu.trove.TIntProcedure;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;

import com.infomatiq.jsi.Rectangle;
import com.infomatiq.jsi.SpatialIndex;
import com.infomatiq.jsi.rtree.RTree;

import com.vividsolutions.jts.geom.Envelope;

import de.geotech.systems.LGLSpecial.LGLValues;
import de.geotech.systems.features.Feature;
import de.geotech.systems.layers.LayerInterface;

public class WFSLayer implements LayerInterface {
	private static final String CLASSTAG = "WFSLayer";
	// color code for all wfs layers
	private static int COLORCODE = 0;
	// type definitions for layers
	public static final int LAYER_TYPE_POINT = 0;
	public static final int LAYER_TYPE_LINE = 1;
	public static final int LAYER_TYPE_POLYGON = 2;

	// the in the db saved variables
	private long layerID;
	private long projectID;
	private String name;
	private String title;
	private String workspace;
	private String namespace;
	private String typeString;
	private String geometryColumn;
	private String epsg;
	private String url;
	private boolean isMultiGeom;
	private boolean isActive;
	private int timestamp;
	private int color;
	private int countFeatures;
	private boolean sync;
	private float sizeMB;
	private boolean isLocked;
	private String lockID;
	private long lockExpiry;
	private Calendar lockDate = Calendar.getInstance();
	private Calendar releaseDate = Calendar.getInstance();
	// more variables
	private Context context;
	private boolean checked;
	private boolean isExact;
	private int typeInt;
	private ArrayList<Feature> featureContainer;
	private ArrayList<WFSLayerAttributeTypes> attributes;
	private SpatialIndex rTree;

	/**
	 * Default constructor getting a WFSLayer from DB.
	 */
	public WFSLayer(Context c, long layerID, String layerName, long projectID,
			String url, String title, String layerWorkspace, String namespace,
			String type, String layerSRS, String geom, boolean isactive,
			boolean ismulti, float size_mb, int timestamp, int color,
			int countf, boolean sync, boolean isLocked, String lockID,
			long lockExpiry, long lockDate, long unlockDate) {
		this.layerID = layerID;
		this.name = layerName;
		this.projectID = projectID;
		this.workspace = layerWorkspace;
		this.namespace = namespace;
		this.epsg = layerSRS;
		this.url = url;
		this.title = title;
		this.isMultiGeom = ismulti;
		this.isActive = isactive;
		this.timestamp = timestamp;
		this.color = color;
		this.countFeatures = countf;
		this.sizeMB = size_mb;
		this.typeString = type;
		this.sync = sync;
		this.geometryColumn = geom;
		this.isLocked = isLocked;
		this.lockDate.setTimeInMillis(lockDate);
		this.releaseDate.setTimeInMillis(unlockDate);
		this.lockID = lockID;
		this.lockExpiry = lockExpiry;
		this.context = c;
		this.typeInt = geometryTypeToInt(typeString);
		this.featureContainer = new ArrayList<Feature>();
		this.attributes = new ArrayList<WFSLayerAttributeTypes>();
		this.rTree = new RTree();
		this.rTree.init(null);
	}

	/**
	 * Default constructor creating a new WFSLayer from XML-Parsing.
	 */
	public WFSLayer(Context c, String layerName, long projectID, String url,
			String title, String layerWorkspace, String namespace, String type,
			String layerSRS, String geom, boolean isactive, boolean ismulti,
			float size_mb, int timestamp, int countf, boolean sync,
			ArrayList<WFSLayerAttributeTypes> list) {
		this.layerID = -1;
		this.name = layerName;
		this.projectID = projectID;
		this.workspace = layerWorkspace;
		this.namespace = namespace;
		this.epsg = layerSRS;
		this.url = url;
		this.title = title;
		this.isActive = isactive;
		this.isMultiGeom = ismulti;
		this.timestamp = timestamp;
		this.countFeatures = countf;
		this.sizeMB = size_mb;
		this.typeString = type;
		this.sync = sync;
		this.geometryColumn = geom;
		this.isLocked = false;
		this.lockDate = Calendar.getInstance();
		this.releaseDate = Calendar.getInstance();
		this.lockID = "";
		this.lockExpiry = 0;
		this.context = c;
		this.typeInt = geometryTypeToInt(typeString);
		this.color = setLGLColor(COLORCODE);
		if (list != null) {
			this.attributes = new ArrayList<WFSLayerAttributeTypes>(list);
		} else {
			this.attributes = new ArrayList<WFSLayerAttributeTypes>();
		}
		this.featureContainer = new ArrayList<Feature>();
		this.rTree = new RTree();
		this.rTree.init(null);
	}

	private int setLGLColor(int currentCode) {
		// LGL Codes fuer farben rein!!!
		for (int i = 0; i < LGLValues.STANDARD_LGL_LAYER_NAMES.length; i++) {
			if (this.getName().equalsIgnoreCase(LGLValues.STANDARD_LGL_LAYER_NAMES[i])) {
				Log.e(CLASSTAG + " + setLGLColor", "Layer " + name + " has color: " + currentCode);
				return LGLValues.STANDARD_LGL_COLOR_CODES[i];
			}
		}
		return setInitColor(currentCode);
	}

	private int setInitColor(int currentCode) {
		// increase the static variable
		COLORCODE = COLORCODE + 1;
		Log.i(CLASSTAG + " + setInitColor", "Layer " + name + " has color: " + currentCode + ". Static ColorCODE is now: " + COLORCODE);
		if (currentCode > 7) {
			currentCode = 0;
			COLORCODE = 0;
		}
		switch (currentCode) {
		case 0:
			return Color.RED;
		case 1:
			return Color.GREEN;
		case 2:
			return Color.MAGENTA;
		case 3:
			return Color.WHITE;
		case 4:
			return Color.LTGRAY;
		case 5:
			return Color.BLUE;
		case 6:
			return Color.YELLOW;
		case 7:
			return Color.DKGRAY;
		case 8:
			return Color.CYAN;
		case 9:
			return Color.BLACK;
		case 10:
			return Color.GRAY;
		case 11:
			return Color.TRANSPARENT;
		default:
			return Color.GRAY;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result	+ ((attributes == null) ? 0 : attributes.hashCode());
		result = prime * result + ((epsg == null) ? 0 : epsg.hashCode());
		result = prime * result + (isMultiGeom ? 1231 : 1237);
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((title == null) ? 0 : title.hashCode());
		result = prime * result	+ ((typeString == null) ? 0 : typeString.hashCode());
		result = prime * result + ((url == null) ? 0 : url.hashCode());
		result = prime * result	+ ((workspace == null) ? 0 : workspace.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		WFSLayer other = (WFSLayer) obj;
		if (attributes == null) {
			if (other.attributes != null) {
				return false;
			}
		} else {
			if (!attributes.equals(other.attributes)) {
				return false;
			}
		}
		if (epsg == null) {
			if (other.epsg != null) {
				return false;
			}
		} else {
			if (!epsg.equals(other.epsg)) {
				return false;
			}
		}
		if (isMultiGeom != other.isMultiGeom) {
			return false;
		}
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else {
			if (!name.equals(other.name)) {
				return false;
			}
		}
		if (title == null) {
			if (other.title != null) {
				return false;
			}
		} else {
			if (!title.equals(other.title)) {
				return false;
			}
		}
		if (typeString == null) {
			if (other.typeString != null) {
				return false;
			}
		} else {
			if (!typeString.equals(other.typeString)) {
				return false;
			}
		}
		if (url == null) {
			if (other.url != null) {
				return false;
			}
		} else {
			if (!url.equals(other.url)) {
				return false;
			}
		}
		if (workspace == null) {
			if (other.workspace != null) {
				return false;
			}
		} else {
			if (!workspace.equals(other.workspace)) {
				return false;
			}
		}
		return true;
	}

	public int geometryTypeToInt(String type) {
		if (type.contains("Point")) {
			return LAYER_TYPE_POINT;
		} else if (type.contains("LineString")) {
			return LAYER_TYPE_LINE;
		} else if (type.contains("Polygon") || type.contains("Surface")
				|| type.contains("POLYGON")) {
			return LAYER_TYPE_POLYGON;
		} else
			return -1;
	}

	/**
	 * Returns layers name.
	 * 
	 * @return
	 */
	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns layers SRS.
	 * 
	 * @return
	 */
	public String getEPSG() {
		return this.epsg;
	}

	public void setEPSG(String epsg) {
		this.epsg = epsg;
	}

	public String getWorkspace() {
		return this.workspace;
	}

	public void setTitle(String title) {
		this.workspace = title;
	}

	/**
	 * Returns the SRS ID as integer.
	 * 
	 * @return
	 */
	public int getSRSAsInt() {
		String[] srsSplit = epsg.split(":");
		return Integer.parseInt(srsSplit[srsSplit.length - 1]);
	}

	/**
	 * Adds an Attribute to the layer.
	 * 
	 * @param attr
	 */
	public void addAttribute(WFSLayerAttributeTypes attr) {
		attributes.add(attr);
	}

	/**
	 * Sets the attributes list to given list.
	 * 
	 * @param attrs
	 */
	public void setAttributeList(ArrayList<WFSLayerAttributeTypes> attrs) {
		attributes = new ArrayList<WFSLayerAttributeTypes>(attrs);
	}

	/**
	 * Returns the ArrayList with the layer's attributes.
	 * 
	 * @return
	 */
	public ArrayList<WFSLayerAttributeTypes> getAttributeTypes() {
		return this.attributes;
	}

	/**
	 * Return the number of Attributes.
	 *
	 * @return the count attributes
	 */
	public int getCountAttributes() {
		return this.attributes.size();
	}

	/**
	 * Returns the layer's geometry type.
	 * 
	 * @return
	 */
	public String getType() {
		return this.typeString;
	}

	/**
	 * Sets the layer's namespace.
	 * 
	 * @param targetNamespace
	 */
	public void setNamespace(String targetNamespace) {
		this.namespace = targetNamespace;
	}

	/**
	 * Returns the layer's namespace.
	 * 
	 * @return
	 */
	public String getNamespace() {
		return this.namespace;
	}

	/**
	 * Sets the layer's geometry column.
	 * 
	 * @param column
	 */
	public void setGeometryColumn(String column) {
		this.geometryColumn = column;
	}

	/**
	 * Returns the layer's geometry column.
	 * 
	 * @return
	 */
	public String getGeometryColumn() {
		return this.geometryColumn;
	}

	/**
	 * Sets if layer contains multi-geometry features.
	 * 
	 * @param multi
	 */
	public void setMultiGeom(boolean multi) {
		this.isMultiGeom = multi;
	}

	/**
	 * Sets number of features within the layer
	 * 
	 * @param multi
	 */
	public int getCountFeatures() {
		return this.countFeatures;
	}

	/**
	 * Returns if layer contains multi-geometry features.
	 * 
	 * @return
	 */
	public boolean isMultiGeom() {
		return this.isMultiGeom;
	}

	public String getUrl() {
		return this.url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public boolean isActive() {
		return this.isActive;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
		for (Feature f : featureContainer){
			f.setActive(isActive);
		}
	}

	public int getColor() {
		return this.color;
	}

	public void setColor(int color) {
		this.color = color;
		for (Feature feature : featureContainer) {
			feature.setColor(color);
		}
	}

	public long getLayerID() {
		return this.layerID;
	}

	public ArrayList<Feature> getFeatureContainer() {
		return this.featureContainer;
	}

	public int getTypeInt() {
		return this.typeInt;
	}

	public boolean isSync() {
		return this.sync;
	}

	/**
	 * Sets the sync. Rebuilds the index if false is given.
	 *
	 * @param newSyncStatus the new sync
	 */
	public void setSync(boolean newSyncStatus) {
		// falls nicht mehr synchron
		if (!newSyncStatus) {
			updateIndex();
		}
		this.sync = newSyncStatus;
		// Log.e(CLASSTAG,	"WFS-Layers " + this.getName() + " Sync-Satus changed to " + this.isSync());
	}

	public boolean isExact() {
		return this.isExact;
	}

	public void setExact(boolean isExact) {
		this.isExact = isExact;
	}

	public boolean isChecked() {
		return this.checked;
	}

	public void setChecked(boolean checked) {
		this.checked = checked;
	}

	/**
	 * returns if the layer is locked on the WFS-T
	 * 
	 * @return it the layer isLocked
	 */
	public boolean isLocked() {
		// falls islocked noch true ist
		if (this.isLocked) {
			// ueberpruefen ob der lock noch "aktiv" ist
			if (Calendar.getInstance().after(releaseDate)) {
				// wenn nicht mehr aktiv, unlocken
				this.isLocked = false;
				this.lockID = "";
				this.lockExpiry = 0;
				Log.e(CLASSTAG + " isLocked", Calendar.getInstance().getTime()
						.toString()
						+ " is after " + releaseDate.getTime().toString());
				Log.e(CLASSTAG + " isLocked", "Layer " + this.getName()
						+ " RELEASED! -> islocked = FALSE - " + isLocked);
			} else {
				// Log.e(CLASSTAG + " isLocked", "TRUE - " + isLocked);
			}
		} else {
			// Log.e(CLASSTAG + " isLocked", "FALSE - " + isLocked);
		}
		return this.isLocked;
	}

	/**
	 * locks the layer
	 * 
	 * @return success
	 */
	public boolean lock(Calendar lockDate, int newLockExpiry, String lockID) {
		this.lockID = lockID;
		this.lockDate = lockDate;
		this.releaseDate = (Calendar) lockDate.clone();
		this.releaseDate.add(Calendar.SECOND, newLockExpiry);
		this.lockExpiry = newLockExpiry;
		this.isLocked = true;
		return this.isLocked;
	}

	/**
	 * releases the layer
	 * 
	 * @return success
	 */
	public boolean release() {
		// beim unlock/release sollte die ID des locks zum server zum unlocken 
		// gegeben werden...
		// TODO: lock id zum Release zum server uebertragen!
		// wenn der layer noch wirklich gelockt ist
		if (this.isLocked) {
			// ueberpruefen ob der lock noch "aktiv" ist
			if (!Calendar.getInstance().after(releaseDate)) {
				// TODO HIER DEM SERVER SAGEN, ER SOLL RELEASEN!!!!

			}
		}
		// wenn der layer auf dem server sowieso schon released ist,
		// einfach hier alles unlocken
		this.isLocked = false;
		this.lockID = "";
		this.lockExpiry = 0;
		return true;
	}

	/**
	 * @return the lockID
	 */
	public String getLockID() {
		return this.lockID;
	}

	/**
	 * @return the lockExpiry
	 */
	public long getLockExpiry() {
		return this.lockExpiry;
	}

	/**
	 * @return the lockDate
	 */
	public Calendar getLockDate() {
		return this.lockDate;
	}

	/**
	 * @return the unLockDate
	 */
	public Calendar getReleaseDate() {
		return this.releaseDate;
	}

	public float getSizeMB() {
		return sizeMB;
	}

	public int getTimestamp() {
		return timestamp;
	}

	public String getTitle() {
		return title;
	}

	public long getProjectID() {
		return projectID;
	}

	public Calendar getUnLockDate() {
		return releaseDate;
	}

	/**
	 * Sets the layer id.
	 *
	 * @param newLayerID the new layer id
	 */
	public void setLayerID(long newLayerID) {
		this.layerID = newLayerID;
	}

	/**
	 * Gets the envelope of the outer bounds of the layer.
	 * STILL DUMMY - DOES NOTHING!
	 *
	 * @return the envelope
	 */
	public Envelope getEnvelope() {
		// TODO: envelope des layers berechnen lassen!
		return new Envelope();
	}

	/**
	 * @author Sven Weisker (uucly@student.kit.edu)
	 * Index ohne Datenbanknutzung (voruebergehend)
	 * gibt Liste mit allen Feature des Layer wieder
	 */
	public ArrayList<Feature> getAllFeature() {
		return featureContainer;
	}

	/**
	 * @author Sven Weisker (uucly@student.kit.edu)
	 * Index ohne Datenbanknutzung (voruebergehend)
	 * Sucht Feature mit ID aus uebergebener Liste
	 * 
	 * @param List mit featureIDs
	 * @return gibt List mit Feature zurueck
	 */
	public ArrayList<Feature> getFeatures(List<Integer> featureIDList) {
		ArrayList<Feature> resultList = new ArrayList<Feature>();
		for (int i = 0; i < featureIDList.size(); i++){
			int currentFeatureID = featureIDList.get(i);
			Feature currentFeature = featureContainer.get(currentFeatureID);
			resultList.add(currentFeature);
		}
		return resultList;
	}

	/**
	 * Adds a Feature to the rTree-index.
	 *
	 * @param newFeature the new feature
	 * @param featureIndexID the feature id
	 */
	public void addToIndex(Feature newFeature){
		// get bbox envelope
		Envelope env = newFeature.getGeom().getEnvelopeInternal();
		// put it in a rectangle for rTree
		Rectangle recGeom = new Rectangle((float) env.getMinX(),
				(float) env.getMinY(), (float) env.getMaxX(), 
				(float) env.getMaxY());
		newFeature.setIndexID(rTree.size());
		rTree.add(recGeom, rTree.size());
	}

	/**
	 * diese Methode gibt alle FeatureIDs wieder, die durch eine intersects-Anfrage 
	 * an den Index gefunden wurden
	 * 
	 * @param env Kartenausschnitt
	 * @return alle gefundenen FeatureIDs
	 */
	public ArrayList<Integer> getFeatureIDs(Envelope env) {
		// TODO: Direkt ein RECTANGLE übergeben lassen und diese methode rauswerfen, 
		// anstatt extra umzurechnen (s.untere methode)
		Rectangle searchRec = new Rectangle((float) env.getMinX(),
				(float) env.getMinY(), (float) env.getMaxX(),
				(float) env.getMaxY());
		IndexHelper saveToList = new IndexHelper();
		rTree.intersects(searchRec, saveToList);
		return saveToList.getIds();
	}

	/**
	 * Diese Methode gibt alle FeatureIDs wieder, die durch eine intersects-Anfrage 
	 * an den Index gefunden wurden
	 * 
	 * @param searchRec the search rec
	 * @return the feature i ds
	 */
	public ArrayList<Integer> getFeatureIDs(Rectangle searchRec) {
		IndexHelper saveToList = new IndexHelper();
		rTree.intersects(searchRec, saveToList);
		return saveToList.getIds();
	}

	/**
	 * Index wird gelöscht und neu initialisiert. Ist am Ende leer.
	 */
	public void restartIndex() {
		Log.i(CLASSTAG + " restartIndex()", "Restarting Index of WFS-Layer " + this.getName() + ".");
		this.rTree = new RTree();
		this.rTree.init(null);
	}

	/**
	 * Index wird gelöscht und initialisiert. Danach werden alle im Layer enthaltenen Features neu in den 
	 * Index eingetragen. Nötig bei Veränderungen an Features.
	 */
	private void updateIndex() {
		restartIndex();
		for (Feature feature : featureContainer) {
			addToIndex(feature);
		}
	}

	/**
	 * @author Sven Weisker (uucly@student.kit.edu)
	 *  
	 * Innere Hilfsklasse fuer den Index. In ids werden alle 
	 * FeatureIDs gehalten
	 */
	private static class IndexHelper implements TIntProcedure {
		private ArrayList<Integer> ids = new ArrayList<Integer>();

		@Override
		public boolean execute(int id) {
			// Log.e(CLASSTAG + " execute", "Indexheper executed.");
			this.ids.add(id);
			return true;
		};

		private ArrayList<Integer> getIds() {
			return ids;
		}
	}

}