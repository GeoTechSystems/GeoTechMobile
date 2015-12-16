/**
 * Simple data storage for from the {@link WMSDefaultHandler} Handler
 * Based upon OGC 01-068r3 but not yet full!
 * 
 * @author Mathias Menninghaus
 * @author Torsten Hoch
 * @version 02.10.2009
 */

package de.geotech.systems.wms;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import de.geotech.systems.layers.LayerInterface;

public class ParsedWMSDataSetModel {
	private static final String CLASSTAG = "ParsedWMSDataSet";

	public String url = "";
	public String version = "1.0.0";
	public String name;
	public String title;
	public String description = "";
	public String getMapURL;
	public ParsedLayer rootLayer;
	public boolean supportsPNG;

	// Inner class that represents a WMS - Layer
	public class ParsedLayer implements LayerInterface {
		public HashSet<String> parsedSRS = new HashSet<String>();
		public List<LayerInterface> parsedLayers = new LinkedList<LayerInterface>();
		public float bbox_maxx;
		public float bbox_maxy;
		public float bbox_miny;
		public float bbox_minx;
		public String legend_url;
		public String attribution_logourl;
		public String attribution_url;
		public String attribution_title;
		public ParsedLayer rootLayer;
		public String description;
		private String name;
		private String title;
		private String epsg="";
		private String mapURL="";
        public Integer ID;
        
		/**
		 * Constructs a ParsedLayer and sets it as root layer in the
		 * corresponding ParsedWMSDataSet.
		 * 
		 * @throws IllegalStateException 	if the corresponding ParsedWMSDataSet 
		 * 									already has a root layer
		 */
		public ParsedLayer() {
			if (ParsedWMSDataSetModel.this.rootLayer == null) {
				ParsedWMSDataSetModel.this.rootLayer = this;
			} else {
				throw new IllegalStateException(
						"ParsedWMSDataSet rootlayer already defined: cannot build parsedLayer without exlpicit rootLayer.");
			}
		}

		/**
		 * This is not a copy constructor! Sets the given ParsedLayer as root
		 * layer for the new constructed and adds the new one to the given root.
		 * 
		 * @throws IllegalStateException
		 *             if the corresponding ParsedWMSDataSet has not a root
		 *             layer.
		 */
		public ParsedLayer(ParsedLayer root) {
			if (ParsedWMSDataSetModel.this.rootLayer == null) {
				throw new RuntimeException(
						"no root parsedLayer defined in corresponding ParsedWMSDataSet");
			}
			this.rootLayer = root;
			root.parsedLayers.add(this);
		}
               
		/**
		 * Returns name and title of this ParsedLayer
		 */
		public String toString() {
			return name + "|" + title;
		}

		public String getEPSG() {
			return epsg;
		}

		public void setEPSG(String epsg) {
			this.epsg = epsg;
		}
		
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getWorkspace() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}
		
		public void setURL(String url){
			ParsedWMSDataSetModel.this.url = url;
		}

		public String getURL(){
			return url;
		}
		/**
		 * @author Sven Weisker (uucly@student.kit.edu)
		 * diese Methode muss noch gefüllt werden
		 */
		public boolean loadFromDatabase() {
			return true;
		}

		@Override
		public int getCountFeatures() {
			return 0;
		}

		public boolean updateWFSDatabase() {
			return false;
		}

		public boolean wfsWriteIntoDatabase() {
			return false;
		}

		public String getMapURL() {
			return mapURL;
		}

		public void setMapURL(String mapURL) {
			this.mapURL = mapURL;
		}

	}
	
}
