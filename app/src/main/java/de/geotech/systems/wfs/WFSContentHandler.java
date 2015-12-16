/**
 *
 * @author Torsten Hoch
 */

package de.geotech.systems.wfs;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;


import android.content.Context;
import android.util.Log;

import de.geotech.systems.layers.LayerInterface;
import de.geotech.systems.projects.ProjectHandler;
import de.geotech.systems.utilities.Functions;
import de.geotech.systems.wfs.WFSAttributeTypeContentHandler.LayerAttributeTypesResultSet;

public class WFSContentHandler implements ContentHandler {
	private static final String CLASSTAG = "WFSContentHandler";
	
	private Context context; 
	private String url;
	private List<LayerInterface> wfsLayers;
	private StringBuilder currentValue;
	private String layerName;
	private String layerSRS;
	private String version;
	private String layerTitle;

	public WFSContentHandler(Context c){
		this.context = c;
		this.wfsLayers = new ArrayList<LayerInterface>();
	}
 
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		currentValue.append(ch, start, length);
	}

	@Override
	public void endDocument() throws SAXException {
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		// Get service version
		if (localName.equals("ServiceTypeVersion")) {
			version = currentValue.toString();
		}
		// Get layer name (workspace:name)
		else if (localName.equals("Name")) {
			layerName = currentValue.toString();
		}
		else if (localName.equals("Title")) {
			layerTitle = currentValue.toString();
		}
		// Get layer srs
		else if (localName.equals("DefaultSRS")) {
			String[] tempEPSG = currentValue.toString().split(":");
			layerSRS = "EPSG:" + tempEPSG[tempEPSG.length - 1];
		}
		// Save layer in ArrayList
		else if (localName.equals("FeatureType")) {
			String[] name = layerName.split(":");
			long projectID = ProjectHandler.getCurrentProject().getProjectID();
			LayerAttributeTypesResultSet result = WFSLoaderCon.getLayerAttributes(Functions.reviseWfsUrl(url), version, layerName);
			String layerType = "";
			String geomColumn = ""; 
			String namespace = "";
			int countFeatures = 0;
			ArrayList<WFSLayerAttributeTypes> list = null;
			if (result != null) {
				list = result.getAttributeList();
				layerType = result.getGeometryType();
				geomColumn = result.getGeometryColumn();
				namespace = result.getTargetNamespace();
				// TODO das count features muss hier mal raus, macht alles superlangsam
				// idee: tabelle baut sich erstmal mit nur nullen bei der spalte countfeatures auf,
				// dann wird sie fuer jede zeile einmal ganz ueberschrieben, wenn fuer die jeweilige zeile
				// der wert vom server erfragt wurde, dafuer brauchen wir zusaetzlichen asynctask
				// countFeatures = 0;
				countFeatures = WFSLoaderCon.countFeatures(Functions.reviseWfsUrl(url), version, layerName);
				Log.i(CLASSTAG, "Received Layer: " + layerName);
			}
			boolean ismulti = false;
			if (layerType.startsWith("Multi") || layerType.startsWith("MULTI")) {
				ismulti = true;
			}
			LayerInterface layer = new WFSLayer(context, name[1], projectID, url, layerTitle, name[0], namespace, 
					layerType, layerSRS, geomColumn, true, ismulti, -1, -1, countFeatures, true, list);
			wfsLayers.add(layer);
		}
	}

	@Override
	public void endPrefixMapping(String prefix) throws SAXException {
	}

	@Override
	public void ignorableWhitespace(char[] ch, int start, int length)
			throws SAXException {
	}

	@Override
	public void processingInstruction(String target, String data)
			throws SAXException {
	}

	@Override
	public void setDocumentLocator(Locator locator) {
	}

	@Override
	public void skippedEntity(String name) throws SAXException {
	}

	@Override
	public void startDocument() throws SAXException {
		currentValue = new StringBuilder();
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes atts) throws SAXException {
		currentValue.setLength(0);
	}

	@Override
	public void startPrefixMapping(String prefix, String uri)
			throws SAXException {
	}

	/**
	 * Sets the WFS url which is needed for the DescribeFeatureType requests.
	 * @param wfsURL
	 */
	public void setURL(String wfsURL) {
		url = wfsURL;
	}

	/**
	 * Returns ArrayList with parsed content.
	 * @return
	 */
	public List<LayerInterface> getWFSLayerList() {
		return wfsLayers;
	}

	/**
	 * Returns the WFS Version as String.
	 * @return
	 */
	public String getVersion() {
		return version;
	}

}
