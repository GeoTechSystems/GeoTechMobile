/**
 * 
 * @author Torsten Hoch
 * @author bschm
 * 
 */

package de.geotech.systems.features;

import java.util.ArrayList;
import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import de.geotech.systems.R;
import de.geotech.systems.main.MainActivity;
import de.geotech.systems.projects.ProjectHandler;
import de.geotech.systems.utilities.Functions;
import de.geotech.systems.wfs.WFSLayer;
import de.geotech.systems.wfs.WFSLayerAttributeTypes;

public class FeatureSelectActivity extends Activity {
	private final static String CLASSTAG = "FeatureSelectActivity";
	private final static int ENTRIES_PER_PAGE = 50;

	// Animation details
	private static final ScaleAnimation icon_animation = new ScaleAnimation(0.7f, 1f, 0.7f,
			1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);

	// layer name
	private String layerClearName = null;
	// the context
	private Context context;
	// current wfs layer of features in table
	private WFSLayer currentWfsLayer = null;
	// if something was deleted/cnahged
	boolean anyChange;
	// layout
	private final static TableRow.LayoutParams zParameter = new TableRow.LayoutParams(
			LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);

	private static LinearLayout zelle;

	// when activity is called
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.context = this;
		this.anyChange = false;
		// set layout
		this.setContentView(R.layout.element_selector);
		// manage animations
		// this.icon_animation.setDuration(300);
		// Namen des Layers raussuchen aus Intent-metadaten
		this.layerClearName = getIntent().getStringExtra("de.geotech.systems.layerName");
		// tabelle erzeugen
		this.buildTable(context, 0);
	}

	// Called on return from another activity
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		// requestCode = RC_LAYER_VIEW_SELECTOR = 6
		case MainActivity.LAYERMANAGERACTIVITY:
			buildTable(context, 0);
		}
	}

	// Called when back button is pressed
	@Override
	public void onBackPressed(){
		Intent returnToCaller = new Intent();
		setResult(RESULT_CANCELED, returnToCaller);			
		finish();			
	}

	// methode, die die tabelle erstellt
	private void buildTable(final Context context, final int page) {
		// get current WFS layer
		this.currentWfsLayer = ProjectHandler.getCurrentProject().getCurrentWFSLayer();
		// TODO Uebersetzungen ueberall in dieser klasse!
		// Gebe Layer-Details aus...
		TextView desc = (TextView) findViewById(R.id.element_selector_desc);
		desc.setText("Layer " + layerClearName + " - Anzahl angezeigter Elemente: "
				+ String.valueOf(currentWfsLayer.getFeatureContainer().size())
				+ "\nListe aller Features: ");
		// initialize table
		TableLayout table = (TableLayout) findViewById(R.id.element_selector_table);
		table.removeAllViews();
		// helping array-lists
		ArrayList<String> layerAttributeList = new ArrayList<String>();
		ArrayList<String> attributePrecisionList = new ArrayList<String>();
		layerAttributeList.clear();
		attributePrecisionList.clear();
		// get all attributes into help list
		for (WFSLayerAttributeTypes attrib : currentWfsLayer.getAttributeTypes()) {
			layerAttributeList.add(attrib.getName());
		}
		// Erzeuge Tabellenzeile
		TableRow tRow = new TableRow(context);
		tRow.setPadding(0, 2, 0, 0);
		tRow.addView(createZelle(context, "GeoServer-ID", true));
		tRow.addView(createZelle(context, "Internal ID", true));
		// Füge Titelspalten ein
		for (String spalte : layerAttributeList) {
			tRow.addView(createZelle(context, spalte, true));
		}
		//for (String spalte : attributePrecisionList) {
		//	tRow.addView(createZelle(context, spalte, true));
		//}

		//Füge Spalte für Accuracy ein...
		tRow.addView(createZelle(context, "Accuracy", false));
		// füge ueberschriftenzeile in tabelle ein
		table.addView(tRow);
		// Gebe eine Liste mit den Objekten des betreffenden Layers aus
		int i = 1;
		TableRow row = new TableRow(context);
		zelle = new LinearLayout(context);
		Button show = new Button(context);
		Button edit = new Button(context);
		Feature feature;
		//		for (Feature feature : currentWfsLayer.getFeatureContainer()) {

		for (int k = page * ENTRIES_PER_PAGE; k <= (page + 1) * ENTRIES_PER_PAGE; k++) {
			if (k < currentWfsLayer.getFeatureContainer().size()) {
				feature = currentWfsLayer.getFeatureContainer().get(k);

				// PrecisionCon precision = new PrecisionCon(f.getFeatureID(), currentWfsLayer.getLayerID(), context);
				row = new TableRow(context);
				if (i % 2 == 0) {
					row.setBackgroundColor(Color.LTGRAY);	
				} else {
					row.setBackgroundColor(Color.WHITE);
				}
				i++;
				row.setPadding(0, 2, 0, 0);
				// geoServer-ID
				row.addView(createZelle(context, String.valueOf(feature.getGeoServerID()), true));
				// internal ID
				row.addView(createZelle(context, String.valueOf(feature.getFeatureID()), true));
				// Insert layer-columns...
				for (String col : layerAttributeList) {
					row.addView(createZelle(context, feature.getAttributes().getAsString(col), true));
				}
				// Füge Spalte für Bewertung der Genauigkeit ("Ampel") ein
				if (feature.hasPrecision()) {
					row.addView(createZelle(context, accuracyImage(feature.getPrecision().getWorstAccuracy()), true));
					row.getChildAt(row.getChildCount()-1).setOnClickListener(new FeatureClick(feature.getPrecision()));
				} else {
					row.addView(createZelle(context, accuracyImage(0), true));
				}
				// Erzeuge Zellen mit Buttons zum Loeschen, Ansehen, Details anzeigen
				// und für Satelitten-Details
				zelle = new LinearLayout(context);
				// zelle.setBackgroundColor(Color.WHITE);
				// Buttons erstellen
				// TODO Uebersetzungen!
				show = new Button(context);
				show.setText("Center Feature");
				show.setOnClickListener(new FeatureClick(feature.getFeatureID(), feature.getWKTGeometry(), currentWfsLayer.getLayerID()));
				// show.setPadding(3, 2, 3, 2);
				edit = new Button(context);
				edit.setText("Edit Feature");
				edit.setOnClickListener(new FeatureClick(feature.getFeatureID(), feature.getWKTGeometry(), currentWfsLayer.getLayerID(), true));
				// edit.setPadding(3, 2, 3, 2);
				//			Button delete = new Button(context);
				//			if (currentWfsLayer.isLocked()) { 
				//				delete.setText("Remove Feature");
				//				delete.setOnClickListener(new FeatureClick(feature.getFeatureID(), currentWfsLayer.getLayerID()));
				//			} else {
				//				delete.setText("No Lock - not removable");
				//				delete.setEnabled(false);
				//			}
				// Buttons in tabellenzelle eintragen
				zelle.addView(show);
				zelle.addView(edit);
				//			zelle.addView(delete);
				// zelle in zeile eintragen
				row.addView(zelle);
				// zeile in tabelle eintragen
				table.addView(row);
			} else {
				// fertig, abbruch
				k = (page + 1) * ENTRIES_PER_PAGE;
			}
		}
		zelle = new LinearLayout(context);
		Button last = new Button(context);
		last.setText(context.getResources().getString(R.string.last_feature_table) + " " + ENTRIES_PER_PAGE);
		if (page < 1) {
			last.setClickable(false);
			last.setActivated(false);
			last.setEnabled(false);
		} else {
			last.setClickable(true);
			last.setActivated(true);
			last.setEnabled(true);
		}
		OnClickListener lastClicked = new OnClickListener() {
			@Override
			public void onClick(View v) {
				buildTable(context, page - 1);
			}
		};
		last.setOnClickListener(lastClicked);
		Button next = new Button(context);
		next.setText(context.getResources().getString(R.string.next_feature_table) + " " + ENTRIES_PER_PAGE);
		if ((page + 1) * ENTRIES_PER_PAGE < currentWfsLayer.getFeatureContainer().size()) {
			next.setClickable(true);
			next.setActivated(true);
			next.setEnabled(true);
		} else {
			next.setClickable(false);
			next.setActivated(false);
			next.setEnabled(false);
		}
		OnClickListener nextClicked = new OnClickListener() {
			@Override
			public void onClick(View v) {
				buildTable(context, page + 1);
				
			}
		};
		next.setOnClickListener(nextClicked);
		row = new TableRow(context);
		zelle.addView(last);
		zelle.addView(next);
		row.addView(zelle);
		table.addView(row);
	}

	// Methode zum Hinzufügen einer Zelle in die Tabelle
	public static View createZelle(Context context, String inhalt, Boolean zparams) {
		zelle = new LinearLayout(context);
		// zelle.setBackgroundColor(Color.WHITE);
		// Lasse auf der rechten Seite etwas frei, sofern die Zelle nicht am
		// rechten Ende der Tabelle ist...
		if (zparams) {
			// Speichere Parameter für Zelle

			zParameter.setMargins(0, 0, 2, 0);
			zelle.setLayoutParams(zParameter);
		}
		// text in textview eingeben
		TextView text = new TextView(context);
		text.setPadding(4, 2, 4, 2);
		text.setText(inhalt);
		zelle.addView(text);
		return zelle;
	}

	public static View createZelle(Context context, View icon, Boolean zparams) {
		zelle = new LinearLayout(context);
		// zelle.setBackgroundColor(Color.WHITE);
		// Lasse auf der rechten Seite etwas frei, sofern die Zelle nicht am
		// rechten Ende der Tabelle ist...
		if (zparams) {
			// Speichere Parameter für Zelle
			zParameter.setMargins(0, 0, 2, 0);
			zelle.setLayoutParams(zParameter);
		}
		// text in textview eingeben
		icon.setPadding(3, 2, 3, 2);
		zelle.addView(icon);
		return zelle;
	}

	// Innere Klasse für einen ClickListener (Show, Remove)
	public class FeatureClick implements OnClickListener {
		private static final int JUST_FLY = 1;
		private static final int DELETE_FEATURE = 2;
		private static final int FLY_AND_EDIT = 3;
		private static final int ECHO_ACCURACY = 4;

		private long id;
		private long layerId;
		private String coord;
		private int task;
		private FeaturePrecision precision;


		// Constructor 
		public FeatureClick(long newFlyID, String newCoord, long newLayerID) {
			this(newFlyID, newCoord, newLayerID, false);
		}

		// Default Constructor 
		public FeatureClick(long newFlyID, String newCoord, long newLayerID, boolean edit) {
			this.id = newFlyID;
			this.coord = newCoord;
			this.layerId = newLayerID;
			if (!edit) {
				this.task = JUST_FLY;
			} else {
				this.task = FLY_AND_EDIT;
			}
		}

		// Constructor: Gebe Accuracy
		public FeatureClick(FeaturePrecision precision) {
			this.precision=precision;
			this.task=ECHO_ACCURACY;
		}

		// Deletion Constructor
		public FeatureClick(long newDelID, long newDelLayerId) {
			this.task = DELETE_FEATURE;
			this.id = newDelID;
			this.layerId = newDelLayerId;
		}

		@Override
		public void onClick(final View newView) {
			newView.startAnimation(icon_animation);
			switch (task) {
			case JUST_FLY:
				// Element anfliegen
				Log.i(CLASSTAG + " fly to show", "Feature: " + String.valueOf(id) + " - Layer: " + String.valueOf(layerId) + " - Coordinate: " + coord);
				Intent flyToShow = new Intent();
				flyToShow.putExtra("de.geotech.systems.fly", true);
				flyToShow.putExtra("de.geotech.systems.flyID", String.valueOf(id));
				flyToShow.putExtra("de.geotech.systems.flyLayerID", String.valueOf(layerId));
				flyToShow.putExtra("de.geotech.systems.flyCoord", coord);
				setResult(RESULT_OK, flyToShow);			
				finish();				
				break;
			case DELETE_FEATURE:
				// TODO hier feature löschen (RAM, SQL, WFS-Server (ist ja locked)),
				// vorher sicherheitsabfrage und hinterher bestaetigung
				// Element löschen - Öffne Datenbank ALT!

				/*
				 * DbHelper helper = new DbHelper(context, database);
				 * SQLiteDatabase db = helper.getReadableDatabase();
				 * 
				 * // Lösche Element db.delete("layer" +
				 * String.valueOf(layerId), "internfeatureid = ?", new String[]
				 * { String.valueOf(id) });
				 * 
				 * // Lösche Informationen zur Genauigkeit, sofern vorhanden
				 * db.delete( DbHelper.TABLE_PRECISION,
				 * "id = ? AND layerId = ?", new String[] { String.valueOf(id),
				 * String.valueOf(layerId) });
				 * 
				 * // Lade neu, schließe Datenbank db.close();
				 */
				anyChange = true;
				break;
			case FLY_AND_EDIT:
				// Element anfliegen, um es zu editieren
				Log.i(CLASSTAG + " fly to edit", "Feature: " + String.valueOf(id) + " - Layer: " + String.valueOf(layerId) + " - Coordinate: " + coord);
				Intent flyToEdit = new Intent();
				flyToEdit.putExtra("de.geotech.systems.fly", true);
				flyToEdit.putExtra("de.geotech.systems.flyID", String.valueOf(id));
				flyToEdit.putExtra("de.geotech.systems.flyLayerID", String.valueOf(layerId));
				flyToEdit.putExtra("de.geotech.systems.flyCoord", coord);
				flyToEdit.putExtra("de.geotech.systems.editfeature", true);
				setResult(RESULT_OK, flyToEdit);	
				finish();			
				break;
			case ECHO_ACCURACY:
				//Gebe Genauigkeit(en) aus (Toast):
				StringBuffer toastText = new StringBuffer();
				int[] idAndKey;
				for (int i = 0; i < precision.getList().size(); i++)	{
					idAndKey=Functions.getIdAndKey(precision.getRealtions(), i);
					if(precision.getList().size()>0) toastText.append("P#"+String.valueOf(idAndKey[0])+"_"+String.valueOf(idAndKey[1])+":\t");
					toastText.append(context.getString(R.string.radius_point_toast) + ": " + String.valueOf(precision.getList().get(i).getAccuracy()) + " m\n");
					if(precision.getList().size()>0) toastText.append("\t\t\t");
					toastText.append(context.getString(R.string.satCount_point_toast) + ": " + String.valueOf(precision.getList().get(i).getAccuracy()) + " m\n");

				}
				if(precision.getList().size()>0) toastText.append("\n\t=>\t\t" + context.getString(R.string.worstAccuracy_toast) + ": " + precision.getWorstAccuracy() +" m");
				Toast.makeText(context, toastText.toString(), Toast.LENGTH_LONG).show();
			}
		}
	}

	private View accuracyImage(float accuracy)	{
		ImageView icon = new ImageView(context);
		if (accuracy == 0) {
			icon.setImageResource(R.drawable.score_gray);
		} else if(accuracy <= Feature.border[0]) {
			icon.setImageResource(R.drawable.score_green);
		} else if(accuracy <= Feature.border[1]) {
			icon.setImageResource(R.drawable.score_yellow);
		} else {
			icon.setImageResource(R.drawable.score_red);
		}
		return icon;
	}

}
