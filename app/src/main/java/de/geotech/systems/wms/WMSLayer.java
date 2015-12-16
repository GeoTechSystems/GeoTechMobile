/**
 * WMSLayer 
 * 
 * @author Sven Weisker (uucly@student.kit.edu)
 * @author Torsten Hoch
 */

package de.geotech.systems.wms;

import android.content.Context;
import android.util.Log;

import de.geotech.systems.database.DBAdapter;
import de.geotech.systems.layers.LayerInterface;

public class WMSLayer implements LayerInterface {
	private static final String CLASSTAG = "WMSLayer";
	private Context context;

	private long layerID;
	private String name = "";
	private long projectID;	
	private String epsg = "";
	private String workspace = "";
	private String url = "";
	private String description = "";
	private String legendURL = "";
	private String logoURL = "";
	private String attributionURL = "";
	private String attributionTitle = "";
	private String attributionLogoURL = "";
	private float bbox_maxX = -1;
	private float bbox_maxY = -1;
	private float bbox_minX = -1;
	private float bbox_minY = -1;
	
	// Constructor or DB-readouts with layerID
	public WMSLayer(Context c, long layerID, String name, long projectID, 
			String epsg, String workspace, String url, String description, 
			String legendURL, String logoURL, String attributionURL, 
			String attribution_title, String attribution_logourl, 
			float bbox_maxX, float bbox_maxY, float bbox_minX, float bbox_minY) {
		this.context = c;
		this.layerID = layerID;
		this.name = name;
		this.projectID = projectID;
		this.epsg = epsg;
		this.workspace = workspace;
		this.url = url;
		this.description = description;
		this.legendURL = legendURL;
		this.logoURL = logoURL;
		this.attributionURL = attributionURL;
		this.attributionTitle = attribution_title;
		this.attributionLogoURL = attribution_logourl;
		this.bbox_maxX = bbox_maxX;
		this.bbox_maxY = bbox_maxY;
		this.bbox_minX = bbox_minX;
		this.bbox_minY = bbox_minY;
	}

	// Constructor for new instanciation without layerID
	public WMSLayer(Context c, String name, long projectID, String epsg, 
			String workspace, String url, String description, 
			String legendURL, String logoURL, String attributionURL, 
			String attribution_title, String attribution_logourl,
			float bbox_maxX, float bbox_maxY, float bbox_minX, 
			float bbox_minY) {
		this.context = c;
		this.name = name;
		this.projectID = projectID;
		this.epsg = epsg;
		this.workspace = workspace;
		this.url = url;
		this.description = description;
		this.legendURL = legendURL;
		this.logoURL = logoURL;
		this.attributionURL = attributionURL;
		this.attributionTitle = attribution_title;
		this.attributionLogoURL = attribution_logourl;
		this.bbox_maxX = bbox_maxX;
		this.bbox_maxY = bbox_maxY;
		this.bbox_minX = bbox_minX;
		this.bbox_minY = bbox_minY;
	}

	@Override
	public int getCountFeatures() {
		return 0;
	}

	@Override
	public boolean equals(Object object) {
		if (object == null) { 
			return false;
		} else if (this == object){
			return true;
		} else if (getClass() != object.getClass()) {
			return false;
		}
		WMSLayer other = (WMSLayer) object;
		if (name == null) {
			if (other.name != null)	return false;
		} else if (!name.equals(other.name)) {
			return false;
		}
		if (workspace == null) {
			if (other.workspace != null) {
				return false;
			}
		} else if (!workspace.equals(other.workspace)) {
			return false;
		}
		if (url == null) {
			if (other.url != null) {
				return false;
			}
		} else if (!url.equals(other.url)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((workspace == null) ? 0 : workspace.hashCode());
		result = prime * result + ((url == null) ? 0 : url.hashCode());
		return result;
	}

	public String getEPSG() {
		return epsg;
	}

	public String getName() {
		return name;
	}

	public String getWorkspace() {
		return workspace;
	}

	public String getURL() {
		return url;
	}

	public String getDescription() {
		return description;
	}

	public long getProjectID() {
		return projectID;
	}

	public Context getContext() {
		return context;
	}

	public long getLayerID() {
		return layerID;
	}

	public float getBbox_maxX() {
		return bbox_maxX;
	}

	public float getBbox_maxY() {
		return bbox_maxY;
	}

	public float getBbox_minX() {
		return bbox_minX;
	}

	public float getBbox_minY() {
		return bbox_minY;
	}

	public String getAttributionTitle() {
		return attributionTitle;
	}

	public String getLegendURL() {
		return legendURL;
	}

	public String getLogoURL() {
		return logoURL;
	}

	public String getAttributionURL() {
		return attributionURL;
	}

	public String getAttributionLogoURL() {
		return attributionLogoURL;
	}

	/**
	 * Sets the layer id.
	 *
	 * @param newLayerID the new layer id
	 */
	public void setLayerID(long newLayerID) {
		this.layerID = newLayerID;
	}

}
