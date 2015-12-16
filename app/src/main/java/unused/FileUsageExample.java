/**
 * NEW:
 * creates lists of servers, reads and writes them 
 * in files and handles them in a spinner
 * 
 * creates tables, if u click on a server in the spinner
 * 
 * @author Torsten Hoch
 * @author tubatubsen
 */

package unused;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import de.geotech.systems.R;
import de.geotech.systems.layerTables.LayerTableLockedWFS;
import de.geotech.systems.layerTables.LayerTableWFS;
import de.geotech.systems.layerTables.LayerTableWMS;

public class FileUsageExample {
	private static final String CLASSTAG = "ServerSpinner";
	// namen der dateien zur speicherung der server
	private static final String wmsFile = "WMSServer.txt";
	private static final String wfsFile = "WFSServer.txt";
	// standardserver mit WMS und WFS Anbindung
	private static final String[] SERVERS = {"scc-bilbo.scc.kit.edu", "falscheURLtesten.com", "ows.terrestris.de", "scc-geodroid.scc.kit.edu"};
	
	// der spinner
	private Spinner serverSpinner;
	private Context context;
	private Activity act;
	private String currentServerAddress;
	private List<String> serverWMSList;
	private List<String> serverWFSList;
	private LayerTableWFS layerTableWFS;
	private LayerTableWMS layerTableWMS;
	private LayerTableLockedWFS layerTableLockedWFS;
	private int layout_R_ID;
	private boolean wms_on;
	private int tableLayout_R_ID;

	// Constructor
	public FileUsageExample(Context c, int layout_R_ID, boolean wms_on, int tableLayout_R_ID) {
		this.context = c;
		this.act = (Activity) context;
		this.serverWFSList = new ArrayList<String>();		
		this.serverWMSList = new ArrayList<String>();
		this.layout_R_ID = layout_R_ID;
		this.wms_on = wms_on;
		this.tableLayout_R_ID = tableLayout_R_ID;
		// spinner initialisieren
		this.initialize();
	}
	
	// Constructor 2
	public FileUsageExample(Context c) {
	 this.context = c;
	 this.act = (Activity) context;
	 this.serverWFSList = new ArrayList<String>();  
	 this.serverWMSList = new ArrayList<String>();
	 this.wms_on = false;
	}
	
	// erstellt je eine liste von servern, die vom user
	// eingegeben werden koennen oder bereits wurden,
	// zudem wird hier die tabellenerzeugung angestossen
	public void initialize() {
		// spinner mit servereintraegen erstellen
		this.serverSpinner = (Spinner) this.act.findViewById(this.layout_R_ID);
		this.serverSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			// wenn ein server ausgewählt wird
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				// neuen angeklickten server setzen
				currentServerAddress = (String) serverSpinner.getSelectedItem();
				Log.i("ServerLists", "CURRENT URL ist : " + currentServerAddress);
				// fuer WFS
				if (tableLayout_R_ID == R.id.addLayeractivity_layer_table_layout) {
					if (!wms_on) {
						// Server ganz oben in die Liste eintragen
						serverWFSList.remove(currentServerAddress);
						serverWFSList.add(0, currentServerAddress);
						// Layer des Servers in Tabelle bringen und neu laden!!!
//						layerTableWFS = new LayerTableWFS(context, tableLayout_R_ID, dbAdapter.getServerByURL(currentServerAddress));
					} else {
						// fuer WMS
						// Server ganz oben in die Liste eintragen
						serverWMSList.remove(currentServerAddress);
						serverWMSList.add(0, currentServerAddress);
						// Layer des Servers in Tabelle bringen  und neu laden!!!
//						layerTableWMS = new LayerTableWMS(context, tableLayout_R_ID, currentServerAddress);
					}
				} else if (tableLayout_R_ID == R.id.getFeatureWithLockActivity_layer_table_layout) {
					// Server ganz oben in die Liste eintragen
					serverWFSList.remove(currentServerAddress);
					serverWFSList.add(0, currentServerAddress);
					// Layer des Servers in Tabelle bringen und neu laden!!!
//					layerTableLockedWFS = new LayerTableLockedWFS(context, tableLayout_R_ID, currentServerAddress);
				}

			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {}
		});
	}
		
	// writes server list into file
	private void writeServerListIntoFile(String fileName, List<String> serverList) {
		try {
			FileOutputStream output = this.act.openFileOutput(fileName,
					Context.MODE_PRIVATE);
			Log.i(CLASSTAG, "Writing Server File: " + fileName);
			Log.i(CLASSTAG, "Number of Server for File: " + serverList.size());
			DataOutputStream dout = new DataOutputStream(output);
			dout.writeInt(serverList.size());
			// Speichert Serveradressen
			for (String server : serverList){
				dout.writeUTF(server);
				Log.i(CLASSTAG, "Es wird geschrieben: " + server);
			}
			dout.flush();
			dout.close();
		} catch (IOException e) {
			e.printStackTrace();
			Log.e(CLASSTAG, "IOExeption!");
		}
	}

	// gets server list out of file
	private List<String> getServerListFromFile(String fileName) {
		LinkedList<String> serverList = new LinkedList<String>();
		try {
			FileInputStream input = this.act.openFileInput(fileName);
			DataInputStream din = new DataInputStream(input);
			int sz = din.readInt();
			Log.i(CLASSTAG, "Reading Server File: " + fileName);
			// Serveradressen auslesen
			for (int i = 0; i < sz; i++) {
				String serverAddress = din.readUTF();
				Log.i(CLASSTAG, "Es wird gelesen: " + serverAddress + ", i = " + i);
				serverList.add(serverAddress);
			}
			din.close();
		} catch (FileNotFoundException e) {
			Log.v(CLASSTAG, "No existing Server File!");
			// e.printStackTrace();
			this.initializeOneServerList();
			return getServerListFromFile(fileName);
		} catch (IOException e) {
			Log.e(CLASSTAG, "READ- OR WRITE-ERROR!");
			e.printStackTrace();
		}
		return serverList;
	}

	// wenn Server-Liste leer, wird sie hier 
	// mit standardservern gefuellt
	private void initializeOneServerList() {
		Log.i(CLASSTAG, "Lege selbst Standardserver in Datei an!");
		for (int i = 0; i < SERVERS.length; i++) {
			addServerAddressInSpinner(SERVERS[i], wms_on);
		}
		if (this.wms_on) {
			writeServerListIntoFile(this.wmsFile, this.serverWMSList);
		} else {
			writeServerListIntoFile(this.wfsFile, this.serverWFSList);
		}
	}

	// add server address just to spinner list
	private void addServerAddressInSpinner(String serverAddress, boolean wms_on) {
		this.wms_on = wms_on;
		// add server address to list + file of wms or wfs list + file
		if (this.wms_on) {
			this.serverWMSList.add(0, serverAddress);
		} else {
			this.serverWFSList.add(0, serverAddress);
		}
	}

	// add server address to list and file
	public void addServerAddress(String serverAddress, boolean wms_on) {
		this.wms_on = wms_on;
		// add server address to list + file of wms or wfs list + file
		if (this.wms_on) {
			this.serverWMSList.add(0, serverAddress);
			writeServerListIntoFile(this.wmsFile, this.serverWMSList);
		} else {
			this.serverWFSList.add(0, serverAddress);
			writeServerListIntoFile(this.wfsFile, this.serverWFSList);
		}
	}

	// spinner laden: mit elementen besetzen
	public void loadSpinner(boolean wms_on) {
		this.wms_on = wms_on;
		ArrayAdapter<String> helpArray = null;
		// entweder liste fuer wms oder fuer wfs einladen
		if (wms_on) {
			if (this.serverWMSList.size() == 0) {
				this.serverWMSList = getServerListFromFile(wmsFile);
			}
			helpArray = new ArrayAdapter<String>(this.context,
					android.R.layout.simple_spinner_item, this.serverWMSList);
		} else {
			if (this.serverWFSList.size() == 0) {
				this.serverWFSList = getServerListFromFile(wfsFile);
			}
			helpArray = new ArrayAdapter<String>(this.context,
					android.R.layout.simple_spinner_item, this.serverWFSList);
		}
		helpArray.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		// alle elemente aus spinner loeschen
		this.serverSpinner.removeAllViewsInLayout();
		// spinner neu besetzen
		this.serverSpinner.setAdapter(helpArray);
	}

	// reset server list
	public void reset() {
	 this.initializeOneServerList();
	 this.setWMSOn(!wms_on);
	 this.initializeOneServerList();
	}
		
	// get WMS-Serverlist
	public List<String> getServerWmsList() {
		return this.serverWMSList;
	}

	// get WFS-Serverlist
	public List<String> getServerWfsList() {
		return this.serverWFSList;
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

