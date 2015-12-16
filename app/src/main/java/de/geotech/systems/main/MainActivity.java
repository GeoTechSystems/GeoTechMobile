/**
 * The Main Activity
 * 
 * @author sven weisker
 * @author Torsten Hoch
 * @author tubatubsen
 * @author bschm
 */

package de.geotech.systems.main;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.osmdroid.views.MapView;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.LinearLayout;

import de.geotech.systems.R;
import de.geotech.systems.drawing.DrawingPanelView;
import de.geotech.systems.navigationDrawer.NavigationDrawersManager;
import de.geotech.systems.projects.Project;
import de.geotech.systems.projects.ProjectHandler;
import de.geotech.systems.utilities.Alerts;
import de.geotech.systems.utilities.NetworkStateReceiver;
import de.geotech.systems.utilities.NetworkStateReceiver.OnNetworkStateChangedListener;
import de.geotech.systems.wfs.WFSLayer;
import de.geotech.systems.wfs.WFSLayerSynchronization;
import de.geotech.systems.wfs.WFSLayerSynchronization.OnSyncFinishedListener;
import de.geotech.systems.wfs.WFSLayerSynchronization.OnSyncStartListener;
import de.geotech.systems.wms.WMSProgressAnimationManager;
import de.geotech.systems.wms.WMSOverlay;

public class MainActivity extends Activity {
	private static final String CLASSTAG = "MainActivity";
	private static final String NOTIFICATION_SYNC_TAG = "de.geotech.systems.autosync";
	private static final String LASTUSEDPROJECTNAMESTRING = "de.geotech.systems.lastProjectLoaded";
	private static final int NOTIFICATION_SYNC_ID = 0;
	// Activity request codes
	public static final int PROJECTMANAGERACTIVITY = 0;
	public static final int LAYERMANAGERACTIVITY = 1;
	public static final int LOCKWFSLAYERACTIVITY = 2;
	public static final int ADDLAYERACTIVITY = 3;
	public static final int GEOTECHMOBILESETTINGSACTIVITY = 4;
	public static final int LOCKEDWFSMANAGERACTIVITY = 5;
	public static final int FEATUREATTRIBUTESCREATORACTIVITY = 6;
	// Activities that do not report directly to main 
	public static final int ELEMENTFILTERACTIVITY = 101;
	public static final int FEATUREDETAILSACTIVITY = 102;
	public static final int FEATURESELECTACTTIVITY = 103;

	// the context
	private Context context;
	// current project name
	private String lastUsedProjectName;
	// Connectivity
	private ConnectivityManager connectivityManager;
	// the drawing panel
	public DrawingPanelView drawingPanel;
	// Navigation-Drawer-Layout
	private NavigationDrawersManager navigationDrawer;
	// shared preferences
	private SharedPreferences prefs;
	// OSM map as mapview of osmdoid - TODO raus hier!! in drawingpanel!
	private MapView osm;
	// WMS Overlay - TODO raus hier!! in drawingpanel!
	private WMSOverlay wmsOverlay;
	// the toolbars layout - TODO raus hier!! in drawingpanel!
	private LinearLayout toolbarLayout;

	// Called on activity start-up
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.context = this;
		this.startAndDisplayProperties();
		this.setContentView(R.layout.main);
		this.prefs = getPreferences(MODE_PRIVATE);
		this.setNetworkListener();
		this.connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		// Liste der Projekte wird in Arbeitsspeicher geladen
		ProjectHandler.loadProjectList(this);
		// get last used projects name
		this.lastUsedProjectName = prefs.getString(LASTUSEDPROJECTNAMESTRING, null);
		// Log.i(CLASSTAG, "Last used Project String read: \"" + lastUsedProjectName + "\"");
		// wenn name gegeben, projekt als aktuell setzen
		if (lastUsedProjectName != null) {
			if (ProjectHandler.setCurrentProject(this.lastUsedProjectName)) {
				// set projects name as headline
				this.setTitle(ProjectHandler.getCurrentProject().getProjectName());
				// Log.i(CLASSTAG, "Last used Project \"" + lastUsedProjectName + "\" set as current project.");
			} else {
				// set app name as headline
				this.setTitle(R.string.app_name);
				Log.i(CLASSTAG, "The last used Project " + lastUsedProjectName + " is unavailable.");
			} 
		} else {
			Log.i(CLASSTAG, "No last used Project Name given. Initializing Bounds.");
		} 
		// the OSM mapview for the panelview
		this.osm = (MapView) findViewById(R.id.OSMMapView);
		// View for the panel
		this.drawingPanel = (DrawingPanelView) findViewById(R.id.drawingPanel); 
		// set the toolbars layout 
		this.toolbarLayout = (LinearLayout) findViewById(R.id.toolbar_bottom);
		// initialize the drawingpanelview
		this.drawingPanel.initDrawingPanelView(osm, prefs, toolbarLayout);

		// load drawing panel
		this.drawingPanel.startFirstPanelDrawing();
		// initialize the navigation drawer
		this.navigationDrawer = new NavigationDrawersManager(drawingPanel);
		// instantiate WMS
		if (this.wmsOverlay == null) {
			// wms overlay initialisieren
			this.wmsOverlay = new WMSOverlay(new WMSProgressAnimationManager(this), drawingPanel);
			this.drawingPanel.setWMSOverlay(this.wmsOverlay);
		}
		// wenn aktuelles projekt geladen werden konnte - WMS checken und ggf. laden
		if (lastUsedProjectName != null) {
			// setze project und lade WMS Overlay-layer
			this.drawingPanel.updateWMSOverlay();
		} else {
			// wenn kein aktuelles projekt geladen - keinen WMS laden
		}
	}

	/** 
	 * Called on return from another activity 
	 * 
	 * Methode wird aufgerufen, wenn die MainActivity nach dem Beenden einer von 
	 * ihr aufgerufenen Activity wieder aufgerufen wird. 
	 * Es gibt mehrere moegliche Activities, die hier unterschieden werden muessen: 
	 * z.B.: requestCode = 0 -> zurueck von der ProjectManagerActivity
	 * Der requestcode wird fuer alle Activities hinterlegt durch
	 * bspw. 	Intent returnToMain = new Intent();
	 * 			returnToMain.putExtra("de.geotech.systems.onlyBack", "true");
	 * 			returnToMain.putExtra("de.geotech.systems.project", projectName);
	 * 			setResult(RESULT_OK, returnToMain);
	 * 			startActivityForResult(returnToMain,
	 * 						MainActivity.RC_ELEMENT_SELECTOR);           ) 
	 * 
	 * @param requestCode bestimmt welche Activity zuletzt ausgefuehrt wurde
	 * @param resultCode gesetzter Code der letzten Activity
	 * @param intentData Intent-Extra-Daten, die ins Intent geschrieben wurden
	 * 
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intentData) {
		super.onActivityResult(requestCode, resultCode, intentData);

		// requestCode - welche activity reported zurück?!
		Log.e(CLASSTAG +" onActivityResult", "RequestCode: " + requestCode + " - ResultCode: " + resultCode);
		switch (requestCode) {
		case PROJECTMANAGERACTIVITY:
			Log.i(CLASSTAG, "Back from ProjectManagerActivity!");
			backFromProjectManager(resultCode, intentData);
			break;
		case LAYERMANAGERACTIVITY:
			Log.i(CLASSTAG, "Back from LayerManagerActivity!");
			backFromLayerManager(resultCode, intentData);
			break;
		case LOCKWFSLAYERACTIVITY:
			Log.i(CLASSTAG, "Back from LockWFSLayerActivity!");
			// nothing to do, no features could have been loaded
			break;
		case ADDLAYERACTIVITY:
			Log.i(CLASSTAG, "Back from AddLayerActivity!");
			// maybe a WMS Overlay has been addded
			drawingPanel.updateWMSOverlay();
			// nothing to do, no features could have been loaded
			break;
		case GEOTECHMOBILESETTINGSACTIVITY:
			Log.i(CLASSTAG, "Back from GeoTechMobileSettingsActivity!");
			backFromGeoTechMobileSettings(resultCode, intentData);
			break;		
		case LOCKEDWFSMANAGERACTIVITY:
			Log.i(CLASSTAG, "Back from LockedWFSManagerActivity!");
			// backFromLayerManager works here too
			backFromLayerManager(resultCode, intentData);
			break;
		case FEATUREATTRIBUTESCREATORACTIVITY:
			Log.i(CLASSTAG, "Back from FeatureAttributesCreatorActivity!");
			backFromFeatureAttributesCreator(resultCode, intentData);
			break;
		default: 
			break;
		}
	}

	// wenn zurueck vom project manager
	private void backFromProjectManager(int resultCode, Intent intentData) {
		// Wenn ein Projekt geladen wurde
		if (resultCode == RESULT_OK) {
			// project setzen und laden
			lastUsedProjectName = ProjectHandler.getCurrentProject().getProjectName();
			Log.i(CLASSTAG, "Loading last used project: " + lastUsedProjectName);
			drawingPanel.showCurrentProject(true);
			prefs.edit().putString(LASTUSEDPROJECTNAMESTRING, lastUsedProjectName).commit();
			prefs.edit().putInt("de.geotech.systems.drawingPanelWidth", drawingPanel.getWidth()).commit();
			prefs.edit().putInt("de.geotech.systems.drawingPanelHeight", drawingPanel.getHeight()).commit();
		} else if (resultCode == RESULT_CANCELED) {
			// cancel button gedrueckt, evtl. aktuelles projekt geloescht
			if (!ProjectHandler.isCurrentProjectSet()) {
				prefs.edit().putString(LASTUSEDPROJECTNAMESTRING, null).commit();
				drawingPanel.zoomToOutestBounds();
			}
		}
	}

	// wenn zurueck vom layer manager
	private void backFromLayerManager(int resultCode, Intent intentData) {
		if (resultCode == RESULT_OK) {
			// Fly to some location
			if (intentData.getBooleanExtra("de.geotech.systems.fly", false) == true) {
				long flyID = Long.parseLong(intentData.getStringExtra("de.geotech.systems.flyID"));
				long layerID = Long.parseLong(intentData.getStringExtra("de.geotech.systems.flyLayerID"));
				String coordStr = intentData.getStringExtra("de.geotech.systems.flyCoord");
				drawingPanel.flyTo(flyID, layerID, coordStr);
				Log.i(CLASSTAG + " backFromLayerManager", "Flown to: " + String.valueOf(flyID) + " of Layer " + String.valueOf(layerID) + ". Coordinates: " + coordStr);
				if (intentData.getBooleanExtra("de.geotech.systems.editfeature", false) == true) {
					drawingPanel.getEditFeatureToolbar().openBar(flyID, layerID);
					Log.i(CLASSTAG + " backFromLayerManager", "Should open EditFeatureBar!!!!!!!!!!!!!!!!!!!!!!!!!");
				}
				drawingPanel.invalidate();
			} else {
				// wenn back button gedrueckt
			}
		} 
		// egal ob RESULT_OK oder anderes: falls etwas geaendert wurde
		if ((intentData.getBooleanExtra("de.geotech.systems.imported", false) == true)
				|| (intentData.getBooleanExtra("de.geotech.systems.anychange", false) == true)) {
			// Projekt neu laden
			drawingPanel.invalidate();
			drawingPanel.updateWMSOverlay();
			// .showCurrentProject(false);
		}
	}

	// back from geotechmobile results
	private void backFromGeoTechMobileSettings(int resultCode, Intent intentData) {
		if (resultCode == RESULT_OK) {
			// eigentlich nicht moeglich, wird in der activity nicht gesetzt
		} else if (resultCode == RESULT_CANCELED) {
			// falls etwas geaendert wurde
			if (intentData.getBooleanExtra("de.geotech.systems.anychange", false)) {
				drawingPanel.showCurrentProject(false);
			} else {
				// nichts geaendert - nichts zu tun
				// TODO - gut als Testzone

				Log.e(CLASSTAG, "Es sind momentan " +ProjectHandler.getCurrentProject().getWMSContainer().size() + " WMS-Layer im Container.");




			}
		}
	}

	// back from attribute editor
	private void backFromFeatureAttributesCreator(int resultCode, Intent intentData) {
		// attribute fuer ein feature wurden in maske eingetragen
		// und speichern button wurde gedrueckt
		if (resultCode == RESULT_OK) {
			ProjectHandler.getCurrentProject().setSync(false);
			drawingPanel.addWktToBounds(
					intentData.getStringExtra("de.geotech.systems.editor.geom"),
					intentData.getIntExtra("de.geotech.systems.editor.type", -1));
			//Synchronisiere den Punkt
			NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
			if (ProjectHandler.getCurrentProject().getAutoSync() == Project.AUTO_SYNC_ON
					&& networkInfo != null
					&& networkInfo.isConnectedOrConnecting()) {
				WFSLayer tempLayer = ProjectHandler.getCurrentProject().getCurrentWFSLayer();
				WFSLayerSynchronization sync = new WFSLayerSynchronization(this, false, tempLayer);
				sync.setOnSyncStartListener(new OnSyncStartListener() {
					@Override
					public void onSyncStart() {
						startSyncNotification();
					}
				});
				sync.setOnSyncFinishedListener(new OnSyncFinishedListener() {
					@Override
					public void onSyncFinished(boolean result, WFSLayer selectedLayer) {
						stopSyncNotification();
						if (result) {
//							ProjectHandler.getCurrentProject().setSync(true);
						} else {
							AlertDialog.Builder alert = new AlertDialog.Builder(context);
							// TODO Uebersetzungen
							alert.setTitle("Feature Sync");
							alert.setMessage(getString(R.string.main_sync_error_message));
							alert.setNegativeButton(R.string.ok,
									new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.cancel();
								}
							});
						}
					}
				});
				sync.execute();
			}
			Log.i(CLASSTAG, "Feature saved. Reloading editor...");
			// Toolbar wieder neu laden und project neu laden
			drawingPanel.getAddFeatureToolbar().reloadCreator();
			// drawingPanel.showCurrentProject(false);
			switch (intentData.getIntExtra("de.geotech.systems.editor.type", -1)) {
			case WFSLayer.LAYER_TYPE_POINT:
				Alerts.errorMessage(context, context.getString(R.string.feature_editor_save_title),
						context.getString(R.string.feature_editor_save_point)).show();
				break;
			case WFSLayer.LAYER_TYPE_LINE:
				Alerts.errorMessage(context, context.getString(R.string.feature_editor_save_title),
						context.getString(R.string.feature_editor_save_line)).show();
				break;
			case WFSLayer.LAYER_TYPE_POLYGON:
				Alerts.errorMessage(context, context.getString(R.string.feature_editor_save_title),
						context.getString(R.string.feature_editor_save_polygon)).show();
				break;
			}
		} else {
			if (resultCode == RESULT_CANCELED) {
				// speichern-button wurde nicht geklickt
			}
		}
	}

	// Called when menu bar item is selected
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		//	drawingPanel.getThread().cleanSurface();
		drawingPanel.cleanSurface();
		// Wenn Der Homebutton GeoTechMobile gedrueckt wird oeffnet sich der linke
		// DrawerLayer
		if (navigationDrawer.getmDrawerToggle().onOptionsItemSelected(item)) {
			return true;
		}
		switch (item.getItemId()) {
		case R.id.navigation_drawer_test_left:
			// Wenn der HILFE-Button in der Settingsleiste rechts-oben gedrueckt
			// wird, oeffnet oder schliesst sich der rechte DrawerLayer
			if (navigationDrawer.getmDrawerLayout().isDrawerOpen(
					navigationDrawer.getmDrawerListRight()))
				navigationDrawer.getmDrawerLayout().closeDrawer(
						navigationDrawer.getmDrawerListRight());
			else {
				navigationDrawer.getmDrawerLayout().openDrawer(
						navigationDrawer.getmDrawerListRight());
			}
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	// gehört zum DrawerLayer
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		navigationDrawer.getmDrawerToggle().syncState();
	}

	// gehört zum DrawerLayer
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// Pass any configuration change to the drawer toggls
		navigationDrawer.getmDrawerToggle().onConfigurationChanged(newConfig);
	}

	// Called on start-up creating the menu bar
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	/**
	 * Sets the network listener.
	 */
	private void setNetworkListener() {
		NetworkStateReceiver stateReceiver = new NetworkStateReceiver();
		stateReceiver.setOnNetworkStateChangedListener(
				new OnNetworkStateChangedListener() {
					@Override
					public void onNetworkStateChanged(boolean connected) {
						if (ProjectHandler.getCurrentProject() != null && !ProjectHandler.
								getCurrentProject().synchronize() && connected) {
							WFSLayerSynchronization sync = new WFSLayerSynchronization(getApplicationContext(), false);
							sync.setOnSyncStartListener(new OnSyncStartListener() {
								@Override
								public void onSyncStart() {
									startSyncNotification();
								}
							});
							sync.setOnSyncFinishedListener(new OnSyncFinishedListener() {
								@Override
								public void onSyncFinished(boolean result,
										WFSLayer selectedLayer) {
									stopSyncNotification();
									if (result) {
//										ProjectHandler.getCurrentProject().setSync(true);
									} else {
										// TODO: Uebersetzungen
										Alerts.errorMessage(getApplicationContext(),
												"Error!", "New feature could not " + "be synchronized.").show();
									}
								}
							});
							sync.execute();
						}
					}
				});
	}

	/**
	 * Start sync notification.
	 */
	private void startSyncNotification() {
		NotificationManager notMan = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		// TODO: Uebersetzungen
		Notification not = new Notification(android.R.drawable.stat_notify_sync, "Auto-sync...", System.currentTimeMillis());
		CharSequence contentTitle = "Synchronization";
		CharSequence contentText = "Automatic synchronization of new features...";
		Intent notInt = new Intent(this, MainActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notInt, 0);
		not.setLatestEventInfo(getApplicationContext(), contentTitle, contentText, contentIntent);
		notMan.notify(NOTIFICATION_SYNC_TAG, NOTIFICATION_SYNC_ID, not);
	}

	/**
	 * Hides the synchronization notification.
	 */
	private void stopSyncNotification() {
		NotificationManager notMan = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notMan.cancel(NOTIFICATION_SYNC_TAG, NOTIFICATION_SYNC_ID);
	}

	/**
	 * Start and display properties.
	 */
	private void startAndDisplayProperties() {
		Log.i(CLASSTAG, "-----------------------------------------------------------------------------");
		Log.i(CLASSTAG, "------------------------                             ------------------------");
		Log.i(CLASSTAG, "------------------------       STARTING   GTMB       ------------------------");
		Log.i(CLASSTAG, "------------------------                             ------------------------");
		Log.i(CLASSTAG, "------------------------                             ------------------------");
		Log.i(CLASSTAG, "-----------------------------------------------------------------------------");
		Calendar now = Calendar.getInstance();
		Log.i(CLASSTAG, "Date and Time: " + now.getTime().toGMTString());
		Log.i(CLASSTAG, "OS-Version: " + System.getProperty("os.version"));
		Log.i(CLASSTAG, "Release: " + android.os.Build.VERSION.RELEASE);
		Log.i(CLASSTAG, "Device: " + android.os.Build.DEVICE); 
		Log.i(CLASSTAG, "Model: " + android.os.Build.MODEL); 
		Log.i(CLASSTAG, "Product: " + android.os.Build.PRODUCT); 
		Log.i(CLASSTAG, "Brand: " + android.os.Build.BRAND); 
		Log.i(CLASSTAG, "Display: " + android.os.Build.DISPLAY); 
		Log.i(CLASSTAG, "CPU_ABI: " + android.os.Build.CPU_ABI); 
		Log.i(CLASSTAG, "CPU_ABI2: " + android.os.Build.CPU_ABI2); 
		Log.i(CLASSTAG, "Unknown: " + android.os.Build.UNKNOWN); 
		Log.i(CLASSTAG, "Hardware: " + android.os.Build.HARDWARE);
		Log.i(CLASSTAG, "ID: " + android.os.Build.ID); 
		Log.i(CLASSTAG, "Manufacturer: " + android.os.Build.MANUFACTURER); 
		Log.i(CLASSTAG, "Serial: " + android.os.Build.SERIAL); 
		Log.i(CLASSTAG, "User: " + android.os.Build.USER); 
		Log.i(CLASSTAG, "Host: " + android.os.Build.HOST); 
		Log.i(CLASSTAG, "-----------------------------------------------------------------------------");	
		DisplayMetrics displaymetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
		Log.i(CLASSTAG, "Pixel of Window: Width: " + displaymetrics.widthPixels + " - Height: " + displaymetrics.heightPixels);
		Log.i(CLASSTAG, "Density: " + displaymetrics.density + " - DensityDPI: " + displaymetrics.densityDpi);
		Log.i(CLASSTAG, "Height of Pixels: " + displaymetrics.heightPixels + " - Scaled Density: " + displaymetrics.scaledDensity);
		Log.i(CLASSTAG, "External Cache Dir: " + context.getExternalCacheDir());
		Log.i(CLASSTAG, "Package Code Path: " + context.getPackageCodePath());
		Log.i(CLASSTAG, "Package Name: " + context.getPackageName());
		Log.i(CLASSTAG, "Package Resource Path: " + context.getPackageResourcePath());
		Log.i(CLASSTAG, "Files Dir: " + context.getFilesDir().getAbsolutePath());
		this.saveLogCatToFile(context);
		Log.i(CLASSTAG, "-----------------------------------------------------------------------------");	
		// save log directly after restart to see exceptions
	}

	// TODO nur copy&paste-Methode fuer die rueckkehr von activities
	private void backFromAnywhere(int resultCode, Intent intentData) {
		if (resultCode == RESULT_OK) {
		} else if (resultCode == RESULT_CANCELED) {

		}
	}

	/**
	 * Save logcat to file. TODO: Einbauen an guter Stelle im Code.
	 *
	 * @param context the context
	 */
	private void saveLogCatToFile(Context context) {    
		// build a file name with the date in it
		Calendar now = Calendar.getInstance();
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd_HHmm");
		String formattedDate = df.format(now.getTime());
		String fileName = formattedDate  + "_" + System.currentTimeMillis() + "_GeoTechMobileLogCat" + ".txt";
		File outputFile = new File(context.getExternalCacheDir(), fileName);
		try {
			Log.i(CLASSTAG + " saveLogcatToFile", "Writing Log-File " + outputFile.getAbsolutePath());	
			Process process = Runtime.getRuntime().exec("logcat -f " + outputFile.getAbsolutePath());
			// clear the old logcat history
			clearLogCat(context);
		} catch (IOException e) {
			Log.e(CLASSTAG + " saveLogcatToFile", "IOException while logging: " + e.getMessage());	
			e.printStackTrace();
		}
	}


	/**
	 * Clear log cat.
	 *
	 * @param context the context
	 */
	private void clearLogCat(Context context) {    
		try {
			Process process = Runtime.getRuntime().exec("logcat -c ");
		} catch (IOException e) {
			Log.e(CLASSTAG + " saveLogcatToFile", "IOException while logging: " + e.getMessage());	
			e.printStackTrace();
		}
	}

}
