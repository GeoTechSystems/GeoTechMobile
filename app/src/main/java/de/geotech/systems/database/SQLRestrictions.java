/**
 * 
 * @author Torsten Hoch
 */

package de.geotech.systems.database;

public class SQLRestrictions {
	public static final String TABLE_NAME = "restrictions";
	public static final String CREATE_TABLE = "create table restrictions (" 
			+ "_idrestriction integer primary key autoincrement, "
			+ "_idlayer integer, "
			+ "res_attribute text, "
			+ "res_type integer, " 
			+ "res_name text, " 
			+ "res_value text);";
	
	// Restriction_Database_Handling
	public static final String ID = "_idrestriction";
	private static final String NAME = "res_name";
	private static final String TYPE = "res_type";
	private static final String ATTRIBUTE = "res_attribute";
	private static final String VALUE = "res_value";
	public static final String[] RESTRICTIONATTRIBUTES = new String[] {
		ID,	
		SQLWFS.ID, 
		NAME, 
		TYPE,
		ATTRIBUTE,
		VALUE};
	
}
