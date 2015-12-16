/**
 * Provides whether a Map Overlay is able to sleep or not. This means, that a
 * sleeping Overlay will not be drawn or react on any Events.
 * 
 * @author Mathias Menninghaus 
 */

package de.geotech.systems.wms;

public interface SleepableOverlayInterface {
	public void makeSleeping();
	public void makeAwake();
}
