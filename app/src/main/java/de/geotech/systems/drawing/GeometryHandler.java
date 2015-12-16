/**
 * Unused - shall be used as GeometryHandler later
 * 
 * @author Torsten Hoch - tohoch@uos.de
 */
package de.geotech.systems.drawing;

import java.util.StringTokenizer;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.jhlabs.map.proj.Projection;
import com.jhlabs.map.proj.ProjectionFactory;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.WKTReader;

import de.geotech.systems.editor.AddFeatureBar;
import de.geotech.systems.editor.EditFeatureBar;
import de.geotech.systems.features.Feature;
import de.geotech.systems.features.FeatureContainer;
import de.geotech.systems.geometry.Pixel;
import de.geotech.systems.projects.Project;
import de.geotech.systems.wfs.WFSLayer;

public class GeometryHandler {

	private static final double BOUNDUPPERLEFTX = 0;
	private static final double BOUNDUPPERLEFTY = 0;
	private static final double BOUNDLOWERRIGHTY = 0;
	private static final double BOUNDLOWERRIGHTX = 0;
	private static final double STANDARDMINIMUMSPAN = 0;
	private static final double MAXIMUM_ZOOM_SCALE = 0;
	private static final double MINIMUM_ZOOM_SCALE = 0;
	private static final String CLASSTAG = null;
	private static final float SNAPPING_RANGE = 0;
	private Project currentProject;
	private FeatureContainer featureContainer;
	private Coordinate bound_upperLeft;
	private Coordinate bound_lowerRight;
	private int height;
	private int width;
	private Coordinate upperLeft;
	private double scale;
	private MapView osmMapView;
	private DrawingPanelView panel;
	private Projection mapProjection;
	private float firstPointerX;
	private float firstPointerY;
	private AddFeatureBar addFeatureBar;
	private EditFeatureBar editFeatureBar;
	private float movedPointerX;
	private float movedPointerY;

	public GeometryHandler() {
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * Fly to coordinate as String.
	 *
	 * @param flyID the features id to fly to
	 * @param layerID the layers name of feature stored in
	 * @param coordStr the coord where to fly
	 */
	public void flyTo(long flyID, long layerID, String coordStr) {
		StringTokenizer st = new StringTokenizer(coordStr, " ()");
		Coordinate coord = new Coordinate();
		for (int i = 0; i < 3; i++) {
			String curToken = st.nextToken();
			if (curToken.contains(",")) {
				curToken = curToken.substring(0, curToken.length() - 1);
			}
			switch (i) {
			case 1:
				coord.x = Double.parseDouble(curToken);
				break;
			case 2:
				coord.y = Double.parseDouble(curToken);
				break;
			}
		}
		moveTo(coord);	
	}

	/**
	 * Zoom to outest bounds of all active Features.
	 */
	public void zoomToOutestBounds() {
		// wenn projekt gesetzt ist, alle aktiven features laden
		if (currentProject != null) {
			if (currentProject.getWFSContainer() != null && currentProject.getWFSContainer().size() > 0) {
				this.featureContainer = panel.loadAllActiveFeatures(currentProject);
			}
		}
		// wenn keine features aktiv sind bzw. angezeigt werden muessen
		if (featureContainer.isEmpty()) {
			// standardansicht mit mittelpunkt aus Konstanten
			bound_upperLeft = new Coordinate(BOUNDUPPERLEFTX, BOUNDUPPERLEFTY);
			bound_lowerRight = new Coordinate(BOUNDLOWERRIGHTX, BOUNDLOWERRIGHTY);
		} else {
			// bounds zuruecksetzen
			bound_upperLeft = null;
			bound_lowerRight = null;
			// bounds aus addtobounds fuer jedes feature ermitteln
			for (Feature f : featureContainer.getAllFeatures()) {
				this.addToBounds(f.getGeom());
			}
		}
		// wenn kein display
		if (this.width == 0 && this.height == 0) {
			return;
		}
		// Check if bounds are null - standardansicht mit mittelpunkt aus Konstanten
		if (this.bound_upperLeft == null) {
			bound_upperLeft = new Coordinate(BOUNDUPPERLEFTX, BOUNDUPPERLEFTY);
		}
		if (this.bound_lowerRight == null) {
			bound_lowerRight = new Coordinate(BOUNDLOWERRIGHTX, BOUNDLOWERRIGHTY);
		}
		// Set upper left corner coordinates
		this.upperLeft.x = this.bound_upperLeft.x;
		this.upperLeft.y = this.bound_upperLeft.y;
		// Calculate scale in x and y axis: höhe und breite der BBox
		double span_x = this.bound_lowerRight.x - this.bound_upperLeft.x;
		// ursrpruenglich x statt y und ohne -1:
		double span_y = (this.bound_lowerRight.y - this.bound_upperLeft.y) * (-1); 
		// Wenn Span Werte = 0, z.b. wenn nur ein einziger punkt gezeichnet wurde
		if (span_x == 0.0) {
			span_x = STANDARDMINIMUMSPAN;
		}
		if (span_y == 0.0) {
			span_y = STANDARDMINIMUMSPAN;
		}
		// skalierung bzw. zoom
		double scale_x = (double) this.width / (span_x);
		double scale_y = (double) this.height / (span_y);
		// Choose scale to fit whole dataset into view - kleinere scale waehlen, groesserer zoom
		if (scale_x < scale_y) {
			this.scale = scale_x;
		} else {
			this.scale = scale_y;
		}
		// minimale und maximale zoomstufen setzen
		if (scale > MAXIMUM_ZOOM_SCALE) {
			scale = MAXIMUM_ZOOM_SCALE;
		} else {
			if (scale < MINIMUM_ZOOM_SCALE) {
				scale = MINIMUM_ZOOM_SCALE;
			}
		}
		// osmmap updaten fuer neue eigenschaften
		if (this.osmMapView.isActivated()) { 
			updateMapView();
		}
		// neu zeichnen
		panel.invalidate();
		// neue ansichtsoptionen merken fuer evtl. absturz, restart etc.
		panel.writeSharedPreferences();
	}
	

	// gets the zoom level
	public double getZoomLevel() {
		return this.scale;
	}
	
	
	/**
	 * Calculates bounds of loaded dataset and returns center coordinates
	 *
	 * @return the coordinate
	 */
	private void calculateBounds() {
		Log.e(CLASSTAG + " calculateBounds()", "calculateBounds() starts");
		// load all features in bounding box
		this.featureContainer = panel.loadActiveFeaturesInBox(currentProject, this.getUpperLeft(), this.getLowerRight(), false);
		Log.e(CLASSTAG + " calculateBounds()", "Now " + featureContainer.countAllFeatures() + " Features in Container.");
		// wenn keine Features anzuzeigen sind setze standardbounds
		// TODO: ueberpruefen, ob der richtige weg
		if (featureContainer.isEmpty()) {
			bound_upperLeft = new Coordinate(BOUNDUPPERLEFTX, BOUNDUPPERLEFTY);
			bound_lowerRight = new Coordinate(BOUNDLOWERRIGHTX, BOUNDLOWERRIGHTY);
			// Log.e(CLASSTAG, "calculateBounds() ends if");
		} else {
			// bounds zuruecksetzen
			bound_upperLeft = null;
			bound_lowerRight = null;
			// bounds aus addtobounds fuer jedes feature ermitteln
			for (Feature feature : featureContainer.getAllFeatures()) {
				this.addToBounds(feature.getGeom());
			}
			// if bounds are still null -> standardansicht mit mittelpunkt aus Konstanten
			if (bound_upperLeft == null && bound_lowerRight == null) {
				bound_upperLeft = new Coordinate(BOUNDUPPERLEFTX, BOUNDUPPERLEFTY);
				bound_lowerRight = new Coordinate(BOUNDLOWERRIGHTX, BOUNDLOWERRIGHTY);
			}
			// Log.e(CLASSTAG, "calculateBounds() ends else");
		}
	}

	/**
	 * Adds geometries to bounds. Checks if given geometry extends current bounds 
	 * and adjusts them, recursiv for multigeometries
	 *
	 * @param geometry the geometry
	 * @return true, if bounds were extende
	 */
	public boolean addToBounds(Object geometry) {
		// the return parameter, if it was extended
		boolean extended = false;
		if (geometry instanceof MultiPoint) {
			MultiPoint multiGeom = (MultiPoint) geometry;
			for (int i = 0; i < multiGeom.getNumGeometries(); i++) {
				if (addToBounds(multiGeom.getGeometryN(i))) {
					extended = true;
				}
			}
		} else if (geometry instanceof MultiLineString) {
			MultiLineString multiGeom = (MultiLineString) geometry;
			for (int i = 0; i < multiGeom.getNumGeometries(); i++) {
				if (addToBounds(multiGeom.getGeometryN(i))) {
					extended = true;
				}
			}
		} else if (geometry instanceof MultiPolygon) {
			MultiPolygon multiGeom = (MultiPolygon) geometry;
			for (int i = 0; i < multiGeom.getNumGeometries(); i++) {
				if (addToBounds(multiGeom.getGeometryN(i))) {
					extended = true;
				}
			}
		} else if (geometry instanceof Point) {
			extended = addPointToBounds(new Coordinate(((Point) geometry).getX(), ((Point) geometry).getY()));
		} else if (geometry instanceof LineString) {
			Coordinate[] lineCoords = ((LineString) geometry).getCoordinates();
			for (Coordinate c : lineCoords) {
				extended = addPointToBounds(c);
			}
		} else if (geometry instanceof Polygon) {
			for (Coordinate c : ((Polygon) geometry).getExteriorRing().getCoordinates()) {
				extended = addPointToBounds(c);
			}
		}
		return extended;
	}

	/**
	 * Adds the point to bounds.
	 *
	 * @param coord the coord of point to add
	 * @return true, if bounds were extended
	 */
	private boolean addPointToBounds(Coordinate coord) {
		boolean extended = false;
		// if bounds are not set
		if (bound_upperLeft == null && bound_lowerRight == null) {
			bound_upperLeft = new Coordinate(coord.x, coord.y);
			bound_lowerRight = new Coordinate(coord.x, coord.y);
			// if bounds are the same
		} else if (bound_upperLeft.equals2D(bound_lowerRight)) {
			if (coord.x > bound_upperLeft.x)  {
				bound_lowerRight.x = coord.x;
			} else  {
				bound_upperLeft.x = coord.x;
			}
			if (coord.y > bound_lowerRight.y) {
				bound_upperLeft.y = coord.y;
			} else {
				bound_lowerRight.y = coord.y;
			}
		} else {
			if (coord.x < bound_upperLeft.x) {
				bound_upperLeft.x = coord.x;
				extended = true;
			}
			if (coord.x > bound_lowerRight.x) {
				bound_lowerRight.x = coord.x;
				extended = true;
			}
			if (coord.y < bound_lowerRight.y) {
				bound_lowerRight.y = coord.y;
				extended = true;
			}
			if (coord.y > bound_upperLeft.y) {
				bound_upperLeft.y = coord.y;
				extended = true;
			}
			extended = true;
		}
		return extended;
	}


	// when attributes of feature changed, get new bounds
	public boolean addWktToBounds(String geometryName, int geometryType) {
		try {
			WKTReader reader = new WKTReader();
			switch (geometryType) {
			case WFSLayer.LAYER_TYPE_POINT:
				return this.addToBounds((Point) (reader.read(geometryName)));
			case WFSLayer.LAYER_TYPE_LINE:
				return this.addToBounds((LineString) (reader.read(geometryName)));
			case WFSLayer.LAYER_TYPE_POLYGON:
				return this.addToBounds((Polygon) (reader.read(geometryName)));
			default:
				return false;
			}
		} catch (Exception e) {
			Log.e(CLASSTAG + "addWktToBounds", "Exception: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}


	// Calculates pixel values from coordinates
	public Pixel coordinateToPixel(Coordinate coordinate) {
		return coordinateToPixel(coordinate.x, coordinate.y);
	}

	// Calculates pixel values from coordinates
	public Pixel coordinateToPixel(double x, double y) {
		return new Pixel((float) ((x - upperLeft.x) * scale), 
				(float) ((upperLeft.y - y) * scale));
	}

	// Calculates coordinates from pixel values
	public Coordinate pixelToCoordinate(Pixel p) {
		return new Coordinate((double) (upperLeft.x + p.getCol() / scale),
				(double) (upperLeft.y - p.getRow() / scale));
	}

	// Calculates coordinates from pixel values
	public Coordinate pixelToCoordinate(float c, float r) {
		return new Coordinate((double) (upperLeft.x + c / scale),
				(double) (upperLeft.y - r / scale));
	}

	/**
	 * Sets the map projection; use epsg codes, for example "epsg:3785" (Google Mercator)
	 *
	 * @param epsg the new projection
	 */
	private void setProjection(String epsg) {
		Log.i(CLASSTAG + "setProjection()", "Projection set to: " + epsg);
		mapProjection = ProjectionFactory.getNamedPROJ4CoordinateSystem(epsg);
	}

	/**
	 * Returns the map's projection.
	 * 
	 * @return the maps projection
	 */
	public Projection getProjection() {
		return mapProjection;
	}

	// TODO Ausgabe ist immer eingabe, da immer mapProjection.getEPSGCode() = 0
	/**
	 * Transforms coordinate to the reference system of the map
	 * DOES NOTHING?!?!?!
	 *
	 * @param src the src
	 * @return the coordinate
	 */
	public Coordinate transformToMapCRS(Coordinate src) {
		//Falls kein Projekt geladen wurde...
		if (currentProject == null) {
			return src;	
		}
		if (mapProjection.getEPSGCode() == 4326 || mapProjection.getEPSGCode() == 0) {
			return src;
		}
		Coordinate dst = new Coordinate(0.0, 0.0);
		Log.i(CLASSTAG, "Coordinate transformed to Map CRS: epsg:" + mapProjection.getEPSGCode());
		mapProjection.transform(src, dst);
		return dst;
	}

	// TODO Ausgabe ist immer eingabe, da immer mapProjection.getEPSGCode() = 0 
	/**
	 * Transforms coordinate from the reference system of the map.
	 * DOES NOTHING?!?!?!
	 *
	 * @param src the src
	 * @return the coordinate
	 */
	public Coordinate transformFromMapCRS(Coordinate src) {
		if (mapProjection == null) {
			return src;
		}
		if (mapProjection.getEPSGCode() == 4326 || mapProjection.getEPSGCode() == 0) {
			return src;
		}
		Coordinate dst = new Coordinate(0.0, 0.0);
		Log.i(CLASSTAG, "Coordinate transformed from Map CRS: epsg:" + mapProjection.getEPSGCode());
		mapProjection.inverseTransform(src, dst);
		return dst;
	}

	// Returns distance between touched position (first pointer) and a given Coordinate
	private float touchedDistance(Coordinate c) {
		Pixel p = this.coordinateToPixel(c);
		Pixel t = new Pixel(firstPointerX, firstPointerY);
		return p.distance(t);
	}

	// Returns the coordinate next to the touched position within snapping range
	private int[] getNearestCreatedPointIndex(Point[][] pts) {
		int[] idx = { -3, -3 };
		boolean first = true;
		float dist = 0f;
		float minDist = 0f;
		// check if first point is touched
		if (addFeatureBar.getFirst() != null) {
			dist = touchedDistance(addFeatureBar.getFirst());
			if (dist < SNAPPING_RANGE) {
				if (first) {
					minDist = dist;
					idx[0] = -1;
					idx[1] = -1;
					first = false;
				} else {
					if (dist < minDist) {
						minDist = dist;
						idx[0] = -1;
						idx[1] = -1;
					}
				}
			}
		}
		// check if second point is touched
		if (addFeatureBar.getSecond() != null) {
			dist = touchedDistance(addFeatureBar.getSecond());
			if (dist < SNAPPING_RANGE) {
				if (first) {
					minDist = dist;
					idx[0] = -2;
					idx[1] = -2;
					first = false;
				} else {
					if (dist < minDist) {
						minDist = dist;
						idx[0] = -2;
						idx[1] = -2;
					}
				}
			}
		}
		// check other points
		if (pts != null) {
			for (int i = 0; i < pts.length; i++) {
				for (int j = 0; j < pts[i].length; j++) {
					if (pts[i][j] != null) {
						dist = touchedDistance(pts[i][j].getCoordinate());
						if (dist < SNAPPING_RANGE) {
							if (first) {
								minDist = dist;
								idx[0] = i;
								idx[1] = j;
								first = false;
							} else {
								if (dist < minDist) {
									minDist = dist;
									idx[0] = i;
									idx[1] = j;
								}
							}
						}
					}
				}
			}
		}
		return idx;
	}

	// Returns the coordinate next to the touched position within snapping range
	private int[] getNearestEditedPointIndex(Point[][] pts) {
		int[] idx = { -3, -3 };
		boolean first = true;
		float dist = 0f;
		float minDist = 0f;
		// check if first point is touched
		if (editFeatureBar.getFirst() != null) {
			dist = touchedDistance(editFeatureBar.getFirst());
			if (dist < SNAPPING_RANGE) {
				if (first) {
					minDist = dist;
					idx[0] = -1;
					idx[1] = -1;
					first = false;
				} else {
					if (dist < minDist) {
						minDist = dist;
						idx[0] = -1;
						idx[1] = -1;
					}
				}
			}
		}
		// check if second point is touched
		if (editFeatureBar.getSecond() != null) {
			dist = touchedDistance(editFeatureBar.getSecond());
			if (dist < SNAPPING_RANGE) {
				if (first) {
					minDist = dist;
					idx[0] = -2;
					idx[1] = -2;
					first = false;
				} else {
					if (dist < minDist) {
						minDist = dist;
						idx[0] = -2;
						idx[1] = -2;
					}
				}
			}
		}
		// check other points
		if (pts != null) {
			for (int i = 0; i < pts.length; i++) {
				for (int j = 0; j < pts[i].length; j++) {
					if (pts[i][j] != null) {
						dist = touchedDistance(pts[i][j].getCoordinate());
						if (dist < SNAPPING_RANGE) {
							if (first) {
								minDist = dist;
								idx[0] = i;
								idx[1] = j;
								first = false;
							} else {
								if (dist < minDist) {
									minDist = dist;
									idx[0] = i;
									idx[1] = j;
								}
							}
						}
					}
				}
			}
		}
		return idx;
	}

	/**
	 * 	Returns distance between two fingers/pointers on the panel
	 *  used to calculate scale factor during zoom
	 *
	 * @param ev the ev
	 * @return the float
	 */
	private float gestureSpacing(MotionEvent ev) {
		float dx = ev.getX(0) - ev.getX(1);
		float dy = ev.getY(0) - ev.getY(1);
		return FloatMath.sqrt(dx * dx + dy * dy);
	}

	/**
	 * Returns center of two pointers/fingers on the panel 
	 * used for focal point during zoom
	 *
	 * @param ev the ev
	 * @return the coordinate
	 */
	private Coordinate gestureCenter(MotionEvent ev) {
		float mx = (ev.getX(0) + ev.getX(1)) / 2;
		float my = (ev.getY(0) + ev.getY(1)) / 2;
		return this.pixelToCoordinate(mx, my);
	}

	/**
	 * Returns distance between first touched and moved position in pixel units.
	 * normal phytagoras a2 + b2 = c2
	 * 
	 * @return moved distance
	 */
	private float movedDistance() {
		float diffX = movedPointerX - firstPointerX;
		float diffY = movedPointerY - firstPointerY;
		float dist = FloatMath.sqrt(diffX * diffX + diffY * diffY);
		Log.i(CLASSTAG, "Moved distance: " + dist);
		return dist;
	}

	/**
	 * Updates the MapView's zoom level. Attention: 
	 * leads to Dead Ends when called on wrong locations
	 */
	private void updateMapViewZoom() {
		// Log.e(CLASSTAG, "updateMapViewZoom()");
		if (osmMapView.getVisibility() == View.VISIBLE) {
			Log.i(CLASSTAG + " updateMapViewZoom()", "Screen size: " + width + "x" + height);
			// transform corner coordinates
			Coordinate ul_deg = transformFromMapCRS(getUpperLeft());
			Coordinate lr_deg = transformFromMapCRS(getLowerRight());
			Log.i(CLASSTAG + " updateMapViewZoom()", "Transformed coordinates: UpperLeft; " + ul_deg.x + " " + ul_deg.y + " -- LowerRight: " + lr_deg.x + " " + lr_deg.y);
			// calculate spans
			int diff_lon = (int) ((lr_deg.x - ul_deg.x) * 1E6);
			int diff_lat = (int) ((ul_deg.y - lr_deg.y) * 1E6);
			// zoom to span
			osmMapView.getController().zoomToSpan(diff_lat, diff_lon);
			// get corner coordinates of map projection
			MapView.Projection proj = osmMapView.getProjection();
			IGeoPoint ul_proj = proj.fromPixels(0f, 0f);
			IGeoPoint lr_proj = proj.fromPixels((float) width, (float) (height));
			Log.i(CLASSTAG + " updateMapViewZoom()",	"projected corner coordinates:\n" 
					+ "UpperLeft: "	+ ul_proj.getLongitudeE6() + " " + ul_proj.getLatitudeE6() 
					+ " -- LowerRight " + lr_proj.getLongitudeE6() + " "	+ lr_proj.getLatitudeE6());
			// calculate spans of projected corner coordinates
			int diff_lon_proj = lr_proj.getLongitudeE6() - ul_proj.getLongitudeE6();
			int diff_lat_proj = ul_proj.getLatitudeE6()	- lr_proj.getLatitudeE6();
			// calculate scales
			float scale_lon = (float) diff_lon_proj / (float) diff_lon;
			float scale_lat = (float) diff_lat_proj / (float) diff_lat;
			Log.i(CLASSTAG + " updateMapViewZoom()", "Scales: lon " + scale_lon + " -- lat " + scale_lat);
			// adjust scale
			osmMapView.setScaleX(scale_lon);
			osmMapView.setScaleY(scale_lat);
		}
		panel.writeSharedPreferences();
	}


	/**
	 * Updates the MapView's center
	 */
	private void updateMapViewCenter() {
		// Log.e(CLASSTAG, "updateMapViewCenter");
		if (osmMapView.isActivated()) {
			// calculate center
			Coordinate center = transformFromMapCRS(getCenter());
			osmMapView.getController().setCenter(new GeoPoint((int) (center.y * 1E6), (int) (center.x * 1E6)));
		}
	}

	/**
	 * Updates the panel without osm-mapview
	 */
	public void updateMapView() {
		// Log.e(CLASSTAG, "updateMapView()");
		this.updateMapView(false, null);
	}

	/**
	 * Updates the MapView's center and zoom level with initialization.
	 * 
	 * @param visible 
	 * @param center center coordinate, if null no center change
	 */
	private void updateMapView(boolean visible, Coordinate center) {
		// Log.e(CLASSTAG, "updateMapView(...)");
		// if the osm mapview is 
		if (visible) {
			Log.i(CLASSTAG + " updateMapView", "Screen size for background initialization: " + width + " x " + height);
			// Calculate zoom level
			double GLOBE_WIDTH = 256.0;
			Coordinate ul_deg = transformFromMapCRS(getUpperLeft());
			Coordinate lr_deg = transformFromMapCRS(getLowerRight());
			double angle = lr_deg.x - ul_deg.x;
			if (angle < 0) {
				angle += 360.0;
			}
			int zoomLevel = (int) Math.round(Math.log((double) width * 360.0 / angle / GLOBE_WIDTH) / Math.log(2.0));
			Log.i(CLASSTAG + " updateMapView", "New zoom level: " + zoomLevel);
			//			Log.e(CLASSTAG + " updateMapView", "Calculated zoom level: (int) Math.round(Math.log((double)" + width + " * 360.0	/ " + angle + "/ 256) / Math.log(2.0));");
			osmMapView.getController().setZoom(zoomLevel);
			// Set center
			if (center != null) {
				Log.i(CLASSTAG + " updateMapView", "Called with coordinates: CENTER " + center.x + " " + center.y);
				Coordinate centerWGS84 = this.transformFromMapCRS(center);
				osmMapView.getController().setCenter(new GeoPoint(
						(int) (centerWGS84.y * 1E6), (int) (centerWGS84.x * 1E6)));
			} else {
				Log.i(CLASSTAG + " updateMapView", "Called with coordinates: NULL");
			}
			Log.i(CLASSTAG + " updateMapView", "Map center: " + osmMapView.getMapCenter().getLongitudeE6() + "   "	+ osmMapView.getMapCenter().getLatitudeE6());
			Log.i(CLASSTAG + " updateMapView", "Our center: " + getCenter());
			// Adjust zoom scale to corner coordinates
			// calculate spans
			int diff_lon = (int) ((lr_deg.x - ul_deg.x) * 1E6);
			int diff_lat = (int) ((ul_deg.y - lr_deg.y) * 1E6);
			// get corner coordinates of map projection
			MapView.Projection proj = osmMapView.getProjection();
			IGeoPoint ul_proj = proj.fromPixels(0f, 0f);
			IGeoPoint lr_proj = proj.fromPixels((float) width, (float) (height));
			Log.i(CLASSTAG + " updateMapView",	"Projected corner coordinates:\n" + "UpperLeft " + ul_proj.getLongitudeE6() + " " + ul_proj.getLatitudeE6() 
					+ " -- LowerRight " + lr_proj.getLongitudeE6() + " " + lr_proj.getLatitudeE6());
			// calculate spans of projected corner coordinates
			int diff_lon_proj = lr_proj.getLongitudeE6() - ul_proj.getLongitudeE6();
			int diff_lat_proj = ul_proj.getLatitudeE6()	- lr_proj.getLatitudeE6();
			// calculate scales
			float scale_lon = (float) diff_lon_proj / (float) diff_lon;
			float scale_lat = (float) diff_lat_proj / (float) diff_lat;
			Log.i(CLASSTAG + " updateMapView", "Initial scales: lon " + scale_lon + " -- lat " + scale_lat);
			// Set scales
			osmMapView.setScaleX(scale_lon);
			osmMapView.setScaleY(scale_lat);
		} else {
			// ohne mapview anpassen
			this.updateMapViewCenter();
			this.updateMapViewZoom();
		}
	}
	
	/**
	 * Gets the upper left pixel (0,0) of the screen as georeferenced Coordinate.
	 *
	 * @return the upper left
	 */
	public Coordinate getUpperLeft() {
		return this.upperLeft;
	}

	/**
	 * Gets the lower right pixel of the screen as georeferenced Coordinate.
	 *
	 * @return the lower right
	 */
	public Coordinate getLowerRight() {
		return pixelToCoordinate((float) this.width, (float) this.height);
	}

	/**
	 * Get the center of the screen as georeferenced Coordinate.
	 *
	 * @return the center Coordinate
	 */
	public Coordinate getCenter() {
		return pixelToCoordinate((float) this.width / 2f,
				(float) this.height / 2f);
	}

	/**
	 * Sets the display's center to given coordinate.
	 * 
	 * @param coord
	 */
	public void moveTo(Coordinate coord) {
		Log.i(CLASSTAG + " moveTo", "Moving to " + coord.x + " -- " + coord.y);
		// oberen linken punkt neu setzen
		this.upperLeft.x = coord.x - (this.width / 2.0) / this.scale;
		this.upperLeft.y = coord.y + (this.height / 2.0) / this.scale;
		// osmmap auch updaten
		if (this.osmMapView.isActivated()) {
			this.updateMapView();
		}
		// calculate its bounds (internally it loads also features from database into FeatureContainer)
		this.calculateBounds();
		panel.invalidate();
	}
	
	
	
}
