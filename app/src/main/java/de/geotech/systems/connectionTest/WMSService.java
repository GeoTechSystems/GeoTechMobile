/**
 * Helper class for ServerResponseHandler.java
 * 
 * @author tubatubsen
 */

package de.geotech.systems.connectionTest;

public class WMSService {
	public String wmsTitle;
	public String wmsAbstract;
	public String wmsType;
	public String wmsVersion;
	
	public String organization;
	public String person;
	public String position;
	public String city;
	public String country;
	
	public WMSService() {
		wmsTitle = "unknown";
		wmsAbstract = "unknown";
		wmsType = "unknown";
		wmsVersion = "unknown";
		
		organization = "unknown";
		person = "unknown";
		position = "unknown";
		city = "unknown";
		country = "unknown";
	}
}
