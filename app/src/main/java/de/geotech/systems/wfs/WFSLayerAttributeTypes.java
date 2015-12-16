/**
 * Possible Attributes of a Layer
 * 
 * @author Torsten Hoch
 */

package de.geotech.systems.wfs;

import java.util.HashMap;
import java.util.Iterator;

import de.geotech.systems.utilities.Functions;

public class WFSLayerAttributeTypes {
	// Type ids
	public static final int UNKNOWN = -1;
	public static final int INTEGER = 0;
	public static final int MEASUREMENT = 1;
	public static final int STRING = 2;
	public static final int DATE = 3;
	public static final int DATETIME = 4;
	public static final int BOOLEAN = 5;
	public static final int DOUBLE = 6;
	public static final int DECIMAL = 7;

	private String name;
	private int type;
	private long attributeID;
	private long wfsLayerID;
	
	private HashMap<String, String> restrictions;

	// constructor to load from Import
	/**
	 * Default constructor setting name and type of the attribute.
	 * @param attrName		Attribute name.
	 * @param typeId		Attribute type id.
	 * @param restrictions	Optional restrictions; can be "null".
	 */
	public WFSLayerAttributeTypes(String attrName, int typeId, HashMap<String, String> res) {
		this.attributeID = -1;
		this.wfsLayerID = -1;
		this.name = attrName;
		// initialize restrictions
		this.restrictions = new HashMap<String, String>();
		this.type = createType(typeId, res);
	}
	
	// constructor to load from SQL DB
	public WFSLayerAttributeTypes(long attributeID, long wfsLayerID, String attrName, 
			int typeId) {
		this.attributeID = attributeID;
		this.wfsLayerID = wfsLayerID;
		this.name = attrName;
		// initialize restrictions
		this.restrictions = new HashMap<String, String>();
		this.type = createType(typeId, null);
	}
	
	/**
	 * Creates a String which describes the attribute's type with option restrictions.
	 * @param typeId			Attribute's type id.
	 * @param res				String with restrictions; please read below for correct restrictions; set "null" if there are no restrictions.
	 * @return
	 * 
	 * Possible restrictions:
	 * INTEGER: totalDigits
	 * MEASUREMENT: uom
	 * STRING: maxLength, length
	 * DATE: no restrictions
	 * DATETIME: no restrictions
	 * BOOLEAN: no restrictions
	 * DOUBLE: no restrictions
	 * DECIMAL: totalDigits, fractionDigits
	 */
	private int createType(int typeId, HashMap<String, String> res) {
		switch(typeId) {
					case INTEGER:
				if (res == null) {
					restrictions = new HashMap<String, String>();
				} else {
					if(checkRestrictions(typeId, res)) {
						restrictions = res;
					}
				}
				return typeId;
			case MEASUREMENT:
				if (res == null) {
					restrictions = new HashMap<String, String>();
				} else {
					if(checkRestrictions(typeId, res)) {
						restrictions = res;
					}
				}
				return typeId;
			case STRING:
				if (res == null) {
					restrictions = new HashMap<String, String>();
				} else {
					if(checkRestrictions(typeId, res)) {
						restrictions = res;
					}
				}
				return typeId;
			case DATE:
				return typeId;
			case DATETIME:
				return typeId;
			case BOOLEAN:
				return typeId;
			case DOUBLE:
				return typeId;
			case DECIMAL:
				if (res == null) {
					restrictions = new HashMap<String, String>();
				} else {
					if(checkRestrictions(typeId, res)) {
						restrictions = res;
					}
				}
				return typeId;
			default:
				return -1;
		}
	}
	
	/**
	 * Checks if restrictions are valid for this type. 
	 * @param typeId
	 * @param res
	 * @return
	 */
	private boolean checkRestrictions(int typeId, HashMap<String, String> res) {
		boolean correct = true;
		String[] possible = getPossibleRestrictions(typeId);
		Iterator<String> iter = res.keySet().iterator();
		while(iter.hasNext()) {
			if(!Functions.stringInArray(iter.next(), possible)) correct = false;
		}
		return correct;
	}
	
	/**
	 * Returns a string array with the names of possible restrictions for this type.
	 * @param typeId
	 * @return
	 */
	public static String[] getPossibleRestrictions(int typeId) {
		switch(typeId) {
			case INTEGER:
				return new String[]{ 
					"totalDigits"
				};
			case MEASUREMENT:
				return new String[]{
					"uom"	
				};
			case STRING:
				return new String[]{
					"maxLength",
					"length"
				};
			case DECIMAL:
				return new String[]{
					"totalDigits",
					"fractionDigits"
				};
			default:
				return new String[]{};
		}
	}
	
	public static String getDatabaseType(int typeId) {
		switch(typeId) {
			case INTEGER:
				return "INTEGER";
			case MEASUREMENT:
				return "REAL";
			case STRING:
				return "TEXT";
			case DATE:
				return "TEXT";
			case DATETIME:
				return "TEXT";
			case BOOLEAN:
				return "INTEGER";
			case DOUBLE:
				return "REAL";
			case DECIMAL:
				return "REAL";
			default:
				return "UNKNOWN";
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((restrictions == null) ? 0 : restrictions.hashCode());
		result = prime * result + type;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		WFSLayerAttributeTypes other = (WFSLayerAttributeTypes) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (restrictions == null) {
			if (other.restrictions != null)
				return false;
		} else if (!restrictions.equals(other.restrictions))
			return false;
		if (type != other.type)
			return false;
		return true;
	}
	
	/**
	 * Gets the name.
	 *
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Gets the type.
	 *
	 * @return the type
	 */
	public int getType() {
		return type;
	}
	
	/**
	 * Returns a HashMap with the attribute's restrictions.
	 * HashMap will be empty if no restrictions are set.
	 * 
	 * @return HashMap
	 */
	public HashMap<String,String> getRestrictions() {
		return restrictions;
	}
		
	public long getWFSLayerID() {
		return wfsLayerID;
	}


	public long getAttributeID() {
		return attributeID;
	}

	public void setAttributeID(long attributeID) {
		this.attributeID = attributeID;
	}
}
