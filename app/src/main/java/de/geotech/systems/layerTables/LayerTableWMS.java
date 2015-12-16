/**
 * NEU:
 * zur erstellung von Layer tabellen
 * mit ueberschriften und titel
 * für WMS-Tabellen
 * 
 * @author Torsten Hoch
 */

package de.geotech.systems.layerTables;

import de.geotech.systems.projects.Server;
import de.geotech.systems.utilities.Functions;
import de.geotech.systems.wms.WMSDefaultHandler;
import android.content.Context;

public class LayerTableWMS extends LayerTable {
	private static final String CLASSTAG = "LayeTableWMS";
	private static final String SERVICEWMS = "?SERVICE=WMS";
	private static final String GETCAPABILITIES = "&REQUEST=GetCapabilities";
	private static final String VERSION = "&VERSION=1.1.1";
	
	// the context
	private Context context;
	// standard URL-String
	private String urlName;
	// full URL-String
	private String fullURL;
	// the handler for WMS
	private WMSDefaultHandler handler;
	// the server object
	private Server server;

	// constructor
	public LayerTableWMS(Context context, int layout_R_ID, Server currentServer) {
		super(context, layout_R_ID);
		this.urlName = currentServer.getUrl();
		this.context = context;
		this.server = currentServer;
		this.initializeParsing();
	}

	public void initializeParsing() {
		// handler initialisieren
		this.handler = new WMSDefaultHandler(this.context);
		// standard-URL dafuer setzen
		((WMSDefaultHandler) handler).setURL(this.urlName);
		// komplette URL erstellen
		this.fullURL = Functions.reviseWmsUrl(this.urlName)	+ SERVICEWMS + GETCAPABILITIES + VERSION;
		// initialize parsing-task
		LayerTableAsyncTask task = new LayerTableAsyncTask(this, this.handler, server);
		// task ausfuehren mit voller url
		task.execute(this.fullURL);
	}

}
