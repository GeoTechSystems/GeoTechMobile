/**
 * Handles the server response after testing connection
 * 
 * @author Torsten Hoch
 * @author tubatubsen
 */

package de.geotech.systems.connectionTest;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

public class ServerResponseHandler implements ContentHandler {
	private StringBuilder currentValue;

	private boolean validServer;
	private boolean isWFS;
	private WFSServiceIdentification wfsServiceIdent;
	private WFSServiceProvider wfsProvider;
	private WMSService wmsService;
	private boolean inwfsIdent;
	private boolean inwfsProvider;
	private boolean inwmsIdent;
	
	@Override
	public void startDocument() throws SAXException {
		currentValue = new StringBuilder();
		validServer = false;
		isWFS = false;

		wfsServiceIdent = new WFSServiceIdentification();
		wfsProvider = new WFSServiceProvider();
		inwfsIdent = false;
		inwfsProvider = false;

		wmsService = new WMSService();
		inwmsIdent = false;
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes atts) throws SAXException {
		currentValue.setLength(0);
		if (localName.equals("WFS_Capabilities")) {
			isWFS = true;
			wfsServiceIdent.wfsVersion = getVersion(atts);
		} 
		else if (localName.equals("WMS_Capabilities")) {
			isWFS = false;
			wmsService.wmsVersion = getVersion(atts);
		} 
		else if (localName.equals("ServiceIdentification"))
			inwfsIdent = true;
		else if (localName.equals("ServiceProvider"))
			inwfsProvider = true;
		else if (localName.equals("Service"))
			inwmsIdent = true;
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {

		if (isWFS) {
			if (localName.equals("ServiceIdentification"))
				inwfsIdent = false;
			else if (localName.equals("ServiceProvider"))
				inwfsProvider = false;
			else if (localName.equals("WFS_Capabilities"))
				validServer = true;
			// get WFS Informations
			else if (inwfsIdent) {
				// Get identification values
				if (localName.equals("Title"))
					wfsServiceIdent.wfsTitle = currentValue.toString();
				else if (localName.equals("Abstract"))
					wfsServiceIdent.wfsAbstract = currentValue.toString();
				else if (localName.equals("ServiceType"))
					wfsServiceIdent.wfsType = currentValue.toString();
			} else if (inwfsProvider) {
				// Get wfsProvider values
				if (localName.equals("ProviderName"))
					wfsProvider.name = currentValue.toString();
				else if (localName.equals("IndividualName"))
					wfsProvider.individual = currentValue.toString();
				else if (localName.equals("PositionName"))
					wfsProvider.position = currentValue.toString();
				else if (localName.equals("City"))
					wfsProvider.city = currentValue.toString();
				else if (localName.equals("Country"))
					wfsProvider.country = currentValue.toString();
			}
		} else {
			// get WMS Informations
			if (localName.equals("Service"))
				inwmsIdent = false;
			else if (localName.equals("WMS_Capabilities"))
				validServer = true;
			else if (inwmsIdent) {
				// Get identification values
				if (localName.equals("Title"))
					wmsService.wmsTitle = currentValue.toString();
				else if (localName.equals("Abstract"))
					wmsService.wmsAbstract = currentValue.toString();
				else if (localName.equals("Name"))
					wmsService.wmsType = currentValue.toString();
				// Get wfsProvider values
				else if (localName.equals("ContactOrganization"))
					wmsService.organization = currentValue.toString();
				else if (localName.equals("ContactPerson"))
					wmsService.person = currentValue.toString();
				else if (localName.equals("ContactPosition"))
					wmsService.position = currentValue.toString();
				else if (localName.equals("City"))
					wmsService.city = currentValue.toString();
				else if (localName.equals("Country"))
					wmsService.country = currentValue.toString();
			}
		}
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		currentValue.append(ch, start, length);
	}

	/**
	 * Returns whether the response is a valid Server response.
	 * 
	 * @return
	 */
	protected boolean isValidServer() {
		return this.validServer;
	}

	/**
	 * Returns whether the response is a WMS or WFS response.
	 * 
	 * @return
	 */
	protected boolean isWFS() {
		return this.isWFS;
	}

	/**
	 * Returns the WFS identification.
	 * 
	 * @return
	 */
	protected WFSServiceIdentification getWFSIdent() {
		return this.wfsServiceIdent;
	}

	/**
	 * Returns the WFS wfsProvider.
	 * 
	 * @return
	 */
	protected WFSServiceProvider getProvider() {
		return this.wfsProvider;
	}

	/**
	 * Returns the WMS Informations.
	 * 
	 * @return
	 */
	protected WMSService getWMSIdent() {
		return this.wmsService;
	}

	private String getVersion (Attributes atts) {
		String version = "unknown";
		// Erfassen der Attribute in den Starttags
        if (atts != null) {
        	for (int i = 0; i < atts.getLength(); i++) {
                if (atts.getLocalName(i).equals("version")) {
    	           version = atts.getValue(i);
                }
        	}
        }
		return version;
	}
	
	// unimplemented methods
	@Override
	public void endDocument() throws SAXException {
	}
	
	@Override
	public void startPrefixMapping(String arg0, String arg1)
			throws SAXException {
	}

	@Override
	public void endPrefixMapping(String arg0) throws SAXException {
	}

	@Override
	public void ignorableWhitespace(char[] arg0, int arg1, int arg2)
			throws SAXException {
	}

	@Override
	public void processingInstruction(String arg0, String arg1)
			throws SAXException {
	}

	@Override
	public void setDocumentLocator(Locator arg0) {
	}

	@Override
	public void skippedEntity(String arg0) throws SAXException {
	}
}
