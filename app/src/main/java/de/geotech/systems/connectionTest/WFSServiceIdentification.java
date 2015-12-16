/**
 * Helper class for ServerResponseHandler.java
 * 
 * @author tubatubsen
 */

package de.geotech.systems.connectionTest;

public class WFSServiceIdentification {
	public String wfsTitle;
	public String wfsAbstract;
	public String wfsType;
	public String wfsVersion;

	public WFSServiceIdentification() {
		wfsTitle = "unknown";
		wfsAbstract = "unknown";
		wfsType = "unknown";
		wfsVersion = "unknown";
	}

}
