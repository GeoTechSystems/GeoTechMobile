/**
 * Properties for a table for Precisions
 * 
 * @author Torsten Hoch
 */

package de.geotech.systems.database;

public class SQLPrecision {
	public static final String TABLE_NAME = "precisions";
	public static final String CREATE_TABLE = "create table precisions (" 
			+ "_idprecision integer primary key autoincrement, "
			+ "pre_layerId integer, "
			+ "pre_featureId integer, "
			+ "pre_accuracy text, "
			+ "pre_satcount text, "
			+ "pre_azimuth text, "
			+ "pre_elevation text);";

	// Precision_Database_Handling
	public static final String ID = "_idprecision";
	public static final String LAYERID = "pre_layerId";
	public static final String FEATUREID = "pre_featureId";
	public static final String ACCURACY = "pre_accuracy";
	public static final String SATCOUNT = "pre_satcount";
	public static final String AZIMUTH = "pre_azimuth";
	public static final String ELEVATION = "pre_elevation";
	// String with all attributes
	public static final String[] PRECISIONATTRIBUTES = new String[] {
				ID,
				LAYERID,
				FEATUREID,
				ACCURACY,
				SATCOUNT,
				AZIMUTH,
				ELEVATION,};
	
}
