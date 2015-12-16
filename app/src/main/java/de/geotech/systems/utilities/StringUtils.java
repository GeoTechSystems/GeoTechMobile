package de.geotech.systems.utilities;

/**
 * Helper class for Strings
 * 
 * @author tubatubsen
 *
 */
public class StringUtils {

	  /**
	   * Liefert einen Substring VOR einem ausgewählten Trennzeichen. 
	   * Das Trennzeichen ist nicht Teil des Ergebnis.
	   *
	   * @param string    String von dem man den substring haben möchte.
	   * @param delimiter Trennzeichen nach dem gesucht werden soll.
	   * @return          Substring vor auftauchen des Trennzeichen.
	   */

	public static String substringBefore(String string, String delimiter) {
		int pos = string.indexOf(delimiter);
		
		return pos >= 0 ? string.substring(0, pos) : string;
	}
	
	  /**
	   * Liefert einen Substring NACH einem ausgewählten Trennzeichen. 
	   * Das Trennzeichen ist nicht Teil des Ergebnis.
	   *
	   * @param string    String von dem man den substring haben möchte.
	   * @param delimiter Trennzeichen nach dem gesucht werden soll.
	   * @return          Substring nach auftauchen des Trennzeichen.
	   */
	  public static String substringAfter( String string, String delimiter )
	  {
	    int pos = string.indexOf( delimiter );

	    return pos >= 0 ? string.substring( pos + delimiter.length() ) : "";
	  }
	
}
