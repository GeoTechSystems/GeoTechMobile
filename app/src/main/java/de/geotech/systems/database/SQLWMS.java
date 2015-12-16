/**
 * 
 * @author Torsten Hoch
 */

package de.geotech.systems.database;

public class SQLWMS {
	// table name
	public static final String TABLE_NAME = "wmslayer";
	// create table String
	public static final String CREATE_TABLE = "create table wmslayer (" 
			+ "_idwmslayer integer primary key autoincrement, "
			+ "layer_name text, "
			+ "_idproject integer, "
			+ "layer_epsg text, "
			+ "layer_title text, "
			+ "layer_description text, "
			+ "layer_legend_url text, "
			+ "layer_logo_url text, "
			+ "layer_attribution_title text, "
			+ "layer_attribution_url text, "
			+ "layer_attribution_logo_url text, "
			+ "layer_bbox_maxX real, "
			+ "layer_bbox_maxY real, "
			+ "layer_bbox_minX real, "
			+ "layer_bbox_minY real, " + "layer_url text);";
	
	// primary key String public
	public static final String ID = "_idwmslayer";
	// SQL Database Strings
	private static final String NAME = "layer_name";
	private static final String EPSG = "layer_epsg";
	private static final String WORKSPACE = "layer_title";
	private static final String URL = "layer_url";
	private static final String DESCRIPTION = "layer_description";
	private static final String LEGEND_URL = "layer_legend_url";
	private static final String LOGO_URL = "layer_logo_url";
	private static final String ATT_URL = "layer_attribution_url";
	private static final String ATT_TITLE = "layer_attribution_title";
	private static final String ATT_LOGO_URL = "layer_attribution_logo_url";
	private static final String BBOX_MAX_X = "layer_bbox_maxX";
	private static final String BBOX_MAX_Y = "layer_bbox_maxY";
	private static final String BBOX_MIN_X = "layer_bbox_minX";
	private static final String BBOX_MIN_Y = "layer_bbox_minY";
	// List of Attributes of a WMSLayer		
	public static final String[] WMSLAYERATTRIBUTES = {
		ID,
		SQLProjects.ID, 
		NAME, 
		EPSG, 
		WORKSPACE, 
		URL, 
		DESCRIPTION, 
		LEGEND_URL, 
		LOGO_URL, 
		ATT_URL, 
		ATT_TITLE, 
		ATT_LOGO_URL, 
		BBOX_MAX_X, 
		BBOX_MAX_Y, 
		BBOX_MIN_X, 
		BBOX_MIN_Y
		};
}
