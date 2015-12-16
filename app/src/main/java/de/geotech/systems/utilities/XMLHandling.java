package de.geotech.systems.utilities;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class XMLHandling {

	/**
	 * Gibt den Attributwert eines XML Dokuments zurueck. Vorsicht: Nur den
	 * Ersten!
	 * 
	 * @param attribute
	 * @return
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws IOException
	 */
	public static String getAttributeValue(String xml, String attribute) {

		int start = xml.indexOf(attribute) + attribute.length() + 2;
		int end = xml.indexOf("\"", start + 2);

		if (start == attribute.length() + 1)
			return null;
		return xml.substring(start, end);
	}
}
