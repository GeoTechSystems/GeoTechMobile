/**
 * 
 * @author Torsten Hoch
 */

package de.geotech.systems.database;

public class SQLProjects {
	// Table Name
	public static final String TABLE_NAME = "projects";
	// create Table String
	public static final String CREATE_TABLE = "create table projects (" 
			+ "_idproject integer primary key autoincrement, "
			+ "proj_name text, "
			+ "proj_epsg integer, "
			+ "proj_projection integer, "
			+ "proj_description text, "
			+ "proj_osm boolean, "
			+ "proj_sync integer, "
			+ "proj_autosync integer);";
	
	// Attributes with primary key public
	public static final String ID = "_idproject";
	private static final String NAME = "proj_name";
	private static final String EPSG = "proj_epsg";
	private static final String PROJECTION = "proj_projection";
	private static final String DESC = "proj_description";
	private static final String OSM = "proj_osm";
	private static final String SYNC = "proj_sync";
	private static final String AUTOSYNC = "proj_autosync";
	// List of Attributes
	public static final String[] PROJECTATTRIBUTES = {
		ID, 
		NAME, 
		EPSG, 
		PROJECTION, 
		DESC, 
		OSM, 
		SYNC, 
		AUTOSYNC};
	
}
