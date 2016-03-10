/**
 * Diese Klasse kümmert sich um den NavigationDrawer
 * 
 * @author sven weisker
 * @author Torsten Hoch
 */

package de.geotech.systems.navigationDrawer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.GpsStatus;
import android.location.Location;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.vividsolutions.jts.geom.Coordinate;

import de.geotech.systems.R;
import de.geotech.systems.drawing.DrawingPanelView;
import de.geotech.systems.layers.AddLayerActivity;
import de.geotech.systems.layers.LayerManagerActivity;
import de.geotech.systems.main.GeoTechMobileSettingsActivity;
import de.geotech.systems.main.MainActivity;
import de.geotech.systems.projects.ProjectHandler;
import de.geotech.systems.projects.ProjectManagerActivity;
import de.geotech.systems.utilities.LocationFactory;
import de.geotech.systems.utilities.LocationFactory.OnLocationByGPSListener;

public class NavigationDrawersManager {
	// the classtag
	final static String CLASSTAG = "NavigationDrawersManager ";
	// this drawers layout
	private DrawerLayout mDrawerLayout;
	// the panel view
	private DrawingPanelView drawingPanel;
	// zwei ausklappbare listviews
	private ListView mDrawerListRight;
	private ListView mDrawerListLeft;
	// toggle element
	private ActionBarDrawerToggle mDrawerToggle;
	// the activity
	private Activity act;
	// für GPS Abfragen
	private LocationFactory location;
	// Strings of titles for listViews
	private String[] mLeftTitles;
	private String[] mRightTitles;

	/**
	 * Instantiates a new navigation drawers manager.
	 *
	 * @param panel its panel view
	 */
	public NavigationDrawersManager(DrawingPanelView panel) {
		this.drawingPanel = panel;
		this.act = (Activity) panel.getContext();
		this.init();
		this.setActionToggle();
	}

	/**
	 * Initialisiert die NavigationDrawer: Einen für die linke Seite und einen für die rechte Seite
	 */
	private void init() {
		// initialisierung des Navigationdrawer
		mDrawerLayout = (DrawerLayout) act.findViewById(R.id.drawer_layout);
		// initialisiert den MenuAdapter
		MenuAdapter adapterLeft = new MenuAdapter(act);
		MenuAdapter adapterRight = new MenuAdapter(act);
		// Text der einzeilnen Items wird gesetzt
		// linker navigationdrawer
		mLeftTitles = act.getResources().getStringArray(R.array.menue_item_array);
		// rechter navigationdrawer
		mRightTitles = act.getResources().getStringArray(R.array.menue_tool_array);
		// menuicons werden gesetzt
		String[] menuIcons = act.getResources().getStringArray(R.array.ns_menu_items_icon);
		String[] toolIcons = act.getResources().getStringArray(R.array.ns_menu_tools_icon);
		mDrawerListRight = (ListView) act.findViewById(R.id.right_drawer);
		mDrawerListLeft = (ListView) act.findViewById(R.id.left_drawer);
		// setzt den schatten der beim öffnen des navdrawer entsteht
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
		// bewirkt dass man die navigationdrawer nur durch drücken eines buttons
		// öffnen kann
		mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
		// Linke Seite navdrawer
		int res1 = 0;
		for (String item : mLeftTitles) {
			int id_title = act.getResources().getIdentifier(item, "string", act.getPackageName());
			int id_icon = act.getResources().getIdentifier(menuIcons[res1], "drawable", act.getPackageName());
			MenuItemModel model = new MenuItemModel(id_title, id_icon);
			adapterLeft.addItem(model);
			res1++;
		}
		// Rechte Seite navdrawer
		int res2 = 0;
		for (String item : mRightTitles) {
			int id_title = act.getResources().getIdentifier(item, "string", act.getPackageName());
			int id_icon = act.getResources().getIdentifier(toolIcons[res2], "drawable", act.getPackageName());
			MenuItemModel model = new MenuItemModel(id_title, id_icon);
			adapterRight.addItem(model);
			res2++;
		}
		mDrawerListLeft.setAdapter(adapterLeft);
		mDrawerListRight.setAdapter(adapterRight);
		mDrawerListLeft.setOnItemClickListener(new DrawerItemClickListenerLeft());
		mDrawerListRight.setOnItemClickListener(new DrawerItemClickListenerRight());
		// Homebutton wird aktiviert
		act.getActionBar().setDisplayHomeAsUpEnabled(true);
		act.getActionBar().setHomeButtonEnabled(true);
	}

	// Kümmert sich um die Action, wenn ein Drawer geöffnet oder geschlossen wird
	private void setActionToggle() {
		mDrawerToggle = new ActionBarDrawerToggle(act, mDrawerLayout, R.drawable.ic_drawer, R.string.app_name, R.string.app_name) {
			@Override
			public void onDrawerClosed(View view) {
				String name = null;
				if (ProjectHandler.getCurrentProject() != null) {
					name = ProjectHandler.getCurrentProject().getProjectName();
					act.getActionBar().setTitle(name);
				} else {
					act.getActionBar().setTitle(act.getString(R.string.app_name));
				}
				// close the RIGHT AND LEFT drawer
				mDrawerLayout.closeDrawer(mDrawerListRight);
				mDrawerLayout.closeDrawer(mDrawerListLeft);
				drawingPanel.invalidate();
			}

			@Override
			public void onDrawerOpened(View drawerView) {
				// if one drawer is started, stop catching the gps signal for
				//				// holding gps function
				//				if (location != null) {
				//					Log.d("setActionToogel()", "One Drawer opened! Stop GPSing!");
				//					location.stopGPSing();
				//				}
				if (ProjectHandler.getCurrentProject() != null) {
					act.getActionBar().setTitle(
							act.getString(R.string.app_name) + " - " + act.getString(R.string.project) + " \""
									+ ProjectHandler.getCurrentProject().getProjectName() + "\"");

				} else {
					act.getActionBar().setTitle(
							act.getString(R.string.app_name) + " - " + act.getString(R.string.project) + " "
									+ act.getString(R.string.project_no_project_chosen));
				}
			}
		};
		mDrawerLayout.setDrawerListener(mDrawerToggle);
	}

	/**
	 * bestimmt was passiert wenn man ein Item im LINKEN Navigationdrawer drückt, funktionserklärungen teilweise siehe code
	 * rechter drawer
	 */
	private void selectItemLeft(int position, View v) {
		String project = null;
		// nur wenn ein projekt gewählt ist den namen oben anzeigen
		if (ProjectHandler.getCurrentProject() != null) {
			project = ProjectHandler.getCurrentProject().getProjectName();
		}
		// je nachdem welche Positionnummer gewaehlt wurde
		switch (position) {
		case 0:
			// Projektverwaltung
			Intent project_manager = new Intent(act, ProjectManagerActivity.class);
			act.startActivityForResult(project_manager, MainActivity.PROJECTMANAGERACTIVITY);
			// close add-feature bar
			drawingPanel.getAddFeatureToolbar().closeBar();
			break;
		case 1:
			// Server- und Layerverwaltung-Button
			if (project == null) {
				askLoadProject(v);
			} else {
				Intent addlayeractivity = new Intent(act, AddLayerActivity.class);
				addlayeractivity.putExtra("de.geotech.systems.project", project);
				act.startActivityForResult(addlayeractivity, MainActivity.ADDLAYERACTIVITY);
			}
			break;
			//		case 2:
			//			// Sperren/Lock-Verwaltung
			//			if (project == null) {
			//				askLoadProject(v);
			//			} else {
			//				Intent addLockedLayerActivity = new Intent(act, LockWFSLayerActivity.class);
			//				addLockedLayerActivity.putExtra("de.geotech.systems.project", project);
			//				act.startActivityForResult(addLockedLayerActivity, MainActivity.LOCKWFSLAYERACTIVITY);
			//			}
			//			break;
		case 2:
			// Einstellungs Activity
			if (project == null) {
				askLoadProject(v);
			} else {
				Intent GeoTechMobileSettingsActivity = new Intent(act, GeoTechMobileSettingsActivity.class);
				GeoTechMobileSettingsActivity.putExtra("de.geotech.systems.project", project);
				act.startActivityForResult(GeoTechMobileSettingsActivity, MainActivity.GEOTECHMOBILESETTINGSACTIVITY);
			}
			break;
		case 3:
			// Hilfe-Button
			Builder helpDialog = new AlertDialog.Builder(act);
			LinearLayout alertDialogLayout = new LinearLayout(act);
			ImageView helpLineImage = new ImageView(act);
			helpLineImage.setImageResource(R.drawable.helpline);
			WebView contactView = new WebView(act);
			contactView.setBackgroundColor(Color.WHITE);
			WebSettings contactViewSettings = contactView.getSettings();
			contactViewSettings.setDefaultTextEncodingName("UTF-8");
			String contactHtml = act.getString(R.string.help_button);
			contactView.loadData(contactHtml, "text/html; charset=utf-8", null);
			alertDialogLayout.addView(helpLineImage);
			alertDialogLayout.addView(contactView);
			helpDialog.setView(alertDialogLayout);
			helpDialog.setCancelable(false);
			helpDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			});
			helpDialog.show();
			break;
		
		}
		// close the RIGHT AND LEFT drawer
		mDrawerLayout.closeDrawer(mDrawerListRight);
		mDrawerLayout.closeDrawer(mDrawerListLeft);
	}

	/**
	 * Bestimmt, was passiert wenn man ein Item im RECHTEN Navigationdrawerdrückt
	 */
	private void selectItemRight(int position, View v) {
		String project = null;
		if (ProjectHandler.getCurrentProject() != null)
			project = ProjectHandler.getCurrentProject().getProjectName();
		// Der Position im Array in res/values/array_navigation_drawer.xml
		// zugeordnet... geht sicher besser ueber Zuordnung der Namen
		// der Buttons
		switch (position) {
		case 0: {
			// Layer laden-Button (Position 0 im Array)
			if (project != null) {
				Intent layer_manager = new Intent(v.getContext(), LayerManagerActivity.class);
				layer_manager.putExtra("de.geotech.systems.project", project);
				act.startActivityForResult(layer_manager, MainActivity.LAYERMANAGERACTIVITY);
			} else {
				askLoadProject(v);
			}
			break;
		}
		//		case 1: {
		//			// GPS-Position-Button
		//			if (drawingPanel == null) {
		//				Log.i(CLASSTAG, "No DrawingPanel.");
		//			} else {
		//				location = new LocationFactory(v.getContext(), drawingPanel);
		//				location.setOnLocationByGPSListener(new OnLocationByGPSListener() {
		//					@Override
		//					public void onLocationByGPS(final Location location, final GpsStatus gpsStatus) {
		//						drawingPanel.moveTo(drawingPanel.transformToMapCRS(new Coordinate(location.getLongitude(), location
		//								.getLatitude())));
		//					}
		//				});
		//				location.getCoordinateByGPS();
		//			}
		//			break;
		//		}
		case 1: {
			// Zoom to Bounds-Button
			if (drawingPanel == null) {
				Log.i(CLASSTAG, "No DrawingPanel.");
			} else {
				drawingPanel.zoomToOutestBounds();
			}
			break;
		}
		case 2: {
			// Features hinzufügen-Button
			if (drawingPanel == null) {
				Log.i(CLASSTAG, "No DrawingPanel.");
			} else {
				if (project != null) {
					if (!ProjectHandler.getCurrentProject().getWFSContainer().isEmpty()) { 
						drawingPanel.getAddFeatureToolbar().openBar();
						
						drawingPanel.invalidate();
					} else {
						askLoadLayer(v);
					}
				} else {
					askLoadProject(v);
				}
			}
			break;
		}
		case 3: {
			// an Position 4 im Array: GPS Position halten-Button
			// if there is no drawing panel do nothing
			if (drawingPanel == null) {
				Log.i(CLASSTAG, "No DrawingPanel.");
				break;
			} else	if (location != null) {
				Log.d(CLASSTAG + " Case 3", "Stop GPSing!");
				location.stopGPSing();
				location = null;
			} else {
				// initialize locationfactory to handle GPS signal
				location = new LocationFactory(v.getContext(), drawingPanel);
				// listen and zoom to new measured GPS position of the locationfactory
				location.setOnLocationByGPSListener(new OnLocationByGPSListener() {
					// every time the locationfactory-listener reports a new position
					// zoom to this new position
					@Override
					public void onLocationByGPS(final Location location, final GpsStatus gpsStatus) {
						drawingPanel.moveTo(drawingPanel.transformToMapCRS(new Coordinate(location.getLongitude(), location
								.getLatitude())));
						drawingPanel.drawGPSCoordinate(new Coordinate(location.getLongitude(), location.getLatitude()));
					}
				});
				// starting the locationfactory
				location.holdGPSCoordinates();
			}
			break;
		}
		case 4:
			// :::::::::::::::::::::::::::::::::::::::::::::::::::::::::://///
			// hier funktion wenn emergency button gedrueckt wird
			// ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::////
			
			if(drawingPanel.getKrigingToolbar().openBar())
				drawingPanel.setKrigingMode(true);
			
		
			drawingPanel.invalidate();
			/*Builder test = new AlertDialog.Builder(act);
			LinearLayout alerttest = new LinearLayout(act);
			ImageView testimage = new ImageView(act);
			testimage.setImageResource(R.drawable.helpline);
			WebView testcontact = new WebView(act);
			testcontact.setBackgroundColor(Color.WHITE);
			WebSettings testset = testcontact.getSettings();
			testset.setDefaultTextEncodingName("UTF-8");
			String testhtml = act.getString(R.string.help_button);
			testcontact.loadData(testhtml, "text/html; charset=utf-8", null);
			alerttest.addView(testimage);
			alerttest.addView(testcontact);
			test.setView(alerttest);
			test.setCancelable(false);
			test.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			});
			test.show();*/
			break;
			case 5:
				new VehicleDeviceTrigger();
		//		case 5: {
		//			// Position 5 im Array: Bearbeite Feature-Button
		//			if (project != null) {
		//				Intent layer_manager = new Intent(v.getContext(), LockedWFSManagerActivity.class);
		//				layer_manager.putExtra("de.geotech.systems.project", project);
		//				act.startActivityForResult(layer_manager, MainActivity.FEATURESELECTACTTIVITY);
		//			} else {
		//				askLoadProject(v);
		//			}
		//			break;
		//		}
		}
		// close the RIGHT AND LEFT drawer
		mDrawerLayout.closeDrawer(mDrawerListRight);
		mDrawerLayout.closeDrawer(mDrawerListLeft);
	}

	// öffnet ein Pop-Up zur Auswahl eines Layers
	private void askLoadLayer(final View v) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(v.getContext());
		dialog.setTitle(act.getString(R.string.main_ask_load_layer_title));
		dialog.setMessage(act.getString(R.string.main_adk_load_layer_message));
		dialog.setNegativeButton(act.getString(R.string.no), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		dialog.setPositiveButton(act.getString(R.string.yes), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// Start projects activity
				Intent layer_manager = new Intent(v.getContext(), AddLayerActivity.class);
				act.startActivityForResult(layer_manager, MainActivity.ADDLAYERACTIVITY);
				dialog.cancel();
			}
		});
		dialog.show();
	}

	// öffnet ein Pop-Up zur Auswahl eines Projektes
	private void askLoadProject(final View v) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(v.getContext());
		dialog.setTitle(act.getString(R.string.main_title_first_load_project));
		dialog.setMessage(act.getString(R.string.main_message_load_project));
		dialog.setNegativeButton(act.getString(R.string.no), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		dialog.setPositiveButton(act.getString(R.string.yes), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// Start projects activity
				Intent project_manager = new Intent(v.getContext(), ProjectManagerActivity.class);
				act.startActivityForResult(project_manager, MainActivity.PROJECTMANAGERACTIVITY);
				dialog.cancel();
			}
		});
		dialog.show();
	}

	// getter und setter
	public DrawerLayout getmDrawerLayout() {
		return mDrawerLayout;
	}

	public ActionBarDrawerToggle getmDrawerToggle() {
		return mDrawerToggle;
	}

	public ListView getmDrawerListRight() {
		return this.mDrawerListRight;
	}

	/**
	 * Bestimmt, welche action beim auswählen eines MenueItems ausgeführt wird Listener für LINKEN Drawer
	 */
	private class DrawerItemClickListenerRight implements ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			selectItemRight(position, view);
		}
	}

	/**
	 * Bestimmt, welche action beim auswählen eines MenüItems ausgeführt wird Listener für LINKEN Drawer
	 */
	private class DrawerItemClickListenerLeft implements ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			selectItemLeft(position, view);
		}
	}

}
