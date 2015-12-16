/**
 * zur erstellung von Layer tabellen mit ueberschriften und titel
 * 
 * @author Torsten Hoch
 */

package de.geotech.systems.layerTables;

import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.xml.sax.ContentHandler;


import de.geotech.systems.R;
import de.geotech.systems.R.color;
import de.geotech.systems.R.drawable;
import de.geotech.systems.database.DBAdapter;
import de.geotech.systems.layers.LayerInterface;
import de.geotech.systems.locking.LockExpirySetter;
import de.geotech.systems.locking.LockedWFSContentHandler;
import de.geotech.systems.projects.Project;
import de.geotech.systems.projects.ProjectHandler;
import de.geotech.systems.utilities.Functions;
import de.geotech.systems.wfs.WFSContentHandler;
import de.geotech.systems.wfs.WFSCheckedListener;
import de.geotech.systems.wfs.WFSLayer;
import de.geotech.systems.wfs.WFSLoaderCon;
import de.geotech.systems.wms.WMSDefaultHandler;
import de.geotech.systems.wms.WMSCheckedListener;
import de.geotech.systems.wms.WMSLayer;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class LayerTable implements LayerTableInterface {
	private final String CLASSTAG = "LayerTable";
	// the context
	private Context context;
	// Tabellenzeilen ohne ueberschrift, steht fuer anzahl eingetragener layer
	private int layerCount;
	// layout ID in R-Files
	private int layout_R_ID;
	// Tabelle, die alles enthaelt
	private TableLayout table;
	// Hilfszeile zum einlesen
	private TableRow row;
	// hilfsliste fuer vergleiche, enthaelt komplette layer als liste
	private List<LayerInterface> tableValuesList;
	// hilfsliste fuer spaltenueberschriften
	private LinkedList<View> headLines;
	// liste in liste zum speichern in einer tabelle
	private LayerTableViewList viewListForTables;
	// Hilfszeile zum einlesen
	private LinkedList<View> newRow;
	// Hilfsspalte zum einlesen
	private LinkedList<View> newColumn;
	// contenthandler fuer wfs-parsing
	private WFSContentHandler wfsHandler;
	// contenthandler fuer wms-parsing
	private WMSDefaultHandler wmsHandler;

	private DBAdapter dbAdapter;

	// Constructor, for just initialising
	public LayerTable(Context context, int layout_R_ID) {
		super();
		this.context = context;
		this.layout_R_ID = layout_R_ID;
		this.table = (TableLayout) ((Activity) this.context).findViewById(this.layout_R_ID);
		this.tableValuesList = new LinkedList<LayerInterface>();
		this.dbAdapter = new DBAdapter(context);
		this.layerCount = 0;
	}

	// clear the whole table
	@Override
	public void clearTable() {
		this.table.removeAllViews();
		this.viewListForTables = new LayerTableViewList(this.context);
		this.layerCount = 0;
	}

	@Override
	// get the whole table
	public TableLayout getTable() {
		return this.table;
	}

	// get the layer count
	@Override
	public int size() {
		return this.layerCount;
	}

	// checks if the parameter is in the table
	@Override
	public boolean contains(LayerInterface checkLayer) {
		if ((checkLayer != null) && (this.tableValuesList != null)) {
			return (this.tableValuesList.contains(checkLayer));
		} else {
			// irgendein Wert ist null
			return false;
		}
	}

	// gibt die tabelle als liste zurueck
	@Override
	public List<LayerInterface> getLayerList() {
		return this.tableValuesList;
	}

	// finds out whether an wfs or an wms contenthandler has initialized
	// this method and starts the table building
	@Override
	public boolean activateLayerTableBuilding(ContentHandler handler) {

		if (handler instanceof LockedWFSContentHandler) {
			this.wfsHandler = (WFSContentHandler) handler;
			// Log.i(CLASSTAG, "ContentHandler ist ein LockedCapabilitiesContentHandler fuer WFS");
			buildLockedWFSTable();
			return true;
		} else if (handler instanceof WFSContentHandler) {
			this.wfsHandler = (WFSContentHandler) handler;
			// Log.i(CLASSTAG, "ContentHandler ist ein CapabilitiesContentHandler fuer WFS");
			buildWFSTable();

			// TODO hier sollte ein CountFeaturesAsynctask die countFeatures laden und die dann 
			// die gesamte Tabelle für jede zeile jeweils einmal neu ueberschreiben

			// code funktioniert nicht, da serverkommunikation im main thread
			//			Iterator<LayerInterface> iterator = this.wfsHandler.getWFSLayerList().iterator();
			//			while (iterator.hasNext()) {
			//				WFSLayer x = (WFSLayer) iterator.next();
			//				x.setCountFeatures(WFSLoaderCon.countFeatures(Functions.reviseWfsUrl(x.getUrl()), this.wfsHandler.getVersion(), x.getName()));
			//				buildWFSTable();
			//			}
			return true;
		} else if (handler instanceof WMSDefaultHandler) {
			this.wmsHandler = (WMSDefaultHandler) handler;
			// Log.i(CLASSTAG, "ContentHandler ist ein GetCapabilitiesHandler fuer WMS");
			buildWMSTable();
			return true;
		} else {
			Log.e(CLASSTAG, "Wrong Content Handler given. No Table could be created.");
			return false;
		}
	}

	// get the whole view lists
	@Override
	public LayerTableViewList getViewListForTables() {
		return this.viewListForTables;
	}


	public WMSDefaultHandler getWMSHandler() {
		return wmsHandler;
	}

	public WFSContentHandler getWFSHandler() {
		return wfsHandler;
	}

	// build the table with linked lists of views
	private boolean buildTable(LayerTableViewList newViewListForTable) {
		if (newViewListForTable != null) {
			// Tabelle loeschen
			this.clearTable();
			// listenobjekt neu setzen
			this.viewListForTables = newViewListForTable;
			// fuer jede zeile
			for (int i = 0; i < newViewListForTable.getCompleteLists().size(); i++) {
				// hilfszeile neu initialisieren
				this.row = new TableRow(this.context);
				// fuer jeden view
				for (int j = 0; j < newViewListForTable.getCompleteLists()
						.get(i).size(); j++) {
					// werte als views in zeile einfügen
					this.row.setPadding(0, 5, 0, 5);			
					this.row.addView(newViewListForTable.getCompleteLists().get(i).get(j));
				}
				// zeile in tabelle einfuegen
				this.table.addView(this.row);
			}
			// tabelle geschrieben
			return true;
		} else {
			Log.e(CLASSTAG, "ViewListForTables not initialized");
			// tabelle nicht geschrieben
			return false;
		}
	}

	// building a WMS Table
	private void buildWMSTable() {
		// standardelemente eintragen
		if (this.buildRowsAndTableWithHeadlines(this.wmsHandler.getWmsContainer())) {
			// Log.i(CLASSTAG, "Parsing through buildtWMSTable() ok!");
		} else {
			Log.e(CLASSTAG, "Parsing through buildWMSTable() interrupted.");
		}
		// hilfsspalte initialisieren
		this.newColumn = checkBoxColumn(new WMSCheckedListener(this));
		// neue spalte in Listen eintragen
		this.viewListForTables.addColumn(this.newColumn,
				createTitleTextview(context.getString(R.string.addLayerTable_already_chosen)));
		// Log.i(CLASSTAG + " buildWMSTable()", "Spalte erstellt: " +
		// context.getString(R.string.addLayerTable_already_chosen));
		// gesamte tabelle bauen
		this.buildTable(this.viewListForTables);
	}

	// building a WFS Table
	private void buildWFSTable() {
		// standardelemente eintragen					
		if (this.buildRowsAndTableWithHeadlines((this.wfsHandler).getWFSLayerList())) {
			// Log.i(CLASSTAG, "Parsing through buildWFSTable() ok!");
		} else {
			Log.e(CLASSTAG, "Parsing through buildWFSTable() NOT ok!");
		}
		// hilfsspalte countfeatures erstellen
		this.newColumn = this.getWFSType();
		// neue spalte in Listen eintragen
		this.viewListForTables
		.addColumn(
				this.newColumn,
				createTitleTextview(context
						.getString(R.string.addLayerTable_features_column_type)));
		// hilfsspalte countfeatures erstellen
		this.newColumn = this.featureCountColumn();
		// neue spalte in Listen eintragen
		this.viewListForTables
		.addColumn(
				this.newColumn,
				createTitleTextview(context
						.getString(R.string.addLayerTable_features_column_title)));
		// hilfsspalte checkboxen erstellen
		this.newColumn = this.checkBoxColumn(new WFSCheckedListener(this));
		// neue spalte in Listen eintragen
		this.viewListForTables.addColumn(this.newColumn,
				createTitleTextview(context
						.getString(R.string.addLayerTable_already_chosen)));
		// Log.i(CLASSTAG + " buildWFSTable()", "Spalte erstellt: " +
		// context.getString(R.string.addLayerTable_already_chosen));
		// gesamte tabelle bauen
		this.buildTable(this.viewListForTables);
	}

	// building a Locked WFS Table
	private void buildLockedWFSTable() {
		// standardelemente eintragen					
		if (this.buildRowsAndTableWithHeadlines((this.wfsHandler).getWFSLayerList())) {
			// Log.i(CLASSTAG, "Parsing through buildWFSTable() ok!");
		} else {
			Log.e(CLASSTAG, "Parsing through buildWFSTable() NOT ok!");
		}
		// hilfsspalte countfeatures erstellen
		this.newColumn = this.getWFSType();
		// neue spalte in Listen eintragen
		this.viewListForTables
		.addColumn(
				this.newColumn,
				createTitleTextview(context
						.getString(R.string.addLayerTable_features_column_type)));
		// hilfsspalte countfeatures erstellen
		this.newColumn = featureCountColumn();
		// neue spalte in Listen eintragen
		this.viewListForTables
		.addColumn(
				this.newColumn,
				createTitleTextview(context
						.getString(R.string.addLayerTable_features_column_title)));
		// hilfsspalten fuer checkboxen, ob geeichnet und lock button oder text
		LinkedList<LinkedList<View>> liste = checkboxLockedColumn();
		this.newColumn = liste.get(0);
		// neue spalte in Listen eintragen
		this.viewListForTables.addColumn(this.newColumn,
				createTitleTextview(context
						.getString(R.string.add_layer_table_chosen_box)));
		this.newColumn = liste.get(1);
		// neue spalte in Listen eintragen
		this.viewListForTables.addColumn(this.newColumn,
				createTitleTextview(""));
		// Log.i(CLASSTAG + " buildLockedWFSTable()", "Spalte erstellt: " +
		// context.getString(R.string.addLayerTable_already_chosen));
		// gesamte tabelle bauen
		this.buildTable(this.viewListForTables);
	}

	// checkbox und lockbutton-columns
	private LinkedList <LinkedList<View>> checkboxLockedColumn() {
		Project project = ProjectHandler.getCurrentProject();
		// zwei neue spalten
		LinkedList<View> boxColumn = new LinkedList<View>();
		LinkedList<View> buttonColumn = new LinkedList<View>();
		CheckBox box = null;
		WFSLayer currentWFSLayer = null;
		TextView alreadyLocked = null;
		Button lockLayerButton = null;
		boolean isThisLayerLocked = false;
		for (int j = 0; j < this.size(); j++) {
			// checkbox, ob layer bereits angezeigt wird
			box = new CheckBox(context);
			box.setPadding(20, 0, 20, 0);
			box.setId(j);
			box.setEnabled(false);
			box.setClickable(false);
			box.setChecked(false);
			// der lock-button
			lockLayerButton = new Button(this.context);
			lockLayerButton.setId(j);
			lockLayerButton.setBackgroundColor(android.R.color.transparent);
			lockLayerButton.setText(context.getString(R.string.getfeaturewithlock_lock_button_text));
			lockLayerButton.setOnClickListener(new OnClickListener() {
				public void onClick(View onClickView) {
					// TODO

					// expiry time setzen und locken
					LockExpirySetter setExpiry = new LockExpirySetter(context, getThis());
					setExpiry.startLock((WFSLayer) tableValuesList.get(onClickView.getId()));

					// layer speichern als gelockt in der datenbank
					if (!ProjectHandler.getCurrentProject().getWFSContainer().contains((WFSLayer) tableValuesList.get(onClickView.getId()))) {
						dbAdapter.updateWFSLayerInDB((WFSLayer) tableValuesList.get(onClickView.getId()));
					} else {
						// TODO updaten den lock!!!!

					}

					// tabelle neu schreiben - zu frueh hier, aber wohin?!
					activateLayerTableBuilding(getWFSHandler());
				}
			});
			isThisLayerLocked = false;
			// wenn layer im container vorhanden sind
			if (project.getWFSContainer() != null) {
				// aktuellen eintrag in die tabelle als wfs layer umformen
				currentWFSLayer = (WFSLayer) this.getLayerList().get(j);
				// für layer im projekt testen, ob gleich dem aktuellen ist
				if (project.getWFSContainer().contains(currentWFSLayer)) {
					// aktuellen finden
					for (int i = 0; i < project.getWFSContainer().size(); i++) {
						if (project.getWFSContainer().get(i)
								.equals(currentWFSLayer)) {
							// layer ist bereits in der auswahl zur anzeige
							box.setChecked(true);
							// layer auf lock ueberpruefen
							if (project.getWFSContainer().get(i).isLocked()) {
								// Nachricht, dass gelockter Layer da ist
								String strdate = null;
								SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy - HH:mm:ss");
								if (project.getWFSContainer().get(i).getReleaseDate() != null) {
									strdate = sdf.format(project.getWFSContainer().get(i).getReleaseDate().getTime());
								} 
								alreadyLocked = createTextview(context.getString(R.string.getFeatureWithLockActivity_locked_message) + " " + strdate);
								isThisLayerLocked = true;
								// Log.i(CLASSTAG + "before onClick, in for",
								// "Gelockter Layer gefunden!");
								// der layer war gelocked
							} else {
								// kein gelockter layer hier
								// Log.i(CLASSTAG + " before onClick for ",
								// "Nicht gelockter Layer dieses Servers im Projekt gefunden.");
							}
							// abbruch der for-schleife, da gefunden
							i = project.getWFSContainer().size();
						}
					}
				}
			}
			// checkbox in spalte einfuegen
			boxColumn.add(box);
			// wenn layer gelockt
			if (isThisLayerLocked) {
				// textfeld einfuegen
				buttonColumn.add(alreadyLocked);
			} else {
				// button einfuegen
				buttonColumn.add(lockLayerButton);
			}
		}
		// liste neu initialisieren
		LinkedList<LinkedList<View>> liste = new LinkedList<LinkedList<View>>();
		// beide spalten in liste eintragen und zurueckgeben
		liste.add(boxColumn);
		liste.add(buttonColumn);
		return liste;
	}

	protected LayerTable getThis() {
		// TODO Auto-generated method stub
		return this;
	}

	// checkbox column
	private LinkedList<View> checkBoxColumn(OnCheckedChangeListener onCheck) {
		LinkedList<View> superColumn = new LinkedList<View>();
		// fuer wms noch eine spalte: ob ausgewaehlt
		Project project = ProjectHandler.getCurrentProject();
		TextView tchosen = null;
		CheckBox box = null;
		for (int j = 0; j < this.size(); j++) {
			// tchosen neu setzen+
			tchosen = null;
			if (onCheck instanceof WFSCheckedListener) {
				for (WFSLayer checkLayer : project.getWFSContainer()) {
					// wenn der layer schon angezeit wird, keine checkbox
					// anzeigen,
					// sondern ein textfeld mit dieder info
					// Log.i(CLASSTAG, "WFS-Layer" +
					// checkLayer.getName().toString() + " wird getestet");
					if (this.getLayerList().get(j).equals(checkLayer)) {
						// Log.e(CLASSTAG, "WFS-Layer" +
						// checkLayer.getName().toString() + " ist enthalten");
						tchosen = new TextView(this.context);
						tchosen.setText(R.string.add_layer_table_chosen_box);
					} else {
						// Log.i(CLASSTAG, "WFS-Layer" +
						// checkLayer.getName().toString() +
						// " ist NICHT enthalten");
					}
				}
			} else if (onCheck instanceof WMSCheckedListener) {
				// wenn schon ein WMS eingetragen ist
				// TODO: bei Multi-WMS muss das hier raus!
				for (WMSLayer checkLayer : project.getWMSContainer()) {
					// wenn der layer schon angezeit wird, keine checkbox anzeigen,
					// sondern ein textfeld mit dieder info
					// Log.i(CLASSTAG, "WMS-Layer" +
					// checkLayer.getName().toString() + " wird getestet");
					if (this.getLayerList().get(j).equals(checkLayer)) {
						// Log.e(CLASSTAG, "WMS-Layer" +
						// checkLayer.getName().toString() + " ist enthalten");
						tchosen = new TextView(this.context);
						tchosen.setText(R.string.add_layer_table_chosen_box);	
					} else {
						// Log.i(CLASSTAG, "WMS-Layer" +
						// checkLayer.getName().toString() +
						// " ist NICHT enthalten");
						if (project.getWMSContainer().size() > 0) {
							tchosen = new TextView(this.context);
							tchosen.setText(R.string.add_layer_table_chosen_box_WMS_already_there);
						}
					}

				}
			}
			if (tchosen != null) {
				superColumn.add(tchosen);
				// Log.i(CLASSTAG, "Adde Textfield!");
			} else {
				// Checkbox wird hinzugeügt
				box = new CheckBox(this.context);
				box.setPadding(20, 0, 20, 0);
				box.setId(j);
				// listener anfuegen, ist immer der gleiche... ok?!
				box.setOnCheckedChangeListener(onCheck);
				superColumn.add(box);
				// Log.i(CLASSTAG, "Adde Box!");
			}
		}
		return superColumn;
	}

	// feature count column
	private LinkedList<View> featureCountColumn() {
		LinkedList<View> superColumn = new LinkedList<View>();
		// fuer wms noch eine spalte: ob ausgewaehlt
		TextView aTextView = null;
		for (int j = 0; j < this.size(); j++) {
			aTextView = createTextview(tableValuesList.get(j)
					.getCountFeatures());
			superColumn.add(aTextView);
		}
		return superColumn;
	}

	// get the WFS Type - wms has no type, damn
	private LinkedList<View> getWFSType() {
		LinkedList<View> superColumn = new LinkedList<View>();
		// fuer wms noch eine spalte: ob ausgewaehlt
		TextView aTextView = null;
		for (int j = 0; j < this.size(); j++) {
			aTextView = createTextview(((WFSLayer) tableValuesList.get(j))
					.getType());
			superColumn.add(aTextView);
		}
		return superColumn;
	}

	// erstellt eine liste der views
	private boolean buildRowsAndTableWithHeadlines(
			List<LayerInterface> layerList) {
		// tabelle neu initialisieren
		this.tableValuesList = new LinkedList<LayerInterface>(layerList);
		// die Liste der Views initialisieren
		this.viewListForTables = new LayerTableViewList(this.context);
		// falls die Liste nicht initialisiert ist 
		if (layerList == null) {
			Log.e(CLASSTAG, "No Layers found, List was not initialized!");
			// zeilenanzahl ohne ueberschriften setzen
			this.layerCount = 0;
			// keine tabelle initialisiert
			return false;
		} else if (layerList.size() == 0) {
			// falls die liste leer ist
			Log.e(CLASSTAG,
					"No Layers found, List empty, but initialized! Only Headlines created!");
			// trotzdem standardueberschriften setzen
			this.headLines = buildHeadline();
			// in listen eintragen
			this.viewListForTables.addRow(this.headLines);
			Log.i(CLASSTAG, "Headlines pushed in, but List was empty!");
			// zeilenanzahl ohne ueberschriften setzen
			this.layerCount = 0;
			// tabelle initialisiert
			return true;
		} else {
			// trotzdem standardueberschriften setzen
			this.headLines = buildHeadline();
			// in listen eintragen
			this.viewListForTables.addRow(this.headLines);
			// Log.i(CLASSTAG, "Headlines pushed in!");
			// ansonsten tabellenzeilen hinzufuegen
			for (int i = 0; i < layerList.size(); i++) {
				// werte in zeile einfügen
				this.newRow = new LinkedList<View>();
				this.newRow
				.add(createTitleTextview(layerList.get(i).getName()));
				this.newRow
				.add(createTextview(layerList.get(i).getWorkspace()));
				this.newRow.add(createTextview(layerList.get(i).getEPSG()));
				// zeile in tabelle einfuegen
				if (this.viewListForTables.addRow(newRow)) {
					// Log.i(CLASSTAG, "New Row pushed in!");
				} else {
					// Log.i(CLASSTAG, "New Row NOT pushed in! ERROR!!");
				}
			}
			// zeilenanzahl setzen
			this.layerCount = layerList.size();
			// tabelle initialisiert
			return true;
		}
	}

	// erzeugt die views fuer die ueberschriften aller tabellen
	private LinkedList<View> buildHeadline() {
		// ueberschriften fuer spalten erzeugen
		LinkedList<View> newHeadLines = new LinkedList<View>();
		newHeadLines.add(createTitleTextview(context
				.getString(R.string.buildTableTask_name)));
		newHeadLines.add(createTitleTextview(context
				.getString(R.string.buildTableTask_workspace)));
		newHeadLines.add(createTitleTextview(context
				.getString(R.string.buildTableTask_epsg)));
		this.headLines = new LinkedList<View>();
		this.headLines.clear();
		this.clearTable();
		return newHeadLines;
	}

	// create standard Textviews
	private TextView createTextview(String string) {
		TextView head = new TextView(this.context);
		head.setText(string);
		head.setTypeface(null, Typeface.NORMAL);
		head.setPadding(20, 0, 20, 0);
		head.setTextSize(20);
		head.setTextColor(color.text_color);
		return (head);
	}

	// create standard Textviews
	private TextView createTextview(int integer) {
		return (createTextview(Integer.toString(integer)));
	}

	// create BOLD textviews
	private TextView createTitleTextview(String title) {
		TextView head = createTextview(title);
		head.setTypeface(null, Typeface.BOLD);
		head.setTextColor(Color.parseColor("#33aadd"));
		head.setAllCaps(true);
		return (head);
	}

}
