/**
 * Handler for parsing a XML Document returned by a WMS Server on a valid
 * GetCapabilities call. It will estimate a valid and well formed XML based on
 * the currently used OGC Specification.
 * 
 * Based upon OGC 01-068r3 but not yet full!
 * 
 * @author Mathias Menninghaus
 * @author Torsten Hoch
 * 
 */

package de.geotech.systems.wms;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.content.Context;
import android.util.Log;
import de.geotech.systems.layers.LayerInterface;
import de.geotech.systems.projects.ProjectHandler;
import de.geotech.systems.wms.ParsedWMSDataSetModel.ParsedLayer;

public class WMSDefaultHandler extends DefaultHandler {
	public static final String PNGFORMAT = "image/png";
	public static final String DT = "WMSDefaultHandler";
	// tags in xml
	private static final String WMT_MS_Capabilities = "WMT_MS_Capabilities";
	private static final String Service = "Service";
	private static final String Name = "Name";
	private static final String Title = "Title";
	private static final String Abstract = "Abstract";
	private static final String GetMap = "GetMap";
	private static final String Get = "Get";
	private static final String OnlineResource = "OnlineResource";
	private static final String Layer = "Layer";
	private static final String SRS = "SRS";
	private static final String Attribution = "Attribution";
	private static final String LogoURL = "LogoURL";
	private static final String LatLonBoundingBox = "LatLonBoundingBox";
	private static final String LegendURL = "LegendURL";
	private static final String Style = "Style";
	private static final String Format = "Format";
	private static final String Request = "Request";
	private static final String Capability = "Capability";
	// Attributes
	private static final String version = "version";
	private static final String minx = "minx";
	private static final String miny = "miny";
	private static final String maxx = "maxx";
	private static final String maxy = "maxy";
	private static final String queryable = "queryable";
	private static final String one = "1";
	private static final String zero = "0";
	// some attributes have a namespace and a name
	private static final String XLINK_NAMESPACE = "http://www.w3.org/1999/xlink";
	private static final String href = "href";
	private static final String CLASSTAG = "WMSDefaultHandler";
	// in-tag Flags
	private boolean in_WMT_MS_Capabilities;
	private boolean in_Request;
	private boolean in_Capability;
	private boolean in_Service;
	private boolean in_GetMap;
	private boolean in_GetMap_Get;
	private boolean in_queryable_Layer;
	private boolean in_Attribution;
	private boolean in_LogoURL;
	private boolean in_Style;
	private boolean in_LegendURL;
	private boolean in_Name;
	private boolean in_Title;
	private boolean in_Abstract;
	private boolean in_SRS;
	private boolean in_GetMap_Format;
	// other
	private String url="";
	private List<LayerInterface> wmsContainer = new ArrayList<LayerInterface>();
	private final Context context;
	// The currently parsedData
	private ParsedWMSDataSetModel parsedData;
	// Actual Layer in the ParsedData (shall always be definite)
	private ParsedLayer actLayer = null;
	private StringBuffer charBuffer;
	private String name ="";
	private String epsg ="";
	private String workspace ="";
	private String description ="";	
	private String legendURL ="";
	private String logoURL ="";
	private String attributionURL ="";
	private String attribution_title ="";
	private String attribution_logourl ="";
	private float bbox_maxX = -1;
	private float bbox_maxY = -1;
	private float bbox_minX = -1;
	private float bbox_minY = -1;

	/**
	 * Instantiates a new WMS default handler.
	 *
	 * @param context the Context
	 */
	public WMSDefaultHandler(Context context){
		this.context = context;
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		//		Log.e(CLASSTAG + " - parsing", "localName: " + localName + " - qName: " + qName + " - # of Attributes: " + attributes.getLength());
		// begin to read the document
		if (!in_WMT_MS_Capabilities) {
			if (qName.equals(WMT_MS_Capabilities)) {
				parsedData = new ParsedWMSDataSetModel();
				parsedData.version = attributes.getValue(version);
				//parsedData.
				in_WMT_MS_Capabilities = true;
			}
		} else {
			// decide between service and capability
			if (!in_Service && !in_Capability) {
				if (qName.equals(Service)) {
					in_Service = true;
				} else if (qName.equals(Capability)) {
					in_Capability = true;
				}
				// everything in service
			} else if (in_Service) {
				// everything between the service tag
				if (qName.equals(Name)) {
					charBuffer = new StringBuffer();
					in_Name = true;
				} else if (qName.equals(Title)) {
					charBuffer = new StringBuffer();
					in_Title = true;
				} else if (qName.equals(Abstract)) {
					charBuffer = new StringBuffer();
					in_Abstract = true;
				} else if (qName.equals(OnlineResource)) {
					parsedData.url = attributes.getValue(XLINK_NAMESPACE, href);
				}
			} else if (in_Capability) {
				// either parsedLayer or request information
				if (!in_Request && !in_queryable_Layer) {
					if (qName.equals(Layer)) {
						if (attributes.getValue(queryable) != null) {
							if (attributes.getValue(queryable).equals(one) || attributes.getValue(queryable).equals(zero)) {
								// Log.e(CLASSTAG + " - parsing", "We have a queryable Layer! --> queryable = " + attributes.getValue(queryable));
								in_queryable_Layer = true;
								this.initParsedLayer();
							} else {
								// Log.e(CLASSTAG + " - parsing", "We have an unqueryable Layer! --> queryable = " + attributes.getValue(queryable));
							}
						}
					} else if (qName.equals(Request)) {
						in_Request = true;
					}
				} else if (in_Request) {
					if (qName.equals(GetMap)) {
						in_GetMap = true;
					} else if (in_GetMap && qName.equals(Get)) {
						in_GetMap_Get = true;
					} else if (in_GetMap && qName.equals(Format)) {
						charBuffer = new StringBuffer();
						in_GetMap_Format = true;
					} else if (in_GetMap_Get && qName.equals(OnlineResource)) {
						parsedData.getMapURL = attributes.getValue(XLINK_NAMESPACE, href);
					}
				} else if (in_queryable_Layer) {
					if (!in_Attribution && !in_Style) {
						// everything between parsedLayer tag
						if (qName.equals(Layer)) {
							// should be impossible
						} else if (qName.equals(Name)) {
							charBuffer = new StringBuffer();
							in_Name = true;
						} else if (qName.equals(Title)) {
							charBuffer = new StringBuffer();
							in_Title = true;
						} else if (qName.equals(Abstract)) {
							charBuffer = new StringBuffer();
							in_Abstract = true;
						} else if (qName.equals(Style)) {
							charBuffer = new StringBuffer();
							in_Style = true;
						} else if (qName.equals(Attribution)) {
							charBuffer = new StringBuffer();
							in_Attribution = true;
						} else if (qName.equals(SRS)) {
							charBuffer = new StringBuffer();
							in_SRS = true;
						} else if (qName.equals(LatLonBoundingBox)) {
							this.bbox_maxX = Float.valueOf(attributes.getValue(maxx));
							actLayer.bbox_maxx = Float.valueOf(attributes.getValue(maxx));
							this.bbox_maxY = Float.valueOf(attributes.getValue(maxx));
							actLayer.bbox_maxy = Float.valueOf(attributes.getValue(maxy));
							this.bbox_minX = Float.valueOf(attributes.getValue(maxx));
							actLayer.bbox_minx = Float.valueOf(attributes.getValue(minx));
							this.bbox_minY = Float.valueOf(attributes.getValue(maxx));
							actLayer.bbox_miny = Float.valueOf(attributes.getValue(miny));
						}
					} else if (in_Attribution) {
						if (qName.equals(Title)) {
							charBuffer = new StringBuffer();
							in_Title = true;
						} else if (qName.equals(LogoURL)) {
							in_LogoURL = true;
						} else if (!in_LogoURL
								&& qName.endsWith(OnlineResource)) {
							this.attributionURL = attributes.getValue(XLINK_NAMESPACE, href);
							actLayer.attribution_url = attributes.getValue(XLINK_NAMESPACE, href);
						} else if (in_LogoURL) {
							if (qName.equals(OnlineResource)) {
								this.logoURL = attributes.getValue(XLINK_NAMESPACE, href);
								actLayer.attribution_logourl = attributes.getValue(XLINK_NAMESPACE, href);
							}
						}
					} else if (in_Style) {
						if (qName.equals(LegendURL)) {
							in_LegendURL = true;
						} else if (in_LegendURL) {
							if (qName.equals(OnlineResource)) {
								this.legendURL = attributes.getValue(XLINK_NAMESPACE, href);
								actLayer.legend_url = attributes.getValue(XLINK_NAMESPACE, href);
							}
						}
					}
				}
			}
		}
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		if (in_Service) {
			if (in_Name) {
				charBuffer.append(ch, start, length);
			} else if (in_Title) {
				charBuffer.append(ch, start, length);
			} else if (in_Abstract) {
				charBuffer.append(ch, start, length);
			}
		} else if (in_Capability) {
			if (in_queryable_Layer) {
				if (in_Attribution) {
					if (in_Title) {
						charBuffer.append(ch, start, length);
					}
				} else if (in_Name) {
					charBuffer.append(ch, start, length);
				} else if (in_Title) {
					charBuffer.append(ch, start, length);
				} else if (in_Abstract) {
					charBuffer.append(ch, start, length);
				} else if (in_SRS) {
					charBuffer.append(ch, start, length);
				}
			} else if (in_Request) {
				if (in_GetMap_Format) {
					charBuffer.append(ch, start, length);
				}
			}
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		// everything in service
		if (in_Service) {
			// everything between the service tag
			if (qName.equals(Service)) {
				in_Service = false;
			} else if (in_Name && qName.equals(Name)) {
				//parsedData.name = charBuffer.toString();
				in_Name = false;
			} else if (in_Title && qName.equals(Title)) {
				// this.title = charBuffer.toString();
				in_Title = false;
			} else if (in_Abstract && qName.equals(Abstract)) {
				//parsedData.description = charBuffer.toString();
				in_Abstract = false;
			}
		} else if (in_Capability) {
			// either parsedLayer or request information
			if (in_Request) {
				if (in_GetMap && qName.equals(GetMap)) {
					in_GetMap = false;
				} else if (in_GetMap_Get && qName.equals(Get)) {
					in_GetMap_Get = false;
				} else if (in_GetMap_Format && qName.equals(Format)) {
					if (charBuffer.toString().equals(PNGFORMAT)) {
						//parsedData.supportsPNG = true;
					}
					in_GetMap_Format = false;
				} else if (qName.equals(Request)) {
					in_Request = false;
				}
			} else if (in_queryable_Layer) {
				if (!in_Attribution && !in_Style) {
					// everything between parsedLayer tag
					if (in_Name && qName.equals(Name)) {
						this.name = charBuffer.toString();
						//actLayer.setName(charBuffer.toString());
						in_Name = false;
					} else if (in_Title && qName.equals(Title)) {
						//actLayer.setTitle(charBuffer.toString());
						this.workspace = charBuffer.toString();
						in_Title = false;
					} else if (in_Abstract && qName.equals(Abstract)) {
						//actLayer.description = charBuffer.toString();
						this.description = charBuffer.toString();
						in_Abstract = false;
					} else if (in_SRS && qName.equals(SRS)) {
						String[] strings = charBuffer.toString().split(" ");
						for (String s : strings) {
							//	actLayer.setEPSG(s.toUpperCase());
							this.epsg = s.toUpperCase();
							//	actLayer.parsedSRS.add(s.toUpperCase());
						}
						in_SRS = false;
					} else if (qName.equals(Layer)) {
							if (in_queryable_Layer) {
								insertNewLayer();
								in_queryable_Layer = false;
							}
							rootLayer();
					}
				} else if (in_Attribution) {
					if (in_Title && qName.equals(Title)) {
						//actLayer.attribution_title = charBuffer.toString();
						this.attribution_title = charBuffer.toString();
						in_Title = false;
					} else if (in_LogoURL && qName.equals(LogoURL)) {

						in_LogoURL = false;
					} else if (qName.equals(Attribution)) {
						in_Attribution = false;
					}
				} else if (in_Style) {
					if (in_LegendURL && qName.equals(LegendURL)) {
						in_LegendURL = false;
					} else if (qName.equals(Style)) {
						in_Style = false;
					}
				}
			} else if (qName.equals(Capability)) {
				in_Capability = false;
			}
		}
	}

	private void insertNewLayer() {
		// insert a new found layer
		if (!name.equals("")) {
			Log.i(CLASSTAG + " - startNewLayer()", "New Layer found. Name: " + name);
			wmsContainer.add(new WMSLayer(
					context, name, ProjectHandler.getCurrentProject().getProjectID(), epsg, 
					workspace, url, description, legendURL,	logoURL, attributionURL, 
					attribution_title, attribution_logourl, bbox_maxX, bbox_maxY, bbox_minX, 
					bbox_minY));
		} else {
			Log.e(CLASSTAG + " - startNewLayer()", "Layer without name - No new Layer Entered!");
		}
	}

	private void initParsedLayer() {
		// Log.e(CLASSTAG + " - initParsedlayer()", "Initialising ParsedLayer!");
		if (actLayer == null) {
			// Log.e(CLASSTAG + " initParsedlayer()", "actLayer == null!!!");
			actLayer = parsedData.new ParsedLayer();
		} else {
			// Log.e(CLASSTAG + " initParsedlayer()", "actLayer != null!!!");
			ParsedLayer newLayer = parsedData.new ParsedLayer(actLayer);
			actLayer = newLayer;
		}
		actLayer.setMapURL(this.url);	
	}
	
	private void rootLayer() {
		// Log.e(CLASSTAG + " - rootLayer()", "Re-Rooting Layer");
		ParsedLayer newLayer = parsedData.new ParsedLayer(actLayer);
		actLayer = newLayer;
	}
	
	/**
	 * Returns the parsedData
	 * 
	 * @return the Data parsed with this Handler
	 */
	public ParsedWMSDataSetModel getParsedData() {
		return parsedData;
	}

	/**
	 * Sets the url.
	 *
	 * @param url the new url
	 */
	public void setURL(String url){
		this.url = url;
	}

	/**
	 * Gets the wms container.
	 *
	 * @return the wms container
	 */
	public List<LayerInterface> getWmsContainer() {
		return wmsContainer;
	}
}
