/**
 * An Container with all attributes of a file/layer and
 * its possible values and labels.
 * 
 * @author Torsten Hoch - tohoch@uos.de
 */

package de.geotech.systems.LGLSpecial;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

public class OAKAttributes {
	// the default value for unset names
	public static final String UNKNOWN = "Objektart unbekannt";
	// the classtag
	private static final String CLASSTAG = "OAKAttributes";

	// the name of the attributes
	private ArrayList<String> attributeNames;
	// the hashmap for values
	private HashMap<String, TreeSet<String>> aokaValues;

	// should both be be HashMap<String, HashMap<String, String>>
	// -> different attributes are missing, maps are for whole class 
	// the hashmap for labels
	private HashMap<String, String> labelMap;
	// the hashmap for labels Plus
	private HashMap<String, String> labelPlusMap;
	
	// helping set for values
	private TreeSet<String> valueHelpSet;
	// the hashmap for label plus with their values
	private HashMap<String, TreeSet<String>> labelPlusWithValueMap;
	// helping set for labels
	private TreeSet<String> labelHelpSet;	

	/**
	 * Instantiates a new OAK attribute.
	 *
	 * @param newAttributeName the new attributes names
	 */
	public OAKAttributes(ArrayList<String> newAttributeNames) {
		this.attributeNames = newAttributeNames;
		this.aokaValues = new HashMap<String, TreeSet<String>>();
		this.labelPlusWithValueMap = new HashMap<String, TreeSet<String>>();
		this.valueHelpSet = new TreeSet<String>();
		this.labelHelpSet = new TreeSet<String>();
		this.labelMap = new HashMap<String, String>();
		this.labelPlusMap = new HashMap<String, String>();
	}

	/**
	 * Gets the attributes names
	 *
	 * @return the attributes Name
	 */
	public ArrayList<String> getAttributeNames() {
		return attributeNames;
	}

	/**
	 * Put in a new entry.
	 *
	 * @param attributeName the attribute name
	 * @param value the value
	 * @param oakLabel the name of this value from Objektartenkartierung
	 */
	public void insert(String attributeName, String value, String label) {
		// get sets
		this.valueHelpSet = aokaValues.get(attributeName);
		this.labelHelpSet = labelPlusWithValueMap.get(attributeName);
		// if it is empty put it in as new
		if (valueHelpSet == null) {
			this.valueHelpSet = new TreeSet<String>();
			this.aokaValues.put(attributeName, valueHelpSet);
		} 
		if (labelHelpSet == null) {
			this.labelHelpSet = new TreeSet<String>();
			this.labelPlusWithValueMap.put(attributeName, labelHelpSet);
		} 
		// add value to label map and label Plus Map
		this.valueHelpSet.add(value);
		if (label != null) {
			String labelPlus;
			if (!label.equals("")) {
				this.labelMap.put(value, label);
				labelPlus = value + " (" + label + ")";
				this.labelHelpSet.add(labelPlus);
			} else {
				labelPlus = value + " (" + UNKNOWN + ")";
				this.labelHelpSet.add(labelPlus);
			}
			this.labelPlusMap.put(labelPlus, value);
		}
	}

	/**
	 * Gets all values as string.
	 *
	 * @param attributeName the attribute name
	 * @return the all values as string
	 */
	public ArrayList<String> getAllValuesAsString(String attributeName) {
		// get set of values
		valueHelpSet = aokaValues.get(attributeName);
		// get set of labels
		ArrayList<String> valueList = new ArrayList<String>();
		//		String helpString;
		if (valueHelpSet != null) {
			Iterator<String> iterValue = valueHelpSet.iterator();
			while (iterValue.hasNext()) {
				String value = iterValue.next();
				valueList.add(value);	
			}	
		}
		return valueList;
	}

	/**
	 * Gets the list for label: "value plus " - " plus labelname.
	 *
	 * @return the hash map for values and labels
	 */
	public ArrayList<String> getListOfLabelsPlus(String attributeName) {
		labelHelpSet = labelPlusWithValueMap.get(attributeName);
		if (labelHelpSet != null) {
			ArrayList<String> labelPlusList = new ArrayList<String>(labelHelpSet); 
			return labelPlusList;
		} else {
			return new ArrayList<String>();
		}

	}

	/**
	 * Gets the value for a label: "value plus " - " plus labelname.
	 *
	 * @param attributeName the attribute name
	 * @return the value for labels plus
	 */
	public String getValueForLabelsPlus(String attributeName, String labelPlus) {
		if (labelPlusMap.containsKey(labelPlus)) {
			return labelPlusMap.get(labelPlus);
		}
		return null;
	}

}
