/**
 * GPS-signal Handler
 * 
 * @author Karsten
 * @author Torsten Hoch
 */

package de.geotech.systems.utilities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import com.jhlabs.map.proj.Projection;

import de.geotech.systems.R;
import de.geotech.systems.drawing.DrawingPanelView;

public class LocationFactory{
	private static final String CLASSTAG = "LocationFactory";
	// Application
	private Context 			context;
	private DrawingPanelView	panel;
	private Projection 			inputProj;
	// Location
	private LocationManager		locMan;
	private LocationListener	locLis;
	private GpsStatus.Listener 	gpsStatusListener;
	private GpsStatus 			gpsStatus;
	private Boolean 			gotFix = false;
	private ProgressDialog 		progress;
	// Listener
	private OnLocationByGPSListener gpsListener;
	
	/**
	 * Default Constructor.
	 * @param c		Application context.
	 * @param dp	DrawingPanel; needed to get the map projection.
	 */
	public LocationFactory(Context c, DrawingPanelView dp) {
		context 	= c;
		panel		= dp;
	}


	/**
	 * Returns the GPS position as coordinate in the map's system.
	 * @param duration	Duration of GPS measurement in seconds.
	 * @param check 	Turns dialog to check measurement on or off.
	 */
	public void getCoordinateByGPS() {
		// Initialize progress dialog
		progress = new ProgressDialog(context);
		progress.setTitle(context.getString(R.string.locationFactory_requesting_position));
		progress.setMessage(context.getString(R.string.locationFactory_request_waiting_for_fix));
		// Initialize location manager
		locMan 	= (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		//Initialize GPS-Status listener
		gpsStatusListener = new GpsStatus.Listener() {
			@Override
			public void onGpsStatusChanged(int event) {
				//Sofern das Gerät den ersten FIx erhalten hat (was hier relevant ist, da für jeden Punkt der 
				//Listener neu erstellt wird) speichere den gpsStatus.
				switch(event){	
				case GpsStatus.GPS_EVENT_FIRST_FIX:
				case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
					gpsStatus = locMan.getGpsStatus(null);
					Log.d("gpsStatusListener", "got something!");
					// Check if sat-details are known (has to be optimized)
					Iterable<GpsSatellite> satellites = gpsStatus.getSatellites();
					for (GpsSatellite curSat : satellites) {
						if (curSat.usedInFix()) {
							gotFix = true;
							Log.d("gpsStatusListener", "got FIX!");
							break;
						}
					}
				}
			}
		};
		locMan.addGpsStatusListener(gpsStatusListener);
		// Initalize Location listener
		locLis	= new LocationListener(){
			@Override
			public void onLocationChanged(Location location) {
				if (gotFix) {
					locMan.removeGpsStatusListener(gpsStatusListener);
					gpsListener.onLocationByGPS(location, gpsStatus);
					progress.cancel();
				}
			}
			@Override
			public void onProviderDisabled(String arg0) {
				// do nothing
			}
			@Override
			public void onProviderEnabled(String arg0) {
				// do nothing
			}
			@Override
			public void onStatusChanged(String provider, int status, Bundle extras) {
				// do nothing
			}
		};
		// Set listeners
		progress.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				locMan.removeUpdates(locLis);
			}
		});
		//Check if GPS_PROVIDER is active
		if(locMan.isProviderEnabled(LocationManager.GPS_PROVIDER)) {	
			// Yes -> start GPS measurements
			progress.show();
			Log.v(CLASSTAG, "Starting location updates...");
			locMan.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locLis);
		}
		else {
			// No -> Print warning, abort.
			askForGPS();
		}
	}

	/**
	 * Return the GPS position as coordinates in the map's system.
	 * 
	 * @author Torsten Hoch
	 * @return the coordinate by gps
	 */
	public void holdGPSCoordinates() {
		// Initialize progress dialog to display progress to user
		progress = new ProgressDialog(context);
		progress.setTitle(context.getString(R.string.locationFactory_requesting_position));
		progress.setMessage(context.getString(R.string.locationFactory_request_waiting_for_fix));
		// Initialize location manager
		locMan 	= (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		Log.d("getNaviCoordinatesByGPS()", "started!");
		//Initialize GPS-Status listener		
		gpsStatusListener = new GpsStatus.Listener() {
			@Override
			public void onGpsStatusChanged(int event) {
				// Sofern das Gerät den ersten FIX erhalten hat (was hier relevant ist, 
				// da für jeden Punkt der Listener neu erstellt wird) -> speichere 
				// den gpsStatus.
				switch(event) {	
				case GpsStatus.GPS_EVENT_FIRST_FIX:
				case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
					gpsStatus = locMan.getGpsStatus(null);
					Log.d("gpsStatusListener", "(NEW) got something!");
					//Check if sat-details are known (has to be optimized)
					Iterable<GpsSatellite> satellites = gpsStatus.getSatellites();
					for (GpsSatellite curSat : satellites) {
						if (curSat.usedInFix()) {
							gotFix = true;
							Log.d("gpsStatusListener", "(NEW) got FIX!");
							break;
						}
					}
				}
			}
		};
		locMan.addGpsStatusListener(gpsStatusListener);
		// Initalize Location listener
		locLis	= new LocationListener(){
			// if the location has changed
			@Override
			public void onLocationChanged(Location location) {
				if (gotFix)	{
					locMan.removeGpsStatusListener(gpsStatusListener);
					gpsListener.onLocationByGPS(location, gpsStatus);
					progress.cancel();
				}
			}
			// other possible actions, do nothing
			@Override
			public void onProviderDisabled(String arg0) {
				// do nothing
			}
			@Override
			public void onProviderEnabled(String arg0) {
				// do nothing
			}
			@Override
			public void onStatusChanged(String provider, int status, Bundle extras) {
				// do nothing
			}
		};
		// Check if GPS_PROVIDER is active
		if (locMan.isProviderEnabled(LocationManager.GPS_PROVIDER)) {	
			// Yes -> start GPS measurements
			progress.show();
			Log.v(CLASSTAG, "(NEW) Starting location updates...");
			locMan.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locLis);
		} else {
			// No -> Print warning, abort.
			askForGPS();
		}
	}

	/**
	 * Interface for the OnLocationByGPSListener
	 * 
	 * @author Karsten
	 *
	 */
	public interface OnLocationByGPSListener {
		void onLocationByGPS(Location location, GpsStatus gpsStatus);
	}
	
	/**
	 * Sets the OnLocationByGPSListener.
	 * 
	 * @param onLocationByGPSListener
	 */
	public void setOnLocationByGPSListener(
			OnLocationByGPSListener onLocationByGPSListener) {
		gpsListener = onLocationByGPSListener;
	}

	// stops GPSing 
	public void stopGPSing() {
//		Log.d("stopGPSing", "(NEW) stopGPSing");
		locMan.removeUpdates(locLis);	
		panel.setGPSTracking(false);
	}
	
	/**
	 * Dialog der bei deaktiviertem GPS aufgerufen wird. Bietet dem User die Option direkt die Location-Einstellungen aufzurufen.
	 */
	private void askForGPS() {
		Log.i(CLASSTAG, "GPS_PROVIDER IS DISABLED!");
		AlertDialog.Builder enableGPS = new AlertDialog.Builder(context);
		enableGPS.setTitle(context.getResources().getString(R.string.enableGPSTitle));
		enableGPS.setMessage(context.getResources().getString(R.string.enableGPSText));
		enableGPS.setNegativeButton(context.getResources().getString(R.string.cancel),  new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface enableGPS, int which) {
				enableGPS.cancel();
			}
		});
		enableGPS.setPositiveButton(context.getResources().getString(R.string.call_location_settings), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface enableGPS, int which) {
				((Activity) context).startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
				enableGPS.cancel();
			}
		});
		enableGPS.show();
	}
}
