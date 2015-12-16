/**
 * List of Coordinate Reference Systems
 * 
 * @author Torsten Hoch
 */

package de.geotech.systems.utilities;

public abstract class Constants {
	// File endings
	public static final String	FILE_PROJ	= ".proj";
	public static final String 	FILE_DB 	= ".db4o";

	// Coordinate reference system list
	public static CRS[] getCRSList() {
		CRS[] crs = new CRS[]{
				new CRS("WGS 84", 4326),
//				new CRS("Google Mercator", 3785),
//				new CRS("Gauss-Krüger Zone 2", 31466),
//				new CRS("Gauss-Krüger Zone 3", 31467),
//				new CRS("Gauss-Krüger Zone 4", 31468),
//				new CRS("Gauss-Krüger Zone 5", 31469),
//				new CRS("UTM Zone 31N", 32631),
//				new CRS("UTM Zone 32N", 32632),
//				new CRS("UTM Zone 33N", 32633),
//				new CRS("Lambert", 2192)
		};
		return crs;
	}

}
