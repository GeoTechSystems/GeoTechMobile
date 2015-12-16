/**
 * LinkedList of rows (rows as LinkedLists of views) 
 * - a virtual table of views
 * 
 * @author Torsten Hoch
 */
package de.geotech.systems.layerTables;

import java.util.LinkedList;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class LayerTableViewList implements LayerTablesViewListInterface {
	private static final String CLASSTAG = "LayerTableViewList";

	private Context context;
	// the complete list/table of views
	private LinkedList <LinkedList <View>> completeList;
	// number of columns
	private int columnCount;
	// number of rows WITHIN the headlines
	private int rowCount;

	/**
	 * Instantiates a new view list for tables.
	 *
	 * @param headlinesSet the headlines set
	 * @param completeList the complete list
	 */
	public LayerTableViewList(Context context) {
		super();
		// alles initialisieren
		this.context = context;
		this.columnCount = 0;
		this.rowCount = 0;
		this.completeList = new LinkedList <LinkedList <View>>();
	}

	/**
	 * Adds a column.
	 *
	 * @param newColumn the new column
	 * @return true, if successful
	 */
	@Override
	public boolean addColumn(LinkedList <View> newColumn) {
		return this.addColumn(newColumn, null);
	}

	/**
	 * Adds a column.
	 *
	 * @param newColumn the new column
	 * @param newHeadline the new head line
	 * @return true, if successful
	 */
	@Override
	public boolean addColumn(LinkedList <View> newColumn, View newHeadline) {
		// wenn die liste initialisiert ist
		if (newColumn!= null) {
			// wenn die liste NICHT die richtige laenge hat
			if ((this.rowCount - 1) != newColumn.size()) {
				Log.e(CLASSTAG, "Column filled with wrong number of Views!");
				Log.e(CLASSTAG, newColumn.size() + " Views for " + (this.rowCount - 1) + " Rows (already excluding headline)!");
				return false;
			} else {
				// sonst wenn headline dabei ist
				if (newHeadline != null) {
					this.completeList.get(0).addLast(newHeadline);
				} else {
					// eigene headline erstellen
					this.completeList.get(0).addLast(this.createTitleTextview(this.columnCount + 1));
				}
				// ausserdem in jede liste hinten einen view anhaengen
				for (int i = 1; i <= newColumn.size(); i++) {
					this.completeList.get(i).addLast(newColumn.get(i - 1));
				}
				// spaltenanzahl erhoehen
				this.columnCount++;
				// spalte hinzugefuegt
				return true;
			}
		} else {
			// wenn die liste nicht initialisiert ist
			return false;
		}
	}

	/**
	 * Adds a row.
	 *
	 * @param newRow the new row
	 * @return true, if successful
	 */
	@Override
	public boolean addRow(LinkedList <View> newRow) {
		if (this.completeList.size() == 0) {
			this.columnCount = newRow.size();
		}
		// wenn die liste initialisiert ist
		if (newRow != null) {
			if (newRow.size() == this.columnCount) {
				// zeile hinten einfuegen
				this.completeList.addLast(newRow);
				// zeilenanzahl erhoehen
				this.rowCount++;
				// neue zeile eingefuegt
				return true;
			} else { 
				// falsche anzahl an elementen
				Log.e(CLASSTAG, "Column filled with wrong number of Views!");
				Log.e(CLASSTAG, newRow.size() + " Views for " + this.columnCount + " Columns!");
				return false;		
			}
		} else {
			// newRow nicht initialisiert
			Log.e(CLASSTAG, "New Row NOT pushed in! newRow null!");
			return false;
		}
	}

	/**
	 * Sets the whole list. HeadlLines are not changed.
	 *
	 * @param newList the new list
	 * @return true, if successful
	 */
	@Override
	public boolean setList(LinkedList <LinkedList <View>> newList) {
		return (this.setList(newList, null));
	}

	/**
	 * Sets the list. HeadlLines are changed too.
	 *
	 * @param newList the new list
	 * @param newHeadlines the new headlines
	 * @return true, if successful
	 */
	@Override
	public boolean setList(LinkedList <LinkedList <View>> newList, LinkedList <View> newHeadlines) {
		// wenn die liste initialisiert ist
		if (newList != null) {
			// ueberpruefen, ob alle zeilen gleich lang
			int testInt = newList.getFirst().size();
			boolean testLength = true;
			for (int i = 1; i < newList.size(); i++) {
				// wenn eine zeile andere laenge hat
				if (newList.get(i).size() != testInt) {
					testLength = false;
				}
			}
			// nur wenn also alle zeilen gleich lang
			if (testLength) {
				// wenn die headlines gesetzt werden konnten
				if (this.setHeadlines(newHeadlines)) {
					// fuege jede zeile einzeln ein
					for (int i = 0 ; i < newHeadlines.size(); i++) {
						this.addRow(newList.get(i));
					}
					this.columnCount = testInt;
					this.rowCount = newList.size() + 1;
					return true;
				} else {
					// wenn keine headlines gesetzt wurden
					Log.e(CLASSTAG, "setlist false! headlines could not be set");
					return false;
				}
			} else { 
				// wenn zeilen verschieden lang sind
				Log.e(CLASSTAG, "setlist false! rows differ in length");
				return false;			
			}
		} else {
			// wenn die liste nicht initialisiert ist
			Log.e(CLASSTAG, "setlist false! List not initialized");
			return false;
		}
	}

	/**
	 * Get all rows as Linked List.
	 *
	 * @return the Linked List of linked Views
	 */
	@Override
	public LinkedList <LinkedList <View>> getCompleteLists() {
		return this.completeList;
	}

	/**
	 * Sets the headlines.
	 *
	 * @param newHeadLines the new head lines
	 * @return true, if successful
	 */
	private boolean setHeadlines(LinkedList <View> newHeadlines) {
		// wenn die liste initialisiert ist
		if (newHeadlines != null) {
			// wenn die liste nicht so lang wie alle anderen zeilen ist
			if ((this.columnCount) != newHeadlines.size()) {
				Log.e(CLASSTAG, "Headline-Row filled with wrong number of Views!");
				Log.e(CLASSTAG, newHeadlines.size() + " Views for " + this.columnCount + " Columns!");
				return false;
			} else {
				// wenn liste korrekte laenge hat
				this.completeList.clear();
				this.completeList.addFirst(newHeadlines);
				this.columnCount = newHeadlines.size();
				this.rowCount = 1;
				Log.i(CLASSTAG, "Headlines filled!");
				return true;
			} 
		} else {
			// wenn liste nicht initialisiert, eigene liste erstellen
			newHeadlines = new LinkedList <View>();
			Log.e(CLASSTAG, "Headline-Views empty! Creating Column Titles from '1' to 'n'");
			for (int i = 0; i < columnCount; i++) {
				newHeadlines.add(this.createTextview(i));
			}
			this.completeList.clear();
			this.completeList.addFirst(newHeadlines);
			this.columnCount = newHeadlines.size();
			this.rowCount = 1;
			Log.i(CLASSTAG, "Headlines filled with self made titles!");
			return true;
		}
	}

	// create standard Textviews
	private TextView createTextview(String string) {
		TextView head = new TextView(this.context);
		head.setText(string);
		head.setTypeface(null, Typeface.NORMAL);
		head.setPadding(20, 0, 20, 0);
		head.setTextSize(20);
		return (head);
	}

	// create standard Textviews from integer
	private TextView createTextview(int integer) {
		return (createTextview(Integer.toString(integer)));
	}

	// create BOLD textviews from integer
	private TextView createTitleTextview(int integer) {
		return (createTitleTextview(Integer.toString(integer)));
	}

	// create BOLD textviews
	private TextView createTitleTextview(String title) {
		TextView head = createTextview(title);
		head.setTypeface(null, Typeface.BOLD);
		return (head);
	}

}
