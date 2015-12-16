/**
 * Coordinate reference system objects
 * 
 * @author Torsten Hoch
 */
package de.geotech.systems.utilities;

public class CRS {
	// name and code for the coordinate System
	private String name;
	private int code;
	
	// constructor
	public CRS(String n, int c) {
		name = n;
		code = c;
	}
	
	public int getCode() {
		return code;
	}
	
	public String getName() {
		return name;
	}
	
	@Override
	public String toString() {
		return name + " (EPSG:" + code + ")";
	}
	
}
