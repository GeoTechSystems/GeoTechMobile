/**
 * The Panel which shows all views
 * 
 * @author Torsten Hoch
 * @author Sven Weisker
 * @author tubatubsen
 * @author bschm
 */

package de.geotech.systems.drawing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PixelFormat;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TextView;

import com.jhlabs.map.proj.Projection;
import com.jhlabs.map.proj.ProjectionFactory;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.WKTReader;

import de.geotech.systems.R;
import de.geotech.systems.editor.AddFeatureBar;
import de.geotech.systems.editor.EditFeatureBar;
import de.geotech.systems.editor.EditedFeatureDrawer;
import de.geotech.systems.features.Feature;
import de.geotech.systems.features.FeatureContainer;
import de.geotech.systems.geometry.Pixel;
import de.geotech.systems.kriging.KrigingToolbar;
import de.geotech.systems.projects.Project;
import de.geotech.systems.projects.ProjectHandler;
import de.geotech.systems.utilities.Alerts;
import de.geotech.systems.utilities.Functions;
import de.geotech.systems.utilities.StringUtils;
import de.geotech.systems.wfs.WFSLayer;
import de.geotech.systems.wfs.WFSLayerAttributeTypes;
import de.geotech.systems.wms.WMSOverlay;
import de.geotech.systems.wms.WMSUtils;

public class DrawingPanelView extends SurfaceView implements SurfaceHolder.Callback {
	private static final String CLASSTAG = "DrawingPanelView";
	// codes for panning, zooming etc
	private static final int INVALID_POINTER_ID = -1;
	private static final int GESTURE_PAN = 0;
	private static final int GESTURE_ZOOM = 1;
	private static final int GESTURE_TOUCH = 2;
	private static final int GESTURE_MOVE_ADDED_POINT = 3;
	private static final int GESTURE_MOVE_EDITED_POINT = 4;
	// // initializing bounds, scale and center point for germany
	// private static final double UPPERLEFTX = 2.873095044409291;
	// private static final double UPPERLEFTY = 55.31349629047838;
	// private static final double BOUNDUPPERLEFTX = 3.1566178367541875;
	// private static final double BOUNDUPPERLEFTY = 55.205842500701586;
	// private static final double BOUNDLOWERRIGHTX = 17.97094745720341;
	// private static final double BOUNDLOWERRIGHTY = 47.37651360450777;
	// private static final double SCALE = 86.32669611875941;
	// initializing bounds, scale and center point for Baden-Württemberg
	private static final double UPPERLEFTX = 6.520539854486395;
	private static final double UPPERLEFTY = 50.1300013079152;
	private static final double BOUNDUPPERLEFTX = 7.55385762;
	private static final double BOUNDUPPERLEFTY = 49.78753874;
	private static final double BOUNDLOWERRIGHTX = 10.44949721;
	private static final double BOUNDLOWERRIGHTY = 47.56485503;
	private static final double SCALE = 241.23656351074038;
	private static final double STANDARDMINIMUMSPAN = 10.0;
	// avoid short zoom distances (<10 pixel)
	private static final float ZOOM_AVOID_SHORT_DISTANCES = 10f;
	// time in millies to start a long touch event
	private static final int GESTURE_LONG_TOUCH_TIME = 900;
	// maximum travel distance in pixels to start a long touch event
	private static final float GESTURE_LONG_TOUCH_TRAVEL = 15f;
	// snapping range
	private static final float SNAPPING_RANGE = 20f;
	// standard projection for no project loaded mode (WGS 84)
	// private static final String STANDARDPROJECTION = "epsg:4326";
	private static final int GPS_COORD_CONTAINER_SIZE = 10;
	// radius of gps points
	private final static float GPS_POINT_DIAMETER = 10f;
	private static final float GPS_RING_EXTRA_RADIUS = 4f;
	private final static float GPS_OLD_POINT_DIAMETER = 4f;
	// color of gps point
	private final static int GPS_POINT_COLOR = Color.RED;
	private final static int GPS_OUTER_RING_COLOR = Color.GRAY;
	private static final double GPS_SCALE_FOR_BIGGER_POINT = 9000;

	// the focus for the scale
	private Coordinate scaleFocus;
	// Coordinate reference system and projections
	private Projection mapProjection;
	// moving new created point
	private int[] pointToMoveIdx = { -1, -1 };
	// mode for adding features
	private boolean creationMode;
	// the bar for adding features
	private AddFeatureBar addFeatureBar;
	// the bar for editing features
	private EditFeatureBar editFeatureBar;
	// TouchEvent variables
	private int activePointer;
	private int activeGesture;
	// touch time
	private long firstPointerTime;

	// MapView for background map
	private MapView osmMapView;
	// all Features to draw in a container
	// private ArrayList<ArrayList<Feature>> featureContainer;
	private FeatureContainer featureContainer;
	// new drawing elements
	// private Paint paint;
	private Paint editorPoint;
	private Paint editorLine;
	private Paint editorPolygon;
	// initialize a geometry factory
	private GeometryFactory geometryFactory;
	// the current project
	private Project currentProject;
	// WMS
	private WMSOverlay wmsOverlay;
	// Shared Preferences of MainActivity
	private SharedPreferences prefs;
	// editor for shared preferences
	private SharedPreferences.Editor prefsEditor;
	// is edit mode on
	private boolean isEditing;
	// kumulated pan distance
	private float panMovesKumulated;
	// the feature to be edited
	private Feature currentEditedFeature;
	// helper for a fat point when a new feature is drawn
	private int fatPoint;
	// the drawing thread for features
	private DrawingThread drawingThread;
	// Thread handle stuff
	private ExecutorService service = Executors.newSingleThreadExecutor();
	// future Thread
	private Future<?> futureThread;
	// the context
	private Context context;
	// alert dialogs in this class
	private AlertDialog alert;
	// drawer to draw cyan currently being edited features
	private EditedFeatureDrawer editedFeatureDrawer;
	// array for last gotten gps positions to track movement
	private ArrayList<Coordinate> gpsCoords;
	// if the gps tracking is activated
	private boolean gpsTracking;

	// Screen of Tablet
	private MapScreen mapScreen;
	private boolean krigingMode;
	private KrigingToolbar krigingToolbar;
	

	/**
	 * Instantiates a new drawing panel view. Is called by findbyview in our MainActivity.
	 * 
	 * @param context
	 *            the context
	 * @param attrs
	 *            the attrs
	 */
	public DrawingPanelView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
		this.firstPointerTime = 0;
		this.scaleFocus = new Coordinate(0.0, 0.0);
	//	bboxFeatures = new BBOX();
		// this.featureContainer = new ArrayList<ArrayList<Feature>>();
		this.featureContainer = new FeatureContainer();
		this.geometryFactory = new GeometryFactory();
		// this.paint = new Paint();
		this.editorPoint = new Paint();
		this.editorLine = new Paint();
		this.editorPolygon = new Paint();
		this.activePointer = INVALID_POINTER_ID;
		this.setActiveGesture(GESTURE_PAN);
		this.panMovesKumulated = 0f;
		this.gpsCoords = new ArrayList<Coordinate>();
		this.gpsTracking = false;
		this.mapScreen = MapScreen.getInstance();
	}

	/**
	 * Inits the drawing panel view. Called in MainActivity after the findbyview.
	 * 
	 * @param osm
	 *            the osm to be shown as background map
	 * @param prefs
	 *            the shared preferences
	 * @param toolbarLayout
	 *            the toolbar layout for add and edit-feature bars
	 */
	public void initDrawingPanelView(MapView osm, SharedPreferences prefs, LinearLayout toolbarLayout) {
		// init the toolbar
		this.addFeatureBar = new AddFeatureBar(this, toolbarLayout);
		this.addFeatureBar.setAddFeatureListener();
		this.krigingToolbar = new KrigingToolbar(this, toolbarLayout);
		this.editFeatureBar = new EditFeatureBar(this, toolbarLayout);
		this.editFeatureBar.setEditFeatureListener();
		// set shared preferences
		this.prefs = prefs;
		// set the editor
		this.prefsEditor = prefs.edit();
		// // set height and width of current devices display (in pixels)
		// DisplayMetrics displaymetrics = new DisplayMetrics();
		// ((Activity) getContext()).getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
		// this.setWidth(displaymetrics.widthPixels);
		// this.setHeight(displaymetrics.heightPixels);
		// setting up the osm background map
		this.setBackgroundMap(osm);
		// styles for point/polyline/polygon
		this.editorPoint.setARGB(130, 255, 0, 0);
		this.editorPoint.setStyle(Style.FILL);
		this.editorLine.setARGB(130, 255, 0, 0);
		this.editorLine.setStyle(Style.STROKE);
		this.editorLine.setStrokeWidth(3f);
		this.editorPolygon.setARGB(130, 255, 0, 0);
		this.editorPolygon.setStyle(Style.FILL);
		// no feature creation mode yet
		this.setCreationMode(false);
		// no Kriging-mode yet
		this.setKrigingMode(false);
		// setzt SurfaceView transparent
		this.getHolder().addCallback(this);
		this.setZOrderOnTop(true);
		// surfaceholder for thread
		SurfaceHolder sfhTrackHolder = getHolder();
		sfhTrackHolder.setFormat(PixelFormat.TRANSPARENT);
		
	}

	/**
	 * Start first panel drawing. To be called after initDrawingPanelView(...) and after the currentproject attributes are set in
	 * AMinActivity
	 */
	public void startFirstPanelDrawing() {
		// get the latest shared preferences
		this.loadSharedPreferences();
		// show the current view
		this.showCurrentProject(false);
		// no edit mode yet
		this.setEditMode(false);
	}

	/**
	 * Draws the current project. Zoom only, if project is loaded new and no shared preferences are given.
	 * 
	 * @param zoom
	 *            if a zoom to the outest bounds should be done
	 */
	public void showCurrentProject(boolean zoom) {
		// setting the current project
		this.currentProject = ProjectHandler.getCurrentProject();
		// if no project is loaded
		if (currentProject == null) {
			// calculate its bounds (internally it loads also features from database into FeatureContainer)
			this.calculateBounds();
			// set mapview
			if (this.osmMapView.isActivated()) {
				this.updateMapViewCenter();
				this.updateMapView(true, mapScreen.getMapCenter());
			}
		} else {
			// set its projection
			this.setProjection("epsg:" + currentProject.getEpsgCode());
			// calculate its bounds (internally it loads also features from database into FeatureContainer)
			this.calculateBounds();
			if (zoom) {
				this.zoomToOutestBounds();
			}
			// adjust the underlying map
			this.setMapActive(currentProject.isOSM());
			if (this.osmMapView.isActivated()) {
				// zoom to calculated bounds if true
				this.updateMapViewCenter();
				this.updateMapView(true, mapScreen.getMapCenter());
			}
			if (wmsOverlay != null) {
				this.updateWMSOverlay();
			}
		}
		// draw all features
		this.invalidate();
		// write the last shared preferences
		this.writeSharedPreferences();
	}

	// wenn das layout geaendert wird (pixel x pixel geaendert wird) (?)
	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		if (changed) {
			mapScreen.width = right - left;
			mapScreen.height = bottom - top;
		}
		this.writeSharedPreferences();
	}

	// wenn die diplaygroesse (pixel x pixel geaendert wird) (?)
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		if (w != 0) {
			mapScreen.width = w;
		}
		if (h != 0) {
			mapScreen.height = h;
		}
		this.writeSharedPreferences();
	}

	// Called when user touches the DrawingPanel
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// Get kind of action - MotionEvent.ACTION_MASK for multi-touch
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		// -> ein erster finger beruehrt das display
		case MotionEvent.ACTION_DOWN:
			// erst features malen, wenn kein finger mehr auf display
			// this.drawFeaturesNow = false;
			// set active gesture
			setActiveGesture(GESTURE_TOUCH);
			// set pointer values
			mapScreen.firstPointerX = event.getX();
			mapScreen.firstPointerY = event.getY();
			mapScreen.movedPointerX = event.getX();
			mapScreen.movedPointerY = event.getY();
			firstPointerTime = SystemClock.uptimeMillis();
			// set pointer id
			activePointer = event.getPointerId(0);
			return true;
		case MotionEvent.ACTION_POINTER_DOWN:
			// -> ein weiterer finger beruehrt display
			// calculate base distance between fingers
			mapScreen.baseDist = gestureSpacing(event);
			// avoid short bases (<10 pixel)
			if (mapScreen.baseDist > ZOOM_AVOID_SHORT_DISTANCES) {
				// switch to zoom mode
				setActiveGesture(GESTURE_ZOOM);
				// calculate focal point
				scaleFocus.setCoordinate(gestureCenter(event));
				// safe old scale
				mapScreen.oldScale = mapScreen.scale;
				// calculate coordinate differences
				mapScreen.focusX = mapScreen.getMapUpperLeft().x - scaleFocus.x;
				// to focal point
				mapScreen.focusY = mapScreen.getMapUpperLeft().y - scaleFocus.y;
			}
			return true;
		case MotionEvent.ACTION_MOVE:
			// -> ein finger bewegt sich auf display
			// index by id
			int pIndex = event.findPointerIndex(activePointer);
			// wenn nur ein finger auf dem display ist
			if (activeGesture == GESTURE_TOUCH) {
				// dummy
				// Log.e(CLASSTAG + " ACTION_MOVE", "Editfeaturebar Status is " +
				// this.getEditFeatureToolbar().getStatusAsString());
				// Log.e(CLASSTAG + " ACTION_MOVE", "Editing Mode: " + this.isInEditingMode() + " - Creation Mode: " +
				// this.isInCreationMode());
				// wenn gerade neues feature erstellt wird
				if (isInCreationMode() && addFeatureBar.getStatus() == AddFeatureBar.STATUS_ADJUST) {
					pointToMoveIdx = getNearestCreatedPointIndex(addFeatureBar.getCoordinates());
					if (pointToMoveIdx[0] > -3 && pointToMoveIdx[1] > -3) {
						setActiveGesture(GESTURE_MOVE_ADDED_POINT);
					} else {
						setActiveGesture(GESTURE_PAN);
					}
					// wenn gerade feature editiert wird
				} else if (isInEditingMode() && editFeatureBar.getStatus() == EditFeatureBar.STATUS_ADJUST) {
					pointToMoveIdx = getNearestEditedPointIndex(editFeatureBar.getCoordinates());
					// Log.e(CLASSTAG + " ACTION_MOVE", "INEDITINGMODE AND STATUS_ADJUST!!!!!!!!!!!!!!!!!!!!!!!!");
					for (int i = 0; i < pointToMoveIdx.length; i++) {
						// Log.e(CLASSTAG + " ACTION_MOVE", "pointToMoveIdx " + i + ": " + pointToMoveIdx[i]);
					}
					if (pointToMoveIdx[0] > -3 && pointToMoveIdx[1] > -3) {
						// Log.e(CLASSTAG, "Gesture GESTURE_EDIT_POINT set!!!!!!!!!!");
						setActiveGesture(GESTURE_MOVE_EDITED_POINT);
					} else {
						// Log.e(CLASSTAG + " ACTION_MOVE",
						// "NOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOt NEAR!!!!");
						setActiveGesture(GESTURE_PAN);
					}
				} else {
					setActiveGesture(GESTURE_PAN);
				}
			} else {
				// Log.e(CLASSTAG + " ACTION_MOVE", "activeGesture != GESTURE_TOUCH, activeGesture: " +
				// this.getGestureAsString(this.activeGesture));
			}
			// determine if pan or zoom
			switch (activeGesture) {
			case GESTURE_PAN:
				// update upper left corner coordinates
				Coordinate upperLeftMap = mapScreen.getMapUpperLeft();
				upperLeftMap.x -= (event.getX(pIndex) - mapScreen.movedPointerX) / mapScreen.scale;
				upperLeftMap.y += (event.getY(pIndex) - mapScreen.movedPointerY) / mapScreen.scale;
				mapScreen.setBBOXMap(upperLeftMap);
				// set moved pixel values
				mapScreen.movedPointerX = event.getX(pIndex);
				mapScreen.movedPointerY = event.getY(pIndex);
				updateMapView();
				// kumulate the distances of panning
				panMovesKumulated = panMovesKumulated + mapScreen.movedDistance();
				// if panned a big distance
				if (panMovesKumulated > (2 * mapScreen.height)) {
					panMovesKumulated = 0f;
					// calculate its bounds (internally it loads also features from database into FeatureContainer)
					this.calculateBounds();
				}
				this.invalidate();
				break;
			case GESTURE_ZOOM:
				// calculate base for scaling
				float currentScale = gestureSpacing(event) / mapScreen.baseDist;
				// calculate new scale
				if (mapScreen.oldScale * currentScale >= mapScreen.MINIMUM_ZOOM_SCALE) {
					if (mapScreen.oldScale * currentScale <= mapScreen.MAXIMUM_ZOOM_SCALE) {
						mapScreen.scale = mapScreen.oldScale * currentScale;
						// adjust upper left corner coordinates
						double x = (scaleFocus.x + mapScreen.focusX / currentScale);
						// using focal point and new scale
						double y = (scaleFocus.y + mapScreen.focusY / currentScale);
					//	bboxMap = new BBOX(new Coordinate(x, y));
						mapScreen.setBBOXMap(new Coordinate(x, y));
						if (this.osmMapView.isActivated()) {
							updateMapView();
						}
						// calculate its bounds (internally it loads also features from database into FeatureContainer)
						this.calculateBounds();
					}
				}
				break;
			// TODO beide naechste zusammenfassen
			case GESTURE_MOVE_ADDED_POINT:
				if (pointToMoveIdx[0] > -3 && pointToMoveIdx[1] > -3) {
					// Log.i(CLASSTAG, "Moving point with index " + pointToMoveIdx[0] + " " + pointToMoveIdx[1]);
					addFeatureBar.movePointTo(pointToMoveIdx, mapScreen.pixelToCoordinate(event.getX(pIndex), event.getY(pIndex)));
				}
				break;
			case GESTURE_MOVE_EDITED_POINT:
				// Log.e(CLASSTAG, "Case Gesture: GESTURE_EDIT_POINT!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
				if (pointToMoveIdx[0] > -3 && pointToMoveIdx[1] > -3) {
					// Log.e(CLASSTAG, "Moving point with index " + pointToMoveIdx[0] + " " + pointToMoveIdx[1]);
					editFeatureBar.movePointTo(pointToMoveIdx, mapScreen.pixelToCoordinate(event.getX(pIndex), event.getY(pIndex)));
				}
				break;
			}
			this.invalidate();
			return true;
		case MotionEvent.ACTION_POINTER_UP:
			// -> der nicht letzte finger wieder hoch
			// Extract the index of the pointer that left the touch sensor
			final int pointerIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
			final int pId = event.getPointerId(pointerIndex);
			if (pId == activePointer) {
				// This was our active pointer going up. Choose a new
				// active pointer and adjust accordingly.
				final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
				mapScreen.movedPointerX = event.getX(newPointerIndex);
				mapScreen.movedPointerY = event.getY(newPointerIndex);
				activePointer = event.getPointerId(newPointerIndex);
			}
			// switch to pan mode
			setActiveGesture(GESTURE_PAN);
			return true;
		case MotionEvent.ACTION_UP:
			// -> letzter finger hoch
			// erst features malen, wenn kein finger mehr auf display
			// this.drawFeaturesNow = true;
			// wenn nicht gerade verschoben wurde
			if ((SystemClock.uptimeMillis() - firstPointerTime) < GESTURE_LONG_TOUCH_TIME) {
				if (isInCreationMode() && addFeatureBar.getType() > -1) {
					addFeatureBar.createFeature(mapScreen.pixelToCoordinate(mapScreen.firstPointerX, mapScreen.firstPointerY));
					this.invalidate();
				}
			} else if (!isInCreationMode() && !isInEditingMode()
					&& (SystemClock.uptimeMillis() - firstPointerTime) > GESTURE_LONG_TOUCH_TIME
					&& mapScreen.movedDistance() < GESTURE_LONG_TOUCH_TRAVEL) {
				// long touch action
				longTouchAction(event.getX(), event.getY());
			} else if (isInCreationMode() && (SystemClock.uptimeMillis() - firstPointerTime) > GESTURE_LONG_TOUCH_TIME
					&& mapScreen.movedDistance() < GESTURE_LONG_TOUCH_TRAVEL) {
				// long touch action in creation mode
				// TODO: ueberlegen, was hier passieren koennte:
				// Fall ist, dass der user bei erstellen eines neuen features ist und hier lange aufs display gedrueckt hat
			} else if (isInEditingMode() && (SystemClock.uptimeMillis() - firstPointerTime) > GESTURE_LONG_TOUCH_TIME
					&& mapScreen.movedDistance() < GESTURE_LONG_TOUCH_TRAVEL) {
				// long touch action in editing mode
				// TODO: ueberlegen, was hier passieren koennte:
				// Fall ist, dass der user bei editieren eines bestehenden features ist und hier lange aufs display gedrueckt hat
			} else {
				// calculate its bounds (internally it loads also features from database into FeatureContainer)
				this.calculateBounds();
				this.invalidate();
			}
			// gesture finished
			activePointer = INVALID_POINTER_ID;
			return true;
		case MotionEvent.ACTION_CANCEL:
			// -> gesture canceled
			activePointer = INVALID_POINTER_ID;
			return false;
		default:
			return false;
		}
	}

	/**
	 * Sets the active gesture.
	 * 
	 * @param newGesture
	 *            the new active gesture
	 */
	private void setActiveGesture(int newGesture) {
		this.activeGesture = newGesture;
		// Log.e(CLASSTAG + " setActiveGesture", "Active gesture set to " + getGestureAsString(newGesture));
	}

	/**
	 * Gets the active gesture as string.
	 * 
	 * @param gesture
	 *            the gesture code
	 * @return the gesture as string
	 */
	private String getGestureAsString(int gesture) {
		switch (gesture) {
		case GESTURE_PAN:
			return "GESTURE_PAN";
		case GESTURE_ZOOM:
			return "GESTURE_ZOOM";
		case GESTURE_TOUCH:
			return "GESTURE_TOUCH";
		case GESTURE_MOVE_ADDED_POINT:
			return "GESTURE_MOVE_POINT";
		case GESTURE_MOVE_EDITED_POINT:
			return "GESTURE_EDIT_POINT";
		default:
			return "unkknown";
		}
	}

	/**
	 * Function to draw on view, called with invalidate
	 */
	@Override
	public void onDraw(Canvas canvas) {
		// Set map size
		this.mapScreen.width = canvas.getWidth();
		this.mapScreen.height = canvas.getHeight();
		// set WMS Overlay
		if (this.wmsOverlay != null) {
			this.wmsOverlay.draw(canvas);
		}
		// Unterbricht den laufenden Thread, weil hier ondraw erneut aufgerufen wurde
		// und der alte thread damit unnuetz ist
		if (this.futureThread != null) {
			// Log.i(CLASSTAG, "Old Thread stopped.");
			this.futureThread.cancel(true);
			// this.cleanSurface();
		}
		// this.drawingThread.setFeatureContainer(featureContainer);
		this.drawingThread.drawFeatures(true);
		// startet den Thread
		// Log.i(CLASSTAG, "Submitting new FutureThread... ");
		this.futureThread = service.submit(drawingThread);
		// draw new added features
		if (isInCreationMode()) {
			// handle addfeature functions drawing
			this.drawNewAddedFeature(canvas);
		}
		// draw edited features
		if (isInEditingMode()) {
			// draw edited feature
			this.editedFeatureDrawer = new EditedFeatureDrawer(getContext(), editFeatureBar.getfeatureCopy(),
					mapScreen.getMapUpperLeft(), mapScreen.scale);
			this.editedFeatureDrawer.draw(canvas);
		}
		// draw last GPS Points
		if (isGPSTracking()) {
			this.drawGPSPoints(canvas);
		}
	}

	/**
	 * Draws gps points.
	 */
	private void drawGPSPoints(Canvas canvas) {
		Pixel pixel;
		Paint paint = new Paint();
		// kleinen nachgeführten GPS-Punkte malen gpsCoords koennen nicht null sein,
		// da mindestens ein punkt beim aufruf dieser methode hinzugefuegt wurde
		for (Coordinate coordLoop : gpsCoords) {
			paint.setColor(GPS_POINT_COLOR);
			pixel = mapScreen.coordinateToPixel(coordLoop);
			canvas.drawCircle(pixel.getCol(), pixel.getRow(), GPS_OLD_POINT_DIAMETER, paint);
		}
		// den grossen punkt mit grauem Ring malen
		pixel = mapScreen.coordinateToPixel(gpsCoords.get(gpsCoords.size() - 1));
		// falls stark rausgezoomt ist, größer malen
		if (mapScreen.scale < GPS_SCALE_FOR_BIGGER_POINT) {
			paint.setColor(GPS_OUTER_RING_COLOR);
			canvas.drawCircle(pixel.getCol(), pixel.getRow(), GPS_POINT_DIAMETER + GPS_RING_EXTRA_RADIUS, paint);
			paint.setColor(GPS_POINT_COLOR);
			canvas.drawCircle(pixel.getCol(), pixel.getRow(), GPS_POINT_DIAMETER, paint);
		} else {
			paint.setColor(GPS_OUTER_RING_COLOR);
			canvas.drawCircle(pixel.getCol(), pixel.getRow(), (float) ((GPS_POINT_DIAMETER / 2) + GPS_RING_EXTRA_RADIUS), paint);
			paint.setColor(GPS_POINT_COLOR);
			canvas.drawCircle(pixel.getCol(), pixel.getRow(), (float) GPS_POINT_DIAMETER / 2, paint);
		}
	}

	private void drawNewAddedFeature(Canvas canvas) {
		DrawingFeatureManager manager = new DrawingFeatureManager(this);
		// feature to be created
		if (addFeatureBar.getType() > -1) {
			switch (addFeatureBar.getType()) {
			case WFSLayer.LAYER_TYPE_POINT:
				if (addFeatureBar.getNewPoint() != null) {
					manager.drawPoint(addFeatureBar.getNewPoint(), 7f, editorPoint, canvas, true, this);
					manager.drawPoint(addFeatureBar.getNewPoint(), 2f, new Paint(Color.BLACK), canvas, false, this);
				}
				break;
			case WFSLayer.LAYER_TYPE_LINE:
				// Immer der Punkt, der gerade bearbeitet wird soll zur besseren Übersicht fett gezeichnet werden.
				// check if only one point set
				if (addFeatureBar.getFirst() != null && addFeatureBar.getSecond() == null) {
					if (addFeatureBar.aktWorkingPointNum == 0) {
						manager.drawPoint(geometryFactory.createPoint(addFeatureBar.getFirst()), 7f, editorPoint, canvas, true,
								this);
						// manager.drawPoint(addFeatureBar.getNewPoint(), 2f, new Paint(Color.BLACK), canvas, false, this);
						manager.drawPoint(geometryFactory.createPoint(addFeatureBar.getFirst()), 2f, new Paint(Color.BLACK),
								canvas, false, this);
					} else {
						manager.drawPoint(geometryFactory.createPoint(addFeatureBar.getFirst()), 3f, editorPoint, canvas, true,
								this);
					}
				}
				// draw line if more than one point is set
				if (addFeatureBar.getNewLine() != null) {
					LineString currentLine = addFeatureBar.getNewLine();
					// draw line
					manager.drawLine(currentLine, editorLine, canvas, true, this);
					// draw line nodes
					Coordinate[] currentCoords = currentLine.getCoordinates();
					for (int i = 0; i < currentCoords.length; i++) {
						Coordinate c = currentCoords[i];
						// Zeichne den durch aktWorkingPointNum bezeichneten Punkt fett
						if (i == addFeatureBar.aktWorkingPointNum) {
							manager.drawPoint(geometryFactory.createPoint(c), 7f, editorPoint, canvas, true, this);
							manager.drawPoint(geometryFactory.createPoint(c), 2f, new Paint(Color.BLACK), canvas, false, this);
						} else {
							manager.drawPoint(geometryFactory.createPoint(c), 3f, editorPoint, canvas, true, this);
						}
					}
				}
				break;
			case WFSLayer.LAYER_TYPE_POLYGON:
				// check if only one point is set
				if (addFeatureBar.getFirst() != null && addFeatureBar.getSecond() == null) {
					if (addFeatureBar.aktWorkingPointNum == 0) {
						manager.drawPoint(geometryFactory.createPoint(addFeatureBar.getFirst()), 7f, editorPoint, canvas, true,
								this);
						manager.drawPoint(geometryFactory.createPoint(addFeatureBar.getFirst()), 2f, new Paint(Color.BLACK),
								canvas, false, this);
					} else {
						manager.drawPoint(geometryFactory.createPoint(addFeatureBar.getFirst()), 3f, editorPoint, canvas, true,
								this);
					}
				}
				// check if two points are set
				if (addFeatureBar.getFirst() != null && addFeatureBar.getSecond() != null) {
					// draw line
					manager.drawLine(
							geometryFactory.createLineString(new Coordinate[] { addFeatureBar.getFirst(),
									addFeatureBar.getSecond() }), editorLine, canvas, true, this);
					// draw nodes
					Log.d(CLASSTAG, "two points. aktWorkingPointNum=" + String.valueOf(addFeatureBar.aktWorkingPointNum));
					if (addFeatureBar.aktWorkingPointNum == 1) {
						manager.drawPoint(geometryFactory.createPoint(addFeatureBar.getFirst()), 3f, editorPoint, canvas, true,
								this);
						manager.drawPoint(geometryFactory.createPoint(addFeatureBar.getSecond()), 7f, editorPoint, canvas, true,
								this);
						manager.drawPoint(geometryFactory.createPoint(addFeatureBar.getSecond()), 2f, new Paint(Color.BLACK),
								canvas, false, this);
					} else {
						manager.drawPoint(geometryFactory.createPoint(addFeatureBar.getFirst()), 3f, editorPoint, canvas, true,
								this);
						manager.drawPoint(geometryFactory.createPoint(addFeatureBar.getSecond()), 3f, editorPoint, canvas, true,
								this);
					}
				}
				// draw polygon if more than two points are set
				if (addFeatureBar.getNewPolygon() != null) {
					Polygon currentPolygon = addFeatureBar.getNewPolygon();
					// draw polygon
					manager.drawPolygon(currentPolygon, editorPolygon, canvas, true, this);
					// draw polygon nodes
					Coordinate[] currentCoords = currentPolygon.getCoordinates();
					fatPoint = -1;
					if (addFeatureBar.showFatWorkingPoint) {
						// Bestimme, welcher Punkt hervorgehoben werden soll
						if (addFeatureBar.aktWorkingObjNum > 0) {
							fatPoint += addFeatureBar.getNewPolygon().getExteriorRing().getNumPoints();
							if (addFeatureBar.getNewPolygon().getNumInteriorRing() >= addFeatureBar.aktWorkingObjNum) {
								for (int i = 0; i < addFeatureBar.aktWorkingObjNum - 1; i++)
									fatPoint += addFeatureBar.getNewPolygon().getInteriorRingN(i).getNumPoints();
							} else {
								for (int i = 0; i < addFeatureBar.getNewPolygon().getNumInteriorRing(); i++)
									fatPoint += addFeatureBar.getNewPolygon().getInteriorRingN(i).getNumPoints();
							}
						}
						fatPoint += addFeatureBar.aktWorkingPointNum + 1;
					}
					for (int i = 0; i < currentCoords.length; i++) {
						Coordinate c = currentCoords[i];
						if (i != fatPoint) {
							manager.drawPoint(geometryFactory.createPoint(c), 3f, editorPoint, canvas, true, this);
						} else {
							manager.drawPoint(geometryFactory.createPoint(c), 7f, editorPoint, canvas, true, this);
							manager.drawPoint(geometryFactory.createPoint(c), 2f, new Paint(Color.BLACK), canvas, false, this);
						}
					}
				}
				break;
			}
		}
	}

	/**
	 * Performed after a long touch - opens alert with detail view of features near touch point
	 * 
	 * @param eventX
	 *            the event x
	 * @param eventY
	 *            the event y
	 */
	private void longTouchAction(float eventX, float eventY) {
		currentProject = ProjectHandler.getCurrentProject();
		// touched point as coordinate
		Coordinate middle = new Coordinate(mapScreen.pixelToCoordinate(eventX, eventY));
		// the display as bounding box and its corners as coordinates
		Point obenLinks = new GeometryFactory().createPoint(mapScreen.pixelToCoordinate(0, 0));
		Point untenRechts = new GeometryFactory().createPoint(mapScreen.pixelToCoordinate(mapScreen.width, mapScreen.height));
		// difference geteilt durch angemessenen faktor
		double diffX = Math.abs(obenLinks.getX() - untenRechts.getX()) / (64);
		double diffY = Math.abs(obenLinks.getY() - untenRechts.getY()) / (32);
		Coordinate ol = new Coordinate(middle.x - diffX, middle.y - diffY);
		Coordinate ur = new Coordinate(middle.x + diffX, middle.y + diffY);
		ArrayList<Feature> selectedFeatures = new ArrayList<Feature>();
		FeatureContainer featureContainer2 = loadActiveFeaturesInBox(currentProject, ol, ur, true);
		// Log.e(CLASSTAG + " longTouchAction", featureContainer2.countAllFeatures() + " longtouched Features in Container.");
		selectedFeatures = featureContainer2.getAllFeatures();
		// in liste ausgeben und veraenderbar machen, wenn gewuenscht (nur bei gelockten features)
		if (selectedFeatures != null && selectedFeatures.size() > 0) {
			TableLayout layout = new TableLayout(getContext());
			LinearLayout innerLayout = new LinearLayout(getContext());
			// buttons fuer info und editieren und textview
			TextView featureText = new TextView(getContext());
			Button featureInfo = new Button(getContext());
			Button editFeature = new Button(getContext());
			// eingebaut in eine scrollpane
			ScrollView scrollPane = new ScrollView(getContext());
			// WFSLayer, in dem feature liegt
			WFSLayer featureLayer;
			// fuer jedes feature in der bounding box
			for (int i = 0; i < selectedFeatures.size(); i++) {
				// views erstellen
				featureText = new TextView(getContext());
				featureText.setMaxWidth(450);
				featureInfo = new Button(getContext());
				editFeature = new Button(getContext());
				featureInfo.setText(context.getString(R.string.long_touch_detail_button));
				editFeature.setText(context.getString(R.string.long_touch_edit_button));
				// gesuchten layer fuer das feature finden
				featureLayer = currentProject.getWFSLayerbyID(selectedFeatures.get(i).getWFSlayerID());
				String start = "Feature ID:  " + selectedFeatures.get(i).getGeoServerID() + " (intern "
						+ selectedFeatures.get(i).getFeatureID() + ")" + "\nLayer: " + featureLayer.getName() + ", Workspace: "
						+ featureLayer.getWorkspace();
				final String synced;
				if (selectedFeatures.get(i).isSync()) {
					synced = context.getString(R.string.is_synced_with_server);
				} else {
					synced = context.getString(R.string.is_not_synced_with_server);
				}
				final String isDone;
				if (selectedFeatures.get(i).isDone()) {
					isDone = context.getString(R.string.feature_is_done);
				} else {
					isDone = context.getString(R.string.feature_is_not_done);
				}
				featureText.setPadding(1, 1, 1, 1);
				featureText.setText(start);
				// final variablen fuer die folgenden listener
				final WFSLayer selectedLayer = featureLayer; // x
				final Feature selectedFeature = selectedFeatures.get(i); // y
				final Builder editDialog = new AlertDialog.Builder(getContext());
				// final AlertDialog dlg = editDialog.show();
				// bei gedruecktem info-button
				featureInfo.setOnClickListener(new View.OnClickListener() {
					// Perform action on click
					public void onClick(View v) {
						String geom = StringUtils.substringBefore(selectedFeature.getGeom().toString(), "(");
						String coordsbuffer = selectedFeature.getWKTGeometry().toString();
						// Log.e(CLASSTAG + " longtouchaction", "Coordsbufffer: " + selectedFeature.getWKTGeometry().toString());
						int beginIndex = coordsbuffer.indexOf("(");
						int endIndex = coordsbuffer.lastIndexOf(")");
						String coords;
						// Abfrage fuer loecher in polygonen wichtig, sonst exception
						if (endIndex > beginIndex) {
							coords = coordsbuffer.substring(beginIndex + 1, endIndex);
						} else {
							// TODO uebersetzungen
							coords = "Not Found.";
						}
						// TODO uebersetzungen
						editDialog.setTitle("Eigenschaften von Feature " + selectedFeature.getFeatureID() + " aus Layer \""
								+ selectedLayer.getName() + "\"");
						ContentValues x = selectedFeature.getAttributes();
						// Log.e(CLASSTAG, "CV: " + x.toString());
						Set<String> keySet = x.keySet();
						Iterator<String> iterKeySet = keySet.iterator();
						String all = "";
						String key;
						while (iterKeySet.hasNext()) {
							key = iterKeySet.next();
							all = all + key + ": " + x.get(key).toString() + "\n";
						}
						String attributesList = all;
						editDialog.setMessage("GeoServer-ID: \"" + selectedFeature.getGeoServerID() + "\"" + "\n"
								+ "Feature-ID: " + selectedFeature.getFeatureID() + ", Geometry: " + geom + "\n" + "Layer: \""
								+ selectedLayer.getName() + "\"" + " (Layer-ID = " + selectedLayer.getLayerID() + ")" + "\n"
								+ "Layer-Workspace: \"" + selectedLayer.getWorkspace() + "\"" + "\n" + "\n" + synced + "\n"
								+ isDone + "\n" + "\n" + "List of Attributes: \n" + attributesList + "\n" + "Coordinates: "
								+ "\n" + coords + "\n");
						editDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});
						editDialog.show();
					}
				});
				// bei gedruecktem edit-button
				editFeature.setOnClickListener(new View.OnClickListener() {
					// Perform action on click
					public void onClick(View v) {
						flyTo(selectedFeature.getFeatureID(), selectedLayer.getLayerID(), selectedFeature.getWKTGeometry());
						getEditFeatureToolbar().openBar(selectedFeature.getFeatureID(), selectedLayer.getLayerID());
						alert.dismiss();
					}
				});
				// alle views in das innere layout einfuegen
				innerLayout = new LinearLayout(getContext());
				innerLayout.setVerticalScrollBarEnabled(true);
				innerLayout.addView(featureText);
				featureText.setPadding(1, 1, 1, 1);
				innerLayout.addView(featureInfo);
				innerLayout.addView(editFeature);
				// das innere layout ins aeussere einfuegen
				layout.addView(innerLayout);
			}
			scrollPane.addView(layout);
			// wenn ein editierbares (gelocktes) feature gefunden wurde
			AlertDialog.Builder editDialog = new AlertDialog.Builder(getContext());
			editDialog.setView(scrollPane);
			editDialog.setCancelable(false);
			editDialog.setPositiveButton(R.string.cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
					reloadFeaturesAndDraw();
				}
			});
			alert = editDialog.create();
			alert.show();
			// editDialog.show();
		} else {
			// TODO Uebersetzungen
			Alerts.errorMessage(getContext(), "No features intersecting!", "No features found at your Point touched.").show();
		}
		this.invalidate();
	}


	/**
	 * Sets the display's center to given coordinate.
	 * 
	 * @param coord
	 */
	public void moveTo(Coordinate coord) {
		// Log.i(CLASSTAG + " moveTo", "Moving to " + coord.x + " -- " + coord.y);
		// oberen linken punkt neu setzen
		double x = coord.x - (mapScreen.width / 2.0) / mapScreen.scale;
		double y = coord.y + (mapScreen.height / 2.0) / mapScreen.scale;
		mapScreen.setBBOXMap(new Coordinate(x, y));
		// osmmap auch updaten
		if (this.osmMapView.isActivated()) {
			this.updateMapView();
		}
		// calculate its bounds (internally it loads also features from database into FeatureContainer)
		this.calculateBounds();
		this.invalidate();
	}

	/**
	 * Load features of all active wfs-layers in the displays bbox.
	 */
	public FeatureContainer loadActiveFeaturesInBox(Project project, Coordinate upperLeft, Coordinate lowerRight,
			boolean newContainer) {
		FeatureContainer featureContainerLong = new FeatureContainer();
		if (newContainer) {
			featureContainerLong.clear();
		} else {
			// kein neuer FC wegen den moeglichen zu editierenden features
			featureContainer.clear();
		}
		if (project != null) {
			// create the bounding box
			Envelope currentEnvelope = new Envelope(upperLeft.x, lowerRight.x, upperLeft.y, lowerRight.y);
			// list with resulting features
			ArrayList<Feature> resultFeatures;
			// current wfs layer
			WFSLayer layer;
			// for all layers get the features in that envelope
			for (int i = 0; i < project.getWFSContainer().size(); i++) {
				layer = project.getWFSContainer().get(i);
				// wenn aktueller layer aktiv ist
				if (layer.isActive()) {
					// initialize List
					resultFeatures = new ArrayList<Feature>();
					// ergebnis der suche: index jedes features wird in featureIDS gespeichert - sehr ZEITINTENSIV
					ArrayList<Integer> featureIDs = layer.getFeatureIDs(currentEnvelope);
					// Log.i(CLASSTAG + " loadFeatures()", "Indexing Layer: " + currentLayer.getName());
					// TODO: zu optimieren: fuer jeden layer seine bbox abfragen, wenn bboxes
					// sich nicht intersecten kann der layer ganz weggelassen werden, wenn
					// bbox ganz enthalten, dann koenen direkt alle feature geladen werden
					//
					// wenn features zum anzeigen gefunden wurden
					if (featureIDs.size() > 0) {
						// falls alle feature angezeigt werden sollen
						if (featureIDs.size() == layer.getCountFeatures()) {
							resultFeatures = layer.getAllFeature();
						} else {
							// falls nur bestimmte feature angezeigt werden sollen
							resultFeatures = layer.getFeatures(featureIDs);
						}
					}
					// feature in container packen
					if (newContainer) {
						featureContainerLong.addFeatureList(resultFeatures);
					} else {
						featureContainer.addFeatureList(resultFeatures);
					}
				}
			}
		} else {
			Log.i(CLASSTAG + " loadActiveFeaturesInDisplaysBBox", "No Project chosen. No Features load.");
		}
		// Log.i(CLASSTAG + " loadActiveFeaturesInDisplaysBBox", featureContainer.countAllFeatures() +
		// " Features in FeatureContainer.");
		if (newContainer) {
			return featureContainerLong;
		} else {
			return featureContainer;
		}
	}

	/**
	 * Load all active features from the database into the FeatureContainer.
	 */
	public FeatureContainer loadAllActiveFeatures(Project project) {
		// featurecontainer leeren
		featureContainer.clear();
		// wenn kein projekt aktiv
		if (project != null) {
			// wenn wfslayer vorhanden
			if (project.getWFSContainer() != null) {
				// alle wfslayer besorgen
				for (WFSLayer layer : project.getWFSContainer()) {
					// wenn layer aktiv ist, alle features in container laden
					if (layer.isActive()) {
						for (Feature feature : layer.getFeatureContainer()) {
							featureContainer.addFeature(feature);
						}
					}
				}
			}
			Log.i(CLASSTAG + " loadAllFeatures()", "All features of Project \"" + currentProject.getProjectName()
					+ "\" are load now (" + featureContainer.countJustPoints() + "/" + featureContainer.countJustLines() + "/"
					+ featureContainer.countJustPolygons() + ").");
		} else {
			Log.i(CLASSTAG + " loadAllFeatures()", "No Project chosen. No Features load.");
		}
		return featureContainer;
	}

	/**
	 * Calculates bounds of loaded dataset and returns center coordinates
	 * 
	 * @return the coordinate
	 */
	private void calculateBounds() {
		// Log.e(CLASSTAG + " calculateBounds()", "calculateBounds() starts");
		// load all features in bounding box
		this.featureContainer = loadActiveFeaturesInBox(currentProject, mapScreen.getMapUpperLeft(), mapScreen.getMapLowerRight(), false);
		// Log.e(CLASSTAG + " calculateBounds()", "Now " + featureContainer.countAllFeatures() + " Features in Container.");
		// wenn keine Features anzuzeigen sind setze standardbounds
		// TODO: ueberpruefen, ob der richtige weg
		if (featureContainer.isEmpty()) {
			Coordinate upperLeftFeature = new Coordinate(BOUNDUPPERLEFTX, BOUNDUPPERLEFTY);
			Coordinate lowerRightFeature = new Coordinate(BOUNDLOWERRIGHTX, BOUNDLOWERRIGHTY);
			mapScreen.setBBOXFeature(upperLeftFeature, lowerRightFeature);
			// Log.e(CLASSTAG, "calculateBounds() ends if");
		} else {
			// bounds zuruecksetzen
			mapScreen.setBBOXFeature(new Coordinate(), new Coordinate());
			// bounds aus addtobounds fuer jedes feature ermitteln
			for (Feature feature : featureContainer.getAllFeatures()) {
				this.addToBounds(feature.getGeom());
			}
			// if bounds are still null -> standardansicht mit mittelpunkt aus Konstanten
			if (mapScreen.getFeatureUpperLeft().x == 0 && mapScreen.getFeatureLowerRight().x == 0) {
				Coordinate upperLeftFeature = new Coordinate(BOUNDUPPERLEFTX, BOUNDUPPERLEFTY);
				Coordinate lowerRightFeature = new Coordinate(BOUNDLOWERRIGHTX, BOUNDLOWERRIGHTY);
			//	bboxFeatures = new BBOX(upperLeftFeature, lowerRightFeature);
				mapScreen.setBBOXFeature(upperLeftFeature, lowerRightFeature);
			}
			// Log.e(CLASSTAG, "calculateBounds() ends else");
		}
	}

	/**
	 * Adds geometries to bounds. Checks if given geometry extends current bounds and adjusts them, recursiv for multigeometries
	 * 
	 * @param geometry
	 *            the geometry
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
	 * @param coord
	 *            the coord of point to add
	 * @return true, if bounds were extended
	 */
	private boolean addPointToBounds(Coordinate coord) {
		boolean extended = false;
		Coordinate upperLeft = mapScreen.getFeatureUpperLeft();
		Coordinate lowerRight = mapScreen.getFeatureLowerRight();
		// if bounds are not set
		if (upperLeft.x == 0 && lowerRight.x == 0) {
			upperLeft = new Coordinate(coord.x, coord.y);
			lowerRight = new Coordinate(coord.x, coord.y);
			// bboxFeatures = new BBOX(upperLeftFeature, lowerRightFeature);

			// if bounds are the same
		} else if (upperLeft.equals2D(lowerRight)) {
			if (coord.x > upperLeft.x) {
				lowerRight.x = coord.x;
			} else {
				upperLeft.x = coord.x;
			}
			if (coord.y > lowerRight.y) {
				upperLeft.y = coord.y;
			} else {
				lowerRight.y = coord.y;
			}
		} else {
			if (coord.x < upperLeft.x) {
				upperLeft.x = coord.x;
				extended = true;
			}
			if (coord.x > lowerRight.x) {
				lowerRight.x = coord.x;
				extended = true;
			}
			if (coord.y < lowerRight.y) {
				lowerRight.y = coord.y;
				extended = true;
			}
			if (coord.y > upperLeft.y) {
				upperLeft.y = coord.y;
				extended = true;
			}
			extended = true;
		}
	//	bboxFeatures = new BBOX(upperLeft, lowerRight);
		mapScreen.setBBOXFeature(upperLeft, lowerRight);
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


	/**
	 * Sets the map projection; use epsg codes, for example "epsg:3785" (Google Mercator)
	 * 
	 * @param epsg
	 *            the new projection
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
	 * Transforms coordinate to the reference system of the map DOES NOTHING?!?!?!
	 * 
	 * @param src
	 *            the src
	 * @return the coordinate
	 */
	public Coordinate transformToMapCRS(Coordinate src) {
		// Falls kein Projekt geladen wurde...
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
	 * Transforms coordinate from the reference system of the map. DOES NOTHING?!?!?!
	 * 
	 * @param src
	 *            the src
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

	// Returns the coordinate next to the touched position within snapping range
	private int[] getNearestCreatedPointIndex(Point[][] pts) {
		int[] idx = { -3, -3 };
		boolean first = true;
		float dist = 0f;
		float minDist = 0f;
		// check if first point is touched
		if (addFeatureBar.getFirst() != null) {
			dist = mapScreen.touchedDistance(addFeatureBar.getFirst());
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
			dist = mapScreen.touchedDistance(addFeatureBar.getSecond());
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
						dist = mapScreen.touchedDistance(pts[i][j].getCoordinate());
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
			dist = mapScreen.touchedDistance(editFeatureBar.getFirst());
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
			dist = mapScreen.touchedDistance(editFeatureBar.getSecond());
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
						dist = mapScreen.touchedDistance(pts[i][j].getCoordinate());
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
	 * Returns distance between two fingers/pointers on the panel used to calculate scale factor during zoom
	 * 
	 * @param ev
	 *            the ev
	 * @return the float
	 */
	private float gestureSpacing(MotionEvent ev) {
		float dx = ev.getX(0) - ev.getX(1);
		float dy = ev.getY(0) - ev.getY(1);
		return FloatMath.sqrt(dx * dx + dy * dy);
	}

	/**
	 * Returns center of two pointers/fingers on the panel used for focal point during zoom
	 * 
	 * @param ev
	 *            the ev
	 * @return the coordinate
	 */
	private Coordinate gestureCenter(MotionEvent ev) {
		float mx = (ev.getX(0) + ev.getX(1)) / 2;
		float my = (ev.getY(0) + ev.getY(1)) / 2;
		return mapScreen.pixelToCoordinate(mx, my);
	}

	/**
	 * Updates the MapView's zoom level. Attention: leads to Dead Ends when called on wrong locations
	 */
	private void updateMapViewZoom() {
		// Log.e(CLASSTAG, "updateMapViewZoom()");
		if (osmMapView.getVisibility() == View.VISIBLE) {
			Log.i(CLASSTAG + " updateMapViewZoom()", "Screen size: " + mapScreen.width + "x" + mapScreen.height);
			// transform corner coordinates
			Coordinate ul_deg = transformFromMapCRS(mapScreen.getMapUpperLeft());
			Coordinate lr_deg = transformFromMapCRS(mapScreen.getMapLowerRight());
			Log.i(CLASSTAG + " updateMapViewZoom()", "Transformed coordinates: UpperLeft; " + ul_deg.x + " " + ul_deg.y
					+ " -- LowerRight: " + lr_deg.x + " " + lr_deg.y);
			// calculate spans
			int diff_lon = (int) ((lr_deg.x - ul_deg.x) * 1E6);
			int diff_lat = (int) ((ul_deg.y - lr_deg.y) * 1E6);
			// zoom to span
			osmMapView.getController().zoomToSpan(diff_lat, diff_lon);
			// get corner coordinates of map projection
			MapView.Projection proj = osmMapView.getProjection();
			IGeoPoint ul_proj = proj.fromPixels(0f, 0f);
			IGeoPoint lr_proj = proj.fromPixels((float) mapScreen.width, (float) mapScreen.height);
			Log.i(CLASSTAG + " updateMapViewZoom()",
					"projected corner coordinates:\n" + "UpperLeft: " + ul_proj.getLongitudeE6() + " " + ul_proj.getLatitudeE6()
							+ " -- LowerRight " + lr_proj.getLongitudeE6() + " " + lr_proj.getLatitudeE6());
			// calculate spans of projected corner coordinates
			int diff_lon_proj = lr_proj.getLongitudeE6() - ul_proj.getLongitudeE6();
			int diff_lat_proj = ul_proj.getLatitudeE6() - lr_proj.getLatitudeE6();
			// calculate scales
			float scale_lon = (float) diff_lon_proj / (float) diff_lon;
			float scale_lat = (float) diff_lat_proj / (float) diff_lat;
			Log.i(CLASSTAG + " updateMapViewZoom()", "Scales: lon " + scale_lon + " -- lat " + scale_lat);
			Log.e(CLASSTAG, "Scale: " + mapScreen.scale);
			// adjust scale
			osmMapView.setScaleX(scale_lon);
			osmMapView.setScaleY(scale_lat);
		}
		this.writeSharedPreferences();
	}

	/**
	 * Updates the MapView's center
	 */
	private void updateMapViewCenter() {
		// Log.e(CLASSTAG, "updateMapViewCenter");
		if (osmMapView.isActivated()) {
			// calculate center
			Coordinate center = transformFromMapCRS(mapScreen.getMapCenter());
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
	 * @param center
	 *            center coordinate, if null no center change
	 */
	private void updateMapView(boolean visible, Coordinate center) {
		// Log.e(CLASSTAG, "updateMapView(...)");
		// if the osm mapview is
		if (visible) {
			Log.i(CLASSTAG + " updateMapView", "Screen size for background initialization: " + mapScreen.width + " x "
					+ mapScreen.height);
			// Calculate zoom level
			double GLOBE_WIDTH = 256.0;
			Coordinate ul_deg = transformFromMapCRS(mapScreen.getMapUpperLeft());
			Coordinate lr_deg = transformFromMapCRS(mapScreen.getMapLowerRight());
			double angle = lr_deg.x - ul_deg.x;
			if (angle < 0) {
				angle += 360.0;
			}
			int zoomLevel = (int) Math.round(Math.log((double) mapScreen.width * 360.0 / angle / GLOBE_WIDTH) / Math.log(2.0));
			Log.i(CLASSTAG + " updateMapView", "New zoom level: " + zoomLevel);
			// Log.e(CLASSTAG + " updateMapView", "Calculated zoom level: (int) Math.round(Math.log((double)" + width +
			// " * 360.0	/ " + angle + "/ 256) / Math.log(2.0));");
			osmMapView.getController().setZoom(zoomLevel);
			// Set center
			if (center != null) {
				Log.i(CLASSTAG + " updateMapView", "Called with coordinates: CENTER " + center.x + " " + center.y);
				Coordinate centerWGS84 = this.transformFromMapCRS(center);
				osmMapView.getController().setCenter(new GeoPoint((int) (centerWGS84.y * 1E6), (int) (centerWGS84.x * 1E6)));
			} else {
				Log.i(CLASSTAG + " updateMapView", "Called with coordinates: NULL");
			}
			Log.i(CLASSTAG + " updateMapView", "Map center: " + osmMapView.getMapCenter().getLongitudeE6() + "   "
					+ osmMapView.getMapCenter().getLatitudeE6());
			Log.i(CLASSTAG + " updateMapView", "Our center: " + mapScreen.getMapCenter());
			// Adjust zoom scale to corner coordinates
			// calculate spans
			int diff_lon = (int) ((lr_deg.x - ul_deg.x) * 1E6);
			int diff_lat = (int) ((ul_deg.y - lr_deg.y) * 1E6);
			// get corner coordinates of map projection
			MapView.Projection proj = osmMapView.getProjection();
			IGeoPoint ul_proj = proj.fromPixels(0f, 0f);
			IGeoPoint lr_proj = proj.fromPixels((float) mapScreen.width, (float) (mapScreen.height));
			Log.i(CLASSTAG + " updateMapView", "Projected corner coordinates:\n" + "UpperLeft " + ul_proj.getLongitudeE6() + " "
					+ ul_proj.getLatitudeE6() + " -- LowerRight " + lr_proj.getLongitudeE6() + " " + lr_proj.getLatitudeE6());
			// calculate spans of projected corner coordinates
			int diff_lon_proj = lr_proj.getLongitudeE6() - ul_proj.getLongitudeE6();
			int diff_lat_proj = ul_proj.getLatitudeE6() - lr_proj.getLatitudeE6();
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

	// Draws point from GPS-signal to canvas
	public void drawGPSCoordinate(Coordinate coord) {
		this.setGPSTracking(true);
		if (gpsCoords.size() > GPS_COORD_CONTAINER_SIZE) {
			gpsCoords.remove(0);
		}
		gpsCoords.add(coord);
	}

	// Sets the WMSView displaying a map in background
	private void setBackgroundMap(MapView osm) {
		this.osmMapView = osm;
		this.setMapActive(false);
		this.osmMapView.setMultiTouchControls(true);
		// old tile database
		// this.osmMapView.setTileSource(TileSourceFactory.MAPNIK);
		// new tile database
		this.osmMapView.setTileSource(TileSourceFactory.MAPQUESTOSM);
	}

	// Turns background maps on or off
	public void setMapActive(boolean osm) {
		if (this.osmMapView != null) {
			this.osmMapView.setActivated(osm);
			if (osm) {
				this.osmMapView.setVisibility(View.VISIBLE);
			} else {
				this.osmMapView.setVisibility(View.INVISIBLE);
			}
		}
	}

	// Returns the Add Feature Toolbar
	public AddFeatureBar getAddFeatureToolbar() {
		return this.addFeatureBar;
	}

	// Returns the Edit Feature Toolbar
	public EditFeatureBar getEditFeatureToolbar() {
		return this.editFeatureBar;
	}

	/**
	 * Sets the creation mode.
	 * 
	 * @param boo
	 *            the new creation mode
	 */
	public void setCreationMode(boolean boo) {
		this.creationMode = boo;
		// Log.e(CLASSTAG + " setCreationMode", "Creation Mode is " + isInCreationMode());
	}

	/**
	 * Sets the edits mode.
	 * 
	 * @param boo
	 *            the new edits the mode
	 */
	public void setEditMode(boolean boo) {
		this.isEditing = boo;
		// Log.e(CLASSTAG + " setEditMode", "Edit Mode is " + isInEditingMode());
		// this.updateOverlays();
	}

	/**
	 * Sets the edited feature and the status of edit mode.
	 * 
	 * @param editFeature
	 *            the new edited feature
	 */
	public void setEditedFeature(Feature editFeature) {
		this.currentEditedFeature = editFeature;
		if (currentEditedFeature == null) {
			Log.e(CLASSTAG + " setEditedFeature", "Edited feature NOT set.");
		}
		if (editFeature == null) {
			Log.e(CLASSTAG + " setEditedFeature", "Editable feature NOT set.");
		}
		this.featureContainer.hideEditFeature(this.currentEditedFeature);
		this.setEditMode(true);
	}

	public void saveEditedFeature(Feature newfeature) {
		if (newfeature != null) {
			this.featureContainer.updateEditedFeature(newfeature);
		}
		this.currentEditedFeature = null;
	}

	public void resetEditedFeature() {
		this.currentEditedFeature = null;
		this.featureContainer.resetEditedFeature();
		// this.setEditMode(false);
	}

	// Returns whether creation mode is on or off
	public boolean isInCreationMode() {
		return creationMode;
	}

	// Returns whether creation mode is on or off
	public boolean isInEditingMode() {
		return isEditing;
	}

	// sets the WMS Overlay
	public void setWMSOverlay(WMSOverlay wmsOverlay) {
		this.wmsOverlay = wmsOverlay;
	}

	// gets the zoom level
	public double getZoomLevel() {
		return mapScreen.scale;
	}

	// update the wms overlay
	public void updateWMSOverlay() {
		if (wmsOverlay != null) {
			wmsOverlay.clear();
			// drawingPanel.setMapActive(ProjectHandler.getCurrentProject().isOSM());
			Project setting = ProjectHandler.getCurrentProject();
			setWMSOverlay(wmsOverlay);
			if (setting != null && setting.getWMSContainer() != null && !setting.getWMSContainer().isEmpty()) {
				// TODO: nur so schnell schnell verwaltung, ist bescheuert
				// liste der verschiedenen WMS-SERVER
				ArrayList<String> urlList = new ArrayList<String>();
				// gibt es einen SERVER bei verschiedenen LAYERN, dann ist er doppelt
				boolean doppelterWMS = false;
				for (int i = 0; i < setting.getWMSContainer().size(); i++) {
					doppelterWMS = false;
					// vorhandene liste von wms server-urls durchgehen
					Iterator<String> iterServer = urlList.iterator();
					while (iterServer.hasNext()) {
						if (iterServer.next().equals(setting.getWMSContainer().get(i).getURL())) {
							// wenn doppelt auftretende url
							doppelterWMS = true;
						}
					}
					// nur wenn die url nicht doppelt ist eintragen
					if (!doppelterWMS) {
						urlList.add(setting.getWMSContainer().get(i).getURL());
					}
				}
				// vorhandene liste von wms server-urls durchgehen
				Iterator<String> iterServer = urlList.iterator();
				while (iterServer.hasNext()) {
					String currentURL = iterServer.next();
					// liste der verschiedenen WMS-LAYER auf diesem einen SERVER erstellen
					ArrayList<String> wmsLayerList = new ArrayList<String>();
					for (int i = 0; i < setting.getWMSContainer().size(); i++) {
						if (currentURL.equals(setting.getWMSContainer().get(i).getURL())) {
							wmsLayerList.add(setting.getWMSContainer().get(i).getName());
						}
					}
					// arraylist in string[] ueberfuehren, weil die methode das so braucht...
					String[] newWMSLayerList = new String[wmsLayerList.size()];
					Iterator<String> iterLayer = wmsLayerList.iterator();
					int i = 0;
					while (iterLayer.hasNext()) {
						newWMSLayerList[i] = iterLayer.next();
						i++;
					}
					// für jeden Server den Loader erstellen
					wmsOverlay.addLoader(WMSUtils.generateGetMapBaseURL(Functions.reviseWmsUrl(currentURL), newWMSLayerList,
							"EPSG:" + setting.getEpsgCode()));
				}
			}
		}
	}

	/**
	 * Fly to coordinate as String.
	 * 
	 * @param flyID
	 *            the features id to fly to
	 * @param layerID
	 *            the layers name of feature stored in
	 * @param coordStr
	 *            the coord where to fly
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
				this.featureContainer = loadAllActiveFeatures(currentProject);
			}
		}
		// wenn keine features aktiv sind bzw. angezeigt werden muessen
		if (featureContainer.isEmpty()) {
			// standardansicht mit mittelpunkt aus Konstanten

			Coordinate upperLeft = new Coordinate(BOUNDUPPERLEFTX, BOUNDUPPERLEFTY);
			Coordinate lowerRight = new Coordinate(BOUNDLOWERRIGHTX, BOUNDLOWERRIGHTY);
		//	bboxFeatures = new BBOX(upperLeft, lowerRight);
			mapScreen.setBBOXFeature(upperLeft, lowerRight);
		} else {
			// bounds zuruecksetzen
			mapScreen.setBBOXFeature(new Coordinate(), new Coordinate());
			// bounds aus addtobounds fuer jedes feature ermitteln
			for (Feature f : featureContainer.getAllFeatures()) {
				this.addToBounds(f.getGeom());
			}
		}
		// wenn kein display
		if (mapScreen.width == 0 && mapScreen.height == 0) {
			return;
		}
		// Check if bounds are null - standardansicht mit mittelpunkt aus Konstanten
		if (mapScreen.getFeatureUpperLeft().x == 0 && mapScreen.getFeatureLowerRight().x == 0) {
			Coordinate upperLeft = new Coordinate(BOUNDUPPERLEFTX, BOUNDUPPERLEFTY);
			Coordinate lowerRight = new Coordinate(BOUNDLOWERRIGHTX, BOUNDLOWERRIGHTY);
			mapScreen.setBBOXFeature(upperLeft, lowerRight);
		}
		// if (bboxFeatures.lowerRight == null) {
		// bboxFeatures.lowerRight = new Coordinate(BOUNDLOWERRIGHTX, BOUNDLOWERRIGHTY);
		// }
		// Set upper left corner coordinates
		mapScreen.setBBOXMap(mapScreen.getFeatureUpperLeft());
		// Calculate scale in x and y axis: höhe und breite der BBox
		double span_x = mapScreen.getFeatureLowerRight().x - mapScreen.getFeatureUpperLeft().x;
		// ursrpruenglich x statt y und ohne -1:
		double span_y = (mapScreen.getFeatureLowerRight().y - mapScreen.getFeatureUpperLeft().y) * (-1);
		// Wenn Span Werte = 0, z.b. wenn nur ein einziger punkt gezeichnet wurde
		if (span_x == 0.0) {
			span_x = STANDARDMINIMUMSPAN;
		}
		if (span_y == 0.0) {
			span_y = STANDARDMINIMUMSPAN;
		}
		// skalierung bzw. zoom
		double scale_x = (double) mapScreen.width / (span_x);
		double scale_y = (double) mapScreen.height / (span_y);
		// Choose scale to fit whole dataset into view - kleinere scale waehlen, groesserer zoom
		if (scale_x < scale_y) {
			mapScreen.scale = scale_x;
		} else {
			mapScreen.scale = scale_y;
		}
		// minimale und maximale zoomstufen setzen
		if (mapScreen.scale > mapScreen.MAXIMUM_ZOOM_SCALE) {
			mapScreen.scale = mapScreen.MAXIMUM_ZOOM_SCALE;
		} else {
			if (mapScreen.scale < mapScreen.MINIMUM_ZOOM_SCALE) {
				mapScreen.scale = mapScreen.MINIMUM_ZOOM_SCALE;
			}
		}
		// osmmap updaten fuer neue eigenschaften
		if (this.osmMapView.isActivated()) {
			updateMapView();
		}
		// neu zeichnen
		this.invalidate();
		// neue ansichtsoptionen merken fuer evtl. absturz, restart etc.
		this.writeSharedPreferences();
	}

	/**
	 * Write shared preferences. Ansichtsoptionen merken fuer evtl. absturz, restart etc.
	 */
	public void writeSharedPreferences() {
		Coordinate upperLeftFeature = mapScreen.getFeatureUpperLeft();
		Coordinate lowerRightFeature = mapScreen.getFeatureLowerRight();
		Coordinate upperLeftMap = mapScreen.getMapUpperLeft();

		this.prefsEditor.putInt("de.geotech.systems.drawingPanelWidth", getWidth());
		this.prefsEditor.putInt("de.geotech.systems.drawingPanelHeight", getHeight());
		this.prefsEditor.putLong("de.geotech.systems.upperLeftx", Double.doubleToLongBits(upperLeftMap.x));
		this.prefsEditor.putLong("de.geotech.systems.upperLefty", Double.doubleToLongBits(upperLeftMap.y));
		this.prefsEditor.putLong("de.geotech.systems.bound_upperLeftx", Double.doubleToLongBits(upperLeftFeature.x));
		this.prefsEditor.putLong("de.geotech.systems.bound_upperLefty", Double.doubleToLongBits(upperLeftFeature.y));
		this.prefsEditor.putLong("de.geotech.systems.bound_lowerRightx", Double.doubleToLongBits(lowerRightFeature.x));
		this.prefsEditor.putLong("de.geotech.systems.bound_lowerRighty", Double.doubleToLongBits(lowerRightFeature.y));
		this.prefsEditor.putLong("de.geotech.systems.scaleFocusx", Double.doubleToLongBits(scaleFocus.x));
		this.prefsEditor.putLong("de.geotech.systems.scaleFocusy", Double.doubleToLongBits(scaleFocus.y));
		this.prefsEditor.putLong("de.geotech.systems.scale", Double.doubleToLongBits(mapScreen.scale));
		this.prefsEditor.putLong("de.geotech.systems.oldScale", Double.doubleToLongBits(mapScreen.oldScale));
		if (ProjectHandler.isCurrentProjectSet()) {
			this.prefsEditor.putBoolean("de.geotech.systems.marker", ProjectHandler.getCurrentProject().isShowMarker());
			this.prefsEditor.putBoolean("de.geotech.systems.osm", ProjectHandler.getCurrentProject().isOSM());
			this.prefsEditor.putInt("de.geotech.systems.autoSync", ProjectHandler.getCurrentProject().getAutoSync());
			this.prefsEditor.putBoolean("de.geotech.systems.isunsyncascyan", ProjectHandler.getCurrentProject().isUnsyncAsCyan());
		}
		this.prefsEditor.commit();
		// Log.i(CLASSTAG + " writeSharedPreferences()", "Writing shared Preferences! - Scale: " + scale);
		// Log.i(CLASSTAG + " writeSharedPreferences()", "upperLeft: " + upperLeft.toString());
		// Log.i(CLASSTAG + " writeSharedPreferences()", "bound_upperLeft: " + bound_upperLeft.toString());
		// Log.i(CLASSTAG + " writeSharedPreferences()", "bound_lowerRight: " + bound_lowerRight.toString());
		// Log.i(CLASSTAG + " writeSharedPreferences()", "scaleFocus: " + scaleFocus);
	}

	/**
	 * Load shared preferences. Letzte gespeicherte Ansichtsoptionen laden.
	 */
	private void loadSharedPreferences() {
		Coordinate upperLeftFeature = new Coordinate();
		Coordinate lowerRightFeature = new Coordinate();
		Coordinate upperLeftMap = new Coordinate();
		upperLeftMap.x = Double.longBitsToDouble(this.prefs.getLong("de.geotech.systems.upperLeftx", 0));
		upperLeftMap.y = Double.longBitsToDouble(this.prefs.getLong("de.geotech.systems.upperLefty", 0));
		upperLeftFeature.x = Double.longBitsToDouble(this.prefs.getLong("de.geotech.systems.bound_upperLeftx", 0));
		upperLeftFeature.y = Double.longBitsToDouble(this.prefs.getLong("de.geotech.systems.bound_upperLefty", 0));
		lowerRightFeature.x = Double.longBitsToDouble(this.prefs.getLong("de.geotech.systems.bound_lowerRightx", 0));
		lowerRightFeature.y = Double.longBitsToDouble(this.prefs.getLong("de.geotech.systems.bound_lowerRighty", 0));
		mapScreen.scale = Double.longBitsToDouble(this.prefs.getLong("de.geotech.systems.scale", 0));
		mapScreen.oldScale = Double.longBitsToDouble(this.prefs.getLong("de.geotech.systems.oldscale", 0));
		this.scaleFocus.x = Double.longBitsToDouble(this.prefs.getLong("de.geotech.systems.scaleFocusx", 0));
		this.scaleFocus.y = Double.longBitsToDouble(this.prefs.getLong("de.geotech.systems.scaleFocusy", 0));
		if (ProjectHandler.isCurrentProjectSet()) {
			ProjectHandler.getCurrentProject().setShowMarker(this.prefs.getBoolean("de.geotech.systems.marker", true));
			ProjectHandler.getCurrentProject().setOSM(this.prefs.getBoolean("de.geotech.systems.osm", true));
			ProjectHandler.getCurrentProject().setAutoSync(this.prefs.getInt("de.geotech.systems.autoSync", 0));
			ProjectHandler.getCurrentProject().setUnsyncAsCyan(this.prefs.getBoolean("de.geotech.systems.isunsyncascyan", true));
		}
		if (upperLeftFeature.x == 0 && lowerRightFeature.x == 0) {
			upperLeftFeature = new Coordinate(BOUNDUPPERLEFTX, BOUNDUPPERLEFTY);
			lowerRightFeature = new Coordinate(BOUNDLOWERRIGHTX, BOUNDLOWERRIGHTY);
			upperLeftMap = new Coordinate(UPPERLEFTX, UPPERLEFTY);

			mapScreen.scale = SCALE;
			this.scaleFocus = new Coordinate(11.812339500171078, 51.081266939383156);
		}
		mapScreen.setBBOXFeature(upperLeftFeature, lowerRightFeature);
		mapScreen.setBBOXMap(upperLeftMap);
	}

	/**
	 * Erstellt neue Oberfläche mithilfe eines Threads. This is called immediately after the surface is first created.
	 */
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// alle features im ansichtsbereich laden
		this.featureContainer = loadActiveFeaturesInBox(currentProject, mapScreen.getFeatureUpperLeft(), mapScreen.getMapLowerRight(), false);
		
		
		
		// wenn der Thread noch nicht gegeben ist, inittialisieren
		if (drawingThread == null) {
			// lade thread fuer das projekt oder ohne
			if (ProjectHandler.isCurrentProjectSet()) {
				drawingThread = new DrawingThread(getHolder(), this, featureContainer);
			} else {
				drawingThread = new DrawingThread(getHolder(), this, featureContainer);
			}
		}
	}

	/**
	 * This is called immediately before a surface is being destroyed.
	 */
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// nothing
	}

	/**
	 * This is called immediately after any structural changes (format or size) have been made to the surface.
	 */
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		// nothing
	}

	/**
	 * Loescht alle Feature auf dem Surface des Threads. Nötig, damit Features nicht überhalb der Menüs liegen
	 */
	public void cleanSurface() {
		// wenn ein Thread läuft
		if (futureThread != null) {
			// Thread beenden
			futureThread.cancel(true);
		}
		// thread nichts malen lassen
		drawingThread.drawFeatures(false);
		// thread aufrufen, um nichts zu malen
		service.submit(drawingThread);
	}

	/**
	 * Reloads the FeatureContainer and draws the View.
	 */
	public void reloadFeaturesAndDraw() {
		this.loadActiveFeaturesInBox(currentProject, mapScreen.getFeatureUpperLeft(), mapScreen.getMapLowerRight(), false);
		
		this.invalidate();
	}

	/**
	 * Sets the GPS tracking.
	 * 
	 * @param newGPSStatus
	 *            the new GPS tracking
	 */
	public void setGPSTracking(boolean newGPSStatus) {
		if (!newGPSStatus) {
			gpsCoords.clear();
		}
		this.gpsTracking = newGPSStatus;
	}

	/**
	 * Checks if is GPS tracking.
	 * 
	 * @return true, if is GPS tracking
	 */
	public boolean isGPSTracking() {
		return gpsTracking;
	}
	
	public MapScreen getMapScreen(){
		return this.mapScreen;
	}

	
	public KrigingToolbar getKrigingToolbar() {
		return this.krigingToolbar;
	}
	
	public boolean isInKrigingMode() {
		return krigingMode;
	}
	
	public void setKrigingMode(boolean boo) {
		this.krigingMode = boo;
	}

	
	

}