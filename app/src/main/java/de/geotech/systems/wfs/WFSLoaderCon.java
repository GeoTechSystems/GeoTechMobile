/**
 * This class provides functions to load attributes of a given WFS.
 * 
 * @author Torsten Hoch
 */

package de.geotech.systems.wfs;

import java.net.URL;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import de.geotech.systems.wfs.WFSAttributeTypeContentHandler.LayerAttributeTypesResultSet;

//import android.content.Context;
import android.util.Log;

public class WFSLoaderCon {
	private static final String CLASSTAG = "WFSLoaderCon";
	public static final String SERVICE = "?SERVICE=WFS";
	public static final String GETCAPABILITIES = "&REQUEST=getCapabilities";
	public static final String DESCRIBEFEATURETYPE = "&REQUEST=describeFeatureType";
	public static final String COUNTFEATURE = "&REQUEST=GetFeature&resultType=hits";
	public static final String VERSION = "&VERSION=1.1.0";
	private String	version;

	/**
	 * Default Constructor setting context and WFS-Url.
	 * @param wfsurl
	 */
	public WFSLoaderCon() {
		this.version 	= "1.1.0";
	}

	/**
	 * Requests the layer's attributes via DescribeFeatureType request.
	 * @param ver 			Service version
	 * @param typename		Layer's name
	 * @return
	 */
	public static LayerAttributeTypesResultSet getLayerAttributes(String url, String ver, String typename) {
		try {
			// Request url
			URL request = new URL(url + SERVICE + DESCRIBEFEATURETYPE + "&TYPENAME=" + typename + VERSION);
			Log.i(CLASSTAG + " AttributeTypes", "URL: " + request.toString());
			// Define xml reader and content handler
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser parser = spf.newSAXParser();
			XMLReader reader = parser.getXMLReader();
			WFSAttributeTypeContentHandler handler = new WFSAttributeTypeContentHandler();
			reader.setContentHandler(handler);
			// Parse xml string
			reader.parse(new InputSource(request.openStream()));
			// Return ArrayList
			return handler.getResultSet();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Count the Features within the layer
	 * @param ver 			Service version
	 * @param typename		Layer's name
	 * @return
	 */
	public static int countFeatures(String url, String ver, String layerName) {
		try {
			// Request url
			URL request = new URL(url + SERVICE + COUNTFEATURE + "&TYPENAME=" + layerName + VERSION);
			Log.i(CLASSTAG + " countFeatures", "URL: " + request.toString());
			// Define xml reader and content handler
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser parser = spf.newSAXParser();
			XMLReader reader = parser.getXMLReader();
			WFSCountFeaturesHandler handler = new WFSCountFeaturesHandler();
			reader.setContentHandler(handler);
			// Parse xml string
			reader.parse(new InputSource(request.openStream()));
			// Return ArrayList
			return handler.getResult();
		} catch (Exception e) {
			e.printStackTrace();
			return (Integer) null;
		}
	}

	/**
	 * Returns the determined WFS version.
	 * @return
	 */
	public String getVersion() {
		return version;
	}

}
