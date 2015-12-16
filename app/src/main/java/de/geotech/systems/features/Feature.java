/**
 * Features of WFSLayer - to be displayed on the Screen
 *  
 * @author Torsten Hoch (tohoch@uos.de)
 * @author Sven Weisker (uucly@student.kit.edu)
 */

package de.geotech.systems.features;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import de.geotech.systems.projects.ProjectHandler;
import de.geotech.systems.wfs.WFSLayer;

public class Feature {
	// geoserver ID, if no ID was given
	public static final String INVALIDGEOSERVERID = "0";
	
	private static final String CLASSTAG = "Feature";
	private static final int INVALIDFEATUREID = -1;
	
	// Einstellungen für die "Ampel"
	// TODO: Dies sollte mit konfigurierbaren Einstellungen ersetzt werden
	public static int border[] = new int[] {15, 50};
	// the color for not synced features
	public static final int ISNOTSYNCCOLOR = Color.CYAN;

	// the context
	private Context context;
	// the features ID
	private long featureID;
	// its layer ID
	private long wfsLayerID;
	// the rader to read out the geometry
	private WKTReader wktReader;
	// attribut values of the feature
	private ContentValues attributes;
	// type of feature as set in wfslayer
	private int featureType;
	// coordinates as Text representation of a well known Geometry
	// The Well-known Text format is defined in the OGC Simple Features Specification for SQL. 
	private String wktGeometry;
	// is the feature active to draw
	private boolean isActive;
	// the geometry
	private Geometry geom;
	// is the feature synced
	private boolean sync;
	// the features color
	private int color;
	// the features precision
	private FeaturePrecision precision;
	// the id on the geoserver
	private String geoServerID;
	// the ID in the R-Tree-index
	private int indexID;
	// is the feature done
	private boolean done;

	/**
	 * Instantiates a new feature without Feature-ID (-1) of DB. 
	 * (Imported feature)
	 *
	 * @param context the context
	 * @param wfslayerID the wfslayer id
	 * @param wktGeometry the wktGeometry
	 * @param attributes the attributes
	 * @param color the color
	 * @param featureType the feature type
	 * @param sync the sync
	 * @param isactive the isactive
	 * @param geoServerID the id on the geoserver
	 */
	public Feature(Context context, long wfslayerID, String wktGeometry,
			ContentValues attributes, int color, int featureType, boolean sync,
			boolean isactive, String geoServerID, boolean newDone) {
		this(context, INVALIDFEATUREID, wfslayerID, wktGeometry, featureType,
				attributes, color, sync, isactive, geoServerID, newDone);
	}

	/**
	 * Instantiates a new feature without Feature-ID (-1) of DB and
	 * without id of the geoserver (ADD NEW FEATURE).
	 *
	 * @param context the context
	 * @param wfslayerID the wfslayer id
	 * @param wktGeometry the wktGeometry
	 * @param attributes the attributes
	 * @param color the color
	 * @param featureType the feature type
	 * @param sync the sync
	 * @param isactive the isactive
	 */
	public Feature(Context context, long wfslayerID, String wktGeometry,
			ContentValues attributes, int color, int featureType, boolean sync,
			boolean isactive, boolean newDone) {
		this(context, INVALIDFEATUREID, wfslayerID, wktGeometry, featureType,
				attributes, color, sync, isactive, INVALIDGEOSERVERID, newDone);
	}

	/**
	 * Default-Constructor: Instantiates a new feature.
	 *
	 * @param context the context
	 * @param featureID the feature id
	 * @param wfslayerID the wfslayer id
	 * @param wktGeometry the wktGeometry
	 * @param featureType the feature type
	 * @param attributes the attributes
	 * @param color the color
	 * @param sync the sync
	 * @param isactive the isactive
	 * @param geoServerID the id on the geoserver
	 */
	public Feature(Context context, long featureID, long wfslayerID,
			String wktGeometry, int featureType, ContentValues attributes,
			int color, boolean sync, boolean isactive, String geoServerID, 
			boolean newDone) {
		this.featureID = featureID;
		this.context = context;
		this.wfsLayerID = wfslayerID;
		this.wktGeometry = wktGeometry;
		this.attributes = attributes;
		this.sync = sync;
		this.isActive = isactive;
		// must be set before color!
		this.done = newDone;
		this.setColor(color);
		this.featureType = featureType;
		this.wktReader = new WKTReader();
		this.precision = null;
		this.setGeoServerID(geoServerID);
		this.createFeature();
	}

	/**
	 * Creates the feature.
	 *
	 * @return true, if successful
	 */
	private boolean createFeature() {
		boolean valid = true;
		// hier wird auch gecheckt, ob feature auf in bbox, jeweils in allen
		// fill-methoden
		switch (this.featureType) {
		case WFSLayer.LAYER_TYPE_POINT:
			valid = this.createPoint();
			break;
		case WFSLayer.LAYER_TYPE_LINE:
			valid = this.createLine();
			break;
		case WFSLayer.LAYER_TYPE_POLYGON:
			valid = this.createPolygon();
			break;
		default:
			Log.e(CLASSTAG + " createFeature", "Error at loading features: invalid feature type.");
		}
		return valid;
	}


	/**
	 * Creates the point.
	 *
	 * @return true, if successful
	 */
	private boolean createPoint() {
		Object newGeometryObject;
		try {
			newGeometryObject = this.wktReader.read(wktGeometry);
			if (newGeometryObject instanceof MultiPoint) {
				MultiPoint newMultiPoint = (MultiPoint) newGeometryObject;
				for (int i = 0; i < newMultiPoint.getNumGeometries(); i++) {
					if (newMultiPoint.getGeometryN(i) instanceof Point) {
						geom = (Point) newMultiPoint.getGeometryN(i);
					}
				}
				return true;
			} else if (newGeometryObject instanceof Point) {
				geom = (Point) newGeometryObject;
				return true;
			}
			return false;
		} catch (ParseException e) {
			Log.e(CLASSTAG, "ParseException: " + e.getMessage());
			return false;
		}
	}

	/**
	 * Creates the line.
	 *
	 * @return true, if successful
	 */
	private boolean createLine() {
		Object newGeometryObject;
		try {
			newGeometryObject = this.wktReader.read(wktGeometry);
			if (newGeometryObject instanceof MultiLineString) {
				MultiLineString newMultiLine = (MultiLineString) newGeometryObject;
				for (int i = 0; i < newMultiLine.getNumGeometries(); i++) {
					if (newMultiLine.getGeometryN(i) instanceof LineString) {
						geom = (LineString) newMultiLine.getGeometryN(i);
					}
				}
				return true;
			} else if (newGeometryObject instanceof LineString) {
				geom = (LineString) newGeometryObject;
				return true;
			}
			return false;
		} catch (ParseException e) {
			Log.e(CLASSTAG, "ParseException: " + e.getMessage());
			return false;
		}
	}

	/**
	 * Creates the polygon.
	 *
	 * @return true, if successful
	 */
	private boolean createPolygon() {
		Object newGeometryObject;
		try {
			newGeometryObject = this.wktReader.read(wktGeometry);
			if (newGeometryObject instanceof MultiPolygon) {
				MultiPolygon newMultiPolygon = (MultiPolygon) newGeometryObject;
				for (int i = 0; i < newMultiPolygon.getNumGeometries(); i++) {
					geom = (Polygon) newMultiPolygon.getGeometryN(i);
				}
				return true;
			} else if (newGeometryObject instanceof Polygon) {
				geom = (Polygon) newGeometryObject;
				return true;
			}
			return false;
		} catch (ParseException e) {
			Log.e(CLASSTAG, "ParseException: " + e.getMessage());
			return false;
		}
	}

	/**
	 * Gets the geometry
	 *
	 * @return the geom
	 */
	public Geometry getGeom() {
		return geom;
	}

	/**
	 * Gets the feature type as determined in WFSLayer.
	 *
	 * @return the feature type
	 */
	public int getFeatureType() {
		return featureType;
	}

	/**
	 * Gets the feature id.
	 *
	 * @return the feature id
	 */
	public long getFeatureID() {
		return featureID;
	}

	/**
	 * Sets the precision.
	 *
	 * @param precision the new precision
	 */
	public void setPrecision(FeaturePrecision precision) {
		this.precision = precision;
	}

	/**
	 * Gets the precision.
	 *
	 * @return the precision
	 */
	public FeaturePrecision getPrecision() {
		return precision;
	}

	/**
	 * Sets the feature id.
	 *
	 * @param id the new feature id
	 */
	public void setFeatureID(long id) {
		featureID = id;
	}

	/**
	 * Gets the wfslayer id of the feature.
	 *
	 * @return the wfslayer id
	 */
	public long getWFSlayerID() {
		return wfsLayerID;
	}

	/**
	 * Gets the attributes of the feature.
	 *
	 * @return the attributes
	 */
	public ContentValues getAttributes() {
		return attributes;
	}

	/**
	 * Gets the color of the feature.
	 *
	 * @return the color
	 */
	public int getColor() {
		return color;
	}

	/**
	 * Sets the color.
	 *
	 * @param newColor the new color
	 */
	public void setColor(int newColor) {
		if (!isSync()) {
			if (ProjectHandler.getCurrentProject().isUnsyncAsCyan()) {
				this.color = ISNOTSYNCCOLOR;
			} else {
				this.color = newColor;
			}
		} else if (isDone()) {
			this.color = Color.BLACK;
		} else {
			this.color = newColor;			
		}
	}

	/**
	 * Checks if Layer is synchronized with Geoserver.
	 *
	 * @return true, if is sync
	 */
	public boolean isSync() {
		return this.sync;
	}

	/**
	 * Sets if the Layer is synchronized. Changes the Features color to CYAN, if false is given.
	 *
	 * @param sync the new sync
	 */
	public void setSync(boolean sync) {
		this.sync = sync;
		if (isSync()) {
			if (isDone()) {
				this.color = Color.BLACK;
			} else {
				this.setColor(ProjectHandler.getCurrentProject().getWFSLayerbyID(getWFSlayerID()).getColor());
			}
		} else {
			this.setColor(ISNOTSYNCCOLOR);
		}
	}

	/**
	 * Checks if is active on view (shown).
	 *
	 * @return true, if is active
	 */
	public boolean isActive() {
		return isActive;
	}

	/**
	 * Sets it active on the view (shown).
	 *
	 * @param isActive the new active
	 */
	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	/**
	 * Gets the wktGeometry.
	 *
	 * @return the wktGeometry
	 */
	public String getWKTGeometry() {
		return wktGeometry;
	}

	/**
	 * Checks for precision.
	 *
	 * @return the boolean
	 */
	public Boolean hasPrecision() {
		return (precision != null);
	}

	/**
	 * Gets the wktGeometry from linestring.
	 *
	 * @return the wktGeometry from linestring
	 */
	public Coordinate[] getCoordinatesFromLinestring() {
		return geom.getCoordinates();
	}

	/**
	 * Gets a backup feature with same field details but without id (-1).
	 *
	 * @return the backup without id
	 */
	public Feature getCopyOfFieldValuesWithoutID() {
		Feature backupCopy = new Feature(context, this.getWFSlayerID(), new String(getWKTGeometry()),
				new ContentValues(getAttributes()), this.getColor(), this.getFeatureType(), this.isSync(),
				this.isActive(), new String(getGeoServerID()), this.isDone());  
		return backupCopy;
	}

	/**
	 * Sets all internal field values to the other features values.
	 *
	 * @param featureWithNewFieldValues the new all field values to
	 */
	public void setAllFieldValuesTo(Feature featureWithNewFieldValues) {
		this.wfsLayerID = featureWithNewFieldValues.getWFSlayerID();
		this.wktGeometry = new String(featureWithNewFieldValues.getWKTGeometry());
		this.attributes = new ContentValues(featureWithNewFieldValues.getAttributes());
		this.setColor(featureWithNewFieldValues.getColor());
		this.featureType = featureWithNewFieldValues.getFeatureType();
		this.setSync(featureWithNewFieldValues.isSync());
		this.setActive(featureWithNewFieldValues.isActive());
		this.createFeature();
	}

	/**
	 * Sets the attributes.
	 *
	 * @param contentValues the new attributes
	 */
	public void setAttributes(ContentValues contentValues) {
		this.attributes = contentValues;
	}

	/**
	 * Sets the wktGeometry.
	 *
	 * @param newwktGeometry the new wktGeometry
	 */
	public void setWKTGeometry(String newwktGeometry) {
		this.wktGeometry = newwktGeometry;
		this.createFeature();
	}

	/**
	 * Gets the geo server id.
	 *
	 * @return the geo server id
	 */
	public String getGeoServerID() {
		return this.geoServerID;
	}

	/**
	 * Sets the index id.
	 *
	 * @param tempID the new index id
	 */
	public void setIndexID(int tempID) {
		this.indexID = tempID;	
	}

	/**
	 * Gets the index id.
	 *
	 * @return the index id
	 */
	public int getIndexID() {
		return this.indexID;
	}

	/**
	 * Checks if is done. LGL-Flag
	 *
	 * @return true, if is done
	 */
	public boolean isDone() {
		return done;
	}

	/**
	 * Sets the done.
	 *
	 * @param done the new done
	 */
	public void setDone(boolean done) {
		this.done = done;
		if (done) {
			setColor(Color.BLACK);
		} else {
			setColor(ProjectHandler.getCurrentProject().getWFSLayerbyID(this.wfsLayerID).getColor());
		}
	}

	/**
	 * Sets the geo server id.
	 *
	 * @param newID the new geo server id
	 */
	public void setGeoServerID(String newID) {
		// TODO: rausnehmen
//		if (newID.equals("ad_dim.174")) {
//			Log.e(CLASSTAG, "CV set: " + this.getAttributes().toString());
//		}
		this.geoServerID = newID;
	}

}
