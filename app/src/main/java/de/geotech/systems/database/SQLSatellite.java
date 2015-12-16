/**
 * 
 * @author Torsten Hoch
 */

package de.geotech.systems.database;

public class SQLSatellite {
	public static final String TABLE_NAME = "satellites";
	
	public static final String CREATE_TABLE = "create table satellites ("
			+ "_idsatellite integer primary key autoincrement, "
			+ "_idlayer integer, "
			+ "sat_elementid integer, "
			+ "sat_satid int, "
			+ "sat_elevation real, "
			+ "sat_azimuth real, "
			+ "sat_hasEphemeries integer, "
			+ "sat_hasAlmanach integer, "
			+ "sat_signal2noise real);";
	public static final String ID = "_idsatellite";
	public static final String ELEMENTID = "sat_elementId";
	public static final String SATID = "sat_satId";
	public static final String ELEVATION = "sat_elevation";
	public static final String AZIMUTH = "sat_azimuth";
	public static final String SIGNAL2NOISE = "sat_signal2noise";
	public static final String EPHEMERIES = "sat_hasEphemeries";
	public static final String ALMANACH = "sat_hasAlmanach";
	
}
