/**
 * The activity to edit the attributes of new added features.
 * 
 * @author Torsten Hoch
 */

package de.geotech.systems.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;

import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import de.geotech.systems.R;
import de.geotech.systems.LGLSpecial.LGLValues;
import de.geotech.systems.database.DBAdapter;
import de.geotech.systems.features.Feature;
import de.geotech.systems.features.FeaturePrecision;
import de.geotech.systems.main.LeftMenuIconLayout;
import de.geotech.systems.projects.ProjectHandler;
import de.geotech.systems.utilities.Alerts;
import de.geotech.systems.utilities.DateTime;
import de.geotech.systems.wfs.WFSLayer;
import de.geotech.systems.wfs.WFSLayerAttributeTypes;

public class FeatureAttributesCreatorActivity extends Activity {
	private static final String CLASSTAG = "FeatureAttributesCreatorActivity";
	private static final int PADDING = 10;
	private static final int EDIT_TEXT_MINIMUM_WIDTH = 475;
	private static final String TEXT_PLATZHALTER = LGLValues.TEXT_PLATZHALTER;

	private String geom;
	private int type;
	private WFSLayer currentLayer = null;
	private ContentValues insert;
	private TableLayout table;
	private ArrayList<EditText> edits;
	private ArrayList<Spinner> spinners;
	private ArrayList<DateTime> dates;
	private Context context;
	private DBAdapter dbAdapter;

	/**
	 * Called when activity is created.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = this.getIntent();
		this.geom = intent.getStringExtra("de.geotech.systems.editor.geom");
		this.type = intent.getIntExtra("de.geotech.systems.editor.type", -1);
		this.currentLayer = ProjectHandler.getCurrentProject().getCurrentWFSLayer(); 
		this.insert = new ContentValues();
		this.edits = new ArrayList<EditText>();
		this.spinners = new ArrayList<Spinner>();
		this.dates = new ArrayList<DateTime>();
		this.context = this;
		this.dbAdapter = new DBAdapter(context);
		this.buildGUI();
	}

	// Creates user interface to fill in the Features Attributes.
	private void buildGUI() {
		TableRow.LayoutParams params = new TableRow.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		ScrollView scroll = new ScrollView(this);
		LinearLayout linlay = new LinearLayout(this);
		Button save = new Button(this);
		table = new TableLayout(this);
		linlay.setOrientation(LinearLayout.VERTICAL);
		// TODO uebersetzung
		save.setText(R.string.feature_attribute_creator_save);
		save.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				saveFeature();
			}
		});
		linlay.addView(table, params);
		linlay.addView(save, params);
		scroll.addView(linlay, params);
		// TODO: LGL KRAM HART CODIERT
		// wenn es einer der layer mit Objektartenattributierung ist
		if (currentLayer.getName().equalsIgnoreCase(LGLValues.LGL_OAK_LAYER_NAMES[0]) 
				|| currentLayer.getName().equalsIgnoreCase(LGLValues.LGL_OAK_LAYER_NAMES[1]) 
				|| currentLayer.getName().equalsIgnoreCase(LGLValues.LGL_OAK_LAYER_NAMES[2])) {
			for (final WFSLayerAttributeTypes attribute : currentLayer.getAttributeTypes()) {
				if (attribute.getName().equalsIgnoreCase(LGLValues.KNOWN_ATTRIBUTES[0]) 
						|| attribute.getName().equalsIgnoreCase(LGLValues.KNOWN_ATTRIBUTES[1])
						|| attribute.getName().equalsIgnoreCase(LGLValues.KNOWN_ATTRIBUTES[2])
						|| attribute.getName().equalsIgnoreCase(LGLValues.KNOWN_ATTRIBUTES[3])) {
					TableRow row = new TableRow(this);
					row.setGravity(Gravity.CENTER_VERTICAL);
					TextView title = new TextView(this);
					title.setText(attribute.getName() + ":");
					title.setPadding(PADDING, PADDING, PADDING, PADDING);
					row.addView(title, params);
					final AutoCompleteTextView editLGL = new AutoCompleteTextView(this);
					ArrayList<String> items = new ArrayList<String>();
					// TODO: ARRAYLIST setzen!!!!
					Log.e(CLASSTAG + " Attribute-Change", ".getAttributeValuesWithLabels( " + currentLayer.getName() + "," + attribute.getName());
					items = ProjectHandler.getOakAttributeHandler().getAttributeLabelsPlus(currentLayer.getName(), attribute.getName());
					Log.e(CLASSTAG, "Items are: " + items.toString());
					ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1);
					adapter.addAll(items);
					editLGL.setAdapter(adapter);
					editLGL.setGravity(Gravity.LEFT);
					editLGL.setTag(attribute.getName());
					editLGL.setPadding(PADDING, PADDING, PADDING, PADDING);
					editLGL.setThreshold(1);
					editLGL.setDropDownWidth(EDIT_TEXT_MINIMUM_WIDTH);
					String textStr = "STRING";
					if (attribute.getType() == WFSLayerAttributeTypes.DECIMAL) {
						editLGL.setInputType(InputType.TYPE_CLASS_NUMBER
								| InputType.TYPE_NUMBER_FLAG_SIGNED
								| InputType.TYPE_NUMBER_FLAG_DECIMAL);
						textStr = "DECIMAL";
					} else if (attribute.getType() == WFSLayerAttributeTypes.INTEGER) {
						editLGL.setInputType(InputType.TYPE_CLASS_NUMBER
								| InputType.TYPE_NUMBER_FLAG_SIGNED);
						textStr = "INTEGER";
					}
					editLGL.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							editLGL.showDropDown();
						}
					});
					AdapterView.OnItemClickListener l = new OnItemClickListener() {
						@Override
						public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
							String selection = (String) parent.getItemAtPosition(position);
							String y = ProjectHandler.getOakAttributeHandler().getValueForOneLabelPlus(currentLayer.getName(), attribute.getName(), selection);
							if (y != null) {
								editLGL.setText(y);
							} else {
								editLGL.setText(selection);
							}
						}
					};
					editLGL.setOnItemClickListener(l);
					row.addView(editLGL, params);
					TextView textBoolean = new TextView(context);
					textBoolean.setText(textStr);
					textBoolean.setPadding(PADDING, PADDING, PADDING, PADDING);
					row.addView(textBoolean, params);
					edits.add(editLGL);
					table.addView(row, params);
				} else {
					if (attribute.getName().equalsIgnoreCase(LGLValues.TEXT_ATTRIBUTE)) {
						// do nothing, textfield will be inserted automatically
					} else {
						table.addView(createRow(attribute.getName(), attribute.getType(), attribute.getRestrictions()));
					}
				}
			}
			// falls es ein standard-lgl layer ist, der ein erledigt tag hat
		} else if (currentLayer.getName().equalsIgnoreCase(LGLValues.STANDARD_LGL_LAYER_NAMES[0])
				|| currentLayer.getName().equalsIgnoreCase(LGLValues.STANDARD_LGL_LAYER_NAMES[1])
				|| currentLayer.getName().equalsIgnoreCase(LGLValues.STANDARD_LGL_LAYER_NAMES[2])) {
			for (WFSLayerAttributeTypes attribute : currentLayer.getAttributeTypes()) {
				if (attribute.getName().equalsIgnoreCase(LGLValues.LGL_IS_DONE_FLAG_ATTRIBUTE)) {
					TableRow row = new TableRow(this);
					row.setGravity(Gravity.CENTER_VERTICAL);
					TextView title = new TextView(this);
					title.setText(attribute.getName() + ":");
					title.setPadding(PADDING, PADDING, PADDING, PADDING);
					row.addView(title, params);
					final AutoCompleteTextView editLGL = new AutoCompleteTextView(this);
					ArrayList<String> items = new ArrayList<String>();
					items.add("0");
					items.add("1");
					ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1);
					adapter.addAll(items);
					editLGL.setAdapter(adapter);
					editLGL.setGravity(Gravity.LEFT);
					editLGL.setTag(attribute.getName());
					editLGL.setPadding(PADDING, PADDING, PADDING, PADDING);
					editLGL.setThreshold(1);
					editLGL.setDropDownWidth(EDIT_TEXT_MINIMUM_WIDTH);
					editLGL.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							editLGL.showDropDown();
						}
					});
					String textStr = "STRING";
					if (attribute.getType() == WFSLayerAttributeTypes.DECIMAL) {
						editLGL.setInputType(InputType.TYPE_CLASS_NUMBER
								| InputType.TYPE_NUMBER_FLAG_SIGNED
								| InputType.TYPE_NUMBER_FLAG_DECIMAL);
						textStr = "DECIMAL";
					} else if (attribute.getType() == WFSLayerAttributeTypes.INTEGER) {
						editLGL.setInputType(InputType.TYPE_CLASS_NUMBER
								| InputType.TYPE_NUMBER_FLAG_SIGNED);
						textStr = "INTEGER";
					}
					row.addView(editLGL, params);
					TextView textBoolean = new TextView(context);
					textBoolean.setText(textStr);
					textBoolean.setPadding(PADDING, PADDING, PADDING, PADDING);
					row.addView(textBoolean, params);
					edits.add(editLGL);
					table.addView(row, params);							
				} else {
					table.addView(createRow(attribute.getName(), attribute.getType(), attribute.getRestrictions()));
				}
			}
		} else {
			// add a textfield for every attribute
			for (WFSLayerAttributeTypes attribute : currentLayer.getAttributeTypes()) {
				table.addView(createRow(attribute.getName(), attribute.getType(), attribute.getRestrictions()));
			}
		}
		setContentView(scroll, params);
	}

	/**
	 * Creates a table row with attribute name, an editable field and
	 * restrictions if existing.
	 * 
	 * @param attrName
	 * @param attrType
	 * @param restr
	 * @return
	 */
	private TableRow createRow(String attrName, int attrType,
			HashMap<String, String> restr) {
		TableRow.LayoutParams params = new TableRow.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		TableRow row = new TableRow(this);
		row.setGravity(Gravity.CENTER_VERTICAL);
		TextView title = new TextView(this);
		title.setText(attrName + ":");
		title.setPadding(PADDING, PADDING, PADDING, PADDING);
		row.addView(title, params);
		switch (attrType) {
		case WFSLayerAttributeTypes.INTEGER:
			EditText editInt = new EditText(this);
			editInt.setTag(attrName);
			editInt.setMinimumWidth(EDIT_TEXT_MINIMUM_WIDTH);
			editInt.setInputType(InputType.TYPE_CLASS_NUMBER
					| InputType.TYPE_NUMBER_FLAG_SIGNED);
			editInt.setPadding(PADDING, PADDING, PADDING, PADDING);
			row.addView(editInt, params);
			TextView resInt = new TextView(this);
			String textInt = "INTEGER";
			Iterator<String> iterInt = restr.keySet().iterator();
			while (iterInt.hasNext()) {
				String key = iterInt.next();
				textInt = textInt + "\n" + key + ": " + restr.get(key);
			}
			resInt.setText(textInt);
			resInt.setPadding(PADDING, PADDING, PADDING, PADDING);
			row.addView(resInt, params);
			edits.add(editInt);
			break;
		case WFSLayerAttributeTypes.MEASUREMENT:
			EditText editMeas = new EditText(this);
			editMeas.setTag(attrName);
			editMeas.setMinimumWidth(EDIT_TEXT_MINIMUM_WIDTH);
			editMeas.setInputType(InputType.TYPE_CLASS_NUMBER
					| InputType.TYPE_NUMBER_FLAG_SIGNED
					| InputType.TYPE_NUMBER_FLAG_DECIMAL);
			editMeas.setPadding(PADDING, PADDING, PADDING, PADDING);
			row.addView(editMeas, params);
			TextView resMeas = new TextView(this);
			String textMeas = "MEASUREMENT";
			Iterator<String> iterMeas = restr.keySet().iterator();
			while (iterMeas.hasNext()) {
				String key = iterMeas.next();
				textMeas = textMeas + "\n" + key + ": " + restr.get(key);
			}
			resMeas.setText(textMeas);
			resMeas.setPadding(PADDING, PADDING, PADDING, PADDING);
			row.addView(resMeas, params);
			edits.add(editMeas);
			break;
		case WFSLayerAttributeTypes.STRING:
			EditText editStr = new EditText(this);
			editStr.setTag(attrName);
			editStr.setMinimumWidth(EDIT_TEXT_MINIMUM_WIDTH);
			editStr.setInputType(InputType.TYPE_CLASS_TEXT
					| InputType.TYPE_TEXT_VARIATION_NORMAL);
			editStr.setPadding(PADDING, PADDING, PADDING, PADDING);
			row.addView(editStr, params);
			TextView resStr = new TextView(this);
			String textStr = "STRING";
			Iterator<String> iterStr = restr.keySet().iterator();
			while (iterStr.hasNext()) {
				String key = iterStr.next();
				textStr = textStr + "\n" + key + ": " + restr.get(key);
			}
			resStr.setText(textStr);
			resStr.setPadding(PADDING, PADDING, PADDING, PADDING);
			row.addView(resStr, params);
			edits.add(editStr);
			break;
		case WFSLayerAttributeTypes.DATE:
			//			DateTime date = new DateTime(this, false);
			//			date.setTag(attrName);
			//			LinearLayout linlayDate = date.getView();
			//			linlayDate.setPadding(PADDING, PADDING, PADDING, PADDING);
			//			row.addView(linlayDate, params);
			//			TextView resDate = new TextView(this);
			//			String textDate = "DATE";
			//			Iterator<String> iterDate = restr.keySet().iterator();
			//			while (iterDate.hasNext()) {
			//				String key = iterDate.next();
			//				textDate = textDate + "\n" + key + ": " + restr.get(key);
			//			}
			//			resDate.setText(textDate);
			//			resDate.setPadding(PADDING, PADDING, PADDING, PADDING);
			//			row.addView(resDate, params);
			//			dates.add(date);
			//			break;
		case WFSLayerAttributeTypes.DATETIME:
			DateTime dateTime = new DateTime(this, true);
			dateTime.setTag(attrName);
			LinearLayout linlayTime = dateTime.getView();
			linlayTime.setPadding(PADDING, PADDING, PADDING, PADDING);
			row.addView(linlayTime, params);
			TextView resDateTime = new TextView(this);
			String textDateTime = "DATETIME";
			Iterator<String> iterDateTime = restr.keySet().iterator();
			while (iterDateTime.hasNext()) {
				String key = iterDateTime.next();
				textDateTime = textDateTime + "\n" + key + ": "
						+ restr.get(key);
			}
			resDateTime.setText(textDateTime);
			resDateTime.setPadding(PADDING, PADDING, PADDING, PADDING);
			row.addView(resDateTime, params);
			dates.add(dateTime);
			break;
		case WFSLayerAttributeTypes.BOOLEAN:
			Spinner editBoolean = new Spinner(this);
			ArrayAdapter<CharSequence> adapter = ArrayAdapter
					.createFromResource(this, R.array.bool,
							android.R.layout.simple_spinner_item);
			editBoolean.setAdapter(adapter);
			editBoolean.setTag(attrName);
			editBoolean.setPadding(PADDING, PADDING, PADDING, PADDING);
			row.addView(editBoolean, params);
			TextView textBoolean = new TextView(this);
			textBoolean.setText("BOOLEAN");
			textBoolean.setPadding(PADDING, PADDING, PADDING, PADDING);
			row.addView(textBoolean, params);
			spinners.add(editBoolean);
			break;
		case WFSLayerAttributeTypes.DOUBLE:
			EditText editDouble = new EditText(this);
			editDouble.setTag(attrName);
			editDouble.setMinimumWidth(EDIT_TEXT_MINIMUM_WIDTH);
			editDouble.setInputType(InputType.TYPE_CLASS_NUMBER
					| InputType.TYPE_NUMBER_FLAG_SIGNED
					| InputType.TYPE_NUMBER_FLAG_DECIMAL);
			editDouble.setPadding(PADDING, PADDING, PADDING, PADDING);
			row.addView(editDouble, params);
			TextView resDouble = new TextView(this);
			String textDouble = "DOUBLE";
			Iterator<String> iterDouble = restr.keySet().iterator();
			while (iterDouble.hasNext()) {
				String key = iterDouble.next();
				textDouble = textDouble + "\n" + key + ": " + restr.get(key);
			}
			resDouble.setText(textDouble);
			resDouble.setPadding(PADDING, PADDING, PADDING, PADDING);
			row.addView(resDouble, params);
			edits.add(editDouble);
			break;
		case WFSLayerAttributeTypes.DECIMAL:
			EditText editDecimal = new EditText(this);
			editDecimal.setTag(attrName);
			editDecimal.setMinimumWidth(EDIT_TEXT_MINIMUM_WIDTH);
			editDecimal.setInputType(InputType.TYPE_CLASS_NUMBER
					| InputType.TYPE_NUMBER_FLAG_SIGNED
					| InputType.TYPE_NUMBER_FLAG_DECIMAL);
			editDecimal.setPadding(PADDING, PADDING, PADDING, PADDING);
			row.addView(editDecimal, params);
			TextView resDecimal = new TextView(this);
			String textDecimal = "DECIMAL";
			Iterator<String> iterDecimal = restr.keySet().iterator();
			while (iterDecimal.hasNext()) {
				String key = iterDecimal.next();
				textDecimal = textDecimal + "\n" + key + ": " + restr.get(key);
			}
			resDecimal.setText(textDecimal);
			resDecimal.setPadding(PADDING, PADDING, PADDING, PADDING);
			row.addView(resDecimal, params);
			edits.add(editDecimal);
			break;
		default:
			return new TableRow(this);
		}
		return row;
	}

	/**
	 * Stores the feature in the RAM and in the internal sql-database.
	 */
	private void saveFeature() {
		// LGL isDone-Flag
		boolean isDone = false;
		insert.clear();
		String value;
		String key;
		String text = "";
		ListIterator<EditText> iterEdits = edits.listIterator();
		while (iterEdits.hasNext()) {
			EditText e = iterEdits.next();
			// Log.v(CLASSTAG, "Saving: " + e.getTag() + " = " + e.getEditableText().toString());
			value = e.getEditableText().toString();
			key = e.getTag().toString();
			value = value.replace("\n", " ");
			value = value.replace("\t", " ");
			value = value.replace("\"", "_");
			value = value.replace("=", "_");
			if (value.length() == 0){
				value = "";
			}
			isDone = checkIfDone(key, value);
			insert.put(key, value);
			// wenn es einer der attributwerte ist, dann den wert zum attribut text hinzufügen
			// wenn es einer der attributwerte ist, dann den wert zum attribut text hinzufügen
			if (key.equalsIgnoreCase(LGLValues.KNOWN_ATTRIBUTES[0]) 
					|| key.equalsIgnoreCase(LGLValues.KNOWN_ATTRIBUTES[1])
					|| key.equalsIgnoreCase(LGLValues.KNOWN_ATTRIBUTES[2])
					|| key.equalsIgnoreCase(LGLValues.KNOWN_ATTRIBUTES[3])) {
				// platzhalter, wenn schon was drin
				if (!text.equals("") && !value.equals("")) {
					text = text + TEXT_PLATZHALTER;
				}
				text = text + value;
			}
		}
		if (currentLayer.getName().equalsIgnoreCase(LGLValues.LGL_OAK_LAYER_NAMES[0]) 
				|| currentLayer.getName().equalsIgnoreCase(LGLValues.LGL_OAK_LAYER_NAMES[1]) 
				|| currentLayer.getName().equalsIgnoreCase(LGLValues.LGL_OAK_LAYER_NAMES[2])) {
			// text einfuegen, zusammenfassung aller attribute
			insert.put(LGLValues.TEXT_ATTRIBUTE, text);
		}
		ListIterator<Spinner> iterSpinners = spinners.listIterator();
		while (iterSpinners.hasNext()) {
			Spinner s = iterSpinners.next();
			// Log.v(CLASSTAG,	"Saving: " + s.getTag() + " = "	+ s.getSelectedItemPosition());
			insert.put(s.getTag().toString(), s.getSelectedItemPosition());
		}
		ListIterator<DateTime> iterDates = dates.listIterator();
		while (iterDates.hasNext()) {
			DateTime d = iterDates.next();
			// Log.v(CLASSTAG,	"Saving: " + d.getTag() + " = " + d.toString());
			insert.put(d.getTag(), d.getDateAsString());
		}
		Feature newFeature = new Feature(this, currentLayer.getLayerID(), geom, insert,
				currentLayer.getColor(), type, false, currentLayer.isActive(), isDone);
		// newFeature.createFeature();
		newFeature.setFeatureID(dbAdapter.insertFeatureIntoDB(newFeature));
		currentLayer.getFeatureContainer().add(newFeature);
		currentLayer.addToIndex(newFeature);
		currentLayer.setSync(false);
		dbAdapter.updateWFSLayerInDB(currentLayer);
		if (!AddFeatureBar.precision.isEmpty()) {
			// Schreibe Informationen in die Precision-Tabelle
			AddFeatureBar.precision.write(this, newFeature, currentLayer.getLayerID());
			//Erzeuge eine neue Instanz von FeaturePrecision...
			AddFeatureBar.precision = new FeaturePrecision();
		} else {
			// Für das Element sind keine Informationen bzgl. der Genauigkeit verfügbar. Lösche precision um dies zu verdeutlichen.
			newFeature.setPrecision(null);
		}
		Intent back = new Intent();
		back.putExtra("de.geotech.systems.editor.geom", geom);
		back.putExtra("de.geotech.systems.editor.type", type);
		setResult(RESULT_OK, back);
		finish();
	}

	/**
	 * Check if the feature is done (LGL-Flag).
	 *
	 * @param string the string
	 * @param value the value
	 * @return true, if successful
	 */
	private boolean checkIfDone(String string, String value) {
		if (string.equalsIgnoreCase(LGLValues.LGL_IS_DONE_FLAG_ATTRIBUTE)) {
			if (value.equals("1")) {
				return true;
			}
		}
		return false;
	}

}
