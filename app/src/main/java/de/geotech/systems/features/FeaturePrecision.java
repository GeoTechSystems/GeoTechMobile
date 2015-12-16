/**
 * Klasse zum speichern von Informationen zur Genauigkeit 
 * von einzelnen Punkten aller Geometrie-Typen.
 * 
 * @author bschm
 * @author Torsten Hoch (tohoch@uos.de)
 * 
 */

package de.geotech.systems.features;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

import de.geotech.systems.database.DBAdapter;
import de.geotech.systems.database.SQLPrecision;
import de.geotech.systems.utilities.Functions;

import android.content.Context;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.util.Log;
import android.util.SparseIntArray;

// TODO: Auto-generated Javadoc
/**
 * The Class FeaturePrecision.
 */
public class FeaturePrecision {
	// The classtag.
	private static final String CLASSTAG = "FeaturePrecision";
	
	// The db-adapter
	private DBAdapter db;
	// The layer id.
	private long layerId;
	// The feature id.
	private long featureId;
	// ArrayListe, die die einzelnen Precision-Instanzen aufnimmt
	private ArrayList<Precision> precisionList= new ArrayList<Precision>();
	// relations Verknüpft die ID (und Polygon-ID) eines Punkes im Objekt 
	// "Feature" mit der in der ArrayList<Precision>. Für Punkt- und 
	// Linien-Geometrien wird dabei stehts das nullte Element der Liste genutzt, 
	// bei Polygonen immer das idx[0]-te. 
	private ArrayList<SparseIntArray> relations = new ArrayList<SparseIntArray>();
	
	/**
	 * Konstruktor, erzeugt einen leeren Container.
	 */
	public FeaturePrecision() {
		// Log.d(CLASSTAG, "Erzeuge einen frischen Container");
		clear();
	}
	
	/**
	 * Konstruktor, baut das komplett Objekt nur aus einer HashMap auf, welche z.B. von 
	 * der Datenbank geliefert wird.
	 *
	 * @param values the values
	 */
	public FeaturePrecision(HashMap<String, String> values) {
		this.featureId = Long.parseLong(values.get(SQLPrecision.FEATUREID));
		this.layerId = Long.parseLong(values.get(SQLPrecision.LAYERID));
		Log.d(CLASSTAG, "Erzeuge Container und fülle ihn mit Daten aus der Datenbank (Feature-ID: " + String.valueOf(featureId) + ", Layer-ID: " + String.valueOf(layerId) + "):");
		Log.d(CLASSTAG,"\tGenauigkeit:\t\t\t"+values.get(SQLPrecision.ACCURACY));
		Log.d(CLASSTAG,"\tAnzahl verwendeter Satelliten:\t"+values.get(SQLPrecision.SATCOUNT));
		Log.d(CLASSTAG,"\tAzimuth (für jeden Satellit):\t"+values.get(SQLPrecision.AZIMUTH));
		Log.d(CLASSTAG,"\tElavation (für jeden Satellit):\t"+values.get(SQLPrecision.ELEVATION));
		// zerlege die HashMap
		StringTokenizer tAccuracy = new StringTokenizer(values.get(SQLPrecision.ACCURACY), " ");
		StringTokenizer tSatCount = new StringTokenizer(values.get(SQLPrecision.SATCOUNT), " ");
		StringTokenizer tAzimuth = new StringTokenizer(values.get(SQLPrecision.AZIMUTH), "}");
		StringTokenizer tElevation = new StringTokenizer(values.get(SQLPrecision.ELEVATION), "}");
		
		// Besonders im Falle von Mehrpunkt-Geometrien muss hier in die einzelnen Teilpunkte aufgespaltet werden.
		while(tAccuracy.countTokens() >= 1) {
			String[] vAccuracy = tAccuracy.nextToken().split(":");
			String[] vSatCount = tSatCount.nextToken().split(":");
			String[] vAzimuth = tAzimuth.nextToken().replaceAll("[ _0-9]*\\:\\{", "").split(" ");
			String[] vElevation = tElevation.nextToken().replaceAll("[ _0-9]*\\:\\{", "").split(" ");
			float[] azimuth = new float[vAzimuth.length];
			float[] elevation = new float[vAzimuth.length];
			for (int i = 0; i < vAzimuth.length; i++) {
				azimuth[i] = Float.parseFloat(vAzimuth[i]);
				elevation[i] = Float.parseFloat(vElevation[i]);
			}
			// Erzeuge für jeden Punkt eine Precision-Instanz...
			Precision precision = new Precision(Float.parseFloat(vAccuracy[1]), Integer.parseInt(vSatCount[1]), azimuth, elevation);
			// ... und registriere diese. Prüfe zuerst ob schon ein entsprechender SparseIntArray vorhanden ist!
			String[] idAndKey = vAccuracy[0].split("_");
			while (relations.size() <= Integer.parseInt(idAndKey[0])) {
				relations.add(new SparseIntArray());
			}	
			relations.get(Integer.parseInt(idAndKey[0])).append(Integer.parseInt(idAndKey[1]), precisionList.size());
			precisionList.add(precision);
		}
	}

	/**
	 * Löscht den Inhalt des Containers.
	 */
	public void clear()	{
		// Log.d(CLASSTAG, "Leere Container...");
		precisionList.clear();
		relations.clear();
	}
	
	/**
	 * Löscht den kompletten Container und fürgt einen Punkt ein ("Abkürzung" für Punkt-Geometrien).
	 *
	 * @param location the location
	 * @param gpsStatus the gps status
	 */
	public void setForPoint(Location location, GpsStatus gpsStatus)	{
		clear();
		Precision precision = new Precision(location, gpsStatus);
		precisionList.add(precision);
		if (relations.size() < 1) relations.add(new SparseIntArray());
		relations.get(0).append(0, 0);
		Log.d(CLASSTAG, "Füge Punkt in Container ein:" +
				"\n\tAccuracy: " + String.valueOf(this.getAccuracyOfPoint()) +
				"\n\tAnzahl an Statelliten: " + String.valueOf(this.getSatCountOfPoint()));
	}
	
	/**
	 * Schreibt die Informationen zur Genauigkeit in die Precision-Table und in die Feature-Instanz.
	 *
	 * @param c the c
	 * @param feature the feature
	 * @param layerId the layer id
	 * @return the boolean
	 */
	public Boolean write(Context c, Feature feature, long layerId) {
		Log.d(CLASSTAG,"Schreibe Informationen zur Genauigkeit in die Datenbank und in die Feature-Instanz...");
		this.featureId = feature.getFeatureID();
		this.layerId = layerId;
		this.db = new DBAdapter(c);
		
		db.insertPrecisionInDB(this.writeStrings(layerId, feature.getFeatureID()));
		feature.setPrecision(this);
		
		return true;
	}
	
	/**
	 * Speichert die komplette Instanz mit allen relevanten Werten in einer HashMap. Deren einzelne
	 * Strings können an den DB-Adapter übergeben und so in der Datenbank gespeichert werden.
	 *
	 * @param layerId the layer id
	 * @param featureId the feature id
	 * @return the hash map
	 */
	public HashMap<String, String> writeStrings(long layerId, long featureId)	{
		Log.i(CLASSTAG + " writeStrings", "Packe alle Informationen aus dem Container in Strings:");
		StringBuffer vAccuracy = new StringBuffer();
		StringBuffer vSatCount = new StringBuffer();
		StringBuffer vAzimuth = new StringBuffer();
		StringBuffer vElevation = new StringBuffer();
		Log.i(CLASSTAG, relations.toString());
		// Behandele jeden Punkt einzeln. Die "echte" ID wird dabei dem ":" in der Form aktWorkingObjNum_aktWorkingPointNum vorangestellt
		for (int i = 0; i < precisionList.size(); i++) {
			Precision precision = precisionList.get(i);
			int[] idAndKey = Functions.getIdAndKey(relations, i);
			vAccuracy.append(String.valueOf(idAndKey[0]+"_"+String.valueOf(idAndKey[1]))+":"+String.valueOf(precision.accuracy) + " ");
			vSatCount.append(String.valueOf(idAndKey[0]+"_"+String.valueOf(idAndKey[1]))+":"+String.valueOf(precision.satCount) + " ");	
			vAzimuth.append(String.valueOf(idAndKey[0]+"_"+String.valueOf(idAndKey[1]))+":{"+Functions.arrayToString(precision.azimuth) + "} ");
			vElevation.append(String.valueOf(idAndKey[0]+"_"+String.valueOf(idAndKey[1]))+":{"+Functions.arrayToString(precision.elevation) + "} ");
		}
		// Erzeuge die Hash-Map und gebe alles zur Kontrolle aus.
		HashMap<String, String> values = new HashMap<String, String>();
		values.put(SQLPrecision.ACCURACY, vAccuracy.toString());
		values.put(SQLPrecision.SATCOUNT, vSatCount.toString());
		values.put(SQLPrecision.AZIMUTH, vAzimuth.toString());
		values.put(SQLPrecision.ELEVATION, vElevation.toString());
		values.put(SQLPrecision.FEATUREID, String.valueOf(featureId));
		values.put(SQLPrecision.LAYERID, String.valueOf(layerId));
		Log.i(CLASSTAG,"\tGenauigkeit:\t\t\t"+values.get(SQLPrecision.ACCURACY));
		Log.i(CLASSTAG,"\tAnzahl verwendeter Satelliten:\t"+values.get(SQLPrecision.SATCOUNT));
		Log.i(CLASSTAG,"\tAzimuth (für jeden Satellit):\t"+values.get(SQLPrecision.AZIMUTH));
		Log.i(CLASSTAG,"\tElavation (für jeden Satellit):\t"+values.get(SQLPrecision.ELEVATION));
		return values;
	}
	
	/**
	 * Gibt die Genauigkeit im Falle einer Punkt-Geometrie zurück 
	 * ("Abkürzung").
	 *
	 * @return the accuracy of point
	 */
	public float getAccuracyOfPoint() {
		return precisionList.get(relations.get(0).get(0)).accuracy;
	}
	
	/**
	 * Gibt die Genauigkeit des bezeichneten Punktes der Geometrie zurück. 
	 *
	 * @param id0 the id0
	 * @param id1 the id1
	 * @return the accuracy of id
	 */
	public float getAccuracyOfId(int id0, int id1) {
		return precisionList.get(relations.get(id0).get(id1)).accuracy;
	}
	
	/**
	 * Gibt die Genauigkeit des ungenausten Punktes der Geometrie zurück. 
	 * 
	 * @return float	maximale Ungenauigkeit
	 */
	public float getWorstAccuracy()	{
		float wAccuracy=0;
		for (Precision precision : precisionList) {
			if (precision.accuracy>wAccuracy) wAccuracy=precision.accuracy;
		}
		return wAccuracy;
	}
	
	/**
	 * Gibt die Anzahl der Satelliten im Falle einer Punkt-Geometrie zurück 
	 * ("Abkürzung").
	 *
	 * @return the sat count of point
	 */
	public int getSatCountOfPoint()	{
		return precisionList.get(relations.get(0).get(0)).satCount;
	}
	
	/**
	 * Gibt die Anzahl der Satelliten während der Erfassung des mit ID 
	 * bezeichneten Punktes zurück.
	 *
	 * @param id0 the id0
	 * @param id1 the id1
	 * @return the sat count of id
	 */
	public int getSatCountOfId(int id0, int id1) {
		return precisionList.get(relations.get(id0).get(id1)).satCount;
	}
	
	/**
	 * Gibt die Anzahl der Punkte, zu denen Informationen zur Genauigkeit im Container gespeichert wurden zurück.
	 *
	 * @return the int
	 */
	public int size() {
		return precisionList.size();
	}
	
	/**
	 * Gibt true zurück, wenn keine Informationen gespeichert wurden.
	 *
	 * @return the boolean
	 */
	public Boolean isEmpty() {
		return (size() == 0);
	}
	
	/**
	 * Löscht Informationen zu dem bezeichneten Punkt aus der Liste.
	 *
	 * @param id0 the id0
	 * @param id1 the id1
	 * @return true	Wenn ein Eintrag für diesen Punkt vorhanden war
	 */
	public Boolean deleteEntry(int id0, int id1) {
		if (precisionList != null && !precisionList.isEmpty()) {
			// Anpassungen für die jeweils ersten Punkte eienr Geometrie
			if (id1 == -1) id1 = 0;
			if (id1 == -2) id1 = 1;
			if (id0 == -1) id0 = precisionList.size() - 1;
			if (relations.size() > id0) {
				if (relations.get(id0).get(id1, -42) >= 0) {
					Log.d(CLASSTAG, "Lösche Informationen zu Punkt #" + String.valueOf(id0) + "/" + String.valueOf(id1) + " (=" + String.valueOf(relations.get(id0).get(id1)) + ")");
					precisionList.remove(relations.get(id0).get(id1));
					relations.get(id0).delete(id1);
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Adds the entry.
	 *
	 * @param location the location
	 * @param gpsStatus the gps status
	 * @param id0 the id0
	 * @param id1 the id1
	 */
	public void addEntry(Location location, GpsStatus gpsStatus, int id0, int id1) {	
		Precision precision = new Precision(location, gpsStatus);
		precisionList.add(precision);
		while(relations.size() <= id0) {
			relations.add(new SparseIntArray());
		}
		relations.get(id0).append(id1, precisionList.size() - 1);
		Log.d(CLASSTAG, "Füge Punkt mit [id="+String.valueOf(id0)+"/key="+String.valueOf(id1)+"] in Container ein:" +
				"\n\tAccuracy: " + String.valueOf(this.getAccuracyOfPoint()) +
				"\n\tAnzahl an Statelliten: " + String.valueOf(this.getSatCountOfPoint()));
	}
	
	/**
	 * Gets the list.
	 *
	 * @return the list
	 */
	public ArrayList<Precision> getList() {
		return precisionList;
	}
	
	/**
	 * Gets the realtions.
	 *
	 * @return the realtions
	 */
	public ArrayList<SparseIntArray> getRealtions() {
		return relations;
	}
	
	/**
	 * In der inneren Klasse Precision werden alle Genauigkeitsinformationen 
	 * zu einem Punkt einer Geometrie abgelegt.
	 */
	public class Precision {
		// Accuracy-Daten aus Location
		private float accuracy;
		// Daten aus GpsStatus / GpsSatellite
		private int satCount;
		private float[] azimuth;
		private float[] elevation;
		// the id
		private long precisionID;
		
		/**
		 * Konstruktor .
		 *
		 * @param location the location
		 * @param gpsStatus the gps status
		 */
		public Precision(Location location, GpsStatus gpsStatus) {
			accuracy = location.getAccuracy();
			satCount = Functions.getSizeOfIterable(gpsStatus.getSatellites());
			azimuth = new float[satCount];
			elevation = new float[satCount];
			int i = 0;
			for (GpsSatellite gpsSatellite : gpsStatus.getSatellites()) {
					azimuth[i] = gpsSatellite.getAzimuth();
					elevation[i] = gpsSatellite.getElevation();
					i++;
			}
		}
		
		/**
		 * Instantiates a new precision.
		 *
		 * @param accuracy the accuracy
		 * @param satCount the sat count
		 * @param azimuth the azimuth
		 * @param elevation the elevation
		 */
		public Precision(float accuracy, int satCount, float[] azimuth, float[] elevation) {
			Log.d(CLASSTAG, "Speichere Informationen zur Genauigkeit eines Punktes der Geometrie...");
			this.accuracy = accuracy;
			this.satCount = satCount;
			this.azimuth = azimuth;
			this.elevation = elevation;
		}
		
		/**
		 * Gets the accuracy.
		 *
		 * @return the accuracy
		 */
		public float getAccuracy() {
			return accuracy;
		}

		/**
		 * Gets the precision id.
		 *
		 * @return the precision id
		 */
		public long getPrecisionId() {
			return this.precisionID;
		}
		
		/**
		 * Sets the precision id.
		 *
		 * @param newID the new precision id
		 */
		public void setPrecisionId(long newID) {
			this.precisionID = newID;
		}
		
	}
	
}
