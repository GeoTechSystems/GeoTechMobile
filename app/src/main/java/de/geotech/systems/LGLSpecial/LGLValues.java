package de.geotech.systems.LGLSpecial;

import java.util.ArrayList;

import android.graphics.Color;

public class LGLValues {
	// flag, um farbe fuer attribute zu aendern
	public static final String LGL_IS_DONE_FLAG_ATTRIBUTE = "Erledigt";
	// layer names for file reading for attribute relation possibilities
	public static final String[] LGL_OAK_LAYER_NAMES = { "pkt", "lin", "fla" };
	// other LGL-layer names
	public static final String[] STANDARD_LGL_LAYER_NAMES = { "AD_dim", "AD_feld", "AD_forst" };
	// color codes for the LGL-layers
	public static final int[] STANDARD_LGL_COLOR_CODES = { Color.MAGENTA, Color.RED, Color.GREEN };
	// bekannte Attribute aus datei
	public static final String[] KNOWN_ATTRIBUTES = { "oar", "atr_1", "atr_2", "atr_3" };
	// bekannte Attribute aus datei
	public static final String TEXT_ATTRIBUTE = "text";
	// bekannte spalten, in denen die attribute stehen
	public static final int[] KNOWN_COLUMNS = { 1, 3, 5, 7 };
	// BOOLEAN VALUES
	public static final String[] BOOLEAN = { "0", "1" };	
	// the splitter for the csv files
	public static final String SPLITSTRING = "\t";	
	// platzhalter bei textattribut
	public static final String TEXT_PLATZHALTER = "    ";
	
	public static final ArrayList<String> getLayerNamesForFilesArrayList () {
		ArrayList<String> temp = new ArrayList<String>();
		for (int i = 0; i < LGL_OAK_LAYER_NAMES.length; i++) {
			temp.add(LGL_OAK_LAYER_NAMES[i]);
		}
		return temp;
	}
	
	public static final ArrayList<Integer> getRawCodesForFiles() {
		ArrayList<Integer> temp = new ArrayList<Integer>();
		temp.add(de.geotech.systems.R.raw.pkt);
		temp.add(de.geotech.systems.R.raw.lin);
		temp.add(de.geotech.systems.R.raw.fla);
		return temp;
	}
	
	public LGLValues() {
	}
	
}
