/**
 * Handles attributes of files and there possible relations.
 * "oak" stands for Objektartenkartierung
 * 
 * @author Torsten Hoch - tohoch@uos.de
 */

package de.geotech.systems.LGLSpecial;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.util.Log;

public class OAKAttributeHandler {
	private static final String CLASSTAG = "OAKAttributeHandler";

	// the context
	private Context context;
	// the r-ids of the raw-files
	private ArrayList<Integer> rawRessourceID;
	// the file names read out
	private ArrayList<String> fileNames;
	// the hashmap, where a file finds it attributes
	private HashMap<String, OAKAttributes> fileNamesWithAttributesContainer;
	// the hashmap for labelnames and labelnames with values ("value - label")
	private ArrayList<String> labelHelpMap;

	/**
	 * Instantiates a new OAK attribute handler.
	 *
	 * @param newContext the new context
	 * @param newRawRessourceID the new raw ressource id
	 * @param newFileName the new file name
	 */
	public OAKAttributeHandler(Context newContext, ArrayList<Integer> newRawRessourceID, ArrayList<String> fileNames) {
		this.fileNames = fileNames;
		this.context = newContext;
		this.rawRessourceID = newRawRessourceID;
		this.fileNamesWithAttributesContainer = new HashMap<String, OAKAttributes>();
		this.labelHelpMap = new ArrayList<String>();
	}

	/**
	 * Read file and initialize.
	 *
	 * @param splitString the internal split string
	 * @param attributesList the known attribute names
	 * @param columnsList the known columns where the attributes values are - sorted like attributesList
	 */
	public void readFilesAndInitialize(String splitString, int[] columnsList) {
		for (int k = 0; k < rawRessourceID.size(); k++) {
			// Log.e(CLASSTAG , "Inputstream für File # " + rawRessourceID.get(k) + " mit Namen " + fileNames.get(k) + " erstellt.");
			InputStream in = context.getResources().openRawResource(rawRessourceID.get(k));
			try {
				InputStreamReader inputStreamReader = new InputStreamReader(in);
				BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
				// Read everything into a StringBuilder
				String newLine = bufferedReader.readLine();
				String[] splittedLine = newLine.split(splitString);
				// ab hier quick and dirty
				// weil jeder weiss, dass in der ersten zeile in den oben genannten 
				// spalten die namen der spalten stehen :-) :
				ArrayList<String> allAttributesOfFile = new ArrayList<String>();
				for (int i = 0; i < LGLValues.KNOWN_ATTRIBUTES.length; i++) {
					allAttributesOfFile.add(LGLValues.KNOWN_ATTRIBUTES[i]);
					// Log.i(CLASSTAG , "Attribut " + splittedLine[columnsList[i] - 1] + " für File " + fileNames.get(k) + " erstellt.");
				}
				// alle attribute hinzufügen
				fileNamesWithAttributesContainer.put(fileNames.get(k), new OAKAttributes(allAttributesOfFile));
				String oakValue;
				String oakLabel;
				String attributeName;
				while (bufferedReader.ready()) {
					newLine = bufferedReader.readLine();
					if(newLine != null){
					splittedLine = newLine.split(splitString);
					for (int i = 0; i < columnsList.length; i++) {
						// attribut festlegen
						attributeName = allAttributesOfFile.get(i);
						// attributwert in dieser zeile
						oakValue = splittedLine[columnsList[i] - 1];
						// Bezeichnung des attributwertes steht in der naechsten spalte
						oakLabel = splittedLine[columnsList[i]];
						// Log.i(CLASSTAG , "Eintrag in Liste: File: " + fileNames.get(k) + ",  Attributname: \"" + attributeName + "\", Attributwert: \"" + oakValue + "\", Attributlabel: \"" + oakLabel + "\"");
						// eintragen in liste
						if (!oakValue.equals("")) {
							OAKAttributes x = fileNamesWithAttributesContainer.get(fileNames.get(k));
							x.insert(attributeName, oakValue, oakLabel);
						} else {
						}
					}
					}
				}
				bufferedReader.close();
			} catch (IOException e) {
				Log.e(CLASSTAG, "IOException: " + e.getMessage().toString());
				e.printStackTrace();
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException e) {
						Log.e(CLASSTAG, "IOException: " + e.getMessage().toString());
						e.printStackTrace();
					}
				}
			}
		}

	}

	/**
	 * Gets the attribute values with labels as string. Internally creates a new hashmap 
	 * with the values plus labels correlated to the values.
	 *
	 * @param fileName the file name
	 * @param attributeName the attribute name
	 * @return the attribute values
	 */
	public ArrayList<String> getAttributeLabelsPlus(String fileName, String attributeName) {
		// get the labels plus " - labelname" as keys  and the values
		return fileNamesWithAttributesContainer.get(fileName).getListOfLabelsPlus(attributeName);
	}
	
	public String getValueForOneLabelPlus(String fileName, String attributeName, String labelPlus) {
		return fileNamesWithAttributesContainer.get(fileName).getValueForLabelsPlus(attributeName, labelPlus);
	}

}
