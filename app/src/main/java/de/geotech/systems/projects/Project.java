/**
 * Object with all project settings
 * 
 * @author Karsten
 * @author Torsten Hoch
 */

package de.geotech.systems.projects;

import java.util.ArrayList;
import java.util.ListIterator;

import android.content.Context;
import android.util.Log;

import de.geotech.systems.database.DBAdapter;
import de.geotech.systems.features.Feature;
import de.geotech.systems.wfs.WFSCheckedListener;
import de.geotech.systems.wfs.WFSLayer;
import de.geotech.systems.wms.WMSCheckedListener;
import de.geotech.systems.wms.WMSLayer;

public class Project {
	private static final String CLASSTAG = "Project";
	public static final int AUTO_SYNC_OFF = 0;
	public static final int AUTO_SYNC_ON = 1;
	private Context context;

	private long projectID;
	private String projectName = null;
	private int epsg;	
	private int projection;
	private String description = null;
	private boolean osm;	
	private int sync;
	private int auto_sync = AUTO_SYNC_OFF;
	private boolean showMarker;
	private DBAdapter dbAdapter = null;

	// the current WFSLayer
	private WFSLayer currentWFSLayer = null;
	private ArrayList<WFSLayer> wfsContainer;
	private ArrayList<WMSLayer> wmsContainer;
	private OnSyncChangedListener listener;
	private boolean unsyncAsCyan;

	// while creating a new project - do not set a projectID!!
	public Project(Context c, String name, int epsg, String description) {
		this.context = c;
		this.projectName = name;
		this.epsg = epsg;
		this.projection = 0;
		this.description = description;
		this.osm = false;
		this.sync = 1;
		this.auto_sync = AUTO_SYNC_OFF;
		this.loadDefaultValues();
	}

	// read out of DB
	public Project(Context context, long projectID, String pName, 
			int epsg, int projection, String description, 
			boolean osm, int sync, int auto_sync) {
		this.context = context;
		this.projectID = projectID;
		this.projectName = pName;
		this.epsg = epsg;
		this.projection = projection;
		this.description = description;
		this.osm = osm;
		this.sync = sync;
		this.auto_sync = auto_sync;
		this.loadDefaultValues();
	}

	private void loadDefaultValues() {
		this.dbAdapter = new DBAdapter(context);
		this.listener = null;
		this.wmsContainer = new ArrayList<WMSLayer>();
		this.wfsContainer = new ArrayList<WFSLayer>();
		this.showMarker = true;
		this.unsyncAsCyan = true;
	}

	public boolean writeProjectIntoDatabase() {
		projectID = dbAdapter.insertProjectIntoDB(this);
		// Log.i(CLASSTAG + " writeIntoDatabase()", "Projectdata successfully written in DB.");
		return (projectID > 0);
	}

	public boolean updateThisProjectInDatabase() {
		if (dbAdapter.updateProjectInDB(this)) {
			// Log.i(CLASSTAG + " updateDatabase()", "Projectdata successfully updated!");
			return true;
		} else {
			return false;
		}
	}

	public boolean deleteProject() {
		if (dbAdapter.deleteProjectFromDB(this)) {
			return true;
		} else {
			return false;
		}
	}

	public void loadProjectsWMSFromDB(long newProjectID) {
		wmsContainer.clear();
		wmsContainer = dbAdapter.getProjectsWMSFromDB(newProjectID);
	}

	public void loadProjectsWFSFromDB(long newProjectID) {
		wfsContainer.clear();
		wfsContainer = dbAdapter.getProjectsWFSFromDB(newProjectID);
	}

	/**
	 * Returns true, if project data has been 
	 * successfully synchronized.
	 * 
	 * @return
	 */
	public boolean synchronize() {
		dbAdapter.getProjectsFromDB();
		return (sync == 1);
	}

	/**
	 * Sets the sync status.
	 * 
	 * @param isSync
	 */
	public void setSync(boolean isSync) {
		// Log.i(CLASSTAG,	"Setting synchronization status to: " + String.valueOf(isSync));
		if (isSync)	{
			sync = 1;
		}
		else {
			sync = 0;
		}
		// write();
		updateThisProjectInDatabase();
		if (listener != null) {
			listener.onSyncChanged(isSync);
		}
	}

	/**
	 * Interface for the OnSyncChangedListener.
	 */
	public interface OnSyncChangedListener {
		void onSyncChanged(boolean status);
	}

	/**
	 * Sets the OnSyncChangedListener.
	 * 
	 * @param newListener
	 */
	public void setOnSyncChangedListener(OnSyncChangedListener newListener) {
		this.listener = newListener;
	}

	public void setProjectName(String newName) {
		this.projectName = newName;
	}

	public String getProjectName() {
		return projectName;
	}

	public boolean isOSM() {
		return osm;
	}

	public void setOSM(boolean on) {
		osm = on;
	}

	public int getProjection() {
		return projection;
	}

	public int getSync() {
		return sync;
	}

	public int getAutoSync() {
		return auto_sync;
	}

	public void setAutoSync(int value) {
		auto_sync = value;
	}

	public Context getContext() {
		return context;
	}

	public int getEpsgCode() {
		return epsg;
	}

	public String getDescription() {
		return description;
	}

	/**
	 * Gets the WFS container.
	 *
	 * @return the WFS container
	 */
	public ArrayList<WFSLayer> getWFSContainer() {
		return wfsContainer;
	}

	/**
	 * Gets the WMS container.
	 *
	 * @return the WMS container
	 */
	public ArrayList<WMSLayer> getWMSContainer() {
		return wmsContainer;
	}

	/**
	 * Gets the project id.
	 *
	 * @return the project id
	 */
	public long getProjectID() {
		return projectID;
	}

	/**
	 * Gets the current wfs layer.
	 *
	 * @return the current wfs layer
	 */
	public WFSLayer getCurrentWFSLayer() {
		return currentWFSLayer;
	}

	/**
	 * Sets the current wfs layer.
	 *
	 * @param currentWFSLayer the new current wfs layer
	 */
	public void setCurrentWFSLayer(WFSLayer currentWFSLayer) {
		this.currentWFSLayer = currentWFSLayer;
	}

	/**
	 * Gets the WFS layerby id, used in long touch action to get layers by id
	 *
	 * @param layerID the layer id
	 * @return the WFS layerby id
	 */
	public WFSLayer getWFSLayerbyID(long layerID) {
		ArrayList<WFSLayer> wfsContainer = this.getWFSContainer();
		ListIterator<WFSLayer> iterContainer = wfsContainer.listIterator();
		while (iterContainer.hasNext()) {
			WFSLayer currentLayer = iterContainer.next();
			if (currentLayer.getLayerID() == layerID) {
				return currentLayer;
			}
		}
		return null;
	}

	/**
	 * Sets the marker-Overlay.
	 *
	 * @param newShowMarker the new show marker
	 */
	public void setShowMarker(boolean newShowMarker) {
		this.showMarker = newShowMarker;
	}
	
	/**
	 * Checks if is show marker.
	 *
	 * @return true, if is show marker
	 */
	public boolean isShowMarker() {
		return showMarker;
	}

	/**
	 * Adds the wfs layer in container and db.
	 *
	 * @param wfsLayer the wfs layer
	 */
	public void addWFSLayerInContainerAndDB(WFSLayer wfsLayer) {
		wfsLayer.setLayerID(dbAdapter.insertWFSLayerIntoDB(wfsLayer));
		wfsContainer.add(wfsLayer);
		WFSCheckedListener.getWFSCheckedList().add(wfsLayer);
	}
	
	/**
	 * Adds the wms layer in container and db.
	 *
	 * @param wmsLayer the wms layer
	 */
	public void addWMSLayerInContainerAndDB(WMSLayer wmsLayer) {
		wmsLayer.setLayerID(dbAdapter.insertWMSLayerIntoDB(wmsLayer));
		wmsContainer.add(wmsLayer);
		WMSCheckedListener.getWMSCheckedList().add(wmsLayer);
	}
	
	/**
	 * Delete a wfs from projects container.
	 *
	 * @param wfsLayer the layer
	 * @return true, if successful
	 */
	public boolean deleteWFSFromContainerAndDB(WFSLayer wfsLayer) {
		if (wfsLayer.isLocked()) {
			wfsLayer.release();
		}
		if (dbAdapter.deleteWFSLayerFromDB(wfsLayer)) {
			WFSCheckedListener.getWFSCheckedList().remove(wfsLayer);
			this.wfsContainer.remove(wfsLayer);
			Log.i(CLASSTAG, "Deleted Layer " + wfsLayer.getName() + " with LayerID " + wfsLayer.getLayerID()
					+ " in database: Real layerID: " + wfsLayer.getLayerID());
			return true;
		} else {
			Log.i(CLASSTAG, "Can't delete Layer " + wfsLayer.getName() + " with LayerID " + wfsLayer.getLayerID()
					+ " in database: Real layerID: " + wfsLayer.getLayerID());
			return false;
		}
	}
	
	/**
	 * Delete wms from container and db.
	 *
	 * @param wmsLayer the wms layer
	 * @return true, if successful
	 */
	public boolean deleteWMSFromContainerAndDB(WMSLayer wmsLayer) {
		if (dbAdapter.deleteWMSLayerFromDB(wmsLayer.getLayerID())) {
			WMSCheckedListener.getWMSCheckedList().remove(wmsLayer);
			this.wmsContainer.remove(wmsLayer);
			Log.i(CLASSTAG, "Deleted WMS-Layer " + wmsLayer.getName() + " with LayerID " 
					+ wmsLayer.getLayerID()	+ " in database.");
			return true;
		} else {
			Log.e(CLASSTAG, "Can't delete WMS-Layer " + wmsLayer.getName() + " with LayerID " 
					+ wmsLayer.getLayerID()	+ " in database.");
			return false;
		}
	}

	/**
	 * Checks if unsync is shown as colored cyan.
	 *
	 * @return true, if is unsync as cyan
	 */
	public boolean isUnsyncAsCyan() {
		return unsyncAsCyan;
	}

	/**
	 * Sets the unsync as cyan.
	 *
	 * @param newUnsyncAsCyan the new unsync as cyan
	 */
	public void setUnsyncAsCyan(boolean newUnsyncAsCyan) {
		this.unsyncAsCyan = newUnsyncAsCyan;
		for (WFSLayer layer : wfsContainer) {
			if (!layer.isSync()) {
				for (Feature f : layer.getFeatureContainer()) {
					if (!f.isSync()) {
						f.setColor(layer.getColor());
					}
				}
			}
		}
	}

}


//public void writeWMSIntoDatabase() {
//	for (WMSLayer layer : wmsContainer)
//		writeWMSIntoDatabase(layer);
//}

//public void writeWMSIntoDatabase(WMSLayer layer) {
//	dbAdapter.insertWMSLayerIntoDB(layer);
//}
//
//public void writeWFSIntoDatabase(WFSLayer layer) {
//	dbAdapter.insertWFSLayerIntoDB(layer);
//}
//
//public void updateWFSInDatabase(WFSLayer layer) {
//	dbAdapter.updateWFSLayerInDB(layer);
//}