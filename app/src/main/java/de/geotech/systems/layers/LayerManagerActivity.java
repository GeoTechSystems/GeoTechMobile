/**
 * Handles administration for layers to draw 
 * 
 * @author Torsten Hoch
 * @author bschm
 */

package de.geotech.systems.layers;

import java.util.ArrayList;
import java.util.Collections;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrixColorFilter;
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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import de.geotech.systems.R;
import de.geotech.systems.database.DBAdapter;
import de.geotech.systems.features.Feature;
import de.geotech.systems.features.FeatureSelectActivity;
import de.geotech.systems.main.MainActivity;
import de.geotech.systems.projects.Project;
import de.geotech.systems.projects.ProjectHandler;
import de.geotech.systems.utilities.Alerts;
import de.geotech.systems.utilities.ClassesColorModel;
import de.geotech.systems.utilities.Functions;
import de.geotech.systems.utilities.ClassesColorModel.VCColor;
import de.geotech.systems.wfs.WFSCheckedListener;
import de.geotech.systems.wfs.WFSFeatureImport;
import de.geotech.systems.wfs.WFSLayerSynchronization;
import de.geotech.systems.wfs.WFSFeatureImport.OnImportFinishedListener;
import de.geotech.systems.wfs.WFSLayerSynchronization.OnSyncFinishedListener;
import de.geotech.systems.wfs.WFSLayer;
import de.geotech.systems.wms.WMSCheckedListener;
import de.geotech.systems.wms.WMSLayer;

public class LayerManagerActivity extends Activity {
	private static final String CLASSTAG = "LayerManagerActivity";
	private static final int[] STANDARD_PADDING = {5, 10, 15, 10};
	private static final float TEXT_SIZE_TITEL = 26f;
	private static final float TEXT_SIZE_HEADER = 22f;
	private static final float TEXT_SIZE_ITEM = 18f;

	private Project project;
	private int sumFeatures;
	private int sumFeaturesToImport = 0;
	private Context context;
	private float[] colorMatrix = { 
			7, 0, 1, 0, 0, //red
			0, 7, 0, 1, 0, //green
			1, 0, 7, 0, 0, //blue
			0, 1, 0, 1, 0  //alpha    
	};
	private ColorFilter colorFilter = new ColorMatrixColorFilter(colorMatrix);
	private boolean anyChange;
	private boolean imported;
	private int index;
	private DBAdapter dbAdapter;

	// Called when activity is created
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.context = this;
		this.dbAdapter = new DBAdapter(context);
		this.anyChange = false;
		this.imported = false;
		this.index = 0;
		this.buildWFSGUI();
	}

	// Save changes and go back to main
	@Override
	public void onBackPressed() {
		Intent returnToMain = new Intent();
		returnToMain.putExtra("de.geotech.systems.anychange", anyChange);
		returnToMain.putExtra("de.geotech.systems.imported", imported);
		returnToMain.putExtra("de.geotech.systems.fly", false);
		setResult(RESULT_CANCELED, returnToMain);
		finish();
	}

	// Called on return from another activity
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		// if a feature is selected to view
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
				returnToMain.putExtra("de.geotech.systems.imported", imported);
				returnToMain.putExtra("de.geotech.systems.fly", true);
				setResult(RESULT_OK, returnToMain);
				finish();
			}
		}
	}

	// Creates the user interface
	private void buildWFSGUI() {
		// Animation
		final ScaleAnimation icon_animation = new ScaleAnimation(0.7f, 1f,
				0.7f, 1f, Animation.RELATIVE_TO_SELF, 0.5f,
				Animation.RELATIVE_TO_SELF, 0.5f);
		icon_animation.setDuration(300);
		project = ProjectHandler.getCurrentProject();
		setContentView(R.layout.layer_manager_wms);
		TableLayout wfsTable = (TableLayout) findViewById(R.id.layer_manager_table);
		ArrayList<WFSLayer> wfsLayerList = project.getWFSContainer();
		index = 0;
		wfsTable.removeAllViews();
		TextView wfsTitle = new TextView(this);
		wfsTitle.setText("WFS-Layer");
		wfsTitle.setPadding(STANDARD_PADDING[0], STANDARD_PADDING[1], STANDARD_PADDING[2], STANDARD_PADDING[3]);
		wfsTitle.setGravity(Gravity.CENTER);
		wfsTitle.setTextSize(TEXT_SIZE_TITEL);
		wfsTitle.setBackgroundColor(Color.LTGRAY);
		wfsTable.addView(wfsTitle);
		if (wfsLayerList.isEmpty()) {
			TextView emptyWFS = new TextView(this);
			emptyWFS.setText(getString(R.string.layerManager_layer_empty_wfs_list));
			emptyWFS.setPadding(STANDARD_PADDING[0], STANDARD_PADDING[1], STANDARD_PADDING[2], STANDARD_PADDING[3]);
			wfsTable.addView(emptyWFS);
		} else  {
			TableRow row = new TableRow(this);
			String[] columnNames = this.getResources().getStringArray(R.array.wfs_layer_columns);
			for (int i = 0; i < columnNames.length; i++) {
				TextView layerName = new TextView(this);
				layerName.setText(columnNames[i]);
				layerName.setTextSize(TEXT_SIZE_HEADER);
				layerName.setPadding(STANDARD_PADDING[0], STANDARD_PADDING[1], STANDARD_PADDING[2], STANDARD_PADDING[3]);
				row.addView(layerName);
			}
			row.setBackgroundColor(Color.LTGRAY);
			wfsTable.addView(row);
		}
		for (int layerIndex = wfsLayerList.size() - 1; layerIndex >= 0; layerIndex--) {
			final WFSLayer wfsLayer = project.getWFSContainer().get(layerIndex);
			TableRow row = new TableRow(this);
			// Print the layer's name
			TextView layerName = new TextView(this);
			layerName.setText(wfsLayer.getName());
			layerName.setTextSize(TEXT_SIZE_ITEM);
			layerName.setPadding(STANDARD_PADDING[0], STANDARD_PADDING[1], STANDARD_PADDING[2], STANDARD_PADDING[3]);
			row.addView(layerName);
			// print the layers workspace
			TextView workspace = new TextView(this);
			workspace.setText(wfsLayer.getWorkspace());
			workspace.setTextSize(TEXT_SIZE_ITEM);
			workspace.setPadding(STANDARD_PADDING[0], STANDARD_PADDING[1], STANDARD_PADDING[2], STANDARD_PADDING[3]);
			row.addView(workspace);
			// Print the type of the layer
			TextView layerType = new TextView(this);
			layerType.setText(wfsLayer.getType());
			layerType.setTextSize(TEXT_SIZE_ITEM);
			layerType.setTextColor(wfsLayer.getColor());
			layerType.setPadding(STANDARD_PADDING[0], STANDARD_PADDING[1], STANDARD_PADDING[2], STANDARD_PADDING[3]);
			row.addView(layerType);
			// Print number of Attributes within the layer
			TextView countAttributes = new TextView(this);
			countAttributes.setText(String.valueOf(wfsLayer.getCountAttributes()));
			countAttributes.setTextSize(TEXT_SIZE_ITEM);
			countAttributes.setGravity(Gravity.CENTER);
			countAttributes.setPadding(STANDARD_PADDING[0], STANDARD_PADDING[1], STANDARD_PADDING[2], STANDARD_PADDING[3]);
			row.addView(countAttributes);

			// Print number of features within the layer
			TextView countFeatures = new TextView(this);
			countFeatures.setText(String.valueOf(wfsLayer.getFeatureContainer().size()));
			countFeatures.setTextSize(TEXT_SIZE_ITEM);
			countFeatures.setGravity(Gravity.CENTER);
			//countFeatures.setText("#F: " + String.valueOf(wfsLayer.getCountFeatures()));
			countFeatures.setPadding(STANDARD_PADDING[0], STANDARD_PADDING[1], STANDARD_PADDING[2], STANDARD_PADDING[3]);
			row.addView(countFeatures);

			// Estimate the size (MB)
			//TextView sizeMB = new TextView(this);
			//if (wfsLayer.getSizeMB() > 0) {
			//	sizeMB.setText(Float.toString((Math.round(wfsLayer.getSizeMB() * 100) / 100)));
			//} else {
			//	sizeMB.setText("-");
			//}
			//sizeMB.setPadding(STANDARD_PADDING[0], STANDARD_PADDING[1], STANDARD_PADDING[2], STANDARD_PADDING[3]);
			//row.addView(sizeMB);

			VCColor[] colors = Functions.getColorsAsArray();
			// spinner to choose colors
			ArrayAdapter<ClassesColorModel.VCColor> color_adapter = new ArrayAdapter<ClassesColorModel.VCColor>(
					this, R.layout.spinner_item_layermanager, colors);
			final Spinner spinner = new Spinner(this);
			spinner.setAdapter(color_adapter);
			for (int i = 0; i < colors.length; i++) {
				if (wfsLayer.getColor() == colors[i].getColor()) {
					spinner.setSelection(i);
				}
			}
			spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> adapter, View v,
						int pos, long l) {
					VCColor choosenColor = (VCColor) spinner.getItemAtPosition(pos);
					if (choosenColor.getColor() != wfsLayer.getColor()) {
						wfsLayer.setColor(choosenColor.getColor());
						dbAdapter.updateWFSLayerInDB(wfsLayer);
						anyChange = true;
						buildWFSGUI();
					}
				}
				@Override
				public void onNothingSelected(AdapterView<?> arg0) {
				}
			});
			CheckBox wfsActive = new CheckBox(this);
			wfsActive.setChecked(wfsLayer.isActive());
			wfsActive.setPadding(STANDARD_PADDING[0], STANDARD_PADDING[1], STANDARD_PADDING[2], STANDARD_PADDING[3]);
						wfsActive.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView,
						boolean isChecked) {
					wfsLayer.setActive(isChecked);
					if (dbAdapter.updateWFSLayerInDB(wfsLayer)) {
						Log.v(CLASSTAG, wfsLayer.getName() + " is updated to isActive: " + wfsLayer.isActive());
					}
					anyChange = true;
				}
			});
			ImageView moveUp = new ImageView(this);
			moveUp.setImageResource(R.drawable.ic_up);
			moveUp.setPadding(STANDARD_PADDING[0], STANDARD_PADDING[1], STANDARD_PADDING[2], STANDARD_PADDING[3]);
			moveUp.setClickable(true);
			moveUp.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					v.startAnimation(icon_animation);
					moveUp(wfsLayer);
					anyChange = true;
					buildWFSGUI();
				}
			});
			ImageView moveDown = new ImageView(this);
			moveDown.setImageResource(R.drawable.ic_down);
			moveDown.setPadding(STANDARD_PADDING[0], STANDARD_PADDING[1], STANDARD_PADDING[2], STANDARD_PADDING[3]);
			moveDown.setClickable(true);
			moveDown.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					v.startAnimation(icon_animation);
					moveDown(wfsLayer);
					anyChange = true;
					buildWFSGUI();
				}
			});
			TextView empty_up = new TextView(this);
			empty_up.setText(" ");
			TextView empty_down = new TextView(this);
			empty_down.setText(" ");
			row.addView(spinner);
			row.addView(wfsActive);
			if (index > 0) {
				row.addView(moveUp);
			} else {
				row.addView(empty_up);
			}
			if (index < project.getWFSContainer().size() - 1) {
				row.addView(moveDown);
			} else {
				row.addView(empty_down);
			}
			// Import Features fuer einen Layer:
			ImageView importFeature = new ImageView(this);
			if (wfsLayer.getFeatureContainer().size() == 0) {
				importFeature.setImageResource(R.drawable.ic_menu_manage_import_empty);
			} else {
				importFeature.setImageResource(R.drawable.ic_menu_manage_import);
			}
			importFeature.setPadding(STANDARD_PADDING[0], STANDARD_PADDING[1], STANDARD_PADDING[2], STANDARD_PADDING[3]);
			importFeature.setClickable(true);
			importFeature.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View v) {
					v.startAnimation(icon_animation);
					// hier einbau: wenn layer keine feature enthaelt, keine warnung!!
					// if (wfsLayer.getAllFeature().size() > 0) {
					AlertDialog.Builder alert = new AlertDialog.Builder(v.getContext());
					alert.setTitle(getString(R.string.layerManager_feature_import_title));
					alert.setMessage(getString(R.string.warning)
							+ getString(R.string.layerManager_feature_import_message)
							+ getString(R.string.do_continue) + "?");
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
						public void onClick(DialogInterface dialog,	int which) {
							dialog.cancel();
							// Importvorgang!!!
							imported = true;
							WFSFeatureImport importer = new WFSFeatureImport(v.getContext());
							importer.setOnImportFinishedListener(new OnImportFinishedListener() {
								@Override
								public void onImportFinished(boolean finished) {
									// TODO: hier abgucken fuer das andere TODO
									wfsLayer.setSync(true);
									dbAdapter.updateWFSLayerInDB(wfsLayer);
									if (finished) {
										Alerts.errorMessage(v.getContext(),	
												getString(R.string.layerManager_feature_import_result_title) + " " + wfsLayer.getName(), 
												wfsLayer.getFeatureContainer().size() + " " + getString(R.string.layerManager_feature_import_result_message))
												.show();
									} else {
										Alerts.errorMessage(v.getContext(),	getString(R.string.layerManager_feature_import_result_title) + " " + wfsLayer.getName(),
												" " + getString(R.string.layerManager_feature_import_result_error_message)).show();
									}
									buildWFSGUI();
								}
							});
							importer.importFeatures(wfsLayer, 0);
						}
					});
					alert.show();
				}
			});
			row.addView(importFeature);
			// Add Button which allows to view all elements of the selected layer
			ImageView echoAll = new ImageView(this);
			echoAll.setImageResource(R.drawable.ic_menu_manage);
			echoAll.setPadding(STANDARD_PADDING[0], STANDARD_PADDING[1], STANDARD_PADDING[2], STANDARD_PADDING[3]);
			// Fallunterscheidung: Wurden Features geladen? Wenn nicht, deaktiviere den Knopf... 
			if (wfsLayer.getFeatureContainer().size() == 0) {
				echoAll.setColorFilter(colorFilter);
				echoAll.setClickable(false);
			} else {
				echoAll.setClickable(true);
				echoAll.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(final View newView) {
						newView.startAnimation(icon_animation);
						Intent elementSelector = new Intent(newView.getContext(), FeatureSelectActivity.class);
						project.setCurrentWFSLayer(wfsLayer);
						elementSelector.putExtra("de.geotech.systems.layerName", wfsLayer.getName());
						startActivityForResult(elementSelector, MainActivity.FEATURESELECTACTTIVITY);
					}
				});
			}
			row.addView(echoAll);
			//Button zum entefrnen des Layers
			ImageView removeButton = new ImageView(this);
			removeButton.setImageResource(R.drawable.ic_delete);
			removeButton.setPadding(STANDARD_PADDING[0], STANDARD_PADDING[1], STANDARD_PADDING[2], STANDARD_PADDING[3]);
			removeButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View newView) {
					newView.startAnimation(icon_animation);
					AlertDialog.Builder alert = new AlertDialog.Builder(newView
							.getContext());
					alert.setTitle(getString(R.string.layerManager_layer_delete_warning_title) + ", Layer \"" + wfsLayer.getName() + "\"");
					alert.setMessage(getString(R.string.warning)
							+ ": " + getString(R.string.layerManager_layer_delete_warning_message));
					alert.setNegativeButton(getString(R.string.cancel),
							new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
						}
					});
					alert.setPositiveButton(getString(R.string.do_continue),
							new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							anyChange = true;
							if (project.deleteWFSFromContainerAndDB(wfsLayer)) {
								//								WFSCheckedListener.getWFSCheckedList().remove(wfsLayer);
								Log.i(CLASSTAG, "Layer " + wfsLayer.getName() + " geloescht!");
							} else {
								Log.i(CLASSTAG, "Layer " + wfsLayer.getName() + " NICHT geloescht!");
							}
							buildWFSGUI();
						}
					});
					alert.show();
				}
			});
			row.addView(removeButton);
			//Button zum entefrnen des Layers
			ImageView syncButton = new ImageView(this);
			syncButton.setPadding(STANDARD_PADDING[0], STANDARD_PADDING[1], STANDARD_PADDING[2], STANDARD_PADDING[3]);
			if (wfsLayer.isSync()) {
				syncButton.setImageResource(R.drawable.haken_gruen);
				syncButton.setClickable(false);
			} else {
				syncButton.setImageResource(R.drawable.achtung_rot);
				syncButton.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View newView) {
						newView.startAnimation(icon_animation);
						AlertDialog.Builder alert = new AlertDialog.Builder(newView.getContext());
						alert.setTitle(getString(R.string.layerManager_layer_sync_warning_title) + ", Layer \"" + wfsLayer.getName() + "\"");
						alert.setMessage(getString(R.string.layerManager_layer_sync_warning_message));
						alert.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.cancel();
							}
						});
						alert.setPositiveButton(getString(R.string.do_continue),
								new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								anyChange = true;
								WFSLayerSynchronization sync = new WFSLayerSynchronization(context, true, wfsLayer);
								OnSyncFinishedListener syncFinishedListener = new OnSyncFinishedListener() {
									@Override
									public void onSyncFinished(boolean result, WFSLayer selectedLayer) {
										if (result) {
											buildWFSGUI();
											Alerts.errorMessage(context, context.getString(R.string.main_sync_result),
													context.getString(R.string.main_sync_message)).show();
										} else {
											Alerts.errorMessage(context, context.getString(R.string.main_sync_result),
													context.getString(R.string.main_sync_error_message)).show();
										}
									}
								};
								sync.setOnSyncFinishedListener(syncFinishedListener);
								sync.execute();
								buildWFSGUI();
							}
						});
						alert.show();
					}
				});
			}
			row.addView(syncButton);








			wfsTable.addView(row);
			index++;
		}
		// TODO: load all features button from all layers - rausgenommen, da buggy
		//		TableRow row = new TableRow(this);
		//		for (int i = 9; i > 0 ; i--) {
		//			TextView blank = new TextView(this);
		//			blank.setText("");
		//			row.addView(blank);
		//		}
		//		if (wfsLayerList.size() > 1) {
		//			ImageView importAllFeatures = new ImageView(this);
		//			importAllFeatures.setImageResource(R.drawable.ic_menu_manage_import_all);
		//			importAllFeatures.setPadding(STANDARD_PADDING[0], STANDARD_PADDING[1], STANDARD_PADDING[2], STANDARD_PADDING[3]);
		//			importAllFeatures.setClickable(true);
		//			importAllFeatures.setOnClickListener(new OnClickListener() {
		//				@Override
		//				public void onClick(final View v) {
		//					v.startAnimation(icon_animation);
		//					AlertDialog.Builder alert = new AlertDialog.Builder(v.getContext());
		//					alert.setTitle(getString(R.string.layerManager_feature_import_title));
		//					alert.setMessage(getString(R.string.warning)
		//							+ getString(R.string.layerManager_feature_import_all_message)
		//							+ getString(R.string.do_continue) + "?");
		//					alert.setNegativeButton(getString(R.string.cancel),
		//							new DialogInterface.OnClickListener() {
		//						@Override
		//						public void onClick(DialogInterface dialog, int which) {
		//							dialog.cancel();
		//						}
		//					});
		//					alert.setPositiveButton(getString(R.string.do_continue), new DialogInterface.OnClickListener() {
		//						@Override
		//						public void onClick(DialogInterface dialog,	int which) {
		//							dialog.cancel();
		//							// Importvorgang!!!
		//							imported = true;
		//							ArrayList<WFSLayer> wfsLayerList = project.getWFSContainer();
		//							sumFeaturesToImport = 0;
		//							sumFeatures = 0;
		//							for (int i = project.getWFSContainer().size() - 1; i >= 0; i--) {
		//								// alle features etc. des layers aus der DB loeschen
		//								dbAdapter.deleteAllFeaturesOfALayerFromDB(project.getWFSContainer().get(i));
		//								// alle Features aus dem Ram loeschen
		//								project.getWFSContainer().get(i).getFeatureContainer().clear();
		//								// alle features aus Index loeschen
		//								project.getWFSContainer().get(i).restartIndex();
		//								sumFeaturesToImport += wfsLayerList.get(i).getCountFeatures();
		//							}
		//							WFSFeatureImport importer = new WFSFeatureImport(v.getContext());
		//							final WFSLayer wfsLayer = project.getWFSContainer().get(0);
		//							importer.setOnImportFinishedListener(new OnImportFinishedListener() {
		//								@Override
		//								public void onImportFinished(boolean finished) {
		//									sumFeatures = 0;
		//									// TODO: HIER BUG?!
		//									for (int i = project.getWFSContainer().size() - 1; i >= 0; i--) {
		//										project.getWFSContainer().get(i).setSync(true);
		//										dbAdapter.updateWFSLayerInDB(project.getWFSContainer().get(i));
		//										sumFeatures += project.getWFSContainer().get(i).getFeatureContainer().size();
		//									}	
		//									if (finished) {
		//										// Gebe Meldung erst aus, wenn auch der letzte Layer synchronisiert wurde.
		//										if (sumFeatures==sumFeaturesToImport) {
		//											Alerts.errorMessage(v.getContext(),	"Import beendet!", String.valueOf(sumFeatures) + " " + getString(R.string.layerManager_feature_import_result_message)).show();
		//											sumFeaturesToImport=0;
		//											sumFeatures=0;
		//										}
		//									} else {
		//										Alerts.errorMessage(v.getContext(),	getString(R.string.layerManager_feature_import_result_title),
		//												" " + getString(R.string.layerManager_feature_import_result_error_message)).show();
		//									}
		//									buildWFSGUI();
		//								}
		//							});
		//							importer.importFeatures(wfsLayer, sumFeaturesToImport);
		//						}
		//					});
		//					alert.show();
		//				}
		//			});
		//			row.addView(importAllFeatures);
		//		}
		//		wfsTable.addView(row);
		index = 0;
		buildWMSGUI(wfsTable);
	}

	private void buildWMSGUI(TableLayout table) {
		final ScaleAnimation icon_animation = new ScaleAnimation(0.7f, 1f,
				0.7f, 1f, Animation.RELATIVE_TO_SELF, 0.5f,
				Animation.RELATIVE_TO_SELF, 0.5f);
		icon_animation.setDuration(300);
		// Platzhalter-zeile
		TextView wmsTitle = new TextView(this);
		wmsTitle.setText("\n");
		wmsTitle.setPadding(STANDARD_PADDING[0], STANDARD_PADDING[1], STANDARD_PADDING[2], STANDARD_PADDING[3]);
		wmsTitle.setGravity(Gravity.CENTER);
		wmsTitle.setBackgroundColor(Color.WHITE);
		table.addView(wmsTitle);
		// zeile mit wms-ueberschrift
		wmsTitle = new TextView(this);
		wmsTitle.setText("WMS-Layer");
		wmsTitle.setPadding(STANDARD_PADDING[0], STANDARD_PADDING[1], STANDARD_PADDING[2], STANDARD_PADDING[3]);
		wmsTitle.setGravity(Gravity.CENTER);
		wmsTitle.setTextSize(TEXT_SIZE_TITEL);
		wmsTitle.setBackgroundColor(Color.LTGRAY);
		table.addView(wmsTitle);
		// wenn keine layer im container
		if (project.getWMSContainer().isEmpty()) {
			TextView emptyWMS = new TextView(this);
			emptyWMS.setText(getString(R.string.layerManager_layer_empty_wms_list));
			emptyWMS.setTextSize(TEXT_SIZE_HEADER);
			emptyWMS.setPadding(STANDARD_PADDING[0], STANDARD_PADDING[1], STANDARD_PADDING[2], STANDARD_PADDING[3]);
			table.addView(emptyWMS);
		} else  {
			// sonst ueberschriften fuer spalten
			TableRow row = new TableRow(this);
			String[] columnNames = this.getResources().getStringArray(R.array.wms_layer_columns);
			for (int i = 0; i < columnNames.length; i++) {
				TextView layerName = new TextView(this);
				layerName.setText(columnNames[i]);
				layerName.setTextSize(TEXT_SIZE_HEADER);
				layerName.setPadding(STANDARD_PADDING[0], STANDARD_PADDING[1], STANDARD_PADDING[2], STANDARD_PADDING[3]);
				row.addView(layerName);
			}
			row.setBackgroundColor(Color.LTGRAY);
			table.addView(row);
		}
		// fuer jeden wms layer
		for (int layerIndex = project.getWMSContainer().size() - 1; layerIndex >= 0; layerIndex--) {
			final WMSLayer wmsLayer = project.getWMSContainer().get(layerIndex);
			TableRow row = new TableRow(this);
			TextView name = new TextView(this);
			name.setText(wmsLayer.getName());
			name.setTextSize(TEXT_SIZE_ITEM);
			name.setPadding(STANDARD_PADDING[0], STANDARD_PADDING[1], STANDARD_PADDING[2], STANDARD_PADDING[3]);
			row.addView(name);
			TextView title = new TextView(this);
			title.setText(wmsLayer.getWorkspace());
			title.setTextSize(TEXT_SIZE_ITEM);
			title.setPadding(STANDARD_PADDING[0], STANDARD_PADDING[1], STANDARD_PADDING[2], STANDARD_PADDING[3]);
			row.addView(title);
			TextView epsg = new TextView(this);
			epsg.setText(wmsLayer.getEPSG());
			epsg.setTextSize(TEXT_SIZE_ITEM);
			epsg.setPadding(STANDARD_PADDING[0], STANDARD_PADDING[1], STANDARD_PADDING[2], STANDARD_PADDING[3]);
			row.addView(epsg);
			ImageView move_up = new ImageView(this);
			move_up.setImageResource(R.drawable.ic_up);
			move_up.setPadding(STANDARD_PADDING[0], STANDARD_PADDING[1], STANDARD_PADDING[2], STANDARD_PADDING[3]);
			move_up.setClickable(true);
			move_up.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					v.startAnimation(icon_animation);
					moveUp(wmsLayer);
					buildWFSGUI();
					anyChange = true;
				}
			});
			ImageView move_down = new ImageView(this);
			move_down.setImageResource(R.drawable.ic_down);
			move_down.setPadding(STANDARD_PADDING[0], STANDARD_PADDING[1], STANDARD_PADDING[2], STANDARD_PADDING[3]);
			move_down.setClickable(true);
			move_down.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					v.startAnimation(icon_animation);
					moveDown(wmsLayer);
					buildWFSGUI();
					anyChange = true;
				}
			});
			TextView empty_up = new TextView(this);
			empty_up.setText(" ");
			TextView empty_down = new TextView(this);
			empty_down.setText(" ");
			if (index > 0) {
				row.addView(move_up);
			} else {
				row.addView(empty_up);
			}

			if (index < project.getWMSContainer().size() - 1) {
				row.addView(move_down);
			} else {
				row.addView(empty_down);
			}
			ImageView removeButton = new ImageView(this);
			removeButton.setImageResource(R.drawable.ic_delete);
			removeButton.setPadding(STANDARD_PADDING[0], STANDARD_PADDING[1], STANDARD_PADDING[2], STANDARD_PADDING[3]);
			removeButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					v.startAnimation(icon_animation);
					AlertDialog.Builder alert = new AlertDialog.Builder(v
							.getContext());
					alert.setTitle(getString(R.string.layerManager_layer_delete_warning_title)  + ", Layer \"" + wmsLayer.getName() + "\"");
					alert.setMessage(getString(R.string.warning) + ": " + getString(R.string.layerManager_layer_delete_warning_message));
					alert.setNegativeButton(getString(R.string.cancel),
							new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,	int which) {
							dialog.cancel();
						}
					});
					alert.setPositiveButton(getString(R.string.do_continue),
							new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,	int which) {
							anyChange = true;							
							if (project.deleteWMSFromContainerAndDB(wmsLayer)) {
								Log.i(CLASSTAG, "WMS-Layer " + wmsLayer.getName() + " geloescht!");
							} else {
								Log.i(CLASSTAG, "WMS-Layer " + wmsLayer.getName() + " NICHT geloescht!");
							}
							buildWFSGUI();
						}
					});
					alert.show();
				}
			});
			row.addView(removeButton);
			table.addView(row);
			index++;
		}
	}

	// Moves layer up
	private void moveUp(LayerInterface l) {
		if (l instanceof WFSLayer) {
			int i = project.getWFSContainer().indexOf(l);
			Collections.swap(project.getWFSContainer(), i, i + 1);
		} else	{
			int i = project.getWMSContainer().indexOf(l);
			Collections.swap(project.getWMSContainer(), i, i + 1);
		}
	}

	// Moves layer down
	private void moveDown(LayerInterface l) {
		if (l instanceof WFSLayer) {
			int i = project.getWFSContainer().indexOf(l);
			Collections.swap(project.getWFSContainer(), i, i - 1);
		} else {
			int i = project.getWMSContainer().indexOf(l);
			Collections.swap(project.getWMSContainer(), i, i - 1);
		}
	}

}
