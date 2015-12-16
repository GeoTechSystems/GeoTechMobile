/**
 * Klasse behandelt die Auswirkungen auf Checkboxaenderungen 
 * bei Layertabellen mit WMS-Layern
 * 
 * @author Torsten Hoch
 */
package de.geotech.systems.wms;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.util.Log;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import de.geotech.systems.layerTables.LayerTable;
import de.geotech.systems.layers.LayerInterface;
import de.geotech.systems.projects.Project;
import de.geotech.systems.projects.ProjectHandler;

public class WMSCheckedListener implements OnCheckedChangeListener{
	private static final String CLASSTAG = "WMSCheckedListener";
	// liste mit allen layern als tabelle
	private LinkedList<LayerInterface> layerList = null;
	// einstellungen/settings des aktuellen projektes
	private Project project = null;
	// liste mit allen anzuzeigenden layern
	private static List<WMSLayer> wmsCheckedList = new ArrayList<WMSLayer>();
	// wms layer-zeile, deren checkbox geaendert wurde 
	private WMSLayer currentWMSChecked = null;
	// the Layertable
	private LayerTable layerTable;

	// constructor
	public WMSCheckedListener(LayerTable layerTable){
		this.layerList = new LinkedList<LayerInterface>(layerTable.getLayerList());
		this.layerTable = layerTable;
		this.project = ProjectHandler.getCurrentProject();
	}
	
	// what to do when checkbox-status is changed
	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		// wenn box geaendert, dann richtigen layer zur box finden
		currentWMSChecked = (WMSLayer) layerList.get(buttonView.getId());
		if (isChecked) {
			if (!project.getWMSContainer().contains(currentWMSChecked)) {
				Log.i(CLASSTAG, "Adding WMS-Layer " + currentWMSChecked.getName());
//				wmsCheckedList.add(currentWMSChecked);
				project.addWMSLayerInContainerAndDB(currentWMSChecked);
//				project.getWMSContainer().add(currentWMSChecked);
//				project.writeWMSIntoDatabase(currentWMSChecked);
			} else {
				Log.i(CLASSTAG, "WMS-Layer " + currentWMSChecked.getName() + " alreadys contained. Not added again.");
			}
			this.layerTable.activateLayerTableBuilding(this.layerTable.getWMSHandler());
		}
		
	}
	
	// liste aller layer, die gezeichnet werden sollen
	public static List<WMSLayer> getWMSCheckedList() {
		return wmsCheckedList;
	}
	
	// liste zuruecksetzen
	public static void resetCheckedLists() {
		WMSCheckedListener.wmsCheckedList = new ArrayList<WMSLayer>();
	}
}
