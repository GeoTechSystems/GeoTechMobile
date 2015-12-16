/**
 * Diese Klasse muss noch richtig eingebunden und verbessert
 * werden Sie speichert die Präzisions- und Satellitendaten der 
 * GPS gemessenen Punkte. Die Datenbank steht auch schon bereit 
 * um gefüllt zu werden. Allerdings sollten die PrecisionCon-Daten 
 * noch an die Features gekoppelt werden. Z.B als Instanzvariable 
 * in der Feature-Klasse. Allerdings können noch keine Linien und 
 * Polygone durch GPS Messungen erzeugt werden
 *  
 * @author svenweisker
 * @author Torsten Hoch
 * @author bschm
 */

package de.geotech.systems.editor;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.util.Log;

import de.geotech.systems.database.DBAdapter;
import de.geotech.systems.database.SQLPrecision;

public class PrecisionCon {
	private static final String CLASSTAG = "PrecisionCon";
	private static ArrayList<String> precisionAttributes;
	
	static {
		precisionAttributes = new ArrayList<String>();
		precisionAttributes.clear();
		precisionAttributes.add(SQLPrecision.ACCURACY);
		precisionAttributes.add(SQLPrecision.SATCOUNT);
	}
	
	private Context context;
	private String database;
	private boolean details;
	private long featureId;
	private long layerId;
	private float accuracy;
	private int satCount;
	private int satFixCount;
	private float minElevation;
	private float averageElevation;
	private long recordingTime; 

	public PrecisionCon(Context context, long elementId, long layerId) {
		this.context = context;
		this.database = "";
		this.details = true;
		this.featureId = elementId;
		this.layerId = layerId;
		this.accuracy = 0;
		this.satCount = 0; 
		this.satFixCount = 0; 
		this.minElevation = 0; 
		this.averageElevation = 0; 
		this.recordingTime = 0;
		//Cursor cursor = db.getPrecision(0);
		//cursor.getInt(cursor.getColumnIndex(db.PRECISION_SATCOUNT));
		//cursor.getInt(cursor.getColumnIndex(db.PRECISION_SAT_FIX_COUNT));
		//cursor.getFloat(cursor.getColumnIndex(db.PRECISION_MIN_ELEVATION));
		//cursor.getFloat(cursor.getColumnIndex(db.PRECISION_AVERAGE_ELEVATION));
		//cursor.getFloat(cursor.getColumnIndex(db.PRECISION_DELTA));
		//cursor.getLong(cursor.getColumnIndex(db.TIME));
		//cursor.close();
		//Close DB and return.
	}
	
	public PrecisionCon(Context context, long layerId, GpsStatus gpsStatus, Location location) {
		this.context = context;
		this.database = "";
		this.details = true;
		this.featureId = -1;
		this.layerId = layerId;
		this.accuracy = location.getAccuracy();
		this.satCount = 0;
		this.satFixCount = 0;
		this.minElevation = 90;
		this.averageElevation = 0; 
		this.recordingTime = System.currentTimeMillis();
		// initialise database
		DBAdapter dbAdapter = new DBAdapter(context);
		// dbAdapter.insertPrecision(layerId, satCount, accuracy, minElevation, averageElevation, satFixCount, recordingTime);
      	// Update TABLE_PRECISION unsing the fetched location-values. Remember: The information is written 
		// to the DB even if the user won't save the point! In this case the information will be
		// overwriten when the next point will be insertet. 
     	Log.e(CLASSTAG, "Layer-ID: " + String.valueOf(layerId) + " - Element-ID " + String.valueOf(featureId));
        Log.e(CLASSTAG, "Found Accuracy: " + String.valueOf(location.getAccuracy()));
        Iterable<GpsSatellite> satellites = gpsStatus.getSatellites();
		float elevationSum = 0;
		for(GpsSatellite sat : satellites) {
			satCount++;
			if (sat.usedInFix()) {
				satFixCount++;
				elevationSum += sat.getElevation();
				Log.e(CLASSTAG, "Found Satellite with Elevation " + String.valueOf(sat.getElevation()));
				//Save information to TABLE_SATELLITES
				int hasAlmanac = 0;
				int hasEphemeris = 0;
				if (sat.hasAlmanac()) {
					hasAlmanac = 1;
				}
				if (sat.hasEphemeris()) {
					hasEphemeris = 1;
				}
				dbAdapter.insertSatellit((long)layerId, sat.getPrn(), (double)sat.getElevation(), 
						(double)sat.getAzimuth(), (double)sat.getSnr(), hasEphemeris, hasAlmanac);
				if (sat.getElevation() < minElevation) {
					minElevation = sat.getElevation();
				}
			}
		}
		Log.e(CLASSTAG, "Found satFixCount: " + String.valueOf(satFixCount) + " - Found SatCount: " + String.valueOf(satCount));
        if (satFixCount > 0) {
        	averageElevation = elevationSum / satFixCount;
        }
        Log.e(CLASSTAG, "Found minElevation: " + minElevation + " - Found averageElevation: " + averageElevation + " - Found recordingTime: " + String.valueOf(recordingTime));
	}
	
	public boolean hasDetails()	{
		return details;
	}
	
	public long getFeatureId()	{
		return featureId;
	}
	
	public long getLayerId()	{
		return layerId;
	}
	
	public float getAccuracy()	{
		return accuracy;
	}
	
	public String getDatabase()	{
		return database;
	}
	
	public String getSatCount()	{	
		if (this.hasDetails()) {
			return String.valueOf(satCount);
		} else {
			return " - ";
		}
	}
	
	public String getSatFixCount()	{
		if (this.hasDetails()) {
			return String.valueOf(satFixCount);
		} else {
			return " - ";
		}
	}
	
	// TODO UERBERSETZUNGEN
	public String getAccuracyString() {
		if (this.hasDetails()) {
			return String.valueOf(accuracy) + "m";
		} else {
			return " - ";
		}
	}
	
	public String getMinElevation()	{
		if (this.hasDetails()) {
			return String.valueOf(minElevation) + "°";
		} else {
			return " - ";
		}
	}
	
	public String getAverageElevation()	{
		if (this.hasDetails()) {
			return String.valueOf(averageElevation) + "°";
		} else {
			return " - ";
		}
	}
	
	public String getRecordingTime(){
		if (this.hasDetails()){
			return String.valueOf(recordingTime) + "ms";
		} else {
			return " - ";
		}
	}

}

//public String getLayerName() {
//	return ("layer" + String.valueOf(layerId));
//}
//

//public PrecisionCon(Context c, int featureId, int layerId, ArrayList<Location> locationList) {
//	this.context = c;
////	this.db = new DBAdapter(context);
//	this.layerId = layerId;
//	this.featureId = featureId;
//	
//	//Ermittele die maximale Accuracy
//	this.accuracy = 0;
//	for(Location location : locationList)
//	{
//		Log.d("proCon", "accuracy: " +String.valueOf(location.getAccuracy()));
//		if(location.getAccuracy()>this.accuracy) this.accuracy=location.getAccuracy();
//	}
//}

//public Boolean writeIntoDatabase()
//{
//	db.insertPrecisionInDB(this);
//	return true;
//}


//public PrecisionCon(Context c, int layerId, int featureId) {
//this.context = c;
//this.layerId = layerId;
//this.featureId = featureId;
//}
//
//public PrecisionCon(Context c, int layerId) {
//this.context = c;
//this.layerId = layerId;
//}
//
//public void getFromDB()	{
////db.readPrecisionFromDB();
//}
//
//public PrecisionCon(Context c, int featureId, int layerId, Location location)
//{
//this.context = c;
////this.db = new DBAdapter(context);
//this.layerId = layerId;
//this.featureId = featureId;
//
//this.accuracy = location.getAccuracy();
//}

//public HashMap<Integer, Float> getAccuracyMapFormDB()
//{
//	return db.getAccuracyMapFromDB(this);
//}

//public float getAccuracyFormDB()
//{
//	return db.getAccuracyFromDB(this);
//}