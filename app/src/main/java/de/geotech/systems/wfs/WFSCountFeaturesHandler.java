/**
 * handler parst xml nach anzahl der features
 * 
 * @author Torsten Hoch
 */

package de.geotech.systems.wfs;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

public class WFSCountFeaturesHandler implements ContentHandler {
	private int	countFeatures;

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
	}

	@Override
	public void endDocument() throws SAXException {
		// Log.v("countFeaturesHandler", "parsing finished!");
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
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
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes atts) throws SAXException {
		// Read the number of features within the Layer
		if (localName.equals("FeatureCollection")) {
			countFeatures = Integer.parseInt(atts.getValue("numberOfFeatures"));
		}
	}

	@Override
	public void startPrefixMapping(String prefix, String uri)
			throws SAXException {
	}

	public int getResult() {
		return countFeatures;
	}

}