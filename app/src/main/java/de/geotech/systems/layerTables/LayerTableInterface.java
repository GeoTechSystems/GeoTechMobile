/**
 * NEU:
 * interface zur erstellung von tabellen, in denen 
 * Layer angezeigt werden
 * 
 * @author Torsten Hoch
 */

package de.geotech.systems.layerTables;

import java.util.List;

import org.xml.sax.ContentHandler;

import de.geotech.systems.layers.LayerInterface;
import de.geotech.systems.wfs.WFSContentHandler;
import de.geotech.systems.wms.WMSDefaultHandler;

import android.widget.TableLayout;

public interface LayerTableInterface {
	// tabelle leeren bzw. alle elemente aus tabelle löschen
	public void clearTable();
	// gibt die tabelle als tabellenlayout zureuck 
	public TableLayout getTable();
	// gibt die tabelleninhalte ihne ueberschriften als liste zurueck
	public List<LayerInterface> getLayerList();
	// Menge an Layern in der tabelle ausgeben 
	public int size();
	// enthaelt die tabelle den layer checkLayer?!
	public boolean contains(LayerInterface checkLayer);
	// tabellen bauen starten
	public boolean activateLayerTableBuilding(ContentHandler handler);
	// komplette Liste der vies bekommen
	public LayerTableViewList getViewListForTables();
	// get handler for WFS
	public WFSContentHandler getWFSHandler();
	// get handler for WMS
	public WMSDefaultHandler getWMSHandler();
}
