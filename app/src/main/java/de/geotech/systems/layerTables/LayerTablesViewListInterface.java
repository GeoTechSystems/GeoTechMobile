/**
 * Lists to manage views for tables
 * 
 * @author Torsten Hoch
 */
package de.geotech.systems.layerTables;

import java.util.LinkedList;

import android.view.View;

public interface LayerTablesViewListInterface {
	
	/**
	 * Adds a column.
	 *
	 * @param newColumn the new column
	 * @return true, if successful
	 */
	public boolean addColumn(LinkedList <View> newColumn);
	
	/**
	 * Adds a column.
	 *
	 * @param newColumn the new column
	 * @param newHeadLine the new head line
	 * @return true, if successful
	 */
	public boolean addColumn(LinkedList <View> newColumn, View newHeadline);
	
	/**
	 * Adds a row.
	 *
	 * @param newRow the new row
	 * @return true, if successful
	 */
	public boolean addRow(LinkedList <View> newRow);
	
	/**
	 * Sets the whole list. HeadlLines are not changed.
	 *
	 * @param newList the new list
	 * @return true, if successful
	 */
	public boolean setList(LinkedList <LinkedList <View>> newList);
	
	/**
	 * Sets the list. HeadlLines are changed too.
	 *
	 * @param newList the new list
	 * @param newHeadlines the new headlines
	 * @return true, if successful
	 */
	public boolean setList(LinkedList <LinkedList <View>> newList, LinkedList <View> newHeadlines);
	
	/**
	 * Get all rows.
	 *
	 * @return all linked rows
	 */
	public LinkedList <LinkedList <View>> getCompleteLists();
	
}
