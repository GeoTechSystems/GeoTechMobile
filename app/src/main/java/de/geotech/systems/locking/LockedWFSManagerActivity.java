/**
 * Activity zur Anzeige aller gelockter layer, 
 * um spaeter Features bearbeiten zu koennen 
 * oder sperren loesen zu koennen
 * 
 * @author Torsten Hoch
 */

package de.geotech.systems.locking;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import de.geotech.systems.R;
import de.geotech.systems.database.DBAdapter;
import de.geotech.systems.features.FeatureSelectActivity;
import de.geotech.systems.main.MainActivity;
import de.geotech.systems.projects.Project;
import de.geotech.systems.projects.ProjectHandler;
import de.geotech.systems.utilities.ClassesColorModel;
import de.geotech.systems.utilities.Functions;
import de.geotech.systems.utilities.ClassesColorModel.VCColor;
import de.geotech.systems.wfs.WFSLayer;

public class LockedWFSManagerActivity extends Activity {
	private static final String CLASSTAG = "LockedWFSManagerActivity";

	private Project settings;
	private Boolean anyChange;
	private Context context;
	private DBAdapter dbAdapter;

	/** 
	 * Called when activity is created 
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.anyChange = false;
		this.context = this;
		this.dbAdapter = new DBAdapter(context);
		this.buildWFSGUI();
	}

	/** 
	 * Called on return from another activity 
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case MainActivity.FEATURESELECTACTTIVITY:
			Intent returnToMain = new Intent();
			if (resultCode == RESULT_OK) {
				// Wenn ein Ort angeflogen werden soll, direkt wechseln zur Main
				if (data.getBooleanExtra("de.geotech.systems.fly", false) == true) {
					returnToMain = data;
				}
				// Wenn ein feature editiert werden soll
				if (data.getBooleanExtra("de.geotech.systems.editfeature", false) == true) {
					returnToMain = data;
				}
				// Wenn etwas geloescht/veraendert wurde dann ist bereits true
				if (!data.getBooleanExtra("de.geotech.systems.anychange", false) == true) {
					returnToMain.putExtra("de.geotech.systems.anychange", anyChange);
				}
				returnToMain.putExtra("de.geotech.systems.fly", true);
				setResult(RESULT_OK, returnToMain);
				finish();
			}
		}
	}

	// Save changes and go back to main
	@Override
	public void onBackPressed() {
		Intent returnToMain = new Intent();
		returnToMain.putExtra("de.geotech.systems.anychange", anyChange);
		setResult(RESULT_CANCELED, returnToMain);
		finish();
	}

	/**
	 * Creates the user interface. Directly called in OnCreate.
	 */
	private void buildWFSGUI() {
		settings = ProjectHandler.getCurrentProject();
		setContentView(R.layout.layer_manager);
		// Animation
		final ScaleAnimation icon_animation = 
				new ScaleAnimation(0.7f, 1f, 0.7f, 1f, Animation.RELATIVE_TO_SELF, 
						0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		icon_animation.setDuration(300);
		// Erstelle Tabelle mit Layern
		TableLayout table = (TableLayout) findViewById(R.id.layer_manager_table);
		ArrayList<WFSLayer> list = settings.getWFSContainer();
		VCColor[] colors = Functions.getColorsAsArray();
		ArrayAdapter<ClassesColorModel.VCColor> color_adapter = new ArrayAdapter<ClassesColorModel.VCColor>(
				this, android.R.layout.simple_spinner_item, colors);
		table.removeAllViews();
		// eine tabellenueberschrift und attribute setzen
		TextView wfsTitle = new TextView(this);
		wfsTitle.setText(R.string.title_wfs_locking);
		wfsTitle.setPadding(20, 0, 20, 0);
		wfsTitle.setGravity(Gravity.CENTER);
		wfsTitle.setTextSize(20);
		// ueberschrift als view in die tavbelle
		table.addView(wfsTitle);
		// wenn gar keine layer zur darstellung ausgewaehlt wurden
		// oder keine layer gelockt wuren
		if (list.isEmpty()) {
			// view mit nachricht "leere wfs liste" in die tabelle packen
			TextView emptyWFS = new TextView(this);
			emptyWFS.setText(getString(R.string.layerManager_layer_empty_wfs_list));
			emptyWFS.setPadding(20, 0, 20, 0);
			table.addView(emptyWFS);
		} else {
			// fuer jeden layer in der liste der ausgewaehlten layer eine zeile 
			// erstellen mit allen buttons
			for (int layerIndex = list.size() - 1; layerIndex >= 0; layerIndex--) {
				// wenn der layer gelockt ist, dann zur anzeige hinzufuegen
				if (settings.getWFSContainer().get(layerIndex).isLocked()) {
					final WFSLayer layer = settings.getWFSContainer().get(layerIndex);
					TableRow row = new TableRow(this);
					// Print the layer's name
					TextView name = new TextView(this);
					name.setText(layer.getName());
					name.setPadding(20, 0, 20, 0);
					row.addView(name);
					// Print the type of the layer
					TextView type = new TextView(this);
					type.setText(layer.getType());
					type.setTextColor(layer.getColor());
					type.setPadding(0, 0, 20, 0);
					row.addView(type);
					// Print number of features within the layer
					TextView countFeatures = new TextView(this);
					countFeatures.setText(String.valueOf(
							layer.getFeatureContainer().size()));
					countFeatures.setPadding(20, 0, 20, 0);
					row.addView(countFeatures);
					// Print number of Attributes within the layer
					TextView countAttributes = new TextView(this);
					countAttributes.setText(String.valueOf(
							layer.getCountAttributes()));
					countAttributes.setPadding(20, 0, 20, 0);
					row.addView(countAttributes);
					// farbauswahl pro layer hinzufuegen
					final Spinner spinner = new Spinner(this);
					spinner.setAdapter(color_adapter);
					for (int i = 0; i < colors.length; i++) {
						if (layer.getColor() == colors[i].getColor()) {
							spinner.setSelection(i);
						}
					}
					// behandlung bei aenderungen der farbauswahl
					spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
						@Override
						public void onItemSelected(AdapterView<?> adapter, View v,
								int pos, long l) {
							VCColor choosenColor = (VCColor) spinner.getItemAtPosition(pos);
							if (choosenColor.getColor() != layer.getColor()) {
								layer.setColor(choosenColor.getColor());
								dbAdapter.updateWFSLayerInDB(layer);
								anyChange = true;
								buildWFSGUI();
							}
						}
						@Override
						public void onNothingSelected(AdapterView<?> arg0) {
						}
					});
					row.addView(spinner);
					// Button zum Editieren des Layers
					Button editFeatureButton = new Button(this);
					editFeatureButton.setText(getString(R.string.editFeatureButton));
					editFeatureButton.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(final View newView) {
							newView.startAnimation(icon_animation);
							Intent featureSelector = new Intent(newView.getContext(),
									FeatureSelectActivity.class);
							settings.setCurrentWFSLayer(layer);
							featureSelector.putExtra("de.geotech.systems.layerName", layer.getName());
							startActivityForResult(featureSelector,
									MainActivity.FEATURESELECTACTTIVITY);
						}
					});
					row.addView(editFeatureButton);
					// button zur freigabe des locks auf dem layer
					Button releaseLockButton = new Button(this);
					releaseLockButton.setText(getString(R.string.releaseLockButton));
					releaseLockButton.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							v.startAnimation(icon_animation);
							// Warnung vorm Release des Locks herausgeben
							AlertDialog.Builder alert = new AlertDialog.Builder(v.getContext());
							alert.setTitle(getString(R.string.release_lock_warning_dialog_title));
							alert.setMessage(getString(R.string.warning) + ": \n" + getString(R.string.release_lock_warning_dialog_message));
							alert.setNegativeButton(getString(R.string.cancel),
									new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.cancel();
								}
							});
							alert.setPositiveButton(getString(R.string.do_continue),
									new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									if (layer.isLocked()) {
										// layer wird unlocked und geprüft, ob alles geklappt hat
										// durch die ganze Liste der getWFSLockCheckList
										for (int j = 0; j < settings.getWFSContainer().size(); j++) {
											// wenn der layer dem richtigen entspricht
											if (settings.getWFSContainer().get(j).equals(layer)) {
												// layer unlocken in der datenbank der CheckListen

												// TODO LAYER AUF WFS SERVER RELEASEN!!!
												ReleaseWFSWithTask releaser = new ReleaseWFSWithTask(context, layer.getLockID());
												releaser.unLockLayer(layer);

												//												if (settings.getWFSContainer().get(j).unlock()) {
												//													// TODO
												//												
												//													// beim unlock sollte die ID des locks zum server zum unlocken gegeben werden
												//													// und wenn alles klar geht true zurueckgegeben werden
												//													Log.v("onclick LockedFeatureActivity", "Layer " + layer.getName() + " Unlocking successfull!");
												//												} else {
												//													Log.v("onclick LockedFeatureActivity", "Layer " + layer.getName() + ": Unlocking NOT successfull!");
												//												}
												// settings.getWfsContainer().remove(j);
												// jetzt Abbruch fuer die FOR-schelife, da passender Layer gefunden
												j = settings.getWFSContainer().size() + 1;
											}
										}

									}
									buildWFSGUI();
								}
							});
							alert.show();
						}
					});
					row.addView(releaseLockButton);
					table.addView(row);
				}
			}
		}
	}

}
