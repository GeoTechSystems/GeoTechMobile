/**
 * Handling the whole SQL-Database-Action
 * 
 * @author svenweisker
 * @author Torsten Hoch
 * @author bschm
 */

package de.geotech.systems.database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

import de.geotech.systems.features.Feature;
import de.geotech.systems.features.FeaturePrecision;
import de.geotech.systems.projects.Project;
import de.geotech.systems.projects.Server;
import de.geotech.systems.wfs.WFSCheckedListener;
import de.geotech.systems.wfs.WFSLayer;
import de.geotech.systems.wfs.WFSLayerAttributeTypes;
import de.geotech.systems.wms.WMSCheckedListener;
import de.geotech.systems.wms.WMSLayer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

public class DBAdapter {
	// Classtag
	private static final String CLASSTAG = "DBAdapter";
	// Name of internal DB
	private static final String DATABASE_NAME = "GeoTechMobileDB";
	// Version of DB
	private static final int DBVERSION = 3;
	// all needed attributes for a ProjectSetting
	private static final String[] PROJECTATTRIBUTES = SQLProjects.PROJECTATTRIBUTES;
	// all needed attributes for a WFSLayer
	private static final String[] WFSLAYERATTRIBUTES = SQLWFS.WFSLAYERATTRIBUTES;
	// all needed attributes for a WMSLayer
	private static final String[] WMSLAYERATTRIBUTES = SQLWMS.WMSLAYERATTRIBUTES;
	// all attributes of attributes
	private static final String[] ALLATTRIBUTES = SQLAttribute.ALLATTRIBUTES;
	// all feature attributes
	private static final String[] FEATUREATTRIBUTES = SQLFeature.FEATUREATTRIBUTES;
	// all server attributes
	private static final String[] ALLSERVERATTRIBUTES = SQLServer.ALLSERVERATTRIBUTES;
	// all standard servers
	public static final String[] STANDARDSERVERS = SQLServer.STANDARDSERVERS;
	// all special authentication  servers
	public static final Server[] AUTHENTICATEDSERVERS = SQLServer.AUTHENTICATEDSERVERS;
	// all Table Names of DB
	private static final String[] ALLTABLES = { SQLProjects.TABLE_NAME,
		SQLWFS.TABLE_NAME, SQLWMS.TABLE_NAME, SQLAttribute.TABLE_NAME,
		SQLFeature.TABLE_NAME, SQLPrecision.TABLE_NAME,
		SQLRestrictions.TABLE_NAME, SQLSatellite.TABLE_NAME,
		SQLServer.TABLE_NAME };
	// all Create-Table Strings of all Tables
	private static final String[] ALLCREATETABLES = { SQLProjects.CREATE_TABLE,
		SQLWFS.CREATE_TABLE, SQLWMS.CREATE_TABLE,
		SQLAttribute.CREATE_TABLE, SQLFeature.CREATE_TABLE,
		SQLPrecision.CREATE_TABLE, SQLRestrictions.CREATE_TABLE,
		SQLSatellite.CREATE_TABLE, SQLServer.CREATE_TABLE_Server };

	// the context
	private Context context;
	// the SQL DB
	private SQLiteDatabase sqlDB;
	// helper class for DB
	private DataBaseHelper dbHelper;
	// Toast to show messages
	private Toast toast; 

	/**
	 * Instantiates a new DB adapter.
	 *
	 * @param newContext the new context
	 */
	public DBAdapter(Context newContext) {
		this.context = newContext;
		// initialize the helper class
		// Log.e(CLASSTAG, "Initialized.");
		this.dbHelper = new DataBaseHelper(context, DATABASE_NAME, DBVERSION);
	}

	/**
	 * Opens the SQL Lite DB and begins a Transaction.
	 *
	 * @throws SQLException the SQL exception
	 */
	// TODO 
	// - trennen: readable und writeable open()
	// - außerdem abfangen, dass geoeffnet wird, wenn schon offen
	// - genau so bei close()
	// - eigentlich nur bei Operationen nötig, die schreiben 
	private void open() throws SQLException {
		this.sqlDB = this.dbHelper.getWritableDatabase();
		// Log.i(CLASSTAG + " OPENED", "Database \"" + dbHelper.getDatabaseName() + "\" opened.");
		this.sqlDB.beginTransaction();
	}

	/**
	 * Closes the SQL Lite DB and ends a Transaction.
	 */
	private void close() {
		this.sqlDB.setTransactionSuccessful();
		this.sqlDB.endTransaction();
		// Log.i(CLASSTAG + " CLOSED", "Database \"" + dbHelper.getDatabaseName() + "\" closed.");
		this.dbHelper.close();
	}

	// Hier beginnen die SERVER-Methoden
	/**
	 * Gets the content values of servers.
	 *
	 * @param newServer the new server
	 * @return the content values of servers
	 */
	private ContentValues getContentValuesOfServers(Server newServer) {
		ContentValues initialProject = new ContentValues();
		// the whole String without the first (ID) field
		initialProject.put(SQLServer.ALLSERVERATTRIBUTES[1], newServer.getUrl());
		initialProject.put(SQLServer.ALLSERVERATTRIBUTES[2], newServer.isAuthenticate());
		initialProject.put(SQLServer.ALLSERVERATTRIBUTES[3], newServer.getUsername());
		initialProject.put(SQLServer.ALLSERVERATTRIBUTES[4], newServer.getPassword());
		return initialProject;
	}	

	/**
	 * Insert server list into db.
	 *
	 * @param serverList the server list
	 * @return true, if successful
	 */
	public boolean insertServerListIntoDB(ArrayList<Server> serverList) {
		open();
		for (int i = 0; i < serverList.size(); i++) {
			ContentValues initialLayer = getContentValuesOfServers(serverList.get(i));
			long newID = sqlDB.insert(SQLServer.TABLE_NAME, null, initialLayer);
			serverList.get(i).setDBID(newID);
			// Log.i(CLASSTAG + " insertServersIntoDB", "Inserting Server " + serverList.get(i) + " in DB with ID " + newID);
		}
		close();
		return true;
	}

	/**
	 * Insert server into db.
	 *
	 * @param newServer the new server
	 * @return the long
	 */
	public long insertServerIntoDB(Server newServer) {
		open();
		ContentValues initialLayer = getContentValuesOfServers(newServer);
		long newID = sqlDB.insert(SQLServer.TABLE_NAME, null, initialLayer);
		newServer.setDBID(newID);
		// Log.i(CLASSTAG + " insertServerIntoDB", "Inserting Server " + newServer + " in DB with ID " + newID);
		close();
		return newID;
	}

	/**
	 * Delete all servers from db.
	 *
	 * @return true, if successful
	 */
	private boolean deleteAllServersFromDB() {
		if (sqlDB.delete(SQLServer.TABLE_NAME, null, null) > 0) {
			// Log.i(CLASSTAG + " deleteAllServersFromDB", "Deleted all Servers in DB.");
			return true;			
		} else { 
			Log.e(CLASSTAG + " deleteAllServersFromDB", "Failed to delete all Servers in DB.");
			return false;
		}
	}

	/**
	 * Delete server from db.
	 *
	 * @param serverURL the server url
	 * @return true, if successful
	 */
	public boolean deleteServerFromDB(String serverURL) {
		open();
		if (sqlDB.delete(SQLServer.TABLE_NAME, ALLSERVERATTRIBUTES[1] + " = " + serverURL, null) > 0) {
			close();
			// Log.i(CLASSTAG + " deleteServersFromDB", "Deleted Server " + serverURL + " from DB.");
			return true;			
		} else { 
			close();
			Log.e(CLASSTAG + " deleteServersFromDB", "Failed to delete Server " + serverURL + " from DB.");
			return false;
		}
	}

	/**
	 * Initialize Standard-Servers in db (normal and authenticated) 
	 * server in db.
	 */
	public void initializeStandardServersInDB() {
		this.open();
		this.deleteAllServersFromDB();
		for (int i = 0; i < STANDARDSERVERS.length; i++) {
			Server newServer = new Server(STANDARDSERVERS[i]);
			ContentValues initialLayer = getContentValuesOfServers(newServer);
			// Log.i(CLASSTAG + " insertServerIntoDB", "Inserting Server " + newServer.getUrl() + " in DB.");
			sqlDB.insert(SQLServer.TABLE_NAME, null, initialLayer);
		}
		for (int i = 0; i < AUTHENTICATEDSERVERS.length; i++) {
			ContentValues initialLayer = getContentValuesOfServers(AUTHENTICATEDSERVERS[i]);
			// Log.i(CLASSTAG + " insertServerIntoDB", "Inserting Authenticated Server " + AUTHENTICATEDSERVERS[i].getUrl() + " in DB.");
			sqlDB.insert(SQLServer.TABLE_NAME, null, initialLayer);
		}
		this.close();
	}

	/**
	 * Gets the all servers from db.
	 *
	 * @return the all servers from db
	 * @throws SQLException the SQL exception
	 */
	private Cursor getAllServersFromDB() throws SQLException {
		Cursor cursor = sqlDB.query(true, SQLServer.TABLE_NAME, ALLSERVERATTRIBUTES,
				null, null, null, null, null, null);
		if (cursor != null) {
			cursor.moveToFirst();
		}
		return cursor;
	}

	/**
	 * Gets all server urls from db.
	 *
	 * @return the server urls from db
	 */
	public ArrayList<String> getAllServerURLsFromDB() {
		ArrayList<String> serverContainer = new ArrayList<String>();
		try {
			open();
			Cursor cursor = getAllServersFromDB();
			// if no WMS is selected
			if (cursor!= null  && cursor.getCount() > 0) {
				do {
					String serverURL = cursor.getString(cursor.getColumnIndexOrThrow(ALLSERVERATTRIBUTES[1]));
					serverContainer.add(serverURL);
					// Log.i(CLASSTAG + " getAllServerURLsFromDB", "Server " + serverURL + " imported into working memory");
				} while (cursor.moveToNext());
				close();
				// Log.i(CLASSTAG + " getAllServerURLsFromDB", "All Server URLs successfully imported!");
				return serverContainer;
			} else {
				close();
				// Log.i(CLASSTAG + " getAllServerURLsFromDB", "NO Servers in Project!");
				initializeStandardServersInDB();
				return getAllServerURLsFromDB();
			}
		} catch (CursorIndexOutOfBoundsException e) {
			close();
			Log.e(CLASSTAG + " getAllServerURLsFromDB", "CursorIndexOutOfBoundsException: " + e.getStackTrace().toString());
			return new ArrayList<String>();
		}
	}

	/**
	 * Gets the server by url.
	 *
	 * @param newServerURL the new server url
	 * @return the server by url
	 */
	private Server getServerByURL(String newServerURL) {
		open();
		Cursor cursor = sqlDB.query(true, SQLServer.TABLE_NAME, ALLSERVERATTRIBUTES,
				ALLSERVERATTRIBUTES[1] + "=\"" + newServerURL + "\"", null, null, null, null, null);
		if (cursor != null) {
			cursor.moveToFirst();
			long projectID = cursor.getLong(cursor.getColumnIndexOrThrow(ALLSERVERATTRIBUTES[0]));
			String serverURL = cursor.getString(cursor.getColumnIndexOrThrow(ALLSERVERATTRIBUTES[1]));
			boolean auth = cursor.getInt(cursor.getColumnIndex(ALLSERVERATTRIBUTES[2])) > 0;
			String serverUserName = cursor.getString(cursor.getColumnIndexOrThrow(ALLSERVERATTRIBUTES[3]));
			String serverPassword = cursor.getString(cursor.getColumnIndexOrThrow(ALLSERVERATTRIBUTES[4]));
			Server newServer = new Server(serverURL, serverUserName, serverPassword);
			newServer.setDBID(projectID);
			newServer.setAuthenticate(auth);
			// Log.i(CLASSTAG + "getServerByURL", "Server " + serverURL + " imported into working memory");
			close();
			return newServer;
		} else {
			close();
			return new Server(newServerURL);			
		}
	}

	/**
	 * Gets all the complete servers from db.
	 *
	 * @return the complete servers from db
	 */
	public ArrayList<Server> getCompleteServersFromDB() {
		ArrayList<Server> serverContainer = new ArrayList<Server>();
		try {
			open();
			Cursor cursor = getAllServersFromDB();
			// if no WMS is selected
			if (cursor!= null  && cursor.getCount() > 0) {
				do {
					long projectID = cursor.getLong(cursor.getColumnIndexOrThrow(ALLSERVERATTRIBUTES[0]));
					String serverURL = cursor.getString(cursor.getColumnIndexOrThrow(ALLSERVERATTRIBUTES[1]));
					boolean auth = cursor.getInt(cursor.getColumnIndex(ALLSERVERATTRIBUTES[2])) > 0;
					String serverUserName = cursor.getString(cursor.getColumnIndexOrThrow(ALLSERVERATTRIBUTES[3]));
					String serverPassword = cursor.getString(cursor.getColumnIndexOrThrow(ALLSERVERATTRIBUTES[4]));
					Server newServer = new Server(serverURL, serverUserName, serverPassword);
					newServer.setDBID(projectID);
					newServer.setAuthenticate(auth);
					serverContainer.add(newServer);
					// Log.i(CLASSTAG + "getServersFromDB", "Server " + serverURL + " imported into working memory");
				} while (cursor.moveToNext());
				close();
				// Log.i(CLASSTAG + " getCompleteServersFromDB", "All " + serverContainer.size() + " Servers successfully imported!");
				return serverContainer;
			} else {
				close();
				// Log.i(CLASSTAG + " getCompleteServersFromDB", "No Servers in DB found!");
				return new ArrayList<Server>();
			}
		} catch (CursorIndexOutOfBoundsException e) {
			close();
			Log.e(CLASSTAG + " getCompleteServersFromDB", "CursorIndexOutOfBoundsException: " + e.getStackTrace().toString());
			return new ArrayList<Server>();
		}
	}

	// Hier beginnen die Project-Methoden
	/**
	 * Gets the content values of a project.
	 *
	 * @param project the project
	 * @return the content values of a project
	 */
	private ContentValues getContentValuesOfAProject(Project project) {
		ContentValues initialProject = new ContentValues();
		// the whole String without the first (ID) field
		initialProject.put(PROJECTATTRIBUTES[1], project.getProjectName());
		initialProject.put(PROJECTATTRIBUTES[2], project.getEpsgCode());
		initialProject.put(PROJECTATTRIBUTES[3], project.getProjection());
		initialProject.put(PROJECTATTRIBUTES[4], project.getDescription());
		initialProject.put(PROJECTATTRIBUTES[5], project.isOSM());
		initialProject.put(PROJECTATTRIBUTES[6], project.getSync());
		initialProject.put(PROJECTATTRIBUTES[7], project.getAutoSync());
		return initialProject;
	}

	/**
	 * Insert project into db.
	 *
	 * @param project the project
	 * @return the long
	 */
	public long insertProjectIntoDB(Project project) {
		open();
		ContentValues initialProject = getContentValuesOfAProject(project);
		long newProjectID = sqlDB.insert(SQLProjects.TABLE_NAME, null,
				initialProject);
		// Log.i(CLASSTAG + " insertProjectIntoDB", "Project data successfully stored for Project " + project.getProjectName() + " with new ID " + newProjectID);
		close();
		return newProjectID;
	}

	/**
	 * Delete project from db.
	 *
	 * @param projectID the project id
	 * @return true, if successful
	 */
	public boolean deleteProjectFromDB(Project project) {
		for (int i = 0; i < project.getWFSContainer().size(); i++) {
			deleteWFSLayerFromDB(project.getWFSContainer().get(i));
		}
		project.getWFSContainer().clear();
		WFSCheckedListener.getWFSCheckedList().clear();
		for (int i = 0; i < project.getWMSContainer().size(); i++) {
			deleteWMSLayerFromDB(project.getWMSContainer().get(i).getLayerID());
		}
		project.getWMSContainer().clear();
		WMSCheckedListener.getWMSCheckedList().clear();
		open();
		if (sqlDB.delete(SQLProjects.TABLE_NAME, SQLProjects.ID + "=" + project.getProjectID(), null) > 0) {
			close();
			// Log.i(CLASSTAG + " deleteProjectFromDB", "Deleted Project " + project.getProjectName() + " from DB.");
			return true;
		} else {
			close();
			Log.i(CLASSTAG + " deleteProjectFromDB", "Could not delete Project " + project.getProjectName() + " from DB.");
			return false;
		}
	}

	/**
	 * Sql all projects from db.
	 *
	 * @return the cursor
	 */
	private Cursor sqlAllProjectsFromDB() {
		return sqlDB.query(SQLProjects.TABLE_NAME, PROJECTATTRIBUTES, null,
				null, null, null, null);
	}

	/**
	 * Update project in db.
	 *
	 * @param project the project
	 * @return true, if successful
	 */
	public boolean updateProjectInDB(Project project) {
		open();
		ContentValues initialProject = getContentValuesOfAProject(project);
		if (sqlDB.update(SQLProjects.TABLE_NAME, initialProject, SQLProjects.ID
				+ "=" + project.getProjectID(), null) > 0) {
			close();
			return true;
		} else {
			close();
			return false;
		}
	}

	/**
	 * Gets the projects from db.
	 *
	 * @return the projects from db
	 */
	public ArrayList<Project> getProjectsFromDB() {
		ArrayList<Project> projectList = new ArrayList<Project>();
		try {
			open();
			Cursor cursor = sqlAllProjectsFromDB();
			if (cursor != null && cursor.getCount() > 0) {
				cursor.moveToFirst();
				do {
					long projectID = cursor.getLong(cursor.getColumnIndexOrThrow(PROJECTATTRIBUTES[0]));
					String pName = cursor.getString(cursor.getColumnIndexOrThrow(PROJECTATTRIBUTES[1]));
					int epsg = cursor.getInt(cursor.getColumnIndexOrThrow(PROJECTATTRIBUTES[2]));
					int projection = cursor.getInt(cursor.getColumnIndex(PROJECTATTRIBUTES[3]));
					String description = cursor.getString(cursor.getColumnIndex(PROJECTATTRIBUTES[4]));
					boolean osm = cursor.getInt(cursor.getColumnIndex(PROJECTATTRIBUTES[5])) > 0;
					int sync = cursor.getInt(cursor.getColumnIndex(PROJECTATTRIBUTES[6]));
					int auto_sync = cursor.getInt(cursor.getColumnIndex(PROJECTATTRIBUTES[7]));
					projectList.add(new Project(context, projectID, pName, epsg, 
							projection,	description, osm, sync,	auto_sync));
					// Log.i(CLASSTAG + " getProjectsFromDB()", "Project data successfully imported for Project " + pName);
				} while (cursor.moveToNext());
				cursor.close();
				close();
				// Log.i(CLASSTAG + " getProjectsFromDB()", "Project data successfully imported for " + projectList.size() + " Projects!");
				return projectList;
			} else {
				Log.i(CLASSTAG + " getProjectsFromDB()", "No Project information found and/or imported.");
				close();
				return new ArrayList<Project>();
			}
		} catch (CursorIndexOutOfBoundsException e) {
			close();
			Log.i(CLASSTAG + " getProjectsFromDB()", "CursorIndexOutOfBoundsException: " + e.getStackTrace().toString());
			return new ArrayList<Project>();
		}
	}

	// Hier beginnen WFS Methoden
	/**
	 * Gets the content values of a wfs-layer.
	 *
	 * @param layer the layer
	 * @return the content values of a wfs-layer
	 */
	private ContentValues getContentValuesOfAWFS(WFSLayer layer) {
		ContentValues initialLayer = new ContentValues();
		// alle Felder bis auf den primaerschluessel
		initialLayer.put(WFSLAYERATTRIBUTES[1], layer.getProjectID());
		initialLayer.put(WFSLAYERATTRIBUTES[2], layer.getName());
		initialLayer.put(WFSLAYERATTRIBUTES[3], layer.getTitle());
		initialLayer.put(WFSLAYERATTRIBUTES[4], layer.getWorkspace());
		initialLayer.put(WFSLAYERATTRIBUTES[5], layer.getNamespace());
		initialLayer.put(WFSLAYERATTRIBUTES[6], layer.getType());
		initialLayer.put(WFSLAYERATTRIBUTES[7], layer.getGeometryColumn());
		initialLayer.put(WFSLAYERATTRIBUTES[8], layer.getEPSG());
		initialLayer.put(WFSLAYERATTRIBUTES[9], layer.getUrl());
		initialLayer.put(WFSLAYERATTRIBUTES[10], layer.isMultiGeom());
		initialLayer.put(WFSLAYERATTRIBUTES[11], layer.isActive());
		initialLayer.put(WFSLAYERATTRIBUTES[12], layer.getTimestamp());
		initialLayer.put(WFSLAYERATTRIBUTES[13], layer.getColor());
		initialLayer.put(WFSLAYERATTRIBUTES[14], layer.getCountFeatures());
		initialLayer.put(WFSLAYERATTRIBUTES[15], layer.isSync());
		initialLayer.put(WFSLAYERATTRIBUTES[16], layer.getSizeMB());
		initialLayer.put(WFSLAYERATTRIBUTES[17], layer.isLocked());
		initialLayer.put(WFSLAYERATTRIBUTES[18], layer.getLockID());
		initialLayer.put(WFSLAYERATTRIBUTES[19], layer.getLockExpiry());
		initialLayer.put(WFSLAYERATTRIBUTES[20], layer.getLockDate().getTimeInMillis());
		initialLayer.put(WFSLAYERATTRIBUTES[21], layer.getReleaseDate().getTimeInMillis());
		// Log.i(CLASSTAG + " getContentValuesOfAWFS", "Inside ContentValues: "+ initialLayer.toString());
		return initialLayer;
	}

	/**
	 * Insert wfs layer into db.
	 *
	 * @param layer the layer
	 * @return the long
	 */
	public long insertWFSLayerIntoDB(WFSLayer layer) {
		open();
		ContentValues initialLayer = getContentValuesOfAWFS(layer);
		// Log.i(CLASSTAG + " insertWFSLayerIntoDB", "Inserting Layer " + layer.getName() + " in DB.");
		long newLayerID = sqlDB.insert(SQLWFS.TABLE_NAME, null, initialLayer);
		for (WFSLayerAttributeTypes lA : layer.getAttributeTypes()) {
			lA.setAttributeID(insertAttribute(lA, newLayerID));
		}
		close();
		return newLayerID;
	}

	/**
	 * Delete wfs layer from db.
	 *
	 * @param layerID the layer id
	 * @return true, if successful
	 */
	public boolean deleteWFSLayerFromDB(WFSLayer wfsLayer) {
		if (wfsLayer.getLayerID() > 0) {
			deleteAllFeaturesOfALayerFromDB(wfsLayer);
			open();
			deleteAllAttributesOfWFS(wfsLayer);
			if (sqlDB.delete(SQLWFS.TABLE_NAME, WFSLAYERATTRIBUTES[0] + "=\"" + wfsLayer.getLayerID() + "\"", null) > 0) {
				close();
				// Log.i(CLASSTAG + " deleteWFSLayerFromDB", "Successfully deleted WFSLayer " + wfsLayer.getName() + ".");
				return true;
			} else {
				close();
				Log.e(CLASSTAG + " deleteWFSLayerFromDB", "Could not delete WFSLayer " + wfsLayer.getName() + ".");
				return false;
			}
		} else {
			Log.e(CLASSTAG + " deleteWFSLayerFromDB", "Wrong WFSLayer ID to delete given! Layer ID: " + wfsLayer.getLayerID());
			return false;
		}
	}

	/**
	 * Update wfs layer in db.
	 *
	 * @param layer the layer
	 * @return true, if successful
	 */
	public boolean updateWFSLayerInDB(WFSLayer layer) {
		open();
		ContentValues initialLayer = getContentValuesOfAWFS(layer);
		// Log.i(CLASSTAG + " updateWFSLayerInDB", "Updating WFSLayer where " + SQLWFS.ID + "=" + layer.getLayerID());
		if (sqlDB.update(SQLWFS.TABLE_NAME, initialLayer, SQLWFS.ID + "=" + layer.getLayerID(), null) > 0) {
			for (WFSLayerAttributeTypes lA : layer.getAttributeTypes()) {
				updateAttribute(lA, layer.getLayerID());
			}
			close();
			// Log.i(CLASSTAG + " updateWFSLayerInDB", "Successfully UPDATED WFSLayer " + layer.getName() + ".");
			return true;
		} else {
			Log.i(CLASSTAG + " updateWFSLayerInDB", "Could not UPDATE WFSLayer " + layer.getName() + ".");
			close();
			return false;
		}
	}

	/**
	 * Gets the all wfs layers from db.
	 *
	 * @param projectID the project id
	 * @return the all wfs layers from db
	 * @throws SQLException the SQL exception
	 */
	private Cursor getAllWFSLayersFromDB(long projectID) throws SQLException {
		Cursor cursor = sqlDB.query(true, SQLWFS.TABLE_NAME, WFSLAYERATTRIBUTES,
				SQLProjects.ID + "=" + projectID, null, null, null, null, null);
		if (cursor != null) {
			cursor.moveToFirst();
		}
		return cursor;
	}

	/**
	 * Gets the projects wfs from db.
	 *
	 * @param newProjectID the new project id
	 * @return the projects wfs from db
	 */
	public ArrayList<WFSLayer> getProjectsWFSFromDB(long newProjectID) {
		ArrayList<WFSLayer> wfsContainer = new ArrayList<WFSLayer>();
		try {
			open();
			Cursor cursor = getAllWFSLayersFromDB(newProjectID);
			// if no WMS is selected
			if (cursor != null && cursor.getCount() > 0) {
				do {
					long layerID = cursor.getLong(cursor.getColumnIndexOrThrow(WFSLAYERATTRIBUTES[0]));
					long projectID = cursor.getLong(cursor.getColumnIndexOrThrow(WFSLAYERATTRIBUTES[1]));
					String name = cursor.getString(cursor.getColumnIndexOrThrow(WFSLAYERATTRIBUTES[2]));
					String title = cursor.getString(cursor.getColumnIndexOrThrow(WFSLAYERATTRIBUTES[3]));
					String workspace = cursor.getString(cursor.getColumnIndexOrThrow(WFSLAYERATTRIBUTES[4]));
					String namespace = cursor.getString(cursor.getColumnIndexOrThrow(WFSLAYERATTRIBUTES[5]));
					String type = cursor.getString(cursor.getColumnIndexOrThrow(WFSLAYERATTRIBUTES[6]));
					String geom = cursor.getString(cursor.getColumnIndexOrThrow(WFSLAYERATTRIBUTES[7]));
					String epsg = cursor.getString(cursor.getColumnIndexOrThrow(WFSLAYERATTRIBUTES[8]));
					String url = cursor.getString(cursor.getColumnIndexOrThrow(WFSLAYERATTRIBUTES[9]));
					boolean ismulti = cursor.getInt(cursor.getColumnIndexOrThrow(WFSLAYERATTRIBUTES[10])) > 0;
					boolean isactive = cursor.getInt(cursor.getColumnIndexOrThrow(WFSLAYERATTRIBUTES[11])) > 0;
					int timestamp = cursor.getInt(cursor.getColumnIndexOrThrow(WFSLAYERATTRIBUTES[12]));
					int color = cursor.getInt(cursor.getColumnIndexOrThrow(WFSLAYERATTRIBUTES[13]));
					int countf = cursor.getInt(cursor.getColumnIndexOrThrow(WFSLAYERATTRIBUTES[14]));
					boolean sync = cursor.getInt(cursor.getColumnIndexOrThrow(WFSLAYERATTRIBUTES[15])) > 0;
					float size_mb = cursor.getFloat(cursor.getColumnIndexOrThrow(WFSLAYERATTRIBUTES[16]));
					boolean isLocked = cursor.getInt(cursor.getColumnIndexOrThrow(WFSLAYERATTRIBUTES[17])) > 0;
					String lockID = cursor.getString(cursor.getColumnIndexOrThrow(WFSLAYERATTRIBUTES[18]));
					long lockExpiry = cursor.getLong(cursor.getColumnIndexOrThrow(WFSLAYERATTRIBUTES[19]));
					long lockDate = cursor.getLong(cursor.getColumnIndexOrThrow(WFSLAYERATTRIBUTES[20]));
					long releaseDate = cursor.getLong(cursor.getColumnIndexOrThrow(WFSLAYERATTRIBUTES[21]));
					WFSLayer layer = new WFSLayer(context, layerID, name, projectID, url, 
							title, workspace, namespace, type, epsg, geom, isactive, ismulti, 
							size_mb, timestamp, color, countf, sync, isLocked, lockID, lockExpiry,
							lockDate, releaseDate);
					if (getAttributesOfWFS(layer)) {
						// Log.i(CLASSTAG + " getProjectsWFSFromDB", "All " + layer.getAttributes().size() + " Attributes imported from WFSLAYER " + name+ ".");
					} else {
						Log.i(CLASSTAG + " getProjectsWFSFromDB", "No Attributes found for WFSLayer " + name);
					}
					if (getFeaturesofWFSFromDB(layer)) {
						// Log.i(CLASSTAG + " getProjectsWFSFromDB", "All " + layer.getFeatureContainer().size() + " Features imported from WFSLAYER " + name + ".");
					} else {
						Log.i(CLASSTAG + " getProjectsWFSFromDB", "No Features found in WFSLayer " + name);
					}
					wfsContainer.add(layer);
				} while (cursor.moveToNext());
				cursor.close();
				close();
				// Log.i(CLASSTAG + " getProjectsWFSFromDB", "Overall " + wfsContainer.size() + " WFS-Layer successfully imported!");
				return wfsContainer;
			} else {
				cursor.close();
				close();
				// Log.i(CLASSTAG + " getProjectsWFSFromDB", "No WFS Layer contained in Project!");
				return new ArrayList<WFSLayer>();
			}
		} catch (CursorIndexOutOfBoundsException e) {
			close();
			Log.e(CLASSTAG + " getProjectsWFSFromDB", "CursorIndexOutOfBoundsException: " + e.getStackTrace().toString());
			return new ArrayList<WFSLayer>();
		}
	}

	/**
	 * Gets the featuresof wfs from db.
	 *
	 * @param layer the layer
	 * @return the featuresof wfs from db
	 */
	public boolean getFeaturesofWFSFromDB(WFSLayer layer) {
		// tempID ist keine FeatureID, sondern nur der Index im featureContainer (voruebergehend)
		//		int tempID = 0;
		try {
			Cursor cursor = getFeaturesOfLayer(layer.getLayerID());
			// TODO: Uebersetzungen
			toast = Toast.makeText(context, cursor.getCount() + " Features vom Layer " + layer.getName() + " werden geladen...", Toast.LENGTH_LONG);
			toast.show();
			if (cursor != null && cursor.getCount() > 0) {
				do {
					long featureID = cursor.getLong(cursor.getColumnIndexOrThrow(FEATUREATTRIBUTES[0]));
					long wfslayerID = cursor.getLong(cursor.getColumnIndexOrThrow(FEATUREATTRIBUTES[1]));
					String coordinates = cursor.getString(cursor.getColumnIndexOrThrow(FEATUREATTRIBUTES[2]));
					boolean sync = cursor.getInt(cursor.getColumnIndexOrThrow(FEATUREATTRIBUTES[3])) > 0;
					int featureType = cursor.getInt(cursor.getColumnIndexOrThrow(FEATUREATTRIBUTES[4]));
					String attributes = cursor.getString(cursor.getColumnIndexOrThrow(FEATUREATTRIBUTES[5]));
					String geoServerID = cursor.getString(cursor.getColumnIndexOrThrow(FEATUREATTRIBUTES[6]));
					boolean isDone = cursor.getInt(cursor.getColumnIndexOrThrow(FEATUREATTRIBUTES[7])) > 0;
					ContentValues values = convertStringToContentValues(attributes);
//					if (geoServerID.equals("ad_dim.174")) {
//						String x = tomsCVConverter(attributes);
//						Log.e(CLASSTAG, "Set CVs   : " + values.toString());
//					}
					Feature f = new Feature(context, featureID, wfslayerID, coordinates, featureType, 
							values, layer.getColor(), sync, layer.isActive(), geoServerID, isDone);
					layer.getFeatureContainer().add(f);
					layer.addToIndex(f);
					//					tempID++;
				} while (cursor.moveToNext());
				cursor.close();
				Feature feature; 
				for (int i = 0; i < layer.getFeatureContainer().size(); i++) {
					feature = layer.getFeatureContainer().get(i);
					if (feature.hasPrecision()) {
						feature.setPrecision(getPrecision(layer.getLayerID(), feature.getFeatureID()));
						Log.i(CLASSTAG + "getFeaturesofWFSFromDB", "Precision/Genauigkeit an Feature " + feature.getFeatureID() + " gehängt.");
					}
				}
				return true;
			} else {
				Log.i(CLASSTAG + " getFeaturesofWFSFromDB", "No Features found to import in Layer " + layer.getName());
				return false;
			}
		} catch (SQLException se) {
			se.getStackTrace();
			Log.i(CLASSTAG, " SQLException: " + se.getStackTrace().toString());
			return false;
		} catch (CursorIndexOutOfBoundsException e) {
			e.getStackTrace();
			Log.i(CLASSTAG, " CursorIndexOutOfBoundsException: " + e.getStackTrace().toString());
			return false;
		}
	}

	private String tomsCVConverter(String convertMe) {
		// TODO BUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUGGGGGGGGGGGGGG!!!!!!!!!!!!!!!
		Log.e(CLASSTAG + " TOMS", "String: " + convertMe);
		StringTokenizer token = new StringTokenizer(convertMe);
		ContentValues contentValues = new ContentValues();
		String key = "";
		String value = "";
		String temp = "";
		int lastSpace;
		boolean lastSignEquality = false;
		if (convertMe.lastIndexOf("=") == convertMe.length() - 1) {
			lastSignEquality = true;
		}
		Log.e(CLASSTAG, "convertMe.lastIndexOf(=): \"" + convertMe.lastIndexOf("=") + "\"");
		Log.e(CLASSTAG, "convertMe.length(): \"" + convertMe.length() + "\"");
		Log.e(CLASSTAG, "-> lastSignEquality: \"" + lastSignEquality + "\"");
		// das erste token bis "=" hat immer nur den key
		if (token.hasMoreTokens()) {
			// token bis zum nächsten "="
			key = token.nextToken("=");
			Log.e(CLASSTAG, "First Key: \"" + key + "\"");
			while (token.hasMoreTokens()) {
				// token bis zum nächsten "="
				temp = token.nextToken("=");
				Log.e(CLASSTAG, "Next Token (temp): \"" + temp + "\"");
				// wo beginnt der key? hinter dem letzten leerzeichen
				lastSpace = temp.lastIndexOf(" ");
				Log.e(CLASSTAG, "Last Space: " + lastSpace);
				// falls letztes token -> kein neuer key!
				if (token.hasMoreTokens()) {
					// wenn das letzte leerzeichen bei 0, dann kein wert eingetragen
					if (lastSpace > 0) {
						value = temp.substring(0, lastSpace);
						Log.e(CLASSTAG, "Value (if): \"" + value + "\"");
					} else if (lastSpace == -1) {
						value = temp;
						Log.e(CLASSTAG, "Value (else-if): \"" + value + "\"");
					} else {
						value = "";
						Log.e(CLASSTAG, "Value (else): \"" + value + "\"");
					}
					Log.e(CLASSTAG, "More Tokens. Entering Key: \"" + key + "\"" + " - Value: \"" + value + "\"");
					contentValues.put(key, value);
					key = temp.substring(lastSpace + 1, temp.length());	
					Log.e(CLASSTAG, "Next Key: \"" + key + "\"");
				} else {
					// wenn das letzte leerzeichen bei 0, dann kein wert eingetragen
					if (lastSignEquality) {
						value = temp.substring(0, lastSpace);
						Log.e(CLASSTAG, "No more Token. Value (if): \"" + value + "\"");
					} else {
						value = temp;
						Log.e(CLASSTAG, "No more Token. Value (else): \"" + value + "\"");
					}
					Log.e(CLASSTAG, "Last Token. Entering Key: \"" + key + "\"" + " - Value: \"" + value + "\"");
					contentValues.put(key, value);
					if (lastSignEquality) {
						Log.e(CLASSTAG, "Last letter was equality sign. Entering Key: \"" + temp.substring(lastSpace + 1, temp.length()) + "\"" + " - Value: \"\"");
						contentValues.put(temp.substring(lastSpace + 1, temp.length()), "");
					}
				}
			}
		}
		Log.e(CLASSTAG + " TOMS", "CVs: " + contentValues.toString());
		return contentValues.toString();
	}

	

	/**
	 * Convert String to ContentValues.
	 *
	 * @param convertMe the attributes
	 * @return the content values
	 */
	private ContentValues convertStringToContentValues(String convertMe) {
		StringTokenizer token = new StringTokenizer(convertMe);
		ContentValues contentValues = new ContentValues();
		String key = "";
		String value = "";
		String temp = "";
		int lastSpace;
		boolean lastSignEquality = false;
		if (convertMe.lastIndexOf("=") == convertMe.length() - 1) {
			lastSignEquality = true;
		}
		// das erste token bis "=" hat immer nur den key
		if (token.hasMoreTokens()) {
			// token bis zum nächsten "="
			key = token.nextToken("=");
			while (token.hasMoreTokens()) {
				// token bis zum nächsten "="
				temp = token.nextToken("=");
				// wo beginnt der key? hinter dem letzten leerzeichen
				lastSpace = temp.lastIndexOf(" ");
				// falls letztes token -> kein neuer key!
				if (token.hasMoreTokens()) {
					// wenn das letzte leerzeichen bei 0, dann kein wert eingetragen
					if (lastSpace > 0) {
						value = temp.substring(0, lastSpace);
					} else if (lastSpace == -1) {
						value = temp;
					} else {
						value = "";
					}
					contentValues.put(key, value);
					key = temp.substring(lastSpace + 1, temp.length());	
				} else {
					// wenn das letzte leerzeichen bei 0, dann kein wert eingetragen
					if (lastSignEquality) {
						value = temp.substring(0, lastSpace);
					} else {
						value = temp;
					}
					contentValues.put(key, value);
					if (lastSignEquality) {
						contentValues.put(temp.substring(lastSpace + 1, temp.length()), "");
					}
				}
			}
		}
		return contentValues;
	}

	// loads all attributes of a WFSlayer
	/**
	 * Gets the attributes of wfs.
	 *
	 * @param layer the layer
	 * @return the attributes of wfs
	 */
	private boolean getAttributesOfWFS(WFSLayer layer) {
		try {
			Cursor cursor = getAttributes(layer.getLayerID());
			if (cursor != null && cursor.getCount() > 0) {
				cursor.moveToFirst();
				do {
					long attributeID = cursor.getLong(cursor.getColumnIndexOrThrow(ALLATTRIBUTES[0]));
					String attributeName = cursor.getString(cursor.getColumnIndexOrThrow(ALLATTRIBUTES[2]));
					int typeID = cursor.getInt(cursor.getColumnIndexOrThrow(ALLATTRIBUTES[3]));
					layer.getAttributeTypes().add(new WFSLayerAttributeTypes(attributeID, 
							layer.getLayerID(), attributeName, typeID));
				} while (cursor.moveToNext());
				cursor.close();
				return true;
			} else {
				Log.i(CLASSTAG + " getAttributesOfWFS",	"No Attributes found to load for WFSLayer "	+ layer.getName());
				return false;
			}
		} catch (CursorIndexOutOfBoundsException e) {
			e.printStackTrace();
			Log.i(CLASSTAG, " CursorIndexOutOfBoundsException: " + e.getStackTrace().toString());
			return false;
		}
	}

	// Hier beginnen die WMS Methoden
	// ContentValues erzeugen fuer alle Felder des WFS
	/**
	 * Gets the content values of awms.
	 *
	 * @param layer the layer
	 * @return the content values of awms
	 */
	private ContentValues getContentValuesOfAWMS(WMSLayer layer) {
		ContentValues initialLayer = new ContentValues();
		// alle Felder bis auf den primaerschluessel
		initialLayer.put(WMSLAYERATTRIBUTES[1], layer.getProjectID());
		initialLayer.put(WMSLAYERATTRIBUTES[2], layer.getName());
		initialLayer.put(WMSLAYERATTRIBUTES[3], layer.getEPSG());
		initialLayer.put(WMSLAYERATTRIBUTES[4], layer.getWorkspace());
		initialLayer.put(WMSLAYERATTRIBUTES[5], layer.getURL());
		initialLayer.put(WMSLAYERATTRIBUTES[6], layer.getDescription());
		initialLayer.put(WMSLAYERATTRIBUTES[7], layer.getLegendURL());
		initialLayer.put(WMSLAYERATTRIBUTES[8], layer.getLogoURL());
		initialLayer.put(WMSLAYERATTRIBUTES[9], layer.getAttributionURL());
		initialLayer.put(WMSLAYERATTRIBUTES[10], layer.getAttributionTitle());
		initialLayer.put(WMSLAYERATTRIBUTES[11], layer.getAttributionLogoURL());
		initialLayer.put(WMSLAYERATTRIBUTES[12], layer.getBbox_maxX());
		initialLayer.put(WMSLAYERATTRIBUTES[13], layer.getBbox_maxY());
		initialLayer.put(WMSLAYERATTRIBUTES[14], layer.getBbox_minX());
		initialLayer.put(WMSLAYERATTRIBUTES[15], layer.getBbox_minY());
		// Log.i(CLASSTAG + " getContentValuesOfAWMS", "Inside ContentValues: " + initialLayer.toString());
		return initialLayer;
	}

	// insert wms into db
	/**
	 * Insert wms layer into db.
	 *
	 * @param layer the layer
	 * @return the long
	 */
	public long insertWMSLayerIntoDB(WMSLayer layer) {
		open();
		ContentValues initialWMS = getContentValuesOfAWMS(layer);
		long layerID = sqlDB.insert(SQLWMS.TABLE_NAME, null, initialWMS);
		close();
		return layerID;
	}

	// delete wms from db
	/**
	 * Delete wms layer from db.
	 *
	 * @param layerID the layer id
	 * @return true, if successful
	 */
	public boolean deleteWMSLayerFromDB(long layerID) {
		open();
		if (sqlDB.delete(SQLWMS.TABLE_NAME, SQLWMS.ID + "=" + layerID, null) > 0) {
			close();
			// Log.i(CLASSTAG + " deleteWMSLayerFromDB", "Deleted WMS-Layer " + layerID + " from DB.");
			return true;
		} else {
			close();
			Log.i(CLASSTAG + " deleteWMSLayerFromDB", "Could not delete WMS-Layer " + layerID + " from DB.");
			return false;
		}
	}

	// get all wms layer of a project
	/**
	 * Gets the WMS layers from db.
	 *
	 * @param projectID the project id
	 * @return the WMS layers from db
	 * @throws SQLException the SQL exception
	 */
	private Cursor getWMSLayersFromDB(long projectID) throws SQLException {
		Cursor cursor = sqlDB.query(true, SQLWMS.TABLE_NAME,
				WMSLAYERATTRIBUTES, SQLProjects.ID + "=" + projectID, null,
				null, null, null, null);
		if (cursor != null) {
			cursor.moveToFirst();
		}
		return cursor;
	}

	// update a wms layer of the project
	/**
	 * Update wms layer in db.
	 *
	 * @param layer the layer
	 * @return true, if successful
	 */
	public boolean updateWMSLayerInDB(WMSLayer layer) {
		open();
		ContentValues initialWMS = getContentValuesOfAWMS(layer);
		if (sqlDB.update(SQLWMS.TABLE_NAME, initialWMS, SQLWMS.ID + "=" + layer.getLayerID(), null) > 0) {
			close();
			return true;
		} else {
			close();
			return false;
		}
	}

	// gets all WMSlayer with attributes in the DB
	/**
	 * Gets the projects wms from db.
	 *
	 * @param newProjectID the new project id
	 * @return the projects wms from db
	 */
	public ArrayList<WMSLayer> getProjectsWMSFromDB(long newProjectID) {
		ArrayList<WMSLayer> wmsContainer = new ArrayList<WMSLayer>();
		try {
			open();
			Cursor cursor = getWMSLayersFromDB(newProjectID);
			// if no WMS is selected
			if (cursor != null && cursor.getCount() > 0) {
				do {
					long layerID = cursor.getLong(cursor.getColumnIndexOrThrow(WMSLAYERATTRIBUTES[0]));
					long projectID = cursor.getLong(cursor.getColumnIndex(WMSLAYERATTRIBUTES[1]));
					String name = cursor.getString(cursor.getColumnIndexOrThrow(WMSLAYERATTRIBUTES[2]));
					String epsg = cursor.getString(cursor.getColumnIndex(WMSLAYERATTRIBUTES[3]));
					String workspace = cursor.getString(cursor.getColumnIndex(WMSLAYERATTRIBUTES[4]));
					String url = cursor.getString(cursor.getColumnIndex(WMSLAYERATTRIBUTES[5]));
					String description = cursor.getString(cursor.getColumnIndex(WMSLAYERATTRIBUTES[6]));
					String legend_url = cursor.getString(cursor.getColumnIndex(WMSLAYERATTRIBUTES[7]));
					String logoURL = cursor.getString(cursor.getColumnIndex(WMSLAYERATTRIBUTES[8]));
					String attributionURL = cursor.getString(cursor.getColumnIndex(WMSLAYERATTRIBUTES[9]));
					String attribution_title = cursor.getString(cursor.getColumnIndex(WMSLAYERATTRIBUTES[10]));
					String attribution_logo_url = cursor.getString(cursor.getColumnIndex(WMSLAYERATTRIBUTES[11]));
					float bbox_maxX = cursor.getFloat(cursor.getColumnIndex(WMSLAYERATTRIBUTES[12]));
					float bbox_maxY = cursor.getFloat(cursor.getColumnIndex(WMSLAYERATTRIBUTES[13]));
					float bbox_minX = cursor.getFloat(cursor.getColumnIndex(WMSLAYERATTRIBUTES[14]));
					float bbox_minY = cursor.getFloat(cursor.getColumnIndex(WMSLAYERATTRIBUTES[15]));
					wmsContainer.add(new WMSLayer(context, layerID, name, projectID, epsg, 
							workspace, url, description, legend_url, logoURL, attributionURL,
							attribution_title, attribution_logo_url, bbox_maxX, bbox_maxY, 
							bbox_minX, bbox_minY));
					// Log.i(CLASSTAG + " getProjectsWMSFromDB", "WMS-Layer " + name + " imported into working memory");
				} while (cursor.moveToNext());
				close();
				// Log.i(CLASSTAG + " getProjectsWMSFromDB", "All " + wmsContainer.size() + " WMS-Layer successfully imported!");
				return wmsContainer;
			} else {
				close();
				// Log.i(CLASSTAG + " getProjectsWMSFromDB", "NO WMS in Project!");
				return new ArrayList<WMSLayer>();
			}
		} catch (CursorIndexOutOfBoundsException e) {
			close();
			Log.e(CLASSTAG + " getProjectsWMSFromDB", "CursorIndexOutOfBoundsException: " + e.getStackTrace().toString());
			return new ArrayList<WMSLayer>();
		}
	}

	// Hier beginnt Attribute_Table Methoden
	/**
	 * Insert attribute.
	 *
	 * @param lA the l a
	 * @param newLayerID the new layer id
	 * @return the long
	 */
	private long insertAttribute(WFSLayerAttributeTypes lA, long newLayerID) {
		ContentValues initialAttribute = getAttributeContentValues(lA, newLayerID);
		return sqlDB.insert(SQLAttribute.TABLE_NAME, null, initialAttribute);
	}

	/**
	 * Gets the attribute content values.
	 *
	 * @param lA the l a
	 * @param newLayerID the new layer id
	 * @return the attribute content values
	 */
	private ContentValues getAttributeContentValues(WFSLayerAttributeTypes lA, long newLayerID) {
		ContentValues initialAttribute = new ContentValues();
		// alle Felder bis auf den primaerschluessel
		initialAttribute.put(ALLATTRIBUTES[1], newLayerID);
		initialAttribute.put(ALLATTRIBUTES[2], lA.getName());
		initialAttribute.put(ALLATTRIBUTES[3], lA.getType());
		// Log.i(CLASSTAG + "  getAttributeContentValues", "Attribute: " + ALLATTRIBUTES[1] + " - Value: " + newLayerID);
		// Log.i(CLASSTAG + "  getAttributeContentValues", "Attribute: " + ALLATTRIBUTES[2] + " - Value: " + lA.getName());
		// Log.i(CLASSTAG + "  getAttributeContentValues", "Attribute: " + ALLATTRIBUTES[3] + " - Value: " + lA.getType());
		return initialAttribute;
	}

	/**
	 * Gets the attributes.
	 *
	 * @param wfsLayerID the wfs layer id
	 * @return the attributes
	 * @throws SQLException the SQL exception
	 */
	private Cursor getAttributes(long wfsLayerID) throws SQLException {
		// ohne das open stuerzt das sony hier ab! ?!?!?!?
		// open();
		Cursor cursor = sqlDB.query(true, SQLAttribute.TABLE_NAME,
				ALLATTRIBUTES, SQLWFS.ID + "=" + wfsLayerID, null, null, null,
				null, null);
		if (cursor != null) {
			cursor.moveToFirst();
			// Log.e(CLASSTAG + " getAttributes", "Cursor for Attributes of Layer " + wfsLayerID + " not empty");
		} else {
			// Log.e(CLASSTAG + " getAttributes", "Cursor for Attributes of Layer " + wfsLayerID + " empty");
		}
		return cursor;
	}

	/**
	 * Update attribute.
	 *
	 * @param lA the l a
	 * @param newLayerID the new layer id
	 * @return true, if successful
	 */
	private boolean updateAttribute(WFSLayerAttributeTypes lA, long newLayerID) {
		ContentValues initialAttribute = getAttributeContentValues(lA, newLayerID);
		return sqlDB.update(SQLAttribute.TABLE_NAME, initialAttribute,
				SQLAttribute.ID + "=" + lA.getAttributeID(), null) > 0;
	}

	/**
	 * Delete attribute.
	 *
	 * @param attributeID the attribute id
	 * @return true, if successful
	 */
	private boolean deleteAttribute(long attributeID) {
		return sqlDB.delete(SQLAttribute.TABLE_NAME, SQLAttribute.ID + "=" + attributeID, null) > 0;
	}

	/**
	 * Delete all attributes of wfs.
	 *
	 * @param wfsLayer the wfs layer
	 * @return true, if successful
	 */
	private boolean deleteAllAttributesOfWFS(WFSLayer wfsLayer) {
		boolean noError = true;
		for (WFSLayerAttributeTypes a : wfsLayer.getAttributeTypes()) {
			if (deleteAttribute(a.getAttributeID())) {
				// Log.i(CLASSTAG + " deleteAllAttributesOfWFS", "Deleted Attribute " + a.getName() + " of WFS-Layer " + wfsLayer.getName() + " from DB.");
			} else {
				Log.i(CLASSTAG + " deleteAllAttributesOfWFS", "Could not delete Attribute " + a.getName() + " of WFS-Layer " + wfsLayer.getName() + " from DB.");
				noError = false;
			}
		}
		return noError;
	}

	/**
	 * Gets the all attributes.
	 *
	 * @return the all attributes
	 */
	private Cursor getAllAttributes() {
		return sqlDB.query(SQLAttribute.TABLE_NAME, ALLATTRIBUTES, null, null,
				null, null, null);
	}

	/**
	 * Gets the attribute.
	 *
	 * @param attributeID the attribute id
	 * @return the attribute
	 * @throws SQLException the SQL exception
	 */
	private Cursor getAttribute(long attributeID) throws SQLException {
		Cursor cursor = sqlDB.query(true, SQLAttribute.TABLE_NAME, ALLATTRIBUTES, 
				SQLAttribute.ID + "=" + attributeID, null, null, null, null, null);
		if (cursor != null) {
			cursor.moveToFirst();
		}
		return cursor;
	}

	// Hier beginnt Feature Methoden
	/**
	 * Bulkinsert feature into db.
	 *
	 * @author Sven Weisker (uucly@student.kit.edu) schnelleres Einfuegen der
	 *         Featuredaten mit beginTransaction() und endTransaction()
	 * @param newFeature the new feature
	 * @return the long
	 */
	public long insertFeatureIntoDB(Feature newFeature) {
		ContentValues initialAttribute = getFeatureContentValues(newFeature);
		this.open();
		long newFeatureID = sqlDB.insert(SQLFeature.TABLE_NAME, null, initialAttribute);
		this.close();
		return newFeatureID;
	}

	/**
	 * Insert all feature into db and index.
	 *
	 * @param featureList the feature list
	 * @param layer the layer
	 * @return true, if successful
	 */
	public boolean insertAllFeatureIntoDBAndIndex(ArrayList<Feature> featureList, WFSLayer layer) {
		int id = 0;
		ContentValues initialAttribute;
		// Log.e(CLASSTAG + " insertAllFeatureIntoDBAndIndex", "Trying to add " + featureList.size() + " Features to Container of Layer " + layer.getName() + " with now " + layer.getFeatureContainer().size() + " Features.");
		this.open();
		// for every Feature
		if (featureList != null && featureList.size() > 0) {
			for (Feature newFeature : featureList) {
				// get the initial Attributes
				initialAttribute = getFeatureContentValues(newFeature);
				// set the feature ID and insert into DB
				newFeature.setFeatureID(sqlDB.insert(SQLFeature.TABLE_NAME, null, initialAttribute));
				// insert into working memory
				layer.getFeatureContainer().add(newFeature);
				// insert into index
				layer.addToIndex(newFeature);
				id++;
			}
			// Log.e(CLASSTAG + " insertAllFeatureIntoDBAndIndex", "Added " + id + " Features to Container of Layer " + layer.getName() + " with now " + layer.getFeatureContainer().size() + " Features.");
		}
		this.close();
		return true;
	}

	/**
	 * Gets the feature content values.
	 *
	 * @param newFeature the new feature
	 * @return the feature content values
	 */
	private ContentValues getFeatureContentValues(Feature newFeature) {
		ContentValues initialAttribute = new ContentValues();
		// alle Felder bis auf den primaerschluessel
		initialAttribute.put(FEATUREATTRIBUTES[1], newFeature.getWFSlayerID());
		initialAttribute.put(FEATUREATTRIBUTES[2], newFeature.getWKTGeometry());
		initialAttribute.put(FEATUREATTRIBUTES[3], newFeature.isSync());
		initialAttribute.put(FEATUREATTRIBUTES[4], newFeature.getFeatureType());
//		if (newFeature.getGeoServerID().equals("ad_dim.174")) {
//			Log.e(CLASSTAG, "CV from Feature: " + newFeature.getAttributes().toString());
//		}
		initialAttribute.put(FEATUREATTRIBUTES[5], newFeature.getAttributes().toString());
		initialAttribute.put(FEATUREATTRIBUTES[6], newFeature.getGeoServerID());
		initialAttribute.put(FEATUREATTRIBUTES[7], newFeature.isDone());
		// Log.e(CLASSTAG, "Attribute des Features: " + newFeature.getAttributes().toString());
		return initialAttribute;
	}

	/**
	 * Delete all features of a layer from db.
	 *
	 * @param wfsLayerID the wfs id
	 * @return true, if successful
	 */
	public boolean deleteAllFeaturesOfALayerFromDB(WFSLayer wfsLayer) {
		open();
		// TODO: hier alles zugehörigen Attribute etc. aus DB löschen!
		deleteAllPrecisionsOfLayer(wfsLayer);
		if (sqlDB.delete(SQLFeature.TABLE_NAME, SQLWFS.ID + "=" + wfsLayer.getLayerID(), null) > 0) {
			// Log.i(CLASSTAG + " deleteAllFeaturesOfALayerFromDB", "Deleted all Features of Layer " + wfsLayer.getName());
			close();
			return true;
		} else {
			Log.i(CLASSTAG + " deleteAllFeaturesOfALayerFromDB", "Could not delete any Features of Layer " + wfsLayer.getName());
			close();
			return false;
		}
	}

	/**
	 * Gets the all features.
	 *
	 * @return the all features
	 */
	private Cursor getAllFeatures() {
		return sqlDB.query(SQLFeature.TABLE_NAME, FEATUREATTRIBUTES, null,
				null, null, null, null);
	}

	/**
	 * Gets the feature.
	 *
	 * @param featureID the feature id
	 * @return the feature
	 * @throws SQLException the SQL exception
	 */
	private Cursor getFeature(long featureID) throws SQLException {
		Cursor c = sqlDB.query(true, SQLAttribute.TABLE_NAME, FEATUREATTRIBUTES, 
				SQLFeature.ID + "=" + featureID, null, null, null, null, null);
		if (c != null) {
			c.moveToFirst();
		}
		return c;
	}

	/**
	 * Gets the features.
	 *
	 * @param wfslayerID the wfslayer id
	 * @return the features
	 * @throws SQLException the SQL exception
	 */
	private Cursor getFeaturesOfLayer(long wfslayerID) throws SQLException {
		Cursor cursor = sqlDB.query(true, SQLFeature.TABLE_NAME, FEATUREATTRIBUTES, 
				SQLWFS.ID + "=" + wfslayerID, null, null, null, null, null);
		if (cursor != null) {
			cursor.moveToFirst();
		}
		return cursor;
	}

	/**
	 * Gets the features.
	 *
	 * @param idList the id list
	 * @return the features
	 */
	private Cursor[] getFeatures(String[] idList) {
		open();
		int maxCursorSize = 5000;
		int loop = (idList.length/5000) + 1;
		int position =0;
		Cursor[] cursorArray = new Cursor[loop];
		for(int i = 0; i < loop; i++){
			StringBuilder sql= new StringBuilder();
			sql.append(SQLFeature.ID + " in (");
			if (i == (loop - 1)) {
				sql.append(makePlaceholders(idList,position,idList.length));
			} else {
				sql.append(makePlaceholders(idList,position,maxCursorSize));
			}
			sql.append(")");
			Cursor cursor = sqlDB.query(SQLFeature.TABLE_NAME, SQLFeature.FEATUREATTRIBUTES, 
					sql.toString(), null, null, null, null);
			if (cursor != null) {
				cursor.moveToFirst();
			}
			cursorArray[i] = cursor;
		}
		close();
		return cursorArray;
	}

	/**
	 * Make placeholders.
	 *
	 * @param len the len
	 * @param position the position
	 * @param cursorSize the cursor size
	 * @return the string
	 */
	private String makePlaceholders(String[] len,int position,int cursorSize) {
		StringBuilder sb = new StringBuilder(cursorSize* 2 - 1);
		sb.append(len[0]);
		for (int i = 1; i < cursorSize; i++) {
			sb.append("," );
			sb.append(len[position]);
			position++;
		}
		return sb.toString();
	}

	/**
	 * Update feature.
	 *
	 * @param newFeature the new feature
	 * @return true, if successful
	 */
	public boolean updateFeatureInDB(Feature newFeature) {
		open();
		ContentValues initialAttribute = getFeatureContentValues(newFeature);
		// Log.e(CLASSTAG, "Update Feature " + newFeature.getFeatureID() + "with CVs: " + initialAttribute);
		if (sqlDB.update(SQLFeature.TABLE_NAME, initialAttribute, SQLFeature.ID
				+ "=" + newFeature.getFeatureID(), null) > 0) {
			close();
			return true;
		}
		close();
		return false;
	}

	// Precision-Methoden
	/**
	 * Gets the precision.
	 *
	 * @param layerId the layer id
	 * @param featureId the feature id
	 * @return the precision
	 */
	private FeaturePrecision getPrecision(long layerId, long featureId) {	
		Cursor c = sqlDB.query(true, SQLPrecision.TABLE_NAME, SQLPrecision.PRECISIONATTRIBUTES, 
				SQLPrecision.LAYERID + "=? AND "+ SQLPrecision.FEATUREID + "=?", 
				new String[]{String.valueOf(layerId), String.valueOf(featureId)}, null, null, null, null);
		FeaturePrecision precision = null;
		if (c.moveToFirst()) {
			Log.d(CLASSTAG,"Informationen zur Genauigkeit gefunden! -> Lade...");
			if (c.getCount() > 1) {
				Log.e(CLASSTAG, "Zu viele Einträge gefunden!");
			}
			String vAccuracy = c.getString(c.getColumnIndex(SQLPrecision.ACCURACY));
			String vSatCount = c.getString(c.getColumnIndex(SQLPrecision.SATCOUNT));
			String vAzimuth = c.getString(c.getColumnIndex(SQLPrecision.AZIMUTH));
			String vElevation = c.getString(c.getColumnIndex(SQLPrecision.ELEVATION));
			HashMap<String, String> values = new HashMap<String, String>();
			values.put(SQLPrecision.ACCURACY, vAccuracy);
			values.put(SQLPrecision.SATCOUNT, vSatCount);
			values.put(SQLPrecision.AZIMUTH, vAzimuth);
			values.put(SQLPrecision.ELEVATION, vElevation);
			values.put(SQLPrecision.LAYERID, String.valueOf(layerId));
			values.put(SQLPrecision.FEATUREID, String.valueOf(featureId));
			precision = new FeaturePrecision(values);
		}
		c.close();
		return precision;
	}

	/**
	 * Insert precision in db.
	 *
	 * @param values the values
	 * @return the int
	 */
	public long insertPrecisionInDB(HashMap<String, String> values) {
		open();
		Log.d(CLASSTAG, "Speichere Informationen zur Genauigkeit mit dem Schlüssel \n\t" 
				+ "Feature-ID: " + values.get(SQLPrecision.FEATUREID) + "\n\t"
				+ "Layer-ID: " + values.get(SQLPrecision.LAYERID) + "\n\t"
				+ "in die Datenbank");
		ContentValues initialAttribute = getPrecisionContentValues(values);
		long id = sqlDB.insert(SQLPrecision.TABLE_NAME, null, initialAttribute);
		close();
		return id;
	}

	/**
	 * Gets the precision content values.
	 *
	 * @param values the values
	 * @return the precision content values
	 */
	private ContentValues getPrecisionContentValues(HashMap<String, String> values) {
		ContentValues initialAttribute = new ContentValues();
		// alle Felder bis auf den primaerschluessel
		for (int i = 1; i < SQLPrecision.PRECISIONATTRIBUTES.length; i++) {
			initialAttribute.put(SQLPrecision.PRECISIONATTRIBUTES[i], values.get(SQLPrecision.PRECISIONATTRIBUTES[i]));
		}
		return initialAttribute;
	}

	/**
	 * Delete precision.
	 *
	 * @param precisionID the precision id
	 * @return true, if successful
	 */
	private boolean deleteAllPrecisionsOfFeature(long featureID) {
		return sqlDB.delete(SQLPrecision.TABLE_NAME, SQLPrecision.FEATUREID + "=" 
				+ String.valueOf(featureID), null) > 0;
	}

	/**
	 * Deletes all precision of all features of a wfs-layer from db.
	 *
	 * @param wfsLayer the wfs layer
	 * @return true, if successful
	 */
	private boolean deleteAllPrecisionsOfLayer(WFSLayer wfsLayer) {
		if (sqlDB.delete(SQLPrecision.TABLE_NAME, SQLPrecision.LAYERID + "=" 
				+ String.valueOf(wfsLayer.getLayerID()), null) > 0) {
			// Log.i(CLASSTAG, "All Precisions of WFS-Layer " + wfsLayer.getName() + " deleted.");
		} else {
			Log.i(CLASSTAG, "No Precisions of WFS-Layer " + wfsLayer.getName() + " found or deleted.");
		}
		return true;
	}

	// TODO: Hier beginnen Restriction Methoden
	/**
	 * Insert restriction.
	 *
	 * @param layerID the layer id
	 * @param name the name
	 * @param type the type
	 * @param att the att
	 * @param value the value
	 * @return the long
	 */
	private long insertRestriction(long layerID, String name, int type,
			String att, String value) {
		ContentValues initialRestriction = getRestrictionContentValues(layerID,
				name, type, att, value);
		return sqlDB.insert(SQLRestrictions.TABLE_NAME, null,
				initialRestriction);
	}

	/**
	 * Gets the restriction content values.
	 *
	 * @param layerID the layer id
	 * @param name the name
	 * @param type the type
	 * @param att the att
	 * @param value the value
	 * @return the restriction content values
	 */
	private ContentValues getRestrictionContentValues(long layerID,
			String name, int type, String att, String value) {
		ContentValues initialAttribute = new ContentValues();
		// alle Felder bis auf den primaerschluessel
		initialAttribute.put(FEATUREATTRIBUTES[1], layerID);
		initialAttribute.put(FEATUREATTRIBUTES[2], name);
		initialAttribute.put(FEATUREATTRIBUTES[3], type);
		initialAttribute.put(FEATUREATTRIBUTES[4], att);
		initialAttribute.put(FEATUREATTRIBUTES[5], value);
		return initialAttribute;
	}

	/**
	 * Delete restriction.
	 *
	 * @param restrictionID the restriction id
	 * @return true, if successful
	 */
	private boolean deleteRestriction(long restrictionID) {
		return sqlDB.delete(SQLRestrictions.TABLE_NAME, SQLRestrictions.ID
				+ "=" + restrictionID, null) > 0;
	}

	/**
	 * Gets the all restriction.
	 *
	 * @return the all restriction
	 */
	private Cursor getAllRestriction() {
		return sqlDB.query(SQLRestrictions.TABLE_NAME, FEATUREATTRIBUTES, null,
				null, null, null, null);
	}

	/**
	 * Gets the restriction.
	 *
	 * @param restrictionID the restriction id
	 * @return the restriction
	 * @throws SQLException the SQL exception
	 */
	private Cursor getRestriction(long restrictionID) throws SQLException {
		Cursor cursor = sqlDB.query(true, SQLRestrictions.TABLE_NAME,
				FEATUREATTRIBUTES, SQLRestrictions.ID + "=" + restrictionID,
				null, null, null, null, null);
		if (cursor != null) {
			cursor.moveToFirst();
		}
		return cursor;
	}

	/**
	 * Update restriction.
	 *
	 * @param restrictionID the restriction id
	 * @param layerID the layer id
	 * @param name the name
	 * @param type the type
	 * @param att the att
	 * @param value the value
	 * @return true, if successful
	 */
	private boolean updateRestriction(long restrictionID, long layerID,
			String name, int type, String att, String value) {
		ContentValues initialRestriction = getRestrictionContentValues(layerID,
				name, type, att, value);
		return sqlDB.update(SQLRestrictions.TABLE_NAME, initialRestriction,
				SQLRestrictions.ID + "=" + restrictionID, null) > 0;
	}


	// Satellite Methoden
	/**
	 * Insert satellit.
	 *
	 * @param layerID the layer id
	 * @param elementID the element id
	 * @param elevation the elevation
	 * @param azimut the azimut
	 * @param signal2noise the signal2noise
	 * @param hasEphemeries the has ephemeries
	 * @param hasAlmanach the has almanach
	 * @return the long
	 */
	public long insertSatellit(long layerID, int elementID, double elevation, double azimut,
			double signal2noise, int hasEphemeries, int hasAlmanach) {
		open();
		ContentValues initialSatellite = new ContentValues();
		initialSatellite.put(SQLWFS.ID, layerID);
		initialSatellite.put(SQLSatellite.ELEMENTID, elementID);
		initialSatellite.put(SQLSatellite.ELEVATION, elevation);
		initialSatellite.put(SQLSatellite.AZIMUTH, azimut);
		initialSatellite.put(SQLSatellite.SIGNAL2NOISE, signal2noise);
		initialSatellite.put(SQLSatellite.EPHEMERIES, hasEphemeries);
		initialSatellite.put(SQLSatellite.ALMANACH, hasAlmanach);
		long newSatelliteID = sqlDB.insert(SQLSatellite.TABLE_NAME, null, initialSatellite);
		close();
		return newSatelliteID;
	}

	/**
	 * Gets the satellite.
	 *
	 * @param satelliteID the satellite id
	 * @return the satellite
	 * @throws SQLException the SQL exception
	 */
	public Cursor getSatellite(long satelliteID) throws SQLException {
		Cursor c = sqlDB.query(true, SQLSatellite.TABLE_NAME, new String[] {
				SQLSatellite.ID, SQLWFS.ID, SQLSatellite.ELEMENTID, SQLSatellite.ELEVATION, 
				SQLSatellite.AZIMUTH, SQLSatellite.SIGNAL2NOISE, SQLSatellite.EPHEMERIES,
				SQLSatellite.ALMANACH }, SQLSatellite.ID + "=" + satelliteID,
				null, null, null, null, null);
		if (c != null) {
			c.moveToFirst();
		}
		return c;
	}

	/**
	 * Delete satellite.
	 *
	 * @param satelliteID the satellite id
	 * @return true, if successful
	 */
	private boolean deleteSatellite(long satelliteID) {
		return sqlDB.delete(SQLSatellite.TABLE_NAME, SQLSatellite.ID + "="
				+ satelliteID, null) > 0;
	}

	/**
	 * Gets the all satellite.
	 *
	 * @return the all satellite
	 */
	private Cursor getAllSatellite() {
		return sqlDB.query(SQLSatellite.TABLE_NAME, new String[] {
				SQLSatellite.ID, SQLWFS.ID, SQLSatellite.ELEMENTID, SQLSatellite.ELEVATION, 
				SQLSatellite.AZIMUTH, SQLSatellite.SIGNAL2NOISE, SQLSatellite.EPHEMERIES,
				SQLSatellite.ALMANACH }, null, null, null, null, null);
	}

	/**
	 * Update satellite.
	 *
	 * @param satelliteID the satellite id
	 * @param layerID the layer id
	 * @param elementID the element id
	 * @param elevation the elevation
	 * @param azimut the azimut
	 * @param signal2noise the signal2noise
	 * @param hasEphemeries the has ephemeries
	 * @param hasAlmanach the has almanach
	 * @return true, if successful
	 */
	private boolean updateSatellite(long satelliteID, long layerID,
			int elementID, double elevation, double azimut,
			double signal2noise, int hasEphemeries, int hasAlmanach) {
		ContentValues initialSatellite = new ContentValues();
		initialSatellite.put(SQLWFS.ID, layerID);
		initialSatellite.put(SQLSatellite.ELEMENTID, elementID);
		initialSatellite.put(SQLSatellite.ELEVATION, elevation);
		initialSatellite.put(SQLSatellite.AZIMUTH, azimut);
		initialSatellite.put(SQLSatellite.SIGNAL2NOISE, signal2noise);
		initialSatellite.put(SQLSatellite.EPHEMERIES, hasEphemeries);
		initialSatellite.put(SQLSatellite.ALMANACH, hasAlmanach);
		return sqlDB.update(SQLSatellite.TABLE_NAME, initialSatellite,
				SQLSatellite.ID + "=" + satelliteID, null) > 0;
	}

	/**
	 * The Class DataBaseHelper. Executes Code on SQL DB. Creates, deletes and
	 * updates the SQL DB.
	 */
	private static class DataBaseHelper extends SQLiteOpenHelper {
		// the classtag
		private final static String CLASSTAG = "DataBaseHelper in DBAdapter";

		/**
		 * Instantiates a new data base helper.
		 *
		 * @param newContext the new context
		 */
		public DataBaseHelper(Context newContext, String dbName, int dbVersion) {
			super(newContext, dbName, null, dbVersion);
			// Log.e(CLASSTAG, "Initialized. Version: " + dbVersion);
		}

		@Override
		public void onCreate(SQLiteDatabase newDB) {
			Log.e(CLASSTAG, "onCreate");
			try {
				createTables(newDB);
			} catch (SQLException e) {
				Log.i(CLASSTAG,	"SQL-Exception while creating Tables: "	+ e.getMessage());
			}
		}

		// falls DB-Version geupgraded wurde
		@Override
		public void onUpgrade(SQLiteDatabase newDB, int oldVersion, int newVersion) {
			Log.e(CLASSTAG, "onUpgrade, oldversion: " + oldVersion + ", newVersion: " + newVersion);
			switch (newVersion) {
			case DBVERSION:
				switch (oldVersion) {
				// kein break, daher wird nach dem treffer ALLES ausgefuerhrt!
				case 1:
					upgradeFrom1To2(newDB);
				case 2:
					upgradeFrom2To3(newDB);
				}
				break;
				// dropTables(newDB);
				// this.onCreate(newDB);
				// DBVERSION = newVersion;
			}
		}

		/**
		 * Upgrade from 1 to2.
		 *
		 * @param newDB the new db
		 */
		private void upgradeFrom1To2(SQLiteDatabase newDB) {
			// BSP:
			// newDB.execSQL("alter table tablename add column columnname integer default 0");
			// ... von version 1 bis version 2
			// Log.e(CLASSTAG + " upgradeFrom1To2", "UPGRADING TABLE FEATURE: alter table features add column geoserver_id text default 0");
			newDB.execSQL("alter table features add column geoserver_id text default 0");
		}

		/**
		 * Upgrade from 2 to3.
		 *
		 * @param newDB the new db
		 */
		private void upgradeFrom2To3(SQLiteDatabase newDB) {
			// newDB.execSQL("alter table tablename add column columnname2 integer default 0");
			// von Version 2 bis 3 .....
			// Log.e(CLASSTAG + " upgradeFrom2To3", "Is always called, because any breaks are missing in the switch/case statement");
			newDB.execSQL("alter table features add column feature_is_done boolean default false");
		}

		/**
		 * Creates the tables.
		 *
		 * @param newDB the new db
		 */
		private void createTables(SQLiteDatabase newDB) {
			// for every table name in String
			for (int i = 0; i < ALLCREATETABLES.length; i++) {
				newDB.execSQL(ALLCREATETABLES[i]);
				Log.e(CLASSTAG + " createTables", "Created Table: " + ALLTABLES[i]);
			}
		}

		@Override
		public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// nothing
			Log.e(CLASSTAG + " onDowngrade", "Downgrading from Version " + oldVersion + " to " + newVersion + " - by doing nothing.");
		}


		/**
		 * Drop tables.
		 *
		 * @param newDB the new db
		 */
		private void dropTables(SQLiteDatabase newDB) {
			// for every table name in String
			for (int i = 0; i < ALLTABLES.length; i++) {
				newDB.execSQL("DROP TABLE IF EXISTS " + ALLTABLES[i]);
				Log.i(CLASSTAG + " dropTables", "Dropped Table: " + ALLTABLES[i]);
			}
		}
	}

}
