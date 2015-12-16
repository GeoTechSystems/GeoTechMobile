/**
 * Abstract class of special functions
 * 
 * @author Torsten Hoch
 * 
 */
package de.geotech.systems.utilities;

import java.util.ArrayList;


import android.content.Context;
import android.util.Log;
import android.util.SparseIntArray;
import android.widget.ArrayAdapter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

import de.geotech.systems.utilities.ClassesColorModel.VCColor;

public abstract class Functions {
	
	/* Math functions ------------------------------------------------------------- */
	
	/** Rounds float number to given digits */
	public static float roundFloat(float num, int digits) {
		float precision = 1.0F;
	    for (int i = 0; i < digits; i++, precision *= 10);
	    return ((int) (num * precision + 0.5)  / precision);
	}
	
	/** Rounds float number to given digits */
	public static double roundDouble(double num, int digits) {
		double precision = 1.0;
	    for (int i = 0; i < digits; i++, precision *= 10);
	    return ((int) (num * precision + 0.5)  / precision);
	}
	
	/* String functions ----------------------------------------------------------- */
	
	/** Creates file name from given project name */
	public static String projectToDatabaseName(String name) {
		return name.replace(" ", "_")+Constants.FILE_PROJ;
	}
	
	/** Returns project name of given file name */
	public static String fileToProjectName(String name) {
		return name.replace(Constants.FILE_PROJ, "").replace("_", " ");
	}
	
	/** Checks if given string exists in given string array */
	public static boolean stringInArray(String str, String[] array) {
		for(String s : array) {
			if(s.equals(str)) return true;
		}
		return false;
	}
	
	/** Schreibt einen Array in einen String mit " " als Trennzeichen */
	public static String arrayToString(float[] array)
	{
		StringBuffer value = new StringBuffer();
		for(int i=0; i<array.length-1; i++)
		{
			value.append(String.valueOf(array[i])+" ");
		}
		value.append(String.valueOf(array[array.length-1]));
		return value.toString();
	}
	
	/* Color functions ------------------------------------------------------------ */
	
	/** Returns table of usable colors as array of type VCColor */
	public static VCColor[] getColorsAsArray() {
		
		ClassesColorModel table = new ClassesColorModel();
		return table.getColorsAsArray();
		
	}
	
	/* ArrayList functions -------------------------------------------------------- */
	
	/** Inverts the order of an ArrayList 
	 * @param <T>*/
	public static <T> ArrayList<T> reverseOrder(ArrayList<T> input) {
		ArrayList<T> output = new ArrayList<T>();
		
		for(int i = input.size()-1; i >= 0; i--) {
			output.add(input.get(i));
		}
		
		if(input.size() != output.size()) {
			return null;
		}
		
		return output;
	}
	
	/* Geometry functions --------------------------------------------------------- */
	
	/** Adds a new Point to a LineString */
	public static LineString appendLineString(LineString l, Coordinate c) {
		Coordinate[] newCoords = new Coordinate[l.getNumPoints()+1];
		Coordinate[] oldCoords = l.getCoordinates();
		for(int i=0; i<l.getNumPoints(); i++) {
			newCoords[i] = oldCoords[i];
		}
		newCoords[l.getNumPoints()] = c;
		return new GeometryFactory().createLineString(newCoords);
	}
	
	/** Replace point #n of a LineString with the last Point of the same LineString */
	public static LineString replacePointWithLastPointofALineString(LineString l, int n)
	{
		Coordinate[] newCoords = new Coordinate[l.getNumPoints()-1];
		Coordinate[] oldCoords = l.getCoordinates();
		for(int i=0; i<l.getNumPoints()-1; i++) newCoords[i] = oldCoords[i];
		newCoords[n]=oldCoords[l.getNumPoints()-1];
		return new GeometryFactory().createLineString(newCoords);
	}
	
	/** Adds a hole to a polygon */
	public static Polygon addPolygonHole(Polygon p, Coordinate[] c) {
		LinearRing shell 		= new GeometryFactory().createLinearRing(p.getExteriorRing().getCoordinates());
		LinearRing newHole		= Functions.coordinatesToLinearRing(c);
		LinearRing[] holes		= new LinearRing[p.getNumInteriorRing()+1];
		for(int i=0; i<p.getNumInteriorRing(); i++) {
			holes[i] = new GeometryFactory().createLinearRing(p.getInteriorRingN(i).getCoordinates());
		}
		holes[p.getNumInteriorRing()] = newHole;
		return new GeometryFactory().createPolygon(shell, holes);
	}
	
	/** Returns the Polygon's LineString with index i, starting with 0; i=0 is the shell, i>0 are holes */
	public static LineString getPolygonLineStringN(Polygon p, int i) {
		
		if(i == 0) {
			return p.getExteriorRing();
		}
		else {
			if(p.getNumInteriorRing() == 0) {
				return null;
			}
			else if( i > p.getNumInteriorRing() ) {
				return null;
			}
			else {
				return p.getInteriorRingN(i-1);
			}
		}
		
	}
	
	/** Appends the Polygon's exterior ring by given Coordinate */
	public static Polygon appendPolygonExterior(Polygon p, Coordinate c) {
		
		// Create new shell
		Coordinate[] oldShell = p.getExteriorRing().getCoordinates();
		Log.v("Polygon", "number of nodes in shell: " + oldShell.length);
		Coordinate[] newShell = new Coordinate[oldShell.length+1];
		for(int i=0; i<oldShell.length-1; i++) {
			newShell[i] = oldShell[i];
		}
		newShell[oldShell.length-1] = c;
		newShell[oldShell.length] = newShell[0];
		LinearRing shell = new GeometryFactory().createLinearRing(newShell);
		Log.v("Polygon", "new number of nodes in shell: " + shell.getNumPoints());
		
		// Take over old holes if existing
		int numHoles = p.getNumInteriorRing();
		Log.v("Functions", "Polygon has " + numHoles + " holes."); 
		
		if(numHoles > 0) {
			LinearRing[] holes = new LinearRing[p.getNumInteriorRing()];
			for(int i=0; i<p.getNumInteriorRing(); i++) {
				holes[i] = new GeometryFactory().createLinearRing(p.getInteriorRingN(i).getCoordinates());
			}
			return new GeometryFactory().createPolygon(shell, holes);
		}
		else {
			return new GeometryFactory().createPolygon(shell, null);
		}
		
	}
	
	/** Replace point #n of a polygon with the last-1 Point of the ExteriorRing (m=0) or an InteriorRing (m>0) of the same polygon */
	public static Polygon replacePointWithLastPointofAPolygon(Polygon p, int m, int n)
	{
		LinearRing shell;
		Coordinate[] oldCoord, newCoord;
		
		if(m==0)
		{
			oldCoord = p.getExteriorRing().getCoordinates();
			newCoord = new Coordinate[oldCoord.length-1];
		
			if(n==0 || n==oldCoord.length-2)
			{
				for(int i=1; i<oldCoord.length-2;i++) newCoord[i]=oldCoord[i];
				newCoord[0]=oldCoord[oldCoord.length-2];
				newCoord[newCoord.length-1]=oldCoord[oldCoord.length-2];
			}
			else
			{
				for(int i=0; i<oldCoord.length-2;i++) newCoord[i]=oldCoord[i];
				newCoord[n]=oldCoord[oldCoord.length-2];
				newCoord[newCoord.length-1]=oldCoord[oldCoord.length-1];
			}
		
			shell = new GeometryFactory().createLinearRing(newCoord);
		}
		else
			shell = new GeometryFactory().createLinearRing(p.getExteriorRing().getCoordinates());
			
		// Take over old holes if existing
		int numHoles = p.getNumInteriorRing();
		if(numHoles > 0) {
			LinearRing[] holes = new LinearRing[p.getNumInteriorRing()];
			for(int i=0; i<p.getNumInteriorRing(); i++)
			{
				if(i==m-1)
				{
					oldCoord = p.getInteriorRingN(i).getCoordinates();
					newCoord = new Coordinate[oldCoord.length-1];
				
					if(n==0 || n==oldCoord.length-2)
					{
						for(int j=1; j<oldCoord.length-2;j++) newCoord[j]=oldCoord[j];
						newCoord[0]=oldCoord[oldCoord.length-2];
						newCoord[newCoord.length-1]=oldCoord[oldCoord.length-2];
					}
					else
					{
						for(int j=0; j<oldCoord.length-2;j++) newCoord[j]=oldCoord[j];
						newCoord[n]=oldCoord[oldCoord.length-2];
						newCoord[newCoord.length-1]=oldCoord[oldCoord.length-1];
					}
				
					holes[i] = new GeometryFactory().createLinearRing(newCoord);
				}
				else
					holes[i] = new GeometryFactory().createLinearRing(p.getInteriorRingN(i).getCoordinates());
					
			}
			return new GeometryFactory().createPolygon(shell, holes);
		}
		else
		{
			return new GeometryFactory().createPolygon(shell, null);
		}
	}
	
	/** Appends the Polygon's hole by given Coordinate */
	public static Polygon appendPolygonHole(Polygon p, int idx, Coordinate c) {
		
		int holeId = idx - 1;
		
		if(holeId < 0 || holeId >= p.getNumInteriorRing()) {
			return p;
		}
		
		// Take over old shell
		LinearRing shell = new GeometryFactory().createLinearRing(p.getExteriorRing().getCoordinates());
		
		// Append selected hole and take over other ones
		LinearRing[] holes = new LinearRing[p.getNumInteriorRing()];
		for(int i=0; i<p.getNumInteriorRing(); i++) {
			if(i == holeId) {
				Coordinate[] oldHole = p.getInteriorRingN(i).getCoordinates();
				Coordinate[] newHole = new Coordinate[oldHole.length+1];
				for(int j=0; j<oldHole.length-1; j++) {
					newHole[j] = oldHole[j];
				}
				newHole[oldHole.length-1] = c;
				newHole[oldHole.length] = newHole[0];
				holes[i] = new GeometryFactory().createLinearRing(newHole);
			}
			else {
				holes[i] = new GeometryFactory().createLinearRing(p.getInteriorRingN(i).getCoordinates());
			}
		}
		
		return new GeometryFactory().createPolygon(shell, holes);
		
	}
	
	/** Löscht das m-te Loch aus dem Polygon */
	public static Polygon rmInterriorRing(Polygon p, int m) {
		// Take over old shell
		LinearRing shell = new GeometryFactory().createLinearRing(p.getExteriorRing().getCoordinates());
		LinearRing[] holes = new LinearRing[p.getNumInteriorRing()-1];
		
		for(int i=0; i<p.getNumInteriorRing(); i++) {
			if(i != m)
			{
				holes[i] = new GeometryFactory().createLinearRing(p.getInteriorRingN(i).getCoordinates());
			}
		}
		return new GeometryFactory().createPolygon(shell, holes);
	}
	 
	
	/** Returns the Polygon's holes as adapter */
	public static ArrayAdapter<Integer> getAdaper(Context context, Polygon p) {
		ArrayAdapter<Integer> adapter = new ArrayAdapter<Integer>(context, android.R.layout.simple_spinner_item);
		
		if(p.getNumInteriorRing() > 0) {
			for(int i=0; i<p.getNumInteriorRing(); i++) {
				adapter.add(i+1);
			}
		}
		
		return adapter;
	}
	
	/** Creates a LinearRing from given Coordinates by duplicating the first one */
	public static LinearRing coordinatesToLinearRing(Coordinate[] c) {
		Coordinate[] newC = new Coordinate[c.length+1];
		for(int i=0; i<c.length; i++) {
			newC[i] = c[i];
		}
		newC[c.length] = newC[0];
		return new GeometryFactory().createLinearRing(newC);
	}
	
	public static String reviseWfsUrl(String url) {
		url = url.trim();
		if (!url.startsWith("http")) {
			url = "http://" + url;
		}
//		String[] urlPortArray = url.split(":");
//		String[] urlPathArray = url.split("/");
//
//		boolean hasPortNumber = urlPortArray.length == 3;
//		boolean hasPath = urlPathArray.length > 3;
//		boolean endsWithSlash = url.endsWith("/");
//
//		if (hasPortNumber) {
//			if (!hasPath) {
//				if (!endsWithSlash) {
//					url += "/";
//				}
//				url += "geoserver/wfs";
//			}
//		} else {
//			if (hasPath) {
//				url = urlPathArray[0] + "//" + urlPathArray[2] + ":8080";
//				for (int i = 3; i < urlPathArray.length; i++) {
//					url += "/" + urlPathArray[i];
//				}
//				if (endsWithSlash) {
//					url += "/";
//				}
//			} else {
//				if (endsWithSlash) {
//					url = url.substring(0, url.length() - 1);
//				}
//				url += ":8080/geoserver/wfs";
//			}
//		}

		return url;
	}

	public static String reviseWmsUrl(String url) {
		url = url.trim();
		if (!url.startsWith("http")) {
			url = "http://" + url;
		}
//		String[] urlPortArray = url.split(":");
//		String[] urlPathArray = url.split("/");
//		boolean hasPortNumber = urlPortArray.length == 3;
//		boolean hasPath = urlPathArray.length > 3;
//		boolean endsWithSlash = url.endsWith("/");
//		if (hasPortNumber) {
//			if (!hasPath) {
//				if (!endsWithSlash) {
//					url += "/";
//				}
//				url += "geoserver/wms";
//			}
//		} else {
//			if (hasPath) {
//				url = urlPathArray[0] + "//" + urlPathArray[2] + ":8080";
//				for (int i = 3; i < urlPathArray.length; i++) {
//					url += "/" + urlPathArray[i];
//				}
//				if (endsWithSlash) {
//					url += "/";
//				}
//			} else {
//				if (endsWithSlash) {
//					url = url.substring(0, url.length() - 1);
//				}
//				url += ":8080/geoserver/wms";
//			}
//		}

		return url;
	}
	
	/* Iterable functions --------------------------------------------------------- */

	/** Get size of iterable */
	public static int getSizeOfIterable(Iterable<?> iterator)
	{
		int size=0;
		for(Object obj: iterator)
		{
			size++;
		}
		return size;
	}
	
	/* ArrayList and SparseIntArray functions ------------------------------------- */
	
	/** Get ArrayList-ID and SparseIntArray-Key of value. If value can't be found the
	 *  function is going to return negative numbers. */
	 public static int[] getIdAndKey(ArrayList<SparseIntArray> arrayList, int value)
	 {
		 for(int i=0; i<arrayList.size(); i++)
		 {
			 if(arrayList.get(i).indexOfValue(value)>=0)
				 return new int[]{i, arrayList.get(i).keyAt(arrayList.get(i).indexOfValue(value))};
		 }
		 
		 return new int[]{-1, -1};
	 }
	 
}
