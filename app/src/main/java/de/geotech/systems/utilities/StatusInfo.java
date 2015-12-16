/**
 * Class for progress and error information in background task.
 * 
 * @author Karsten
 * @author Torsten Hoch
 */

package de.geotech.systems.utilities;

public class StatusInfo {
	private int progress;
	private String message;
	
	public StatusInfo(int progress, String message) {
		this.progress = progress;
		this.message = message;
	}
	
	public int getStatus() {
		return this.progress;
	}
	
	public String getMessage() {
		return this.message;
	}
	
}
