/**
 * 
 * @author Torsten Hoch
 */

package de.geotech.systems.database;

public class SQLAttribute {
	public static final String TABLE_NAME = "attributes";
	public static final String CREATE_TABLE = "create table attributes (" 
			+ "_idattribute integer primary key autoincrement, "
			+ "attribute_name text, "
			+ "_idwfslayer integer, "
			+ "attribute_type integer);";
	
	// Attriubte_Database_Handling
	public static final String ID = "_idattribute";
	private static final String NAME = "attribute_name";
	private static final String TYPE = "attribute_type";
	// String with all attributes
	public static final String[] ALLATTRIBUTES = new String[] {
		ID, 
		SQLWFS.ID, 
		NAME, 
		TYPE
		};
	
}
