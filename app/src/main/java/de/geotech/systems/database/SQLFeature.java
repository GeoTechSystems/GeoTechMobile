/**
 * 
 * @author Torsten Hoch
 */

package de.geotech.systems.database;

public class SQLFeature {
	public static final String TABLE_NAME = "features";
	public static final String CREATE_TABLE = "create table features ("
			+ "_idfeature integer primary key autoincrement, "
			+ "_idwfslayer integer, " 
			+ "feature_geom text, "
			+ "feature_sync boolean," 
			+ "feature_type integer, "
			+ "feature_attributes text, "
			+ "geoserver_id text, "
			+ "feature_is_done boolean);";

	// Feature_Database_Handling
	public static final String ID = "_idfeature";
	public static final String INDEX = "feature_index";
	private static final String GEOM = "feature_geom";
	private static final String SYNC = "feature_sync";
	private static final String TYPE = "feature_type";
	private static final String ATTRIBUTES = "feature_attributes";
	private static final String GEOSERVERID = "geoserver_id";
	private static final String ISDONE = "feature_is_done";
	// String with all attributes
	public static final String[] FEATUREATTRIBUTES = new String[] {
			SQLFeature.ID, 
			SQLWFS.ID, 
			SQLFeature.GEOM, 
			SQLFeature.SYNC,
			SQLFeature.TYPE, 
			SQLFeature.ATTRIBUTES, 
			SQLFeature.GEOSERVERID,
			SQLFeature.ISDONE };
	
}
