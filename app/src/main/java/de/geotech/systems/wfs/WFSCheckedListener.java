/**
 * Klasse behandelt die Auswirkungen auf Checkboxaenderungen 
 * bei Layertabellen mit WFS-Layern
 * 
 * @author Torsten Hoch
 */
package de.geotech.systems.wfs;

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

public class WFSCheckedListener implements OnCheckedChangeListener {
	private static final String CLASSTAG = "WFSCheckedListener";
	// liste mit allen layern als tabelle
	private LinkedList<LayerInterface> layerList = null;
	// einstellungen/settings des aktuellen projektes
	private Project project = null;
	// liste mit allen anzuzeigenden layern
	private static List<WFSLayer> wfsCheckedList = new ArrayList<WFSLayer>();
	// wfs layer-zeile, deren checkbox geaendert wurde 
	private WFSLayer currentWFSChecked = null;
	// the Layertable
	private LayerTable layerTable;
	
	// constructor
	public WFSCheckedListener(LayerTable layerTable) {
		this(layerTable, false);
	}
	
	// second constructor
	public WFSCheckedListener(LayerTable layerTable, boolean lock) {
		this.layerList = new LinkedList<LayerInterface>(layerTable.getLayerList());
		this.layerTable = layerTable;
		this.project = ProjectHandler.getCurrentProject();
	}

	// what to do when checkbox-status is changed
	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		// wenn box geaendert, dann richtigen layer zur box finden
		currentWFSChecked = (WFSLayer) layerList.get(buttonView.getId());
		if (isChecked) {
			if (!project.getWFSContainer().contains(currentWFSChecked)) {
//				wfsCheckedList.add(currentWFSChecked);
//				project.addWFSLayer(currentWFSChecked);
//				project.writeWFSIntoDatabase(currentWFSChecked);
				project.addWFSLayerInContainerAndDB(currentWFSChecked);

			} else {
				// unnuetzes update??
				// project.updateDatabase();
			}
			this.layerTable.activateLayerTableBuilding(this.layerTable.getWFSHandler());
		}
	}

	// liste aller layer, die gezeichnet werden sollen
	public static List<WFSLayer> getWFSCheckedList() {
		return wfsCheckedList;
	}
	
	// liste zuruecksetzen
	public static void resetCheckedLists() {
		WFSCheckedListener.wfsCheckedList = new ArrayList<WFSLayer>();
	}

}
