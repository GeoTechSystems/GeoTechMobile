/** 
 * Recreates WFS-Layer Objects to put them into a SQL DB
 * 
 * @author Torsten Hoch
 */

package de.geotech.systems.database;

public class SQLWFS {
	// Table Name
	public static final String TABLE_NAME = "wfslayer";
	// create Table String
	public static final String CREATE_TABLE = "create table wfslayer (" 
			+ "_idwfslayer integer primary key autoincrement, "
			+ "layer_name text, "
			+ "_idproject integer, "
			+ "layer_epsg text, "
			+ "layer_title text, "
			+ "layer_type text, "
			+ "layer_workspace text, "
			+ "layer_namespace text, "
			+ "layer_geom text, "
			+ "layer_url text, "
			+ "layer_ismulti boolean, "
			+ "layer_isactive boolean, "
			+ "layer_size_mb real, "
			+ "layer_timestamp integer, "
			+ "layer_color integer, "
			+ "layer_countF integer, "
			+ "layer_sync boolean, "
			+ "layer_islocked boolean, "
			+ "layer_lockid text, "
			+ "layer_lockexpiry integer, "
			+ "layer_lockdate integer, "
			+ "layer_releasedate integer);";
	
	// Attributes with primary key public
	public static final String ID = "_idwfslayer";
	private static final String NAME = "layer_name";
	private static final String TITLE = "layer_title";
	private static final String WORKSPACE = "layer_workspace";
	private static final String NAMESPACE = "layer_namespace";
	private static final String GEOM = "layer_geom";
	private static final String URL = "layer_url";
	private static final String EPSG = "layer_epsg";
	private static final String ISMULTI = "layer_ismulti";
	private static final String ISACTIVE = "layer_isactive";
	private static final String SIZE_MB = "layer_size_mb";
	private static final String TIMESTAMP = "layer_timestamp";
	private static final String COLOR = "layer_color";
	private static final String COUNTF = "layer_countF";
	private static final String TYPE = "layer_type";
	private static final String SYNC = "layer_sync";
	private static final String ISLOCKED = "layer_islocked";
	private static final String LOCKID = "layer_lockid";
	private static final String LOCKEXPIRY = "layer_lockexpiry";
	private static final String LOCKDATE = "layer_lockdate";
	private static final String RELEASEDATE = "layer_releasedate";
	// List of Attributes of a WFSLayer		
	public static final String[] WFSLAYERATTRIBUTES = {
		ID,
		SQLProjects.ID, 
		NAME,
		TITLE,
		WORKSPACE,
		NAMESPACE,
		TYPE,
		GEOM,
		EPSG,
		URL, 
		ISMULTI,
		ISACTIVE,
		TIMESTAMP,
		COLOR,
		COUNTF,
		SYNC,
		SIZE_MB, 
		ISLOCKED,
		LOCKID,
		LOCKEXPIRY,
		LOCKDATE,
		RELEASEDATE
		};

}
