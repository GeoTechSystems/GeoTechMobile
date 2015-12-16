/**
 * 
 * @author Torsten Hoch
 */

package de.geotech.systems.layers;

public interface LayerInterface {
	public String getWorkspace();
	public String getName();
	public String getEPSG();
	public int getCountFeatures();
}
