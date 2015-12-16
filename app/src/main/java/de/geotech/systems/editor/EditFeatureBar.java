/**
 * Toolbar to edit existing features
 * 
 * @author Torsten Hoch
 */

package de.geotech.systems.editor;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ActionBar.LayoutParams;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.GpsStatus;
import android.location.Location;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.WKTWriter;

import de.geotech.systems.R;
import de.geotech.systems.LGLSpecial.LGLValues;
import de.geotech.systems.database.DBAdapter;
import de.geotech.systems.drawing.DrawingPanelView;
import de.geotech.systems.features.Feature;
import de.geotech.systems.features.FeaturePrecision;
import de.geotech.systems.main.LeftMenuIconLayout;
import de.geotech.systems.main.MainActivity;
import de.geotech.systems.media.MediaCapture;
import de.geotech.systems.projects.Project;
import de.geotech.systems.projects.ProjectHandler;
import de.geotech.systems.utilities.Alerts;
import de.geotech.systems.utilities.CRS;
import de.geotech.systems.utilities.Constants;
import de.geotech.systems.utilities.DateTime;
import de.geotech.systems.utilities.Functions;
import de.geotech.systems.utilities.LocationFactory;
import de.geotech.systems.utilities.LocationFactory.OnLocationByGPSListener;
import de.geotech.systems.wfs.WFSFeatureImport;
import de.geotech.systems.wfs.WFSLayer;
import de.geotech.systems.wfs.WFSLayerAttributeTypes;
import de.geotech.systems.wfs.WFSLayerSynchronization;
import de.geotech.systems.wfs.WFSLayerSynchronization.OnSyncFinishedListener;

public class EditFeatureBar {
	// Logging classtag
	private static final String CLASSTAG = "EditFeatureBar";
	// Creator status
	public static final int STATUS_ADD = 1;
	public static final int STATUS_ADJUST = 2;
	public static final int STATUS_SAVED = 3;
	// Menu index
	public static final int MENU_EDIT_HOLES = 1001;
	// the standard color code for the menu
	public static final int STANDADRDCOLOR = Color.BLACK;
	private static final int PADDING = 10;
	private static final int EDIT_TEXT_MINIMUM_WIDTH = 475;
	private static final String TEXT_PLATZHALTER = LGLValues.TEXT_PLATZHALTER;

	// Application context Project 
	private Context context; 
	// current project
	private Project currentProject;
	//DrawingPanel with existing features
	private DrawingPanelView drawingPanel; 
	// LinearLayout to display the toolbar
	private LinearLayout toolbarLayout; 
	// LinearLayout to display the tool buttons according to current feature type
	private LinearLayout innerToolsLayout; 
	// Current creator status
	private int status; 
	// Type of feature to create
	private int type; 
	// Layer to draw on
	private long layerId; 
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
	// coordinates for the first three points
	private Coordinate firstPoint;
	private Coordinate secondPoint;
	private Coordinate thirdPoint;
	// animation of icons
	private ScaleAnimation iconAnimation;
	// listener if new features ready
	private OnNewFeatureListener listener;
	// current WFSLayer
	private WFSLayer currentLayer;
	// dialog to show alerts
	private AlertDialog.Builder alertBuilder;
	// Nummer des Punktes (einer Linie oder Polygons), der gerade bearbeitet wird. (vgl idx[1])
	private int aktWorkingPointNum;
	// Nummer des Objekts, welches gerade bearbeitet wird. Im Falle von Linien und Punkten ist das immer 0! (vgl. idx[0])
	private int aktWorkingObjNum;
	// Nimmt zwischenzeitlich die Anzahl an Punkten eines Bereiches eines Polygones auf...
	private int aktNumPoints;	
	// Kontrollvariable: Wurde das Polygon gerade in diesem Schritt erzeugt?
	private Boolean polyIsFresh;	
	// Kontrollvariable: Anzahl der Löcher vor dem Einpflegen eines Punktes
	private int numInteriorBefore;		
	// current feature to Edit
	private Feature featureToEdit;
	// are changes made
	private boolean changesMade;
	// database adapter to write into sql db
	private DBAdapter dbAdapter;
	// backup of edited feature
	private Feature featureWorkCopy;
	// precision fuer precision informationen eines GPS-punktes
	public FeaturePrecision precision = new FeaturePrecision();
	// ArrayList, in der alle Location-Instanzen (dh. auch die Genauigkeiten) gespeichert werden
	// public static ArrayList<Location> locationList = new ArrayList<Location>();
	private ArrayList<EditText> edits;
	private ArrayList<Spinner> spinners;
	private ArrayList<DateTime> dates;
	ArrayList<EditText> editViewlistX;
	ArrayList<EditText> editViewlistY;

	/**
	 * Instantiates a new editfeaturebar.
	 *
	 * @param drawingPanel the drawing panel
	 * @param toolbarLayout the toolbar layout for the bar
	 */
	public EditFeatureBar(DrawingPanelView drawingPanel, LinearLayout toolbarLayout) {
		// initialize fields
		this.drawingPanel = drawingPanel;
		this.context = drawingPanel.getContext();
		this.dbAdapter = new DBAdapter(context);
		this.toolbarLayout = toolbarLayout;
		this.toolbarLayout.setGravity(Gravity.CENTER);
		this.toolbarLayout.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
		this.toolbarLayout.setDividerPadding(5);
		this.toolbarLayout.setDividerDrawable(this.context.getResources().getDrawable(R.drawable.divider));
		this.iconAnimation = new ScaleAnimation(0.7f, 1f, 0.7f, 1f,	Animation.RELATIVE_TO_SELF, 0.5f, 
				Animation.RELATIVE_TO_SELF, 0.5f);
		this.iconAnimation.setDuration(100);
		this.type = -1;
		this.point = null;
		this.line = null;
		this.polygon = null;
		this.polygonAddHole = false;
		this.selectedHole = 0;
		this.firstPoint = null;
		this.secondPoint = null;
		this.thirdPoint = null;
		// no changes made yet
		this.changesMade = false;
		this.alertBuilder = null;
		this.currentLayer = null;
		this.aktWorkingPointNum = 0;
		this.aktWorkingObjNum = 0;
		this.polyIsFresh = false;	
		this.numInteriorBefore = 0;	
		// first Status is adjusting/editing
		this.setStatus(STATUS_ADJUST);
	}

	/**
	 * Open the editor bar. Initializes the feature attributes to be edited and
	 * the bar with all its buttons
	 *
	 * @param featureID the current feature id
	 * @param layerID the current layer id
	 */
	public void openBar(long featureID, long layerID) {
		// wenn schon im creation mode, dann abbrechen
		if (drawingPanel.isInCreationMode()) {
			return;
		}
		// wenn schon im editing mode, dann abbrechen
		if (drawingPanel.isInEditingMode()) {
			return;
		}
		// if variables not initialized yet
		if (this.innerToolsLayout != null) {
			this.innerToolsLayout.removeAllViews();
		}
		if (this.toolbarLayout != null) {
			this.toolbarLayout.removeAllViews();
		}
		this.changesMade = false; 
		// set the current Project
		this.currentProject =  ProjectHandler.getCurrentProject();
		// layout erstellen für mittlere buttons
		this.innerToolsLayout = new LinearLayout(context);
		// get the current layer
		for (WFSLayer wfsLayer : currentProject.getWFSContainer()) {
			if (wfsLayer.getLayerID() == layerID) {
				this.currentLayer = wfsLayer;
				break;
			}
		}
		for (Feature feature : currentLayer.getFeatureContainer()) {
			if (feature.getFeatureID() == featureID) {
				this.featureToEdit = feature;
				break;
			}
		}
		// Log.e(CLASSTAG, "Feature " + featureToEdit.getFeatureID() + " ausgewählt.");
		// set as current layer in project
		ProjectHandler.getCurrentProject().setCurrentWFSLayer(currentLayer);
		// set this bars attributes needed
		this.type = currentLayer.geometryTypeToInt(currentLayer.getType());
		this.layerId = currentLayer.getLayerID();
		// backup coordinates if nothing is saved later
		this.featureWorkCopy = featureToEdit.getCopyOfFieldValuesWithoutID();
		// Log.e(CLASSTAG, "Feature " + featureWorkCopy.getFeatureID() + " ist Kopie.");
		// editing mode erzwingen mit feature
		this.drawingPanel.setEditedFeature(featureToEdit);
		// Setze die Nummer des zu bearbeitenden Punktes und Objekts (für Polygone) zurück
		this.aktWorkingPointNum = 0;
		this.aktWorkingObjNum = 0;
		this.initButtonsAndToolbar();
		// set status to adjust (adjusting all points possible)
		this.setStatus(STATUS_ADJUST);
		this.drawingPanel.reloadFeaturesAndDraw();
		this.setNewFeatureCoordinates();
	}

	private void setNewFeatureCoordinates() {
		line = null;
		point = null;
		polygon = null;		
		int killLastPoint = 0;
		Coordinate[] x = featureWorkCopy.getCoordinatesFromLinestring();
		if (type == WFSLayer.LAYER_TYPE_POLYGON) {
			killLastPoint = 1;
			x = ((Polygon) featureWorkCopy.getGeom()).getExteriorRing().getCoordinates();
		}
		for (int i = 0; i < x.length - killLastPoint; i++) {
			this.createFeature(x[i]);
			// Log.e(CLASSTAG + " Set Feature", "Coordinate: " + featureWorkCopy.getCoordinatesFromLinestring()[i]);
		}
		if (type == WFSLayer.LAYER_TYPE_POLYGON) {
			// TODO hier noch interior ring etc. einbauen...
			// polygonAddHole = true;

		}
	}

	/**
	 * Close bar.
	 */
	public void closeBar() {
		this.toolbarLayout.setVisibility(View.GONE);
		this.drawingPanel.resetEditedFeature();
		// if variables not initialized yet
		if (this.innerToolsLayout != null) {
			this.innerToolsLayout.removeAllViews();
		}
		if (this.toolbarLayout != null) {
			this.toolbarLayout.removeAllViews();
		}
		// this.changesMade = false;
		this.drawingPanel.setEditMode(false);
		this.drawingPanel.reloadFeaturesAndDraw();
	}

	private void reLoadBar() {
		// close the bar
		this.closeBar();
		this.drawingPanel.setEditMode(true);
		// editing mode erzwingen mit feature
		this.drawingPanel.setEditedFeature(featureToEdit);
		// Setze die Nummer des zu bearbeitenden Punktes und Objekts (für Polygone) zurück
		this.aktWorkingPointNum = 0;
		this.aktWorkingObjNum = 0;
		// re-initialize it
		this.initButtonsAndToolbar();
		// set status to adjust (adjusting all points possible)
		this.setStatus(STATUS_ADJUST);
	}

	private void initButtonsAndToolbar() {
		this.addCancelButton();
		this.addLayerDetailTextViews();
		// this.addEnterCoordsButton();
		this.addEnterAttributesButton();
		this.initMiddleMenu(type);
		this.addSyncButton();
		// this.addGPSButton();
		// this.addPhotoButton();
		// set the whole bar visible
		this.toolbarLayout.setVisibility(View.VISIBLE);
	}

	private void addCancelButton() {
		// schliessen-button einfuegen - Beginn aller Buttons einzufuegen
		this.toolbarLayout.addView(new LeftMenuIconLayout(context,
				context.getString(R.string.featureEditor_cancel_update), STANDADRDCOLOR,
				R.drawable.ic_check_error, 5, 5, new OnClickListener() {
			@Override
			public void onClick(View v) {
				v.startAnimation(iconAnimation);
				if (changesMade) {
					AlertDialog.Builder errorSaveDialog = new AlertDialog.Builder(context);
					errorSaveDialog.setTitle(R.string.editfeaturebar_close_unsaved_title);
					errorSaveDialog.setMessage(R.string.editfeaturebar_close_unsaved_message);
					errorSaveDialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface doubleSaveDialog, int which) {
						}
					});
					errorSaveDialog.setPositiveButton(R.string.editfeaturebar_close_unsaved_title, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface doubleSaveDialog, int which) {
							closeBar();
						}
					});
					errorSaveDialog.show();
				} else {
					closeBar();
				}
			}
		}));
	}

	/**
	 * Creates the middle tool bar menu for a special type
	 *
	 * @param featureTypeID the feature type id
	 */
	private void initMiddleMenu(int featureTypeID) {
		// layout leren
		innerToolsLayout.removeAllViews();
		innerToolsLayout.setGravity(Gravity.CENTER);
		innerToolsLayout.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
		innerToolsLayout.setDividerPadding(5);
		innerToolsLayout.setDividerDrawable(context.getResources().getDrawable(R.drawable.divider));
		// je nach feature punkt hinzufuegen-button oder nicht
		switch (featureTypeID) {
		case WFSLayer.LAYER_TYPE_POINT:
			// no "add-Buton", because you cant add a point to a point
			innerToolsLayout.addView(new LeftMenuIconLayout(context,
					context.getString(R.string.featureEditor_point_update),
					STANDADRDCOLOR, R.drawable.ic_check_ok, 5, 5,
					new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (changesMade) {
						v.startAnimation(iconAnimation);
						saveCopyOfEditedFeatureAsNew();
						Alerts.errorMessage(context, context.getString(R.string.feature_editor_save_title),
								context.getString(R.string.feature_editor_save_point)).show();
					} else {
						v.startAnimation(iconAnimation);
						Alerts.errorMessage(context, context.getString(R.string.feature_editor_save_title),
								context.getString(R.string.feature_editor_save_message)).show();
					}
				}
			}));
			break;
		case WFSLayer.LAYER_TYPE_LINE:
			// New line
			//			setStatus(STATUS_ADJUST);
			//			innerToolsLayout.addView(new LeftMenuIconLayout(context,
			//					context.getString(R.string.featureEditor_add_point),
			//					STANDADRDCOLOR, R.drawable.ic_editor_new_point, 5, 5,
			//					new OnClickListener() {
			//				@Override
			//				public void onClick(View v) {
			//					v.startAnimation(iconAnimation);
			//					setStatus(STATUS_ADJUST_OR_ADD);
			//					// Aktualisiere aktWorkingPointNum
			//					if (line != null) {
			//						aktWorkingPointNum = line.getNumPoints();
			//					} else {
			//						if (firstPoint == null) {
			//							aktWorkingPointNum = 0;
			//						} else {
			//							aktWorkingPointNum = 1;
			//						}
			//					}
			//					// DrawingPanelView muss "neu gezeichnet werden"
			//					drawingPanel.invalidate();
			//				}
			//			}));
			innerToolsLayout.addView(new LeftMenuIconLayout(context,
					context.getString(R.string.featureEditor_line_update),
					STANDADRDCOLOR, R.drawable.ic_check_ok, 5, 5,
					new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (changesMade) {
						v.startAnimation(iconAnimation);
						saveCopyOfEditedFeatureAsNew();
						Alerts.errorMessage(context, context.getString(R.string.feature_editor_save_title),
								context.getString(R.string.feature_editor_save_line)).show();
						// setSaveAlertBuilder(WFSLayer.LAYER_TYPE_LINE);
					} else {
						v.startAnimation(iconAnimation);
						Alerts.errorMessage(context, context.getString(R.string.feature_editor_save_title),
								context.getString(R.string.feature_editor_save_message)).show();
					}
				}
			}));
			break;
		case WFSLayer.LAYER_TYPE_POLYGON:
			// New polygon 
			//			setStatus(STATUS_ADJUST);
			//			innerToolsLayout.addView(new LeftMenuIconLayout(context,
			//					context.getString(R.string.featureEditor_add_point),
			//					STANDADRDCOLOR, R.drawable.ic_editor_new_point, 5, 5,
			//					new OnClickListener() {
			//				@Override
			//				public void onClick(View v) {
			//					v.startAnimation(iconAnimation);
			//					setStatus(STATUS_ADJUST_OR_ADD);
			//					// Aktualisiere aktWorkingPointNum
			//					if (polygon != null) {
			//						aktWorkingPointNum = polygon.getExteriorRing().getNumPoints() - 1;
			//					} else {
			//						if (firstPoint == null) {
			//							aktWorkingPointNum = 0;
			//						} else {
			//							if (secondPoint==null) {
			//								aktWorkingPointNum = 1;
			//							} else {
			//								aktWorkingPointNum = 2;
			//							}
			//						}
			//
			//					}
			//					// DrawingPanelView muss "neu gezeichnet werden"
			//					drawingPanel.invalidate();
			//				}
			//			}));
			//			innerToolsLayout.addView(new LeftMenuIconLayout(context,
			//					context.getString(R.string.featureEditor_edit_hole),
			//					STANDADRDCOLOR, R.drawable.ic_editor_edit_holes, 5, 5,
			//					new OnClickListener() {
			//				@Override
			//				public void onClick(View v) {
			//					v.startAnimation(iconAnimation);
			//					if (polygon != null) {
			//						initMiddleMenu(MENU_EDIT_HOLES);
			//						// DrawingPanelView muss "neu gezeichnet werden"
			//						drawingPanel.invalidate();
			//					}
			//				}
			//			}));
			innerToolsLayout.addView(new LeftMenuIconLayout(context,
					context.getString(R.string.featureEditor_polygon_save),
					STANDADRDCOLOR, R.drawable.ic_check_ok, 5, 5,
					new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (changesMade) {
						v.startAnimation(iconAnimation);
						saveCopyOfEditedFeatureAsNew();
						Alerts.errorMessage(context, context.getString(R.string.feature_editor_save_title),
								context.getString(R.string.feature_editor_save_polygon)).show();
						// setSaveAlertBuilder(WFSLayer.LAYER_TYPE_POLYGON);
					} else {
						v.startAnimation(iconAnimation);
						Alerts.errorMessage(context, context.getString(R.string.feature_editor_save_title),
								context.getString(R.string.feature_editor_save_message)).show();
					}
				}
			}));
			break;
			// New polygon hole 
			//		case MENU_EDIT_HOLES:
			//			setStatus(STATUS_ADJUST_OR_ADD);
			//			polygonAddHole = true;
			//			if (polygon.getNumInteriorRing() > 0) {
			//				TextView holeFound = new TextView(context);
			//				holeFound.setText(context
			//						.getString(R.string.featureEditor_hole) + ": ");
			//				innerToolsLayout.addView(holeFound);
			//				final Spinner chooseHole = new Spinner(context);
			//				chooseHole.setAdapter(Functions.getAdaper(context, polygon));
			//				chooseHole.setSelection(selectedHole - 1);
			//				chooseHole.setOnItemSelectedListener(new OnItemSelectedListener() {
			//					@Override
			//					public void onItemSelected(AdapterView<?> arg0,
			//							View arg1, int arg2, long arg3) {
			//						selectedHole = (Integer) chooseHole
			//								.getSelectedItem();
			//						aktWorkingObjNum=selectedHole;
			//
			//						// DrawingPanelView muss "neu gezeichnet werden"
			//						drawingPanel.invalidate();
			//					}
			//					@Override
			//					public void onNothingSelected(AdapterView<?> arg0) {
			//					}
			//				});
			//				innerToolsLayout.addView(chooseHole);
			//			} else {
			//				TextView noHoles = new TextView(context);
			//				noHoles.setText(context.getString(R.string.featureEditor_error_no_hole));
			//				innerToolsLayout.addView(noHoles);
			//			}
			//			innerToolsLayout.addView(new LeftMenuIconLayout(context,
			//					context.getString(R.string.featureEditor_new_hole),
			//					STANDADRDCOLOR, R.drawable.ic_editor_add_hole, 5, 5,
			//					new OnClickListener() {
			//				@Override
			//				public void onClick(View v) {
			//					v.startAnimation(iconAnimation);
			//					setStatus(STATUS_ADJUST_OR_ADD);
			//					selectedHole = Functions.getAdaper(context, polygon).getCount() + 1;
			//					aktWorkingObjNum = selectedHole;
			//					aktWorkingPointNum = 0;
			//					// DrawingPanelView muss "neu gezeichnet werden"
			//					drawingPanel.invalidate();
			//				}
			//			}));
			//			innerToolsLayout.addView(new LeftMenuIconLayout(context,
			//					context.getString(R.string.featureEditor_add_point),
			//					STANDADRDCOLOR, R.drawable.ic_editor_new_point, 5, 5,
			//					new OnClickListener() {
			//				@Override
			//				public void onClick(View v) {
			//					v.startAnimation(iconAnimation);
			//					setStatus(STATUS_ADJUST_OR_ADD);
			//					// Aktualisiere aktWorkingPointNum
			//					if (aktWorkingObjNum > 0) {
			//						if (polygon.getNumInteriorRing() >= aktWorkingObjNum)	{
			//							aktWorkingPointNum=polygon.getInteriorRingN(aktWorkingObjNum - 1).getNumPoints() - 1;
			//						} else {
			//							if (firstPoint == null) {
			//								aktWorkingPointNum = 0;
			//							} else {
			//								if (secondPoint == null) {
			//									aktWorkingPointNum = 1;
			//								} else {
			//									aktWorkingPointNum = 2;
			//								}
			//							}
			//						}
			//						// DrawingPanelView muss "neu gezeichnet werden"
			//						drawingPanel.invalidate();
			//					}
			//				}
			//			}));
			//			innerToolsLayout.addView(new LeftMenuIconLayout(context,
			//					context.getString(R.string.back), STANDADRDCOLOR,
			//					R.drawable.ic_editor_back, 5, 5, new OnClickListener() {
			//				@Override
			//				public void onClick(View v) {
			//					v.startAnimation(iconAnimation);
			//					initMiddleMenu(WFSLayer.LAYER_TYPE_POLYGON);
			//					polygonAddHole = false;
			//				}
			//			}));
			//			break;
		default:
			TextView error = new TextView(context);
			error.setText(context.getString(R.string.error + R.string.featureEditor_error_unknown_menu_id));
			innerToolsLayout.addView(error);
		}
		toolbarLayout.addView(innerToolsLayout);
	}

	/**
	 * Save copy of edited feature as new.
	 */
	private void saveCopyOfEditedFeatureAsNew() {
		// Log.e(CLASSTAG, "OLD Geom: " + featureToEdit.getWKTGeometry());
		featureToEdit.setWKTGeometry(getCurrentWKTString());
		// Log.e(CLASSTAG, "NEW Geom: " + getCurrentWKTString());
		// Log.e(CLASSTAG, "OLD Attributes: " + featureToEdit.getAttributes());
		featureToEdit.setAttributes(featureWorkCopy.getAttributes());
		// Log.e(CLASSTAG, "NEW Attributes: " + featureWorkCopy.getAttributes());
		featureToEdit.setSync(false);
		featureToEdit.setDone(featureWorkCopy.isDone());
		currentLayer.setSync(false);
		ProjectHandler.getCurrentProject().setSync(false);
		// save edited Feature in db
		if (dbAdapter.updateFeatureInDB(featureToEdit) && dbAdapter.updateWFSLayerInDB(currentLayer)) {
			changesMade = false;
		}
		// save edited feature in Ram
		drawingPanel.saveEditedFeature(featureToEdit);
		setStatus(STATUS_SAVED);
		reLoadBar();
		drawingPanel.reloadFeaturesAndDraw();
	}

	/**
	 * Adds the layer details as Text Views.
	 */
	private void addLayerDetailTextViews() {
		// Textview erstellen fuer den layernamen
		TextView editedFeatureName = new TextView(context);
		editedFeatureName.setPadding(PADDING, PADDING, PADDING, PADDING);
		// TODO Uebersetzungen
		editedFeatureName.setText("Feature-ID: " + featureToEdit.getFeatureID() + "\nGeoServer-ID: " + featureToEdit.getGeoServerID());
		// Textview erstellen fuer den layernamen und typ
		TextView editedWFSLayerName = new TextView(context);
		editedWFSLayerName.setPadding(PADDING, PADDING, PADDING, PADDING);
		// TODO Uebersetzungen
		editedWFSLayerName.setText("Layer: " + currentLayer.getName()  + "\nType: " + currentLayer.getType());
		// create toolbar menu with drawing buttons
		toolbarLayout.addView(editedFeatureName);
		toolbarLayout.addView(editedWFSLayerName);
	}

	private void addEnterAttributesButton() {
		// selbsteintragebutton erstellen
		toolbarLayout.addView(new LeftMenuIconLayout(context,
				context.getString(R.string.editfeaturebar_manually_attribute_input), Color.BLACK,
				R.drawable.ic_editor_input, 5, 5, new OnClickListener() {
			@Override
			public void onClick(View v) {
				v.startAnimation(iconAnimation);
				// Create dialog
				final Dialog dialog = new Dialog(context);
				dialog.setTitle(context.getString(R.string.featureEditor_attribute_input_title));
				dialog.setContentView(R.layout.attribute_input_dialog);
				// Define dialog elements
				Button cancel = (Button) dialog.findViewById(R.id.button_coordinput_cancel);
				Button save = (Button) dialog.findViewById(R.id.button_attribute_set);
				edits = new ArrayList<EditText>();
				spinners = new ArrayList<Spinner>();
				dates = new ArrayList<DateTime>();
				TableLayout table = (TableLayout) dialog.findViewById(R.id.attribute_input_table);
				TableRow.LayoutParams params = new TableRow.LayoutParams(
						LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
				// TODO: LGL KRAM HART CODIERT
				// wenn es einer der layer mit Objektartenattributierung ist
				if (currentLayer.getName().equalsIgnoreCase(LGLValues.LGL_OAK_LAYER_NAMES[0]) 
						|| currentLayer.getName().equalsIgnoreCase(LGLValues.LGL_OAK_LAYER_NAMES[1]) 
						|| currentLayer.getName().equalsIgnoreCase(LGLValues.LGL_OAK_LAYER_NAMES[2])) {
					for (final WFSLayerAttributeTypes attribute : currentLayer.getAttributeTypes()) {
//						if (currentLayer.getAttributes().contains(attribute)) {
							if (attribute.getName().equalsIgnoreCase(LGLValues.KNOWN_ATTRIBUTES[0]) 
									|| attribute.getName().equalsIgnoreCase(LGLValues.KNOWN_ATTRIBUTES[1])
									|| attribute.getName().equalsIgnoreCase(LGLValues.KNOWN_ATTRIBUTES[2])
									|| attribute.getName().equalsIgnoreCase(LGLValues.KNOWN_ATTRIBUTES[3])) {
								String featureValue = "";
								if (featureWorkCopy.getAttributes().get(attribute.getName()) != null) {
									featureValue = featureWorkCopy.getAttributes().get(attribute.getName()).toString();
								}
								// Log.e(CLASSTAG, "ALL Attributes are: " + featureWorkCopy.getAttributes().toString());
								TableRow row = new TableRow(context);
								row.setGravity(Gravity.CENTER_VERTICAL);
								TextView title = new TextView(context);
								title.setText(attribute.getName() + ":");
								title.setPadding(PADDING, PADDING, PADDING, PADDING);
								row.addView(title, params);
								final AutoCompleteTextView editLGL = new AutoCompleteTextView(context);
								ArrayList<String> items = new ArrayList<String>();
								// TODO: ARRAYLIST setzen!!!!
								Log.e(CLASSTAG + " Attribute-Change", ".getAttributeLabelsPlus( " + currentLayer.getName() + "," + attribute.getName());
								items = ProjectHandler.getOakAttributeHandler().getAttributeLabelsPlus(currentLayer.getName(), attribute.getName());
								Log.e(CLASSTAG, "Items are: " + items.toString());
								ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1);
								adapter.addAll(items);
								editLGL.setAdapter(adapter);
								editLGL.setGravity(Gravity.LEFT);
								editLGL.setTag(attribute.getName());
								editLGL.setText(featureValue);
								String textStr = "STRING";
								if (attribute.getType() == WFSLayerAttributeTypes.DECIMAL) {
									editLGL.setInputType(InputType.TYPE_CLASS_NUMBER
											| InputType.TYPE_NUMBER_FLAG_SIGNED
											| InputType.TYPE_NUMBER_FLAG_DECIMAL);
									textStr = "DECIMAL";
								} else if (attribute.getType() == WFSLayerAttributeTypes.INTEGER) {
									editLGL.setInputType(InputType.TYPE_CLASS_NUMBER
											| InputType.TYPE_NUMBER_FLAG_SIGNED);
									textStr = "INTEGER";
								}
								editLGL.setPadding(PADDING, PADDING, PADDING, PADDING);
								editLGL.setThreshold(1);
								editLGL.setImeOptions(EditorInfo.IME_ACTION_NEXT);
								editLGL.setDropDownWidth(EDIT_TEXT_MINIMUM_WIDTH);
								editLGL.setOnClickListener(new OnClickListener() {
									@Override
									public void onClick(View v) {
										editLGL.showDropDown();
									}
								});
								AdapterView.OnItemClickListener l = new OnItemClickListener() {
									@Override
									public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
										String selection = (String) parent.getItemAtPosition(position);
										String y = ProjectHandler.getOakAttributeHandler().getValueForOneLabelPlus(currentLayer.getName(), attribute.getName(), selection);
										if (y != null) {
											editLGL.setText(y);
										} else {
											editLGL.setText(selection);
										}
									}
								};
								editLGL.setOnItemClickListener(l);
								row.addView(editLGL, params);
								TextView resStr = new TextView(context);
								Iterator<String> iterStr = attribute.getRestrictions().keySet().iterator();
								while (iterStr.hasNext()) {
									String key = iterStr.next();
									textStr = textStr + "\n" + key + ": " + attribute.getRestrictions().get(key);
								}
								resStr.setText(textStr);
								resStr.setPadding(PADDING, PADDING, PADDING, PADDING);
								row.addView(resStr, params);
								edits.add(editLGL);
								table.addView(row, params);
							} else {
								if (attribute.getName().equalsIgnoreCase(LGLValues.TEXT_ATTRIBUTE)) {
									// do nothing
								} else {
									table.addView(createRow(attribute.getName(), attribute.getType(), attribute.getRestrictions(), featureWorkCopy));
								}
							}
//							}
					}
					// falls es ein standard-lgl layer ist, der ein erledigt tag hat
				} else if (currentLayer.getName().equalsIgnoreCase(LGLValues.STANDARD_LGL_LAYER_NAMES[0])
						|| currentLayer.getName().equalsIgnoreCase(LGLValues.STANDARD_LGL_LAYER_NAMES[1])
						|| currentLayer.getName().equalsIgnoreCase(LGLValues.STANDARD_LGL_LAYER_NAMES[2])) {
					for (WFSLayerAttributeTypes attribute : currentLayer.getAttributeTypes()) {
						if (attribute.getName().equalsIgnoreCase(LGLValues.LGL_IS_DONE_FLAG_ATTRIBUTE)) {
							String featureValue = "";
							if (featureWorkCopy.getAttributes().get(attribute.getName()) != null) {
								featureValue = featureWorkCopy.getAttributes().get(attribute.getName()).toString();
							}
							TableRow row = new TableRow(context);
							row.setGravity(Gravity.CENTER_VERTICAL);
							TextView title = new TextView(context);
							title.setText(attribute.getName() + ":");
							title.setPadding(PADDING, PADDING, PADDING, PADDING);
							row.addView(title, params);
							final AutoCompleteTextView editLGL = new AutoCompleteTextView(context);
							ArrayList<String> items = new ArrayList<String>();
							items.add("0");
							items.add("1");
							ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1);
							adapter.addAll(items);
							editLGL.setAdapter(adapter);
							editLGL.setGravity(Gravity.LEFT);
							editLGL.setTag(attribute.getName());
							editLGL.setText(featureValue);
							editLGL.setPadding(PADDING, PADDING, PADDING, PADDING);
							editLGL.setThreshold(1);
							editLGL.setImeOptions(EditorInfo.IME_ACTION_NEXT);
							editLGL.setDropDownWidth(EDIT_TEXT_MINIMUM_WIDTH);
							editLGL.setOnClickListener(new OnClickListener() {
								@Override
								public void onClick(View v) {
									editLGL.showDropDown();
								}
							});
							String textStr = "STRING";
							if (attribute.getType() == WFSLayerAttributeTypes.DECIMAL) {
								editLGL.setInputType(InputType.TYPE_CLASS_NUMBER
										| InputType.TYPE_NUMBER_FLAG_SIGNED
										| InputType.TYPE_NUMBER_FLAG_DECIMAL);
								textStr = "DECIMAL";
							} else if (attribute.getType() == WFSLayerAttributeTypes.INTEGER) {
								editLGL.setInputType(InputType.TYPE_CLASS_NUMBER
										| InputType.TYPE_NUMBER_FLAG_SIGNED);
								textStr = "INTEGER";
							}
							row.addView(editLGL, params);
							TextView resStr = new TextView(context);
							Iterator<String> iterStr = attribute.getRestrictions().keySet().iterator();
							while (iterStr.hasNext()) {
								String key = iterStr.next();
								textStr = textStr + "\n" + key + ": " + attribute.getRestrictions().get(key);
							}
							resStr.setText(textStr);
							resStr.setPadding(PADDING, PADDING, PADDING, PADDING);
							row.addView(resStr, params);
							edits.add(editLGL);
							table.addView(row, params);							
						} else {
							table.addView(createRow(attribute.getName(), attribute.getType(), attribute.getRestrictions(), featureWorkCopy));
						}
					}
				} else {
					// add a textfield for every attribute
					for (WFSLayerAttributeTypes attribute : currentLayer.getAttributeTypes()) {
						table.addView(createRow(attribute.getName(), attribute.getType(), attribute.getRestrictions(), featureWorkCopy));
					}
				}
				// Set onClickListener
				cancel.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						// Close dialog
						dialog.cancel();
					}
				});
				save.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						changesMade = true;
						dialog.cancel();
						ContentValues contentVal = new ContentValues();
						ListIterator<EditText> iterEdits = edits.listIterator();
						String value;
						String key;
						String text = "";
						while (iterEdits.hasNext()) {
							EditText e = iterEdits.next();
							value = e.getEditableText().toString();
							value = value.replace("\n", " ");
							value = value.replace("\t", " ");
							value = value.replace("\"", "_");
							value = value.replace("=", "_");
							key = e.getTag().toString();
							// Log.e(CLASSTAG, "Key: " + key + " - Value: " + value);
							// default value ist 0 (integer, boolean etc.)
							// macht scheinbar gar nix, auch wenn string = ""
							if (value == null || value.length() == 0) {
								value = "";
							}
							// Log.i(CLASSTAG, "Saving Attribute " + key + " with value \"" + value + "\" as Copy.");
							if (!key.equalsIgnoreCase(LGLValues.TEXT_ATTRIBUTE)) {
								contentVal.put(key, value);
							}
							// wenn es einer der attributwerte ist, dann den wert zum attribut text hinzufügen
							if (key.equalsIgnoreCase(LGLValues.KNOWN_ATTRIBUTES[0]) 
									|| key.equalsIgnoreCase(LGLValues.KNOWN_ATTRIBUTES[1])
									|| key.equalsIgnoreCase(LGLValues.KNOWN_ATTRIBUTES[2])
									|| key.equalsIgnoreCase(LGLValues.KNOWN_ATTRIBUTES[3])) {
								// platzhalter, wenn schon was drin
								if (!text.equals("") && !value.equals("")) {
									text = text + TEXT_PLATZHALTER;
								}
								text = text + value;
								// Log.e(CLASSTAG, "Text is: " + text);
							}
							if (key.equalsIgnoreCase(LGLValues.LGL_IS_DONE_FLAG_ATTRIBUTE)) {
								Log.e(CLASSTAG, "Attribute " + key + " is similar to " + LGLValues.LGL_IS_DONE_FLAG_ATTRIBUTE);
								if (value.equals("1")) {
									// Log.e(CLASSTAG, "Setting Flag \"" + LGLValues.LGL_IS_DONE_FLAG_ATTRIBUTE + "\" to true.");
									featureWorkCopy.setDone(true);
								} else {
									// Log.e(CLASSTAG, "Setting Flag \"" + LGLValues.LGL_IS_DONE_FLAG_ATTRIBUTE + "\" to false.");
									featureWorkCopy.setDone(false);
								}
							}
						}
						if (currentLayer.getName().equalsIgnoreCase(LGLValues.LGL_OAK_LAYER_NAMES[0]) 
								|| currentLayer.getName().equalsIgnoreCase(LGLValues.LGL_OAK_LAYER_NAMES[1]) 
								|| currentLayer.getName().equalsIgnoreCase(LGLValues.LGL_OAK_LAYER_NAMES[2])) {
							// text einfuegen, zusammenfassung aller attribute
							contentVal.put(LGLValues.TEXT_ATTRIBUTE, text);
						}
						// Log.e(CLASSTAG, "Content-Val-Text is: " + text);
						ListIterator<Spinner> iterSpinners = spinners.listIterator();
						while (iterSpinners.hasNext()) {
							Spinner s = iterSpinners.next();
							// Log.i(CLASSTAG,	"Saving SpinnerItem with key " + s.getTag() + " as Value: "	+ s.getSelectedItemPosition());
							contentVal.put(s.getTag().toString(), s.getSelectedItemPosition());
						}
						ListIterator<DateTime> iterDates = dates.listIterator();
						while (iterDates.hasNext()) {
							DateTime d = iterDates.next();
							// Log.i(CLASSTAG,	"Saving DateItem with key " + d.getTag() + " as Value: " + d.toString());
							contentVal.put(d.getTag(), d.toString());
						}
						// Log.e(CLASSTAG, "old CVs: " + featureWorkCopy.getAttributes().toString());
						featureWorkCopy.setAttributes(contentVal);
						// Log.e(CLASSTAG, "new CVs: " + contentVal.toString());
						// Log.e(CLASSTAG, "new CVs of Feature: " + featureWorkCopy.getAttributes().toString());
						featureWorkCopy.setSync(false);
						drawingPanel.reloadFeaturesAndDraw();
						reLoadBar();
					}
				});
				dialog.show();
			}
		}));
	}

	// adds a Sync-Button 
	private void addSyncButton() {
		int id = 0;
		if (currentLayer.isSync()) {
			id = R.drawable.haken_grau;
			toolbarLayout.addView(new LeftMenuIconLayout(context, context.getString(R.string.sync)
					+ " " + currentLayer.getName(), STANDADRDCOLOR, id, 5, 5, new OnClickListener() {
				@Override
				public void onClick(View v) {
					v.startAnimation(iconAnimation);
					Alerts.errorMessage(context, context.getString(R.string.main_sync_result),
							context.getString(R.string.main_sync_nothing_to_sync) + " " + currentLayer.getName() + ".").show();
				}
			}));
		} else {
			id = R.drawable.ic_menu_manage_sync_false;
			toolbarLayout.addView(new LeftMenuIconLayout(context,
					context.getString(R.string.sync) + " " + currentLayer.getName(), STANDADRDCOLOR, id, 5, 5,
					new OnClickListener() {
				@Override
				public void onClick(View v) {
					v.startAnimation(iconAnimation);
					WFSLayerSynchronization sync = new WFSLayerSynchronization(v.getContext(), true, currentLayer);
					OnSyncFinishedListener syncFinishedListener = new OnSyncFinishedListener() {
						@Override
						public void onSyncFinished(boolean result, WFSLayer selectedLayer) {
							if (result) {
								Alerts.errorMessage(context, context.getString(R.string.main_sync_result),
										context.getString(R.string.main_sync_message)).show();
								changesMade = false;
								closeBar();
							} else {
								Alerts.errorMessage(context, context.getString(R.string.main_sync_result),
										context.getString(R.string.main_sync_error_message)).show();
								reLoadBar();
							}
						}
					};
					sync.setOnSyncFinishedListener(syncFinishedListener);
					sync.execute();
				}
			}));
		}

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
	public void setEditFeatureListener() {
		setOnNewFeatureListener(new OnNewFeatureListener() {
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

	// get coordinaates by gps
	private void getCoordByGPSFunction() {
		Log.d(CLASSTAG + " getCoordByGPSFunction()", "Status: " + String.valueOf(status));
		LocationFactory location = new LocationFactory(context, drawingPanel);
		location.setOnLocationByGPSListener(new OnLocationByGPSListener() {
			@Override
			public void onLocationByGPS(final Location location, final GpsStatus gpsStatus) {
				if (location == null) {
					Alerts.errorMessage(context, context.getString(R.string.error),
							context.getString(R.string.featureEditor_error_no_location_returned)).show();
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
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				});
				dialog.setPositiveButton(context.getString(R.string.yes),
						new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
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
						setStatus(STATUS_ADD);
						switch(type) {
						case WFSLayer.LAYER_TYPE_POINT:
							// Resete precison und füge neue Daten ein.
							precision.setForPoint(location, gpsStatus);
							createFeature(result);
							setStatus(STATUS_ADJUST);
							break;
						case WFSLayer.LAYER_TYPE_LINE:
							// Füge den neuen Punkt zunächst am Ende der Linie ein.
							createFeature(result);
							// Bearbeitet werden soll der Punkt #aktWorkingPointNum.
							if (line != null) {
								if (aktWorkingPointNum == line.getNumPoints() - 1) {
									// Es handelte sich wirklich um einen neuen Punkt
									precision.addEntry(location, gpsStatus, 0, aktWorkingPointNum);
								} else {
									// Es handelt sich um einen "alten Punkt", der bearbeitet werden soll
									line = Functions.replacePointWithLastPointofALineString(line, aktWorkingPointNum);
									precision.deleteEntry(0, aktWorkingPointNum);
									precision.addEntry(location, gpsStatus, 0, aktWorkingPointNum);
								}
							} else {
								// Es muss sich um den 1. Punkt der Linie handeln!
								precision.setForPoint(location, gpsStatus);
							}
							break;
						case WFSLayer.LAYER_TYPE_POLYGON:
							if (polygon == null) {
								polyIsFresh=true;
							} else {
								polyIsFresh=false;
								numInteriorBefore=polygon.getNumInteriorRing();
							}
							createFeature(result);
							if (polygon != null) {
								Log.d(CLASSTAG, String.valueOf(polygon.getNumInteriorRing()) + " InterriorRings found!");
								if (aktWorkingObjNum == 0) {
									aktNumPoints = polygon.getExteriorRing().getNumPoints();
								} else if (polygon.getNumInteriorRing()>=aktWorkingObjNum) {
									aktNumPoints = polygon.getInteriorRingN(aktWorkingObjNum - 1).getNumPoints();
								} else {
									aktNumPoints = 0;
								}
								if (aktWorkingPointNum == aktNumPoints - 2) {
									// Es handelte sich wirklich um einen neuen Punkt
									precision.addEntry(location, gpsStatus, aktWorkingObjNum, aktWorkingPointNum);
								} else if (polyIsFresh) {
									/* Achtung: Es wurde der erste oder zweite Punkt in einem noch-nicht-Polygon bearbeitet. Dadurch wurde
									 * nun ein Polygon erzeugt, was aber im nächsten Schritt wieder entfernt werden muss!*/
									firstPoint=polygon.getExteriorRing().getCoordinateN(0);
									secondPoint=polygon.getExteriorRing().getCoordinateN(2); // Der geänderte Punkt
									thirdPoint=null;
									polygon=null;
									// Aktualisiere Precision
									precision.deleteEntry(aktWorkingObjNum, aktWorkingPointNum);
									precision.addEntry(location, gpsStatus, aktWorkingObjNum, aktWorkingPointNum);
								} else if (aktNumPoints == 0) {
									// Es wurde der erste oder zweite Punkt eines InterriorRings neu hinzugefügt oder bearbeitet, während noch kein LinearRing vorliegt...
									precision.deleteEntry(aktWorkingObjNum, aktWorkingPointNum);
									precision.addEntry(location, gpsStatus, aktWorkingObjNum, aktWorkingPointNum);
								} else if (numInteriorBefore < polygon.getNumInteriorRing()) {
									// Es wurde gerade ein neuer interiorRIng erzeugt. Da aber kein neuer Punkt erzeugt wurde muss ein alter bearbeitet wurden sein...
									firstPoint=polygon.getInteriorRingN(aktWorkingObjNum-1).getCoordinateN(0);
									secondPoint=polygon.getInteriorRingN(aktWorkingObjNum-1).getCoordinateN(2); // Der geänderte Punkt
									thirdPoint=null;
									polygon = Functions.rmInterriorRing(polygon, aktWorkingObjNum-1);			// Lösche den interriorRing aus dem Polygon
									// Aktualisiere Precision
									precision.deleteEntry(aktWorkingObjNum, aktWorkingPointNum);
									precision.addEntry(location, gpsStatus, aktWorkingObjNum, aktWorkingPointNum);
								} else {
									// Es handelt sich um einen "alten Punkt", der bearbeitet werden soll
									polygon = Functions.replacePointWithLastPointofAPolygon(polygon, aktWorkingObjNum, aktWorkingPointNum);
									precision.deleteEntry(aktWorkingObjNum, aktWorkingPointNum);
									precision.addEntry(location, gpsStatus, aktWorkingObjNum, aktWorkingPointNum);
								}
							} else {
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
		case STATUS_ADJUST:
			point = new GeometryFactory().createPoint(pos);
			setStatus(STATUS_ADJUST);
			break;
		}
	}

	// creates a new line
	private void createLine(Coordinate pos) {
		switch (status) {
		case STATUS_ADJUST:
			if (line == null) {
				if (firstPoint == null && secondPoint == null) {
					firstPoint = pos;
					setStatus(STATUS_ADJUST);
					aktWorkingPointNum = 0;
				} else if (firstPoint != null && secondPoint == null) {
					secondPoint = pos;
					Coordinate[] newCoord = { firstPoint, secondPoint };
					line = new GeometryFactory().createLineString(newCoord);
					setStatus(STATUS_ADJUST);
					firstPoint = null;
					secondPoint = null;
					aktWorkingPointNum = 1;
				}
			} else {
				line = Functions.appendLineString(line, pos);
				setStatus(STATUS_ADJUST);
			}
			break;
		}
	}

	// Creates a new polygon 
	private void createPolygon(Coordinate pos) {
		switch (status) {
		case STATUS_ADJUST:
			if (polygon == null) {
				if (firstPoint == null && secondPoint == null && thirdPoint == null) {
					firstPoint = pos;
					setStatus(STATUS_ADJUST);
				} else if (firstPoint != null && secondPoint == null && thirdPoint == null) {
					secondPoint = pos;
					setStatus(STATUS_ADJUST);
				} else if (firstPoint != null && secondPoint != null && thirdPoint == null) {
					thirdPoint = pos;
					Coordinate[] newCoord = { firstPoint, secondPoint, thirdPoint };
					polygon = new GeometryFactory().createPolygon(Functions.coordinatesToLinearRing(newCoord), null);
					setStatus(STATUS_ADJUST);
					firstPoint = null;
					secondPoint = null;
					thirdPoint = null;
				}
			} else {
				if (polygonAddHole) {
					if (Functions.getPolygonLineStringN(polygon, selectedHole) == null) {
						if (firstPoint == null && secondPoint == null && thirdPoint == null) {
							firstPoint = pos;
							setStatus(STATUS_ADJUST);
						} else if (firstPoint != null && secondPoint == null && thirdPoint == null) {
							secondPoint = pos;
							setStatus(STATUS_ADJUST);
						} else if (firstPoint != null && secondPoint != null && thirdPoint == null) {
							thirdPoint = pos;
							Coordinate[] newCoord = { firstPoint, secondPoint, thirdPoint };
							polygon = Functions.addPolygonHole(polygon, newCoord);
							initMiddleMenu(MENU_EDIT_HOLES);
							setStatus(STATUS_ADJUST);
							firstPoint = null;
							secondPoint = null;
							thirdPoint = null;
						}
					} else {
						polygon = Functions.appendPolygonHole(polygon, selectedHole, pos);
						setStatus(STATUS_ADJUST);
					}
				} else {
					polygon = Functions.appendPolygonExterior(polygon, pos);
					setStatus(STATUS_ADJUST);
				}
			}
			break;
		}
	}

	/**
	 * Sets the status.
	 * STATUS_ADD_OR_ADJUST = 1;
	 * STATUS_ADJUST = 2;
	 * STATUS_END = 3;
	 *
	 * @param newStatus the new status
	 */
	private void setStatus(int newStatus) {
		this.status = newStatus;
		// Log.e(CLASSTAG + " setStatus", "Status of EditFeatureBar set to " + getStatusString(newStatus));
	}

	private String getStatusString(int newStatus) {
		switch (newStatus) {
		case STATUS_ADJUST:
			return "STATUS_ADJUST";
		case STATUS_ADD:
			return "STATUS_ADD";
		case STATUS_SAVED:
			return "STATUS_SAVED";
		default:
			return "unknown";
		}
	}

	// TODO: private! nur fuer log-ausgaben
	public String getStatusAsString() {
		return getStatusString(getStatus());
	}

	/** Returns the current creator status */
	public int getStatus() {
		return status;
	}

	/**
	 * Returns the type of feature that is in creation.
	 * Returns -1 if no feature is in creation.
	 *
	 * @return the type
	 */
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
					// Log.e(CLASSTAG, "Returning for Type " + type + " the coordinates: " + coords);
					return coords;
				}
				break;
			case WFSLayer.LAYER_TYPE_LINE:
				if (line != null) {
					coords = new Point[1][line.getNumPoints()];
					for (int i = 0; i < line.getNumPoints(); i++) {
						coords[0][i] = line.getPointN(i);
					}
					// Log.e(CLASSTAG, "Returning for Type " + type + " the coordinates: " + coords);
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
					// Log.e(CLASSTAG, "Returning for Type " + type + " the coordinates: " + coords);
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

	/**
	 * Moves existing point to new coordinate.
	 *
	 * @param idx the index of the existing point
	 * @param newCoordinate the new coordinate
	 */
	public void movePointTo(int[] idx, Coordinate newCoordinate) {
		Log.i(CLASSTAG, "Moving point with index " + idx[0] + " - " + idx[1]);
		changesMade = true;
		if (idx[0] == -1 && idx[1] == -1) {	
			aktWorkingPointNum = 0;
			// Nehme neue Koordinatena auf...
			firstPoint.setCoordinate(newCoordinate);
			// Genauigkeitsinformationen gehen verloren
			precision.deleteEntry(aktWorkingObjNum, aktWorkingPointNum);
		} else if (idx[0] == -2 && idx[1] == -2) {
			// Nur für Polygone relevant!
			aktWorkingPointNum=1;
			// Nehme neue Koordinaten auf
			secondPoint.setCoordinate(newCoordinate);
			// Genauigkeitsinformationen gehen verloren
			precision.deleteEntry(aktWorkingObjNum, aktWorkingPointNum);
		} else {
			switch (type) {
			case WFSLayer.LAYER_TYPE_POINT:
				// Log.e(CLASSTAG, "Point with Index: " + idx[0] + " - " + idx[1]);
				/* Der aktuelle Punkt wurde manuell editiert und verfügt nicht (mehr) über 
				 * Genauigkeitsinformationen - der komplette Container beinhaltet maximal 
				 * diesen Punkt und kann geleert werden! */
				if (!precision.isEmpty()) {
					precision.clear();
				}
				//Nehme neue Koordinaten auf...
				point.getCoordinate().setCoordinate(newCoordinate);
				break;
			case WFSLayer.LAYER_TYPE_LINE:
				// Log.e(CLASSTAG, "Line with Index: " + idx[0] + " - " + idx[1]);
				aktWorkingPointNum=idx[1];
				// Entferne Informationen über Genauigkeiten für den betroffenen Punkt
				precision.deleteEntry(0, aktWorkingPointNum);
				//Nehme neue Koordinatena auf...
				line.getCoordinateN(idx[1]).setCoordinate(newCoordinate);
				break;
			case WFSLayer.LAYER_TYPE_POLYGON:
				// Log.e(CLASSTAG, "Polygon with Index: " + idx[0] + " - " + idx[1]);
				aktWorkingPointNum=idx[1];
				if (idx[0] == 0) {
					// outer ring
					// Log.e(CLASSTAG, "First if!");
					aktWorkingObjNum=0;
					int lastPoint = polygon.getExteriorRing().getNumPoints() - 1;
					if (idx[1] == 0 || idx[1] == lastPoint) {
						// Log.e(CLASSTAG, "Second if!");
						aktWorkingPointNum=0;
						precision.deleteEntry(aktWorkingObjNum, aktWorkingPointNum);
						precision.deleteEntry(aktWorkingObjNum, lastPoint);
						polygon.getExteriorRing().getCoordinateN(0).setCoordinate(newCoordinate);
						polygon.getExteriorRing().getCoordinateN(lastPoint).setCoordinate(newCoordinate);
					} else {
						// Log.e(CLASSTAG, "Second ifs else!");
						aktWorkingPointNum=idx[1];
						precision.deleteEntry(aktWorkingObjNum, aktWorkingPointNum);
						polygon.getExteriorRing().getCoordinateN(idx[1]).setCoordinate(newCoordinate);
					}
				} else if (idx[0] > 0) {
					// inner rings (holes in polygons)
					// Log.e(CLASSTAG, "First ifs else!");
					aktWorkingObjNum=idx[0];								
					int lastPoint = polygon.getInteriorRingN(idx[0] - 1).getNumPoints() - 1;
					if (idx[1] == 0 || idx[1] == lastPoint) {
						aktWorkingPointNum=0;
						polygon.getInteriorRingN(idx[0] - 1).getCoordinateN(0).setCoordinate(newCoordinate);
						polygon.getInteriorRingN(idx[0] - 1).getCoordinateN(lastPoint).setCoordinate(newCoordinate);
						precision.deleteEntry(aktWorkingObjNum, aktWorkingPointNum);
						precision.deleteEntry(aktWorkingObjNum, lastPoint);
					} else {
						aktWorkingPointNum=idx[1];
						precision.deleteEntry(aktWorkingObjNum, aktWorkingPointNum);
						polygon.getInteriorRingN(idx[0] - 1).getCoordinateN(idx[1]).setCoordinate(newCoordinate);
					}
				}
				break;
			}
		}
	}

	private TableRow createRow(String attrName, int attrType,
			HashMap<String, String> restr, Feature featureWorkCopy2) {
		TableRow.LayoutParams params = new TableRow.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		TableRow row = new TableRow(context);
		row.setGravity(Gravity.CENTER_VERTICAL);
		TextView title = new TextView(context);
		title.setText(attrName + ":");
		title.setTextSize(16f);
		title.setPadding(PADDING, PADDING, PADDING, PADDING);
		row.addView(title, params);
		// falls attribute des features anders als die des layer nullpointer abfangen
		String featureValue = "";
		if (featureWorkCopy.getAttributes().get(attrName) != null) {
			featureValue = featureWorkCopy.getAttributes().get(attrName).toString();
		}
		// anhand des typs die textfelder setzen
		switch (attrType) {
		case WFSLayerAttributeTypes.INTEGER:
			EditText editInt = new EditText(context);
			editInt.setTag(attrName);
			editInt.setMinimumWidth(EDIT_TEXT_MINIMUM_WIDTH);
			editInt.setInputType(InputType.TYPE_CLASS_NUMBER
					| InputType.TYPE_NUMBER_FLAG_SIGNED);
			editInt.setPadding(PADDING, PADDING, PADDING, PADDING);
			editInt.setText(featureValue);
			editInt.setSelection(editInt.getText().length());
			row.addView(editInt, params);
			TextView resInt = new TextView(context);
			String textInt = "INTEGER";
			Iterator<String> iterInt = restr.keySet().iterator();
			while (iterInt.hasNext()) {
				String key = iterInt.next();
				textInt = textInt + "\n" + key + ": " + restr.get(key);
			}
			resInt.setText(textInt);
			resInt.setPadding(PADDING, PADDING, PADDING, PADDING);
			row.addView(resInt, params);
			edits.add(editInt);
			break;
		case WFSLayerAttributeTypes.MEASUREMENT:
			EditText editMeas = new EditText(context);
			editMeas.setTag(attrName);
			editMeas.setMinimumWidth(EDIT_TEXT_MINIMUM_WIDTH);
			editMeas.setInputType(InputType.TYPE_CLASS_NUMBER
					| InputType.TYPE_NUMBER_FLAG_SIGNED
					| InputType.TYPE_NUMBER_FLAG_DECIMAL);
			editMeas.setPadding(PADDING, PADDING, PADDING, PADDING);
			editMeas.setText(featureValue);
			editMeas.setSelection(editMeas.getText().length());
			row.addView(editMeas, params);
			TextView resMeas = new TextView(context);
			String textMeas = "MEASUREMENT";
			Iterator<String> iterMeas = restr.keySet().iterator();
			while (iterMeas.hasNext()) {
				String key = iterMeas.next();
				textMeas = textMeas + "\n" + key + ": " + restr.get(key);
			}
			resMeas.setText(textMeas);
			resMeas.setPadding(PADDING, PADDING, PADDING, PADDING);
			row.addView(resMeas, params);
			edits.add(editMeas);
			break;
		case WFSLayerAttributeTypes.STRING:
			EditText editStr = new EditText(context);
			editStr.setTag(attrName);
			editStr.setMinimumWidth(EDIT_TEXT_MINIMUM_WIDTH);
			editStr.setInputType(InputType.TYPE_CLASS_TEXT
					| InputType.TYPE_TEXT_VARIATION_NORMAL);
			editStr.setPadding(PADDING, PADDING, PADDING, PADDING);
			editStr.setText(featureValue);
			row.addView(editStr, params);
			TextView resStr = new TextView(context);
			String textStr = "STRING";
			Iterator<String> iterStr = restr.keySet().iterator();
			while (iterStr.hasNext()) {
				String key = iterStr.next();
				textStr = textStr + "\n" + key + ": " + restr.get(key);
			}
			resStr.setText(textStr);
			resStr.setPadding(PADDING, PADDING, PADDING, PADDING);
			row.addView(resStr, params);
			edits.add(editStr);
			break;
		case WFSLayerAttributeTypes.DATE:
			// doing nothing, just going into DATETIME, because of the missing BREAK:
		case WFSLayerAttributeTypes.DATETIME:
			DateTime dateTime = new DateTime(context, true, new Date(System.currentTimeMillis()));
			dateTime.setTag(attrName);
			LinearLayout linlayTime = dateTime.getView();
			linlayTime.setPadding(PADDING, PADDING, PADDING, PADDING);
			row.addView(linlayTime, params);
			TextView resDateTime = new TextView(context);
			String textDateTime = "DATETIME";
			Iterator<String> iterDateTime = restr.keySet().iterator();
			while (iterDateTime.hasNext()) {
				String key = iterDateTime.next();
				textDateTime = textDateTime + "\n" + key + ": "
						+ restr.get(key);
			}
			resDateTime.setText(textDateTime);
			resDateTime.setPadding(PADDING, PADDING, PADDING, PADDING);
			row.addView(resDateTime, params);
			dates.add(dateTime);
			break;
		case WFSLayerAttributeTypes.BOOLEAN:
			Spinner editBoolean = new Spinner(context);
			ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context, 
					R.array.bool, android.R.layout.simple_spinner_item);
			editBoolean.setAdapter(adapter);
			editBoolean.setTag(attrName);
			// TDOD: hier selection richtig auswaehlen lassen
			if (featureValue.equalsIgnoreCase("true") || featureValue.equalsIgnoreCase("1")) {
				editBoolean.setSelection(1);
			} else {
				editBoolean.setSelection(0);
			}				
			editBoolean.setPadding(PADDING, PADDING, PADDING, PADDING);
			row.addView(editBoolean, params);
			TextView textBoolean = new TextView(context);
			textBoolean.setText("BOOLEAN");
			textBoolean.setPadding(PADDING, PADDING, PADDING, PADDING);
			row.addView(textBoolean, params);
			spinners.add(editBoolean);
			break;
		case WFSLayerAttributeTypes.DOUBLE:
			EditText editDouble = new EditText(context);
			editDouble.setTag(attrName);
			editDouble.setMinimumWidth(EDIT_TEXT_MINIMUM_WIDTH);
			editDouble.setInputType(InputType.TYPE_CLASS_NUMBER
					| InputType.TYPE_NUMBER_FLAG_SIGNED
					| InputType.TYPE_NUMBER_FLAG_DECIMAL);
			editDouble.setPadding(PADDING, PADDING, PADDING, PADDING);
			editDouble.setText(featureValue);
			row.addView(editDouble, params);
			TextView resDouble = new TextView(context);
			String textDouble = "DOUBLE";
			Iterator<String> iterDouble = restr.keySet().iterator();
			while (iterDouble.hasNext()) {
				String key = iterDouble.next();
				textDouble = textDouble + "\n" + key + ": " + restr.get(key);
			}
			resDouble.setText(textDouble);
			resDouble.setPadding(PADDING, PADDING, PADDING, PADDING);
			row.addView(resDouble, params);
			edits.add(editDouble);
			break;
		case WFSLayerAttributeTypes.DECIMAL:
			EditText editDecimal = new EditText(context);
			editDecimal.setTag(attrName);
			editDecimal.setMinimumWidth(EDIT_TEXT_MINIMUM_WIDTH);
			editDecimal.setInputType(InputType.TYPE_CLASS_NUMBER
					| InputType.TYPE_NUMBER_FLAG_SIGNED
					| InputType.TYPE_NUMBER_FLAG_DECIMAL);
			editDecimal.setPadding(PADDING, PADDING, PADDING, PADDING);
			editDecimal.setText(featureValue);
			row.addView(editDecimal, params);
			TextView resDecimal = new TextView(context);
			String textDecimal = "DECIMAL";
			Iterator<String> iterDecimal = restr.keySet().iterator();
			while (iterDecimal.hasNext()) {
				String key = iterDecimal.next();
				textDecimal = textDecimal + "\n" + key + ": " + restr.get(key);
			}
			resDecimal.setText(textDecimal);
			resDecimal.setPadding(PADDING, PADDING, PADDING, PADDING);
			row.addView(resDecimal, params);
			edits.add(editDecimal);
			break;
		default:
			return new TableRow(context);
		}
		return row;
	}

	public Feature getfeatureCopy() {
		return this.featureWorkCopy;
	}

	public void drawBar() {
		this.toolbarLayout.setVisibility(View.VISIBLE);
	}

	private String getCurrentWKTString() {
		WKTWriter writer = new WKTWriter();
		switch (type) {
		case WFSLayer.LAYER_TYPE_POINT:
			if (currentLayer.isMultiGeom()) {
				return writer.write(toMultiGeom(point));
			} else {
				return writer.write(point);
			}
		case WFSLayer.LAYER_TYPE_LINE:
			if (currentLayer.isMultiGeom()) {
				return writer.write(toMultiGeom(line));
			} else {
				return writer.write(line);
			}
		case WFSLayer.LAYER_TYPE_POLYGON:
			if (currentLayer.isMultiGeom()) {
				return writer.write(toMultiGeom(polygon));
			} else {
				return writer.write(polygon);
			}
		default:
			return null;
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

	// Updates the project settings.
	private void updateProject(Project newProject) {
		currentProject = newProject;
		closeBar();
	}

	/**
	 * Adds the gps button.
	 */
	private void addGPSButton() {
		// GPS button erstellen
		// TODO uebersetzungen
		toolbarLayout.addView(new LeftMenuIconLayout(context, "GPS", STANDADRDCOLOR, R.drawable.ic_editor_gps, 
				5, 5, new OnClickListener() {
			@Override
			public void onClick(View v) {
				v.startAnimation(iconAnimation);
				getCoordByGPSFunction();
			}
		}));
	}

	/**
	 * Adds the enter coords button.
	 */
	private void addEnterCoordsButton() {
		// selbsteintragebutton erstellen
		toolbarLayout.addView(new LeftMenuIconLayout(context,
				context.getString(R.string.editfeaturebar_manually_coord_input), STANDADRDCOLOR,
				R.drawable.ic_editor_input, 5, 5, new OnClickListener() {
			@Override
			public void onClick(View v) {
				v.startAnimation(iconAnimation);
				// Create dialog
				final Dialog dialog = new Dialog(context);
				dialog.setTitle(context.getString(R.string.featureEditor_coordinate_input));
				dialog.setContentView(R.layout.coordinate_edit_dialog);
				// Define dialog elements
				final Spinner proj = (Spinner) dialog.findViewById(R.id.spinner_coordinput_crs);
				TableLayout table = (TableLayout) dialog.findViewById(R.id.edit_coordinate_tablelayout);
				editViewlistX = new ArrayList<EditText>();
				editViewlistY = new ArrayList<EditText>();
				// TODO uebersetungen
				for (int i = 0; i < featureWorkCopy.getCoordinatesFromLinestring().length; i++) {
					// TODO je nach koordinatensystem 
					TextView textX = new TextView(context);
					textX.setText("Breite: ");
					TextView textY = new TextView(context);
					textY.setText("Breite: ");
					EditText valueX = new EditText(context);
					EditText valueY = new EditText(context);
					valueX.setInputType(InputType.TYPE_CLASS_NUMBER
							| InputType.TYPE_NUMBER_FLAG_SIGNED
							| InputType.TYPE_NUMBER_FLAG_DECIMAL);
					valueY.setInputType(InputType.TYPE_CLASS_NUMBER
							| InputType.TYPE_NUMBER_FLAG_SIGNED
							| InputType.TYPE_NUMBER_FLAG_DECIMAL);
					valueX.setPadding(PADDING, PADDING, PADDING, PADDING);
					valueY.setPadding(PADDING, PADDING, PADDING, PADDING);
					valueX.setMinimumWidth(EDIT_TEXT_MINIMUM_WIDTH);
					valueY.setMinimumWidth(EDIT_TEXT_MINIMUM_WIDTH);
					valueX.setText(String.valueOf(featureWorkCopy.getCoordinatesFromLinestring()[i].x));
					valueY.setText(String.valueOf(featureWorkCopy.getCoordinatesFromLinestring()[i].y));
					TableRow xRow = new TableRow(context);
					xRow.removeAllViews();
					TableRow yRow = new TableRow(context);
					yRow.removeAllViews();
					TextView textNumber = new TextView(context);
					textNumber.setText("Koordinate " + (i + 1) + ": ");
					editViewlistX.add(valueX);
					editViewlistY.add(valueY);
					xRow.addView(textX);
					xRow.addView(valueX);
					yRow.addView(textY);
					yRow.addView(valueY);
					table.addView(textNumber);
					table.addView(xRow);
					table.addView(yRow);
				}
				Button cancel = (Button) dialog.findViewById(R.id.button_coordinput_cancel);
				Button set = (Button) dialog.findViewById(R.id.button_coordinput_set);
				// Set spinner adapter
				ArrayAdapter<CRS> adapter = new ArrayAdapter<CRS>(context, 
						android.R.layout.simple_spinner_item, Constants.getCRSList());
				proj.setAdapter(adapter);
				proj.setOnItemSelectedListener(new OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> av, View v, int item, long l) {
						//						CRS selected = (CRS) proj.getItemAtPosition(item);
						//						if (CRSType.getType(selected) == CRSType.Unit.DEGREE) {
						//							textX.setText(context.getString(R.string.lon) + ": ");
						//							textY.setText(context.getString(R.string.lat) + ": ");
						//						} else if (CRSType.getType(selected)==CRSType.Unit.METRIC) {
						//							textX.setText(context.getString(R.string.easting) + ": ");
						//							textY.setText(context.getString(R.string.northing) + ": ");
						//						} else {
						//							textX.setText("X: ");
						//							textY.setText("Y: ");
						//						}
					}
					@Override
					public void onNothingSelected(AdapterView<?> arg0) {
						//						textX.setText("X: ");
						//						textY.setText("Y: ");
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
						//						// Coordinate variables
						//						Coordinate lonlat = new Coordinate(0.0, 0.0);
						//						Coordinate result = new Coordinate(0.0, 0.0);
						//						// Set input projection
						//						CRS crs = (CRS) proj.getSelectedItem();
						//						Projection inputProj;
						//						if (CRSType.getType(crs) == CRSType.Unit.DEGREE) {
						//							inputProj = null;
						//							Log.v(CLASSTAG + " CoordinateChange", "Setting input projection to epsg: null?!");
						//						} else {
						//							inputProj = ProjectionFactory.getNamedPROJ4CoordinateSystem("epsg:"	+ crs.getCode());
						//							Log.v(CLASSTAG + " CoordinateChange", "Setting input projection to epsg: " + crs.getCode());
						//						}
						//						// Get entered coordinate
						//						Coordinate[] coords = new Coordinate[editViewlistX.size()];
						//						for (int i = 0; i < editViewlistX.size(); i++) {
						//							coords[i].x = Double.parseDouble(editViewlistX.get(i).getEditableText().toString());
						//							coords[i].y = Double.parseDouble(editViewlistY.get(i).getEditableText().toString());
						//							// Transform entered coordinate
						//							if (inputProj == null) {
						//								lonlat.x = coords[i].x;
						//								lonlat.y = coords[i].y;
						//							} else {
						//								lonlat = inputProj.inverseTransform(coords[i], lonlat);
						//								Log.e(CLASSTAG,	inputProj.getPROJ4Description()	+ " (epsg: " + inputProj.getEPSGCode() + ") ->" + "X : " 
						//										+ lonlat.x + "   Y : " + lonlat.y);
						//							}
						//							if (drawingPanel.getProjection().getEPSGCode() == 4326
						//									|| drawingPanel.getProjection().getEPSGCode() == 0) {
						//								result.x = lonlat.x;
						//								result.y = lonlat.y;
						//							} else {
						//								drawingPanel.getProjection().transform(lonlat, result);
						//								Log.v(CLASSTAG, drawingPanel.getProjection().getPROJ4Description() + " (epsg: "	+ drawingPanel
						//										.getProjection().getEPSGCode() + ") ->" + "X: " + result.x + "   Y: " + result.y);
						//							}
						//							
						//						}
						//						
						//						
						//						
						//						// Add transformed coordinate
						//						WKTWriter writer = new WKTWriter();
						//						String geom;
						//						switch (type) {
						//						case WFSLayer.LAYER_TYPE_POINT:
						//							geom = writer.write(point);
						//							break;
						//						case WFSLayer.LAYER_TYPE_LINE:
						//							geom = writer.write(line);
						//							break;
						//						case WFSLayer.LAYER_TYPE_POLYGON:
						//							geom = writer.write(polygon);
						//							break;
						//						default:
						//							return;
						//						}
						//						if (geom != null) {
						//						}
						//
						//						featureWorkCopy.setWKTGeometry(geom);
						//						createFeature(result);
						//						drawingPanel.moveTo(result);
						//						reLoadBar();
						//						// Close dialog
						dialog.cancel();
					}
				});
				dialog.show();
			}
		}));
	}

	private void addPhotoButton() {
		// Fotobutton in der FeatureBar
		toolbarLayout.addView(new LeftMenuIconLayout(context, "Foto", STANDADRDCOLOR, 
				R.drawable.ic_editor_takepicture, 10, 5, new OnClickListener() {
			@Override
			public void onClick(View v) {
				v.startAnimation(iconAnimation);
				// Uri captureImageUri = MediaCapture.takePicture((Activity)context);
				MediaCapture.takePicture((Activity)context);
			}
		}));
	}

	/**
	 * Saves created feature in internal database on given layer.
	 */
	private void saveCurrentFeature() {
		if (status == STATUS_SAVED || status == STATUS_ADJUST) {
			String geom = getCurrentWKTString();
			if (geom != null) {
				listener.onNewFeature(geom, type, layerId);
			} else {
				innerToolsLayout.removeAllViews();
				TextView error = new TextView(context);
				error.setText(context.getString(R.string.error + R.string.featureEditor_error_feature_cannot_save));
				innerToolsLayout.addView(error);
			}
			setStatus(STATUS_SAVED);
		} else {
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
		}
	}

	/**
	 * Sets the alert builder.
	 *
	 * @param geomType the new alert builder geomtype code
	 */
	private void setAlertBuilder(int geomType) {
		alertBuilder = new AlertDialog.Builder(context);
		if (geomType == WFSLayer.LAYER_TYPE_POINT) {
			alertBuilder.setMessage(context.getString(R.string.feature_editor_alert_point));
		} else if (geomType == WFSLayer.LAYER_TYPE_LINE) {
			alertBuilder.setMessage(context.getString(R.string.feature_editor_alert_line));
		} else if ((geomType == WFSLayer.LAYER_TYPE_POLYGON)) {
			alertBuilder.setMessage(context.getString(R.string.feature_editor_alert_polygon));
		}
		alertBuilder.setCancelable(false);
		alertBuilder.setNeutralButton("OK",	new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		alertBuilder.show();
	}

	/**
	 * Sets the alert builder.
	 *
	 * @param geomType the new alert builder geomtype code
	 */
	private void setSaveAlertBuilder(int geomType) {
		alertBuilder = new AlertDialog.Builder(context);
		if (geomType == WFSLayer.LAYER_TYPE_POINT) {
			alertBuilder.setMessage(context.getString(R.string.feature_editor_save_point));
		} else if (geomType == WFSLayer.LAYER_TYPE_LINE) {
			alertBuilder.setMessage(context.getString(R.string.feature_editor_save_line));
		} else if ((geomType == WFSLayer.LAYER_TYPE_POLYGON)) {
			alertBuilder.setMessage(context.getString(R.string.feature_editor_save_polygon));
		}
		alertBuilder.setCancelable(false);
		alertBuilder.setNeutralButton("OK",	new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		alertBuilder.show();
	}

}
