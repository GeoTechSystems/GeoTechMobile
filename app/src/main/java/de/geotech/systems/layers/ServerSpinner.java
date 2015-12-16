/**
 * NEW:
 * creates lists of servers, reads and writes them 
 * in files and handles them in a spinner
 * 
 * creates tables, if u click on a server in the spinner
 * 
 * @author Torsten Hoch
 */

package de.geotech.systems.layers;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import de.geotech.systems.R;
import de.geotech.systems.database.DBAdapter;
import de.geotech.systems.layerTables.LayerTableLockedWFS;
import de.geotech.systems.layerTables.LayerTableWFS;
import de.geotech.systems.layerTables.LayerTableWMS;
import de.geotech.systems.projects.ProjectHandler;
import de.geotech.systems.projects.Server;

public class ServerSpinner<T> {
	private static final String CLASSTAG = "ServerSpinner";

	// der spinner
	private Spinner serverSpinner;
	private Context context;
	private String currentServerAddress;
	//	private ArrayList<String> serverWMSList;
	private ArrayList<String> serverURLList;
	private boolean wms_on;
	private int tableLayout_R_ID;

	// Constructor
	public ServerSpinner(Context context, boolean wms_on, int tableLayout_R_ID) {
		this.context = context;
		this.serverURLList = new ArrayList<String>();		
		this.wms_on = wms_on;
		this.tableLayout_R_ID = tableLayout_R_ID;
		this.serverSpinner = (Spinner) ((Activity) context).findViewById(R.id.addLayerActivity_spinner_serverlist);
		// spinner initialisieren
		this.initialize();
	}

	// Constructor 2
	public ServerSpinner(Context context, boolean reset) {
		this.context = context;
		this.serverURLList = new ArrayList<String>();
		this.wms_on = false;
		if (reset) {
			this.reset();
		}
	}

	// erstellt je eine liste von servern, die vom user
	// eingegeben werden koennen oder bereits wurden,
	// zudem wird hier die tabellenerzeugung angestossen
	public void initialize() {
		// spinner mit servereintraegen erstellen
		this.serverSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			// wenn ein server ausgewählt wird
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				// neuen angeklickten server setzen
				currentServerAddress = (String) serverSpinner.getSelectedItem();
				// Log.i(CLASSTAG, "CURRENT URL ist : " + currentServerAddress);
				// fuer WFS
				if (tableLayout_R_ID == R.id.addLayeractivity_layer_table_layout) {
					// Server ganz oben in die Liste eintragen
					serverURLList.remove(currentServerAddress);
					serverURLList.add(0, currentServerAddress);
					if (!wms_on) {
						// Layer des Servers in Tabelle bringen und neu laden!!!
						// layerTableWFS = 
						new LayerTableWFS(context, tableLayout_R_ID, ProjectHandler.getServer(currentServerAddress));
					} else {
						// fuer WMS
						// Layer des Servers in Tabelle bringen  und neu laden!!!
						// layerTableWMS = 
						new LayerTableWMS(context, tableLayout_R_ID, ProjectHandler.getServer(currentServerAddress));
					}
				} else if (tableLayout_R_ID == R.id.getFeatureWithLockActivity_layer_table_layout) {
					// Layer des Servers in Tabelle bringen und neu laden!!!
					// layerTableLockedWFS = 
					new LayerTableLockedWFS(context, tableLayout_R_ID, ProjectHandler.getServer(currentServerAddress));
				}
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {}
		});
		
//		this.serverSpinner.setOnItemLongClickListener(new OnItemLongClickListener() {
//			
//			@Override
//			public boolean onItemLongClick(AdapterView<?> parent, View view,
//					int position, long id) {
//									
//				currentServerAddress = (String) serverSpinner.getSelectedItem();
//				Toast.makeText(context, currentServerAddress + "Soll gelöscht werden", Toast.LENGTH_LONG);
//				return true;
//			}
//		});
	}

	// gets server list out of file
	private ArrayList<Server> getServerListFromPH() {
		return ProjectHandler.getServers();
	}

	/**
	 * Adds a server.
	 *
	 * @param newServer the new server
	 */
	public void addNEWServer(Server newServer) {
		// add server address to list + file of wms or wfs list + file
		if (newServer.getName() == "") {
			this.serverURLList.add(0, newServer.getUrl());
		} else {
			this.serverURLList.add(0, newServer.getName() + " (" + newServer.getUrl() + ")");
		}
		this.currentServerAddress = this.serverURLList.get(0);
		// add to projecthandler and db
		ProjectHandler.addServer(context, newServer);
	}

	/**
	 * delete server address from list
	 *
	 * @param serverURL the server url
	 */
	public void deleteServerAddress(String serverURL) {
		for (Server server: ProjectHandler.getServers()) {
			if (serverURL.equals(server.getUrl())) {
				ProjectHandler.deleteServer(context, server);
			}
		}
	}
	
	/**
	 * Adds a server to the list.
	 *
	 * @param newServer the new server
	 */
	public String initAGivenServer(Server newServer) {
		// add server address to list + file of wms or wfs list + file
		if (newServer.getName() == "") {
			return newServer.getUrl();
		} else {
			return newServer.getName() + " (" + newServer.getUrl() + ")";
		}
	}
	
	// spinner laden: mit elementen besetzen
	public void loadSpinner() {
		// standard- liste fuer server aus sql-db einladen, wenn keine server in liste
		if (this.serverURLList.size() == 0) {
			this.serverURLList = getServerURLList();
		}
		// server einsetzen
		ArrayAdapter<String> helpArray = null;
		helpArray = new ArrayAdapter<String>(this.context,
				R.layout.spinner_item, this.serverURLList);
		helpArray.setDropDownViewResource(R.layout.spinner_dropdown_item);
		// alle elemente aus spinner loeschen
		this.serverSpinner.removeAllViewsInLayout();
		// spinner neu besetzen
		this.serverSpinner.setAdapter(helpArray);
	}

	private ArrayList<String> getServerURLList() {
		ArrayList<String> urlList = new ArrayList<String>();
		for (Server s: ProjectHandler.getServers()) {
			urlList.add(s.getUrl());
		}
		return urlList;
	}

	// reset server list
	public void reset() {
		ProjectHandler.initializeServers(context);
	}

	// get WMS-Serverlist
	public ArrayList<String> getServerList() {
		return this.serverURLList;
	}

	// turn on WMS
	public void setWMSOn(boolean wms_on) {
		this.wms_on = wms_on;
	}

	// get Current Server
	public String getCurrentServerAddress() {
		return currentServerAddress;
	}

}
