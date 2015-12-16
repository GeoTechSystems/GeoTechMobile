/**
 * NEU:
 * zur erstellung von Layer tabellen
 * mit ueberschriften und titel
 * für WFS-Tabellen mit Lock-Buttons
 * 
 * @author Torsten Hoch
 */

package de.geotech.systems.layerTables;

import de.geotech.systems.locking.LockedWFSContentHandler;
import de.geotech.systems.projects.Server;
import de.geotech.systems.utilities.Functions;
import android.content.Context;

public class LayerTableLockedWFS extends LayerTable {
	private static final String CLASSTAG = "LayerTableLockedWFS";
	private static final String SERVICEWFS = "?SERVICE=WFS";
	private static final String GETCAPABILITIES = "&REQUEST=GetCapabilities";
	private static final String VERSION = "&VERSION=1.1.0";
	
	// the context
	private Context context;
	// standard URL-String
	private String urlName;
	// full URL-String
	private String fullURL;
	// the handler for WFS
	private LockedWFSContentHandler handler;
	// the server object
	private Server server;

	// constructor
	public LayerTableLockedWFS(Context context, int layout_R_ID, Server currentServer) {
		super(context, layout_R_ID);
		this.urlName = currentServer.getUrl();
		this.context = context;
		this.server = currentServer;
		this.initializeParsing();
	}

	public void initializeParsing() {
		// handler initialisieren
		this.handler = new LockedWFSContentHandler(context);
		// standard-URL dafuer setzen
		((LockedWFSContentHandler) this.handler).setURL(this.urlName);
		// komplette URL erstellen
		this.fullURL = (Functions.reviseWfsUrl(this.urlName) + SERVICEWFS + GETCAPABILITIES + VERSION);
		// initialize parsing-task
		LayerTableAsyncTask task = new LayerTableAsyncTask(this, this.handler, server);
		// task ausfuehren mit voller url
		task.execute(fullURL);
	}

}
