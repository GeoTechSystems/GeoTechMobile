/**
 * Toolbar to create new features
 * 
 * @author svenweisker
 * @author Torsten Hoch
 * @author bschm
 * @author tubatubsen
 * 
 */

package de.geotech.systems.editor;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.GpsStatus;
import android.location.Location;
import android.net.Uri;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.jhlabs.map.proj.Projection;
import com.jhlabs.map.proj.ProjectionFactory;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.WKTWriter;

import de.geotech.systems.R;
import de.geotech.systems.database.DBAdapter;
import de.geotech.systems.drawing.DrawingPanelView;
import de.geotech.systems.features.FeaturePrecision;
import de.geotech.systems.main.LeftMenuIconLayout;
import de.geotech.systems.main.MainActivity;
import de.geotech.systems.media.MediaCapture;
import de.geotech.systems.media.StorageHelper;
import de.geotech.systems.projects.Project;
import de.geotech.systems.projects.ProjectHandler;
import de.geotech.systems.utilities.Alerts;
import de.geotech.systems.utilities.CRS;
import de.geotech.systems.utilities.CRSType;
import de.geotech.systems.utilities.Constants;
import de.geotech.systems.utilities.Functions;
import de.geotech.systems.utilities.LocationFactory;
import de.geotech.systems.utilities.LocationFactory.OnLocationByGPSListener;
import de.geotech.systems.wfs.WFSLayer;
import de.geotech.systems.wfs.WFSLayerSynchronization;
import de.geotech.systems.wfs.WFSLayerSynchronization.OnSyncFinishedListener;

public class AddFeatureBar{
	// Logging tag
	private static final String CLASSTAG = "AddFeatureBar";
	// Creator status
	public static final int STATUS_IDLE = 0;
	public static final int STATUS_ADD = 1;
	public static final int STATUS_ADJUST = 2;
	public static final int STATUS_END = 3;
	// Menu index
	public static final int MENU_EDIT_HOLES = 1001;
	// Application context Project 
	private Context context; 
	// current project
	private Project currentProject;
	//DrawingPanel with existing features
	private DrawingPanelView drawingPanel; 
	// LinearLayout to display the toolbar
	private LinearLayout toolbarLayout; 
	// LinearLayout to display the tools according to current feature type
	private LinearLayout toolbarToolsLayout; 
	// Current creator status
	private int status; 
	// Type of feature to create
	private int type; 
	// Layer to draw on
	private long layerId; 
	// Last chosen layer
	private int lastChosenLayerPosition; 
	// New point
	private Point point; 
	// New line
	private LineString line; 
	// New polygon
	private Polygon polygon; 
	// wird einem polygon ein loch hinzugefuegt?
	private boolean polygonAddHole;
	// nummer des lochs
	private int selectedHole;
	// coordinates
	private Coordinate firstPoint;
	private Coordinate secondPoint;
	private Coordinate thirdPoint;
	// animation of icons
	private ScaleAnimation iconAnimation;
	// listener if new features ready
	private OnNewFeatureListener listener;
	// current WFSLayer
	private WFSLayer currentLayer = null;
	// dialog
	private AlertDialog.Builder alertBuilder = null;

	// ArrayList, in der alle Location-Instanzen (dh. auch die Genauigkeiten) gespeichert werden
	public static ArrayList<Location> locationList = new ArrayList<Location>();
	public static FeaturePrecision precision = new FeaturePrecision();

	// Nummer des Punktes (einer Linie oder Polygons), der gerade bearbeitet wird. (vgl idx[1])
	public int aktWorkingPointNum = 0;
	// Nummer des Objekts, welches gerade bearbeitet wird. Im Falle von Linien und Punkten ist das immer 0! (vgl. idx[0])
	public int aktWorkingObjNum = 0;

	private int aktNumPoints;	// Nimmt zwischenzeitlich die Anzahl an Punkten eines Bereiches eines Polygones auf...
	private Boolean polyIsFresh = false;	// Kontrollvariable: Wurde das Polygon gerade in diesem Schritt erzeugt?
	private int numInteriorBefore = 0;		// Kontrollvariable: Anzahl der Löcher vor dem einpflegen eines Punktes

	// Flag, ob ein Punkt hervorgehoben werden soll (beim Polygon ist es manchmal sinnvoll eben dies zu tun)
	public Boolean showFatWorkingPoint=true;
	private DBAdapter dbAdapter;

	//	Constructor setting context, DrawingPanel and EditingPanel
	public AddFeatureBar(DrawingPanelView drawingPanel,	LinearLayout toolbarLayout) {
		// set fields
		this.context = drawingPanel.getContext();
		this.dbAdapter = new DBAdapter(context);
		this.drawingPanel = drawingPanel;
		this.toolbarLayout = toolbarLayout;
		this.currentProject =  ProjectHandler.getCurrentProject();
		this.toolbarLayout.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
		this.toolbarLayout.setDividerDrawable(this.context.getResources()
				.getDrawable(R.drawable.divider));
		// set Animation
		this.iconAnimation = new ScaleAnimation(0.7f, 1f, 0.7f, 1f,
				Animation.RELATIVE_TO_SELF, 0.5f, 
				Animation.RELATIVE_TO_SELF, 0.5f);
		this.iconAnimation.setDuration(300);
		this.lastChosenLayerPosition = 0;
		this.reset();
	}

	// Opens the creator tool bar
	public void openBar() {
		// wenn keine Layer aktivsind abbrechen
		if (ProjectHandler.getCurrentProject() == null) {
			return;
		} else if (ProjectHandler.getCurrentProject().getWFSContainer() == null) {
			return;
		} else {
			boolean anyActiveLayer = false;
			for (WFSLayer layer : ProjectHandler.getCurrentProject().getWFSContainer()) {
				if (layer.isActive()) {
					anyActiveLayer = true;
				}
			}
			if (!anyActiveLayer) {
				return;
			}
		}
		// wenn schon im creation mode, dann abbrechen
		if (drawingPanel.isInCreationMode()) {
			return;
		}
		// wenn schon im editing mode, dann abbrechen
		if (drawingPanel.isInEditingMode()) {
			return;
		}
		// if variables not initialized yet
		if (toolbarToolsLayout != null) {
			this.toolbarToolsLayout.removeAllViews();
		}
		if (toolbarLayout != null) {
			this.toolbarLayout.removeAllViews();
		}
		this.currentProject = ProjectHandler.getCurrentProject();
		// creation mode erzwingen
		drawingPanel.setCreationMode(true);
		// layout erstellen
		toolbarToolsLayout = new LinearLayout(context);
		// schliessen-button einfuegen
		toolbarLayout.addView(new LeftMenuIconLayout(context,
				context.getString(R.string.close), Color.DKGRAY,
				R.drawable.ic_check_error, 5, 5, new OnClickListener() {
			@Override
			public void onClick(View v) {
				//v.startAnimation(iconAnimation);
				closeBar();
			}
		}));
		// textview erstellen
		final TextView create_type = new TextView(context);
		create_type.setPadding(5, 5, 5, 5);
		// liste der layer erstellen
		ArrayList<String> wfsNameList = new ArrayList<String>();
		for (WFSLayer layer : currentProject.getWFSContainer()) {
			if (layer.isActive()) {
				wfsNameList.add(layer.getName());
			}
		}
		// spinner mit layout erstellen und mit layern fuellen
		LinearLayout spinnerWrapper = new LinearLayout(context);
		spinnerWrapper.setPadding(5, 5, 5, 5);
		final Spinner create_layer = new Spinner(context);
		ArrayAdapter<String> layer_adapter = new ArrayAdapter<String>(context,
				android.R.layout.simple_spinner_item, wfsNameList);
		create_layer.setAdapter(layer_adapter);
		// set selected element of the spinner and with false
		// reinitialize last position, if it exceeds list
		if (wfsNameList.size() - 1 < lastChosenLayerPosition) {
			lastChosenLayerPosition = 0;
		}
		// do NOT run through onItemSelected
		create_layer.setSelection(lastChosenLayerPosition, false);
		// string for the selected layer
		String selectedLayer = (String) create_layer.getItemAtPosition(lastChosenLayerPosition);
		// search for name of layer in layercontainer
		for (WFSLayer layer : currentProject.getWFSContainer()) {
			if (layer.getName().equals(selectedLayer)) {
				// set current layer of this class
				currentLayer = layer;
			}
		}
		// set as current layer in project
		ProjectHandler.getCurrentProject().setCurrentWFSLayer(currentLayer);
		// set this bars attributes needed
		type = currentLayer.geometryTypeToInt(currentLayer.getType());
		layerId = currentLayer.getLayerID();
		// set the spinners text
		create_type.setText(currentLayer.getType());
		// create toolbar menu with drawing buttons
		createMenu(type);
		// listener for spinner
		create_layer.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> adapter, View v, int pos,
					long arg3) {
				// set last chosen position
				lastChosenLayerPosition = pos;
				// reset this bar
				reset();
				// find the layer in container
				String selectedLayer = (String) create_layer.getItemAtPosition(pos);
				for (WFSLayer layer : currentProject.getWFSContainer()) {
					if (layer.getName().equals(selectedLayer)) {
						// set chosen layer as current 
						currentLayer = layer;
					}
				}
				// set as current layer in project
				ProjectHandler.getCurrentProject().setCurrentWFSLayer(currentLayer);
				// set this bars attributes needed
				type = currentLayer.geometryTypeToInt(currentLayer.getType());
				layerId = currentLayer.getLayerID();
				// set the spinners text
				create_type.setText(currentLayer.getType());
				// create toolbar menu with drawing buttons
				createMenu(type);
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
		spinnerWrapper.addView(create_layer);
		toolbarLayout.addView(spinnerWrapper);
		toolbarLayout.addView(create_type);
		toolbarLayout.addView(toolbarToolsLayout);
		// GPS button erstellen
		// TODO uebersetzungen
		toolbarLayout.addView(new LeftMenuIconLayout(context, "GPS-Position",
				Color.DKGRAY, R.drawable.ic_editor_gps, 5, 5,
				new OnClickListener() {
			@Override
			public void onClick(View v) {
				v.startAnimation(iconAnimation);
				getCoordByGPSFunction();
			}
		}));
		toolbarLayout.addView(new LeftMenuIconLayout(context,
				context.getString(R.string.input), Color.DKGRAY,
				R.drawable.ic_editor_input, 5, 5, new OnClickListener() {
			@Override
			public void onClick(View v) {
				v.startAnimation(iconAnimation);
				// Create dialog
				final Dialog dialog = new Dialog(context);
				dialog.setTitle(context.getString(R.string.featureEditor_coordinate_input));
				dialog.setContentView(R.layout.coordinate_input_dialog);
				// Define dialog elements
				final Spinner proj = (Spinner) dialog
						.findViewById(R.id.spinner_coordinput_crs);
				final TextView textX = (TextView) dialog
						.findViewById(R.id.text_coordinput_x);
				final TextView textY = (TextView) dialog
						.findViewById(R.id.text_coordinput_y);
				final EditText valueX = (EditText) dialog
						.findViewById(R.id.edit_coordinput_x);
				final EditText valueY = (EditText) dialog
						.findViewById(R.id.edit_coordinput_y);
				Button cancel = (Button) dialog.findViewById(R.id.button_coordinput_cancel);
				Button set = (Button) dialog.findViewById(R.id.button_coordinput_set);
				// Set spinner adapter
				ArrayAdapter<CRS> adapter = new ArrayAdapter<CRS>(context, 
						android.R.layout.simple_spinner_item, Constants.getCRSList());
				proj.setAdapter(adapter);
				proj.setOnItemSelectedListener(new OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> av, View v, int item, long l) {
						CRS selected = (CRS) proj.getItemAtPosition(item);
						if (CRSType.getType(selected) == CRSType.Unit.DEGREE) {
							textX.setText(context.getString(R.string.lon) + ": ");
							textY.setText(context.getString(R.string.lat) + ": ");
						} else if (CRSType.getType(selected)==CRSType.Unit.METRIC) {
							textX.setText(context.getString(R.string.easting) + ": ");
							textY.setText(context.getString(R.string.northing) + ": ");
						} else {
							textX.setText("X: ");
							textY.setText("Y: ");
						}
					}
					@Override
					public void onNothingSelected(AdapterView<?> arg0) {
						textX.setText("X: ");
						textY.setText("Y: ");
					}
				});
				// Set onClickListener
				cancel.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						// Close dialog
						dialog.cancel();
					}
				});
				set.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						// Coordinate variables
						Coordinate lonlat = new Coordinate(0.0, 0.0);
						Coordinate result = new Coordinate(0.0, 0.0);
						// Set input projection
						CRS crs = (CRS) proj.getSelectedItem();
						Projection inputProj;
						if (CRSType.getType(crs) == CRSType.Unit.DEGREE) {
							inputProj = null;
							Log.v(CLASSTAG + "CoordinateInput", "Setting input projection to epsg: null?!");
						} else {
							inputProj = ProjectionFactory.getNamedPROJ4CoordinateSystem(
									"epsg:"	+ crs.getCode());
							Log.v("CLASSTAG + CoordinateInput", "Setting input projection to epsg: " + crs.getCode());
						}
						// Get entered coordinate
						Coordinate entered = new Coordinate(Double.parseDouble(
								valueX.getEditableText().toString()), Double.parseDouble(
										valueY.getEditableText().toString()));
						// Transform entered coordinate
						if (inputProj == null) {
							lonlat.x = entered.x;
							lonlat.y = entered.y;
						} else {
							inputProj.inverseTransform(entered, lonlat);
							Log.v(CLASSTAG,	inputProj.getPROJ4Description()	+ " (epsg: " + inputProj.getEPSGCode() + ") ->" + "X : " 
									+ lonlat.x + "   Y : " + lonlat.y);
						}
						if (drawingPanel.getProjection().getEPSGCode() == 4326
								|| drawingPanel.getProjection().getEPSGCode() == 0) {
							result.x = lonlat.x;
							result.y = lonlat.y;
						} else {
							drawingPanel.getProjection().transform(lonlat, result);
							Log.v(CLASSTAG, drawingPanel.getProjection().getPROJ4Description() + " (epsg: "	+ drawingPanel
									.getProjection().getEPSGCode() + ") ->" + "X: " + result.x + "   Y: " + result.y);
						}
						// Add transformed coordinate
						createFeature(result);
						drawingPanel.moveTo(result);
						// Close dialog
						dialog.cancel();
					}
				});
				dialog.show();
			}
		}));
//		// Fotobutton in der FeatureBar
//		toolbarLayout.addView(new LeftMenuIconLayout(context, "Foto",
//				Color.DKGRAY, R.drawable.ic_editor_takepicture, 10, 5,
//				new OnClickListener() {
//			private Uri captureImageUri;
//			@Override
//			public void onClick(View v) {
//				v.startAnimation(iconAnimation);
//				captureImageUri = MediaCapture.takePicture((Activity)context);
//			}
//		}));
		// set the bar visible
		
		
		toolbarLayout.setVisibility(View.VISIBLE);
		// Setze die Nummer des zu bearbeitenden Punktes und Objekts (für Polygone) zurück
		aktWorkingPointNum = 0;
		aktWorkingObjNum = 0;
		this.drawingPanel.reloadFeaturesAndDraw();
	}
	

	// Closes the tool bar
	public void closeBar() {
		this.toolbarLayout.setVisibility(View.GONE);
		this.drawingPanel.setCreationMode(false);
		// if variables not initialized yet
		if (toolbarToolsLayout != null) {
			this.toolbarToolsLayout.removeAllViews();
		}
		if (toolbarLayout != null) {
			this.toolbarLayout.removeAllViews();
		}
		this.reset();
		this.drawingPanel.reloadFeaturesAndDraw();
	}

	// Reloads FeatureCreator after saving a new 
	public void reloadCreator() {
		this.closeBar();
		this.openBar();
	}

	// Resets Toolbar
	public void reset() {
		this.status = STATUS_IDLE;
		this.type = -1;
		this.point = null;
		this.line = null;
		this.polygon = null;
		this.polygonAddHole = false;
		this.selectedHole = 0;
		this.firstPoint = null;
		this.secondPoint = null;
		this.thirdPoint = null;
	}

	// Saves created feature in internal database on given layer
	public void saveFeature() {
		if (status == STATUS_END || status == STATUS_ADJUST) {
			Log.d(CLASSTAG + " status", "STATUS_END -> SAVE POINT");
			String geom = null;
			WKTWriter writer = new WKTWriter();
			switch (type) {
			case WFSLayer.LAYER_TYPE_POINT:
				geom = writer.write(point);
				break;
			case WFSLayer.LAYER_TYPE_LINE:
				geom = writer.write(line);
				break;
			case WFSLayer.LAYER_TYPE_POLYGON:
				geom = writer.write(polygon);
				break;
			default:
				return;
			}
			if (geom != null) {
				listener.onNewFeature(geom, type, layerId);
			} else {
				toolbarToolsLayout.removeAllViews();
				TextView error = new TextView(context);
				error.setText(context.getString(R.string.error
						+ R.string.featureEditor_error_feature_cannot_save));
				toolbarToolsLayout.addView(error);
			}
			status = STATUS_ADD;
			Log.d(CLASSTAG + " status", "STATUS_ADD");
		} else {
			Log.d(CLASSTAG + " status", "!=STATUS_END -> abort saving");
			AlertDialog.Builder errorSaveDialog = new AlertDialog.Builder(context);
			errorSaveDialog.setTitle(context.getResources().getString(R.string.nothingToSave));
			errorSaveDialog.setMessage(context.getResources().getString(R.string.errorSaveAlert));
			errorSaveDialog.setNeutralButton("OK", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface doubleSaveDialog, int which) {
					doubleSaveDialog.cancel();
				}
			});
			errorSaveDialog.show();
			this.drawingPanel.reloadFeaturesAndDraw();
		}
	}

	// Creates the tool bar menu
	private void createMenu(int menuId) {
		toolbarToolsLayout.removeAllViews();
		toolbarToolsLayout.setGravity(Gravity.CENTER);
		toolbarToolsLayout.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
		toolbarToolsLayout.setDividerPadding(5);
		toolbarToolsLayout.setDividerDrawable(context.getResources().getDrawable(
				R.drawable.divider));
		switch (menuId) {
		case WFSLayer.LAYER_TYPE_POINT:
			// New point 
			status = STATUS_ADD;
			toolbarToolsLayout.addView(new LeftMenuIconLayout(context,
					context.getString(R.string.featureEditor_point_save),
					Color.DKGRAY, R.drawable.ic_check_ok, 20, 5,
					new OnClickListener() {
				@Override
				public void onClick(View v) {
					v.startAnimation(iconAnimation);
					if (point == null){
						setAlertBuilder(WFSLayer.LAYER_TYPE_POINT);
					}
					else {
						saveFeature();
					}
				}
			}));
			break;
		case WFSLayer.LAYER_TYPE_LINE:
			// New line
			status = STATUS_ADD;
			toolbarToolsLayout.addView(new LeftMenuIconLayout(context,
					context.getString(R.string.featureEditor_add_point),
					Color.DKGRAY, R.drawable.ic_editor_new_point, 20, 5,
					new OnClickListener() {
				@Override
				public void onClick(View v) {
					v.startAnimation(iconAnimation);
					status = STATUS_ADD;

					// Aktualisiere aktWorkingPointNum
					if(line!=null) aktWorkingPointNum=line.getNumPoints();
					else
					{
						if(firstPoint==null) aktWorkingPointNum=0;
						else aktWorkingPointNum=1;
					}

					// DrawingPanelView muss "neu gezeichnet werden"
					drawingPanel.invalidate();	
				}
			}));
			toolbarToolsLayout.addView(new LeftMenuIconLayout(context,
					context.getString(R.string.featureEditor_line_save),
					Color.DKGRAY, R.drawable.ic_check_ok, 20, 5,
					new OnClickListener() {
				@Override
				public void onClick(View v) {
					v.startAnimation(iconAnimation);
					status = STATUS_END;
					if (line == null)
						setAlertBuilder(WFSLayer.LAYER_TYPE_LINE);
					else {
						saveFeature();
						status = STATUS_ADD;
					}
				}
			}));
			break;
		case WFSLayer.LAYER_TYPE_POLYGON:
			// New polygon 
			status = STATUS_ADD;
			toolbarToolsLayout.addView(new LeftMenuIconLayout(context,
					context.getString(R.string.featureEditor_add_point),
					Color.DKGRAY, R.drawable.ic_editor_new_point, 20, 5,
					new OnClickListener() {
				@Override
				public void onClick(View v) {
					v.startAnimation(iconAnimation);
					status = STATUS_ADD;

					// Aktualisiere aktWorkingPointNum
					if(polygon!=null)
					{
						aktWorkingPointNum=polygon.getExteriorRing().getNumPoints()-1;
						showFatWorkingPoint=false;
					}
					else
					{
						if(firstPoint==null) aktWorkingPointNum=0;
						else if(secondPoint==null) aktWorkingPointNum=1;
						else aktWorkingPointNum=2;
					}

					// DrawingPanelView muss "neu gezeichnet werden"
					drawingPanel.invalidate();
				}
			}));
			toolbarToolsLayout.addView(new LeftMenuIconLayout(context,
					context.getString(R.string.featureEditor_edit_hole),
					Color.DKGRAY, R.drawable.ic_editor_edit_holes, 20, 5,
					new OnClickListener() {
				@Override
				public void onClick(View v) {
					v.startAnimation(iconAnimation);
					if (polygon != null) {
						createMenu(MENU_EDIT_HOLES);
						showFatWorkingPoint=false;

						// DrawingPanelView muss "neu gezeichnet werden"
						drawingPanel.invalidate();
					}
				}
			}));
			toolbarToolsLayout.addView(new LeftMenuIconLayout(context,
					context.getString(R.string.featureEditor_polygon_save),
					Color.DKGRAY, R.drawable.ic_check_ok, 20, 5,
					new OnClickListener() {
				@Override
				public void onClick(View v) {
					v.startAnimation(iconAnimation);
					status = STATUS_END;
					if (polygon == null)
						setAlertBuilder(WFSLayer.LAYER_TYPE_POLYGON);
					else {
						saveFeature();
						status = STATUS_ADD;
					}
				}
			}));
			break;
			// New polygon hole 
		case MENU_EDIT_HOLES:
			status = STATUS_ADD;
			polygonAddHole = true;
			if (polygon.getNumInteriorRing() > 0) {
				TextView holeFound = new TextView(context);
				holeFound.setText(context
						.getString(R.string.featureEditor_hole) + ": ");
				toolbarToolsLayout.addView(holeFound);
				final Spinner chooseHole = new Spinner(context);
				chooseHole.setAdapter(Functions.getAdaper(context, polygon));
				chooseHole.setSelection(selectedHole - 1);
				chooseHole
				.setOnItemSelectedListener(new OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> arg0,
							View arg1, int arg2, long arg3) {
						selectedHole = (Integer) chooseHole
								.getSelectedItem();
						aktWorkingObjNum=selectedHole;

						// DrawingPanelView muss "neu gezeichnet werden"
						drawingPanel.invalidate();
					}
					@Override
					public void onNothingSelected(AdapterView<?> arg0) {
					}
				});
				toolbarToolsLayout.addView(chooseHole);
			} else {
				TextView noHoles = new TextView(context);
				noHoles.setText(context
						.getString(R.string.featureEditor_error_no_hole));
				toolbarToolsLayout.addView(noHoles);
			}
			toolbarToolsLayout.addView(new LeftMenuIconLayout(context,
					context.getString(R.string.featureEditor_new_hole),
					Color.DKGRAY, R.drawable.ic_editor_add_hole, 20, 5,
					new OnClickListener() {
				@Override
				public void onClick(View v) {
					v.startAnimation(iconAnimation);
					status = STATUS_ADD;
					selectedHole = Functions
							.getAdaper(context, polygon).getCount() + 1;
					aktWorkingObjNum=selectedHole;
					aktWorkingPointNum=0;

					// DrawingPanelView muss "neu gezeichnet werden"
					drawingPanel.invalidate();
				}
			}));
			toolbarToolsLayout.addView(new LeftMenuIconLayout(context,
					context.getString(R.string.featureEditor_add_point),
					Color.DKGRAY, R.drawable.ic_editor_new_point, 20, 5,
					new OnClickListener() {
				@Override
				public void onClick(View v) {
					v.startAnimation(iconAnimation);
					status = STATUS_ADD;

					// Aktualisiere aktWorkingPointNum
					if(aktWorkingObjNum>0)
					{
						if(polygon.getNumInteriorRing()>=aktWorkingObjNum)
						{
							aktWorkingPointNum=polygon.getInteriorRingN(aktWorkingObjNum-1).getNumPoints()-1;
							showFatWorkingPoint=false;
						}
						else
						{
							if(firstPoint==null) aktWorkingPointNum=0;
							else if(secondPoint==null) aktWorkingPointNum=1;
							else aktWorkingPointNum=2;
						}


						// DrawingPanelView muss "neu gezeichnet werden"
						drawingPanel.invalidate();
					}
				}
			}));
			toolbarToolsLayout.addView(new LeftMenuIconLayout(context,
					context.getString(R.string.back), Color.DKGRAY,
					R.drawable.ic_editor_back, 20, 5, new OnClickListener() {
				@Override
				public void onClick(View v) {
					v.startAnimation(iconAnimation);
					createMenu(WFSLayer.LAYER_TYPE_POLYGON);
					polygonAddHole = false;
				}
			}));
			break;
		default:
			TextView error = new TextView(context);
			error.setText(context.getString(R.string.error
					+ R.string.featureEditor_error_unknown_menu_id));
			toolbarToolsLayout.addView(error);
		}
		addSyncIcon();
	}

	// adds a Sync-Button 
	private void addSyncIcon() {
		int id = 0;
		if (currentLayer.isSync()) {
			id = R.drawable.haken_grau;
		} else {
			id = R.drawable.ic_menu_manage_sync_false;
		}
		toolbarToolsLayout.addView(new LeftMenuIconLayout(context,
				context.getString(R.string.sync), Color.DKGRAY, id, 20, 5,
				new OnClickListener() {
			@Override
			public void onClick(View v) {
				v.startAnimation(iconAnimation);
				WFSLayerSynchronization sync = new WFSLayerSynchronization(v.getContext(), true, currentLayer);
				sync.setOnSyncFinishedListener(new OnSyncFinishedListener() {
					@Override
					public void onSyncFinished(boolean result,WFSLayer selectedLayer) {
						if (result) {
							Alerts.errorMessage(context,
									context.getString(R.string.main_sync_result),
									context.getString(R.string.main_sync_message))
									.show();
							reloadCreator();
						} else
							Alerts.errorMessage(context,
									context.getString(R.string.main_sync_result),
									context.getString(R.string.main_sync_error_message))
									.show();
					}
				});
				sync.execute();
			}
		}));
	}

	// featurelistener
	public void setOnNewFeatureListener(
			OnNewFeatureListener onNewFeatureListener) {
		listener = onNewFeatureListener;
	}

	// Interface for the NewFeatureListener
	public interface OnNewFeatureListener {
		void onNewFeature(String geom, int type, long layerId);
	}

	// feature listener, notifies when a feature is ready to be saved
	public void setAddFeatureListener() {
		setOnNewFeatureListener(
				new OnNewFeatureListener() {
					@Override
					public void onNewFeature(String geom, int type, long layerId) {
						Intent editor = new Intent(context,
								FeatureAttributesCreatorActivity.class);
						editor.putExtra("de.geotech.systems.editor.geom", geom);
						editor.putExtra("de.geotech.systems.editor.type", type);
						editor.putExtra("de.geotech.systems.editor.layerId", layerId);

						((Activity) context).startActivityForResult(editor, MainActivity.FEATUREATTRIBUTESCREATORACTIVITY);
					}
				});
	}



	// Updates the project settings.
	public void updateProject(Project newProject) {
		currentProject = newProject;
		closeBar();
	}

	// get coordinaates by gps
	private void getCoordByGPSFunction() {
		Log.d(CLASSTAG + " getCoordByGPSFunction()", "Status: " + String.valueOf(status));
		LocationFactory location = new LocationFactory(context, drawingPanel);
		location.setOnLocationByGPSListener(new OnLocationByGPSListener() {
			@Override
			public void onLocationByGPS(final Location location, final GpsStatus gpsStatus) 
			{
				if (location == null) {
					Alerts.errorMessage(context, context.getString(R.string.error),
							context.getString(R.string.featureEditor_error_no_location_returned))
							.show();
					return;
				}
				AlertDialog.Builder dialog = new AlertDialog.Builder(context);
				dialog.setTitle(context.getString(R.string.featureEditor_use_gps_loc));
				dialog.setMessage(context.getString(R.string.longitude) + ": "
						+ location.getLongitude() + "\n"
						+ context.getString(R.string.latitude) + ": "
						+ location.getLatitude() + "\n"
						+ context.getString(R.string.accuracy) + ": "
						+ location.getAccuracy());
				dialog.setNegativeButton(context.getString(R.string.no),
						new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog,
							int which) {
						dialog.cancel();
					}
				});
				dialog.setPositiveButton(context.getString(R.string.yes),
						new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog,
							int which) {
						Coordinate loc = new Coordinate(location
								.getLongitude(), location.getLatitude());
						Coordinate result = new Coordinate(0.0, 0.0);
						// Transform coordinate
						if (drawingPanel.getProjection().getEPSGCode() == 4326
								|| drawingPanel.getProjection().getEPSGCode() == 0) {
							result.x = loc.x;
							result.y = loc.y;
						} else {
							drawingPanel.getProjection().transform(loc,	result);
						}


						status = STATUS_ADD;

						switch(type)
						{
						case WFSLayer.LAYER_TYPE_POINT:
							// Resete precison und füge neue Daten ein.
							precision.setForPoint(location, gpsStatus);
							createFeature(result);
							status = STATUS_ADJUST;
							break;
						case WFSLayer.LAYER_TYPE_LINE:
							// Füge den neuen Punkt zunächst am Ende der Linie ein.
							createFeature(result);
							// Bearbeitet werden soll der Punkt #aktWorkingPointNum.
							if(line!=null)
							{
								if(aktWorkingPointNum==line.getNumPoints()-1)
								{
									// Es handelte sich wirklich um einen neuen Punkt
									precision.addEntry(location, gpsStatus, 0, aktWorkingPointNum);
								}
								else
								{
									// Es handelt sich um einen "alten Punkt", der bearbeitet werden soll
									line = Functions.replacePointWithLastPointofALineString(line, aktWorkingPointNum);
									precision.deleteEntry(0, aktWorkingPointNum);
									precision.addEntry(location, gpsStatus, 0, aktWorkingPointNum);
								}
							}
							else
							{
								// Es muss sich um den 1. Punkt der Linie handeln!
								precision.setForPoint(location, gpsStatus);
							}
							break;
						case WFSLayer.LAYER_TYPE_POLYGON:
							if(polygon==null)
								polyIsFresh=true;
							else
							{
								polyIsFresh=false;
								numInteriorBefore=polygon.getNumInteriorRing();
							}

							createFeature(result);

							if(polygon!=null)
							{

								Log.d(CLASSTAG, String.valueOf(polygon.getNumInteriorRing())+" InterriorRings found!");

								if(aktWorkingObjNum==0)
									aktNumPoints=polygon.getExteriorRing().getNumPoints();
								else if(polygon.getNumInteriorRing()>=aktWorkingObjNum)
									aktNumPoints=polygon.getInteriorRingN(aktWorkingObjNum-1).getNumPoints();
								else
									aktNumPoints=0;

								if(aktWorkingPointNum==aktNumPoints-2)
								{
									// Es handelte sich wirklich um einen neuen Punkt
									precision.addEntry(location, gpsStatus, aktWorkingObjNum, aktWorkingPointNum);
								}
								else if(polyIsFresh)
								{
									/* Achtung: Es wurde der erste oder zweite Punkt in einem noch-nicht-Polygon bearbeitet. Dadurch wurde
									 * nun ein Polygon erzeugt, was aber im nächsten Schritt wieder entfernt werden muss!*/

									firstPoint=polygon.getExteriorRing().getCoordinateN(0);
									secondPoint=polygon.getExteriorRing().getCoordinateN(2); // Der geänderte Punkt
									thirdPoint=null;
									polygon=null;

									// Aktualisiere Precision
									precision.deleteEntry(aktWorkingObjNum, aktWorkingPointNum);
									precision.addEntry(location, gpsStatus, aktWorkingObjNum, aktWorkingPointNum);

								}
								else if(aktNumPoints==0)
								{
									// Es wurde der erste oder zweite Punkt eines InterriorRings neu hinzugefügt oder bearbeitet, während noch kein LinearRing vorliegt...
									precision.deleteEntry(aktWorkingObjNum, aktWorkingPointNum);
									precision.addEntry(location, gpsStatus, aktWorkingObjNum, aktWorkingPointNum);
								}
								else if(numInteriorBefore<polygon.getNumInteriorRing())
								{
									// Es wurde gerade ein neuer interiorRIng erzeugt. Da aber kein neuer Punkt erzeugt wurde muss ein alter bearbeitet wurden sein...
									firstPoint=polygon.getInteriorRingN(aktWorkingObjNum-1).getCoordinateN(0);
									secondPoint=polygon.getInteriorRingN(aktWorkingObjNum-1).getCoordinateN(2); // Der geänderte Punkt
									thirdPoint=null;
									polygon = Functions.rmInterriorRing(polygon, aktWorkingObjNum-1);			// Lösche den interriorRing aus dem Polygon

									// Aktualisiere Precision
									precision.deleteEntry(aktWorkingObjNum, aktWorkingPointNum);
									precision.addEntry(location, gpsStatus, aktWorkingObjNum, aktWorkingPointNum);

								}
								else
								{
									// Es handelt sich um einen "alten Punkt", der bearbeitet werden soll
									polygon = Functions.replacePointWithLastPointofAPolygon(polygon, aktWorkingObjNum, aktWorkingPointNum);
									precision.deleteEntry(aktWorkingObjNum, aktWorkingPointNum);
									precision.addEntry(location, gpsStatus, aktWorkingObjNum, aktWorkingPointNum);
								}
							}
							else
							{
								// Es muss sich um einen der ersten beiden Punkte handeln! Lösche Informationen (falls vorhanden) und speichere neue...
								precision.deleteEntry(aktWorkingObjNum, aktWorkingPointNum);
								precision.addEntry(location, gpsStatus, aktWorkingObjNum, aktWorkingPointNum);
							}

							break;

						}
						drawingPanel.moveTo(result);	
						dialog.cancel();
					}
				});
				dialog.show();
			}
		});
		location.getCoordinateByGPS();
	}

	// Starts creating a new feature on given layer at given position 
	public void createFeature(Coordinate pos) {
		switch (type) {
		case WFSLayer.LAYER_TYPE_POINT:
			createPoint(pos);
			break;
		case WFSLayer.LAYER_TYPE_LINE:
			createLine(pos);
			break;
		case WFSLayer.LAYER_TYPE_POLYGON:
			createPolygon(pos);
			break;
		}
	}

	// Creates a new point 
	private void createPoint(Coordinate pos) {
		switch (status) {
		case STATUS_IDLE:
			status = STATUS_ADD;
			createPoint(pos);
			break;
		case STATUS_ADD:
			point = new GeometryFactory().createPoint(pos);
			status = STATUS_ADJUST;
			showFatWorkingPoint=true;
			break;
		}
	}

	// creates a new line
	private void createLine(Coordinate pos) {
		switch (status) {
		case STATUS_IDLE:
			status = STATUS_ADD;
			createLine(pos);
			break;
		case STATUS_ADD:
			showFatWorkingPoint=true;
			if (line == null) {
				if (firstPoint == null && secondPoint == null) {
					firstPoint = pos;
					status = STATUS_ADJUST;
					aktWorkingPointNum=0;
				} else if (firstPoint != null && secondPoint == null) {
					secondPoint = pos;
					Coordinate[] newCoord = { firstPoint, secondPoint };
					line = new GeometryFactory().createLineString(newCoord);
					status = STATUS_ADJUST;
					firstPoint = null;
					secondPoint = null;
					aktWorkingPointNum=1;
				}
			} 
			else 
			{
				line = Functions.appendLineString(line, pos);
				status = STATUS_ADJUST;
			}
			break;
		}
	}

	// Creates a new polygon 
	private void createPolygon(Coordinate pos) {
		switch (status) {
		case STATUS_IDLE:
			status = STATUS_ADD;
			createPolygon(pos);
			break;
		case STATUS_ADD:
			showFatWorkingPoint=true;
			if (polygon == null) {
				if (firstPoint == null && secondPoint == null
						&& thirdPoint == null) {
					firstPoint = pos;
					status = STATUS_ADJUST;
				} else if (firstPoint != null && secondPoint == null
						&& thirdPoint == null) {
					secondPoint = pos;
					status = STATUS_ADJUST;
				} else if (firstPoint != null && secondPoint != null
						&& thirdPoint == null) {
					thirdPoint = pos;
					Coordinate[] newCoord = { firstPoint, secondPoint,
							thirdPoint };
					polygon = new GeometryFactory().createPolygon(
							Functions.coordinatesToLinearRing(newCoord), null);
					status = STATUS_ADJUST;
					firstPoint = null;
					secondPoint = null;
					thirdPoint = null;
				}
			} else {
				if (polygonAddHole) {
					if (Functions.getPolygonLineStringN(polygon, selectedHole) == null) {
						if (firstPoint == null && secondPoint == null
								&& thirdPoint == null) {
							firstPoint = pos;
							status = STATUS_ADJUST;
						} else if (firstPoint != null && secondPoint == null
								&& thirdPoint == null) {
							secondPoint = pos;
							status = STATUS_ADJUST;
						} else if (firstPoint != null && secondPoint != null
								&& thirdPoint == null) {
							thirdPoint = pos;
							Coordinate[] newCoord = { firstPoint, secondPoint,
									thirdPoint };
							polygon = Functions.addPolygonHole(polygon,
									newCoord);
							createMenu(MENU_EDIT_HOLES);
							status = STATUS_ADJUST;
							firstPoint = null;
							secondPoint = null;
							thirdPoint = null;
						}
					} else {
						polygon = Functions.appendPolygonHole(polygon,
								selectedHole, pos);
						status = STATUS_ADJUST;
					}
				} else {
					polygon = Functions.appendPolygonExterior(polygon, pos);
					status = STATUS_ADJUST;
				}
			}
			break;
		}
	}

	// creates alertbuilder
	private void setAlertBuilder(int geomType) {
		alertBuilder = new AlertDialog.Builder(context);
		if (geomType == WFSLayer.LAYER_TYPE_POINT) {
			alertBuilder.setMessage(context
					.getString(R.string.feature_editor_alert_point));
		} else if (geomType == WFSLayer.LAYER_TYPE_LINE) {
			alertBuilder.setMessage(context
					.getString(R.string.feature_editor_alert_line));
		} else if ((geomType == WFSLayer.LAYER_TYPE_POLYGON)) {
			alertBuilder.setMessage(context
					.getString(R.string.feature_editor_alert_polygon));
		}
		alertBuilder.setCancelable(false);
		alertBuilder.setNeutralButton("OK",
				new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		alertBuilder.show();
	}

	//	// Funktion für die Fotoaufnahme
	//	private void takePicture() {
	//		//TODO: Kamerafunktion aufrufen und Bild machen
	//		
	//	}



	// TODO getter and setter
	/** Returns the current creator status */
	public int getStatus() {
		return status;
	}

	// Returns the type of feature that is in creation
	// returns -1 if no feature is in creation
	public int getType() {
		return type;
	}

	/** Returns first point */
	public Coordinate getFirst() {
		return firstPoint;
	}

	/** Returns second point */
	public Coordinate getSecond() {
		return secondPoint;
	}

	/** Returns the point currently in creation */
	public Point getNewPoint() {
		if (type > -1) {
			return point;
		} else {
			return null;
		}
	}

	/** Returns the line currently in creation */
	public LineString getNewLine() {
		if (type > -1) {
			return line;
		} else {
			return null;
		}
	}

	/** Returns the polygon currently in creation */
	public Polygon getNewPolygon() {
		if (type > -1) {
			return polygon;
		} else {
			return null;
		}
	}

	/** Returns the coordinates of the feature in creation as array */
	public Point[][] getCoordinates() {
		if (type > -1) {
			Point[][] coords = null;
			switch (type) {
			case WFSLayer.LAYER_TYPE_POINT:
				if (point != null) {
					coords = new Point[1][1];
					coords[0][0] = point;
					return coords;
				}
				break;
			case WFSLayer.LAYER_TYPE_LINE:
				if (line != null) {
					coords = new Point[1][line.getNumPoints()];
					for (int i = 0; i < line.getNumPoints(); i++) {
						coords[0][i] = line.getPointN(i);
					}
					return coords;
				}
				break;
			case WFSLayer.LAYER_TYPE_POLYGON:
				if (polygon != null) {
					coords = new Point[polygon.getNumInteriorRing() + 1][polygon
					                                                     .getNumPoints()];
					for (int i = 0; i < polygon.getExteriorRing()
							.getNumPoints(); i++) {
						coords[0][i] = polygon.getExteriorRing().getPointN(i);
					}
					for (int j = 1; j <= polygon.getNumInteriorRing(); j++) {
						for (int i = 0; i < polygon.getInteriorRingN(j - 1)
								.getNumPoints(); i++) {
							coords[j][i] = polygon.getInteriorRingN(j - 1)
									.getPointN(i);
						}
					}
					return coords;
				}
				break;

			default:
				return null;
			}
		} else {
			return null;
		}
		return null;
	}

	// Moves point with given index to given position
	public void movePointTo(int[] idx, Coordinate c) {
		if (idx[0] == -1 && idx[1] == -1)
		{	
			aktWorkingPointNum=0;
			// Nehme neue Koordinatena auf...
			firstPoint.setCoordinate(c);
			// Genauigkeitsinformationen gehen verloren
			precision.deleteEntry(aktWorkingObjNum, aktWorkingPointNum);
		}
		else if (idx[0] == -2 && idx[1] == -2)
		{
			// Nur für Polygone relevant!

			aktWorkingPointNum=1;
			// Nehme neue Koordinaten auf
			secondPoint.setCoordinate(c);
			// Genauigkeitsinformationen gehen verloren
			precision.deleteEntry(aktWorkingObjNum, aktWorkingPointNum);
		} 
		else
		{
			switch (type) {
			case WFSLayer.LAYER_TYPE_POINT:
				/*Der aktuelle Punkt wurde manuell editiert und verfügt nicht (mehr) über 
				 * Genauigkeitsinformationen - der komplette Container beinhaltet maximal 
				 * diesen Punkt und kann geleert werden! */
				if(!precision.isEmpty()) precision.clear();
				//Nehme neue Koordinaten auf...
				point.getCoordinate().setCoordinate(c);
				break;
			case WFSLayer.LAYER_TYPE_LINE:
				aktWorkingPointNum=idx[1];
				// Entferne Informationen über Genauigkeiten für den betroffenen Punkt
				precision.deleteEntry(0, aktWorkingPointNum);
				//Nehme neue Koordinatena auf...
				line.getCoordinateN(idx[1]).setCoordinate(c);
				break;
			case WFSLayer.LAYER_TYPE_POLYGON:
				aktWorkingPointNum=idx[1];
				if (idx[0] == 0)
				{
					aktWorkingObjNum=0;
					int lastPoint = polygon.getExteriorRing().getNumPoints() - 1;
					if (idx[1] == 0 || idx[1] == lastPoint)
					{
						aktWorkingPointNum=0;
						precision.deleteEntry(aktWorkingObjNum, aktWorkingPointNum);
						precision.deleteEntry(aktWorkingObjNum, lastPoint);

						polygon.getExteriorRing().getCoordinateN(0).setCoordinate(c);
						polygon.getExteriorRing().getCoordinateN(lastPoint).setCoordinate(c);
					}
					else
					{
						aktWorkingPointNum=idx[1];
						precision.deleteEntry(aktWorkingObjNum, aktWorkingPointNum);
						polygon.getExteriorRing().getCoordinateN(idx[1]).setCoordinate(c);
					}
				}
				else if (idx[0] > 0)
				{
					aktWorkingObjNum=idx[0];								
					int lastPoint = polygon.getInteriorRingN(idx[0] - 1)
							.getNumPoints() - 1;
					if (idx[1] == 0 || idx[1] == lastPoint) {
						aktWorkingPointNum=0;
						polygon.getInteriorRingN(idx[0] - 1).getCoordinateN(0).setCoordinate(c);
						polygon.getInteriorRingN(idx[0] - 1).getCoordinateN(lastPoint).setCoordinate(c);
						precision.deleteEntry(aktWorkingObjNum, aktWorkingPointNum);
						precision.deleteEntry(aktWorkingObjNum, lastPoint);
					}
					else
					{
						aktWorkingPointNum=idx[1];
						precision.deleteEntry(aktWorkingObjNum, aktWorkingPointNum);
						polygon.getInteriorRingN(idx[0] - 1).getCoordinateN(idx[1]).setCoordinate(c);
					}
				}
				break;
			}
		}
	}
}
