/**
 * 
 * @author Torsten Hoch
 */

package de.geotech.systems.wfs;

import java.util.ArrayList;
import java.util.HashMap;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import de.geotech.systems.utilities.Functions;

import android.content.Context;
import android.util.Log;

public class WFSAttributeTypeContentHandler implements ContentHandler {
	private static final String CLASSTAG = "WFSAttributeTypeContentHandler";
	private ArrayList<WFSLayerAttributeTypes> attrs = new ArrayList<WFSLayerAttributeTypes>();
	private String targetNamespace;
	private String geometryType;
	private String geometryColumn;
	private String name;
	private int type = -1;
	private String[] possibleRes = new String[]{};
	private HashMap<String,String> res;

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {

	}

	@Override
	public void endDocument() throws SAXException {
		// Log.v("CLASSTAG", "parsing finished!");
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		// Add attribute to list
		if (localName.equals("element")) {
			if (type != -1) {
				attrs.add(new WFSLayerAttributeTypes(name, type, res));
				// Log.v(CLASSTAG, "New attribute: " + name + "(" + WFSLayerAttributeTypes.getDatabaseType(type) + ")");
			}
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
		// Log.v(CLASSTAG, "Start parsing...");
		// Initialize variables
		targetNamespace = "";
		geometryType = "";
		geometryColumn = "";
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes atts) throws SAXException {
		// Read layer's namespace
		if (localName.equals("schema")) {
			targetNamespace = atts.getValue("targetNamespace");
		}
		// Read attribute's name and type (if simple format)
		else if (localName.equals("element")) {
			if (atts.getIndex("type") == -1) {
				name = atts.getValue("name");
			} else {
				name = atts.getValue("name");
				type = getTypeId(atts.getValue("type"));
				if (type == -1 && getPrefix(atts.getValue("type")).equals("gml") && atts.getValue("nillable") != null) {
					geometryType 	= cutPrefix(atts.getValue("type"));
					geometryColumn 	= atts.getValue("name");
					// Log.v(CLASSTAG, "Geometry type: " + atts.getValue("type"));
				}
				res = null;
			}
		}
		// Read attribute's type (if not simple format)
		else if (localName.equals("restriction")) {
			type = getTypeId(atts.getValue("base"));
			if (type == -1 && getPrefix(atts.getValue("base")).equals("gml")) {
				geometryType = cutPrefix(atts.getValue("base"));
				// Log.v(CLASSTAG, "Geometry type: " + atts.getValue("base"));
			}
			possibleRes = WFSLayerAttributeTypes.getPossibleRestrictions(type);
		}
		// Read attribute's restrictions
		else if (isPossibleRestriction(localName)) {
			res.put(cutPrefix(localName), atts.getValue("value"));
			// Log.v(CLASSTAG, "Added restriction: " + cutPrefix(localName) + " = " + atts.getValue("value"));
		}
	}

	@Override
	public void startPrefixMapping(String prefix, String uri)
			throws SAXException {
	}

	/**
	 * Returns the type id for possible type descriptions.
	 * @param type
	 * @return
	 */
	private int getTypeId(String type) {
		if (isType(type, "int") || isType(type, "integer") || isType(type, "short") || isType(type, "long"))
			return WFSLayerAttributeTypes.INTEGER;
		else if (isType(type, "MeasureType"))
			return WFSLayerAttributeTypes.MEASUREMENT;
		else if (isType(type, "string") || isType(type, "LanguageStringType") || isType(type, "CodeType"))
			return WFSLayerAttributeTypes.STRING;
		else if (isType(type, "date"))
			return WFSLayerAttributeTypes.DATE;
		else if (isType(type, "dateTime"))
			return WFSLayerAttributeTypes.DATETIME;
		else if (isType(type, "boolean"))
			return WFSLayerAttributeTypes.BOOLEAN;
		else if (isType(type, "double") || isType(type, "float"))
			return WFSLayerAttributeTypes.DOUBLE;
		else if (isType(type, "decimal"))
			return WFSLayerAttributeTypes.DECIMAL;
		else
			return -1;
	}

	/**
	 * Checks if attribute's type is searched type.
	 * @param type		Type to check.
	 * @param check		Type to check for.
	 * @return
	 */
	private boolean isType(String type, String check) {
		return check.equalsIgnoreCase(cutPrefix(type));
	}

	/**
	 * Cuts the prefix if existing.
	 * @param qName
	 * @return
	 */
	private String cutPrefix(String qName) {
		String[] split = qName.split(":");
		if (split.length == 1) {
			return split[0];
		} else {
			return split[1];
		}
	}

	/**
	 * Returns the prefix of given qualified name. Returns "null" if no prefix exists.
	 * @param qName
	 * @return
	 */
	private String getPrefix(String qName) {
		String[] split = qName.split(":");
		if (split.length == 1) {
			return null;
		}
		else {
			return split[0];
		}
	}

	/**
	 * Checks if recieved element is a valid restriction for this type.
	 * @param localName
	 * @return
	 */
	private boolean isPossibleRestriction(String localName) {
		return Functions.stringInArray(cutPrefix(localName), possibleRes);
	}

	/**
	 * Returns all results.
	 * @return
	 */
	public LayerAttributeTypesResultSet getResultSet() {
		return new LayerAttributeTypesResultSet(attrs, geometryType, geometryColumn, targetNamespace);
	}

	/**
	 * Returns the attribute list for the layer.
	 * @return
	 */
	public ArrayList<WFSLayerAttributeTypes> getAttributes() {
		return attrs;
	}

	/**
	 * Returns the layer's geometry type.
	 * @return
	 */
	public String getGeometryType() {
		return geometryType;
	}

	/**
	 * This class provides the result of the DescribeFeatureType request including attribute list and geometry type.
	 * @author  Karsten
	 */
	public class LayerAttributeTypesResultSet {
		private ArrayList<WFSLayerAttributeTypes> attrs;
		private String geometryType;
		private String geometryColumn;
		private String targetNamespace;

		/**
		 * Default constructor setting attributes and geometry type.
		 * @param attributes
		 * @param geomType
		 * @param namespace
		 */
		public LayerAttributeTypesResultSet(ArrayList<WFSLayerAttributeTypes> attributes, String geomType, String geomColumn, String namespace) {
			attrs = attributes;
			geometryType = geomType;
			geometryColumn = geomColumn;
			targetNamespace = namespace;
		}

		/**
		 * Returns the attribute list.
		 * @return
		 */
		public ArrayList<WFSLayerAttributeTypes> getAttributeList() {
			return attrs;
		}

		/**
		 * Returns the geometry type.
		 * @return
		 */
		public String getGeometryType() {
			return geometryType;
		}

		/**
		 * Returns name of the geometry column.
		 * @return
		 */
		public String getGeometryColumn() {
			return geometryColumn;
		}

		/**
		 * Returns the layer's namespace.
		 * @return
		 */
		public String getTargetNamespace() {
			return targetNamespace;
		}
	}

}
