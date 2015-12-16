/**
 * Diese Klasse soll den Umgang mit den Projekten handeln
 * 
 * @author svenweisker
 * @author Torsten Hoch
 */

package de.geotech.systems.projects;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.Log;

import de.geotech.systems.LGLSpecial.LGLValues;
import de.geotech.systems.LGLSpecial.OAKAttributeHandler;
import de.geotech.systems.database.DBAdapter;
import de.geotech.systems.database.SQLServer;
import de.geotech.systems.wfs.WFSLayer;
import de.geotech.systems.wms.WMSLayer;

public class ProjectHandler {
	private static final String CLASSTAG = "ProjectHandler";
	// Speichert die Projekte
	private static List<Project> projects = new ArrayList<Project>();
	// Speichert die Server
	private static ArrayList<Server> servers = new ArrayList<Server>();
	// das aktuelle Projekt, auf das mit einem getter von ueberall 
	// zugegriffen werden kann
	private static Project currentProject = null;
	// the attribute handler for LGL
	private static OAKAttributeHandler oakAttributeHandler;
	
	public static OAKAttributeHandler getOakAttributeHandler() {
		return oakAttributeHandler;
	}

	// adds a project
	public static boolean addProject(Context context, String name,
			String description, int epsg) {
		for (int i = 0; i < projects.size(); i++)
			if (projects.get(i).getProjectName().equals(name)) {
				return false;
			}
		Project project = new Project(context, name, epsg, description);
		// Project in RAM und in SQLschreiben
		if (project.writeProjectIntoDatabase() && projects.add(project)) {
			return true;
		} else {
			return false;
		}
	}

	// Läd die komplette Projektlist aus sql in den Arbeitsspeicher
	public static void loadProjectList(Context context) {
		projects.clear();
		DBAdapter db = new DBAdapter(context);
		projects = db.getProjectsFromDB();
		servers.clear();
		setServers(db.getCompleteServersFromDB());
		if (servers == null || servers.size() < 1) {
			initializeServers(context);
		}
		oakAttributeHandler = new OAKAttributeHandler(context, LGLValues.getRawCodesForFiles(), LGLValues.getLayerNamesForFilesArrayList());
		oakAttributeHandler.readFilesAndInitialize(LGLValues.SPLITSTRING, LGLValues.KNOWN_COLUMNS);
	}

	public static void initializeServers(Context context) {
		DBAdapter db = new DBAdapter(context);
		// server neu initialisieren
		servers.clear();
		db.initializeStandardServersInDB();
		setServers(db.getCompleteServersFromDB());
	}

	// Setzt das aktuell geladene project
	public static boolean setCurrentProject(String name) {
		for (int i = 0; i < projects.size(); i++) {
			if (projects.get(i).getProjectName().equals(name))	{
				currentProject = projects.get(i);
				currentProject.loadProjectsWMSFromDB(currentProject.getProjectID());
				currentProject.loadProjectsWFSFromDB(currentProject.getProjectID());
				// abbruch, da gefunden
				return true;
			}
		}
		return false;
	}

	// Löscht ein Project dem arbeitsspeicher
	public static boolean deleteProject(String name) {
		if (currentProject != null)
			if (currentProject.getProjectName().equals(name)) {
				currentProject = null;
			}
		for (int i = 0; i < projects.size(); i++)
			if (projects.get(i).getProjectName().equals(name)) {
				if (projects.get(i).deleteProject()) {
					Log.i(CLASSTAG, "Project " + name + " has been deleted.");
					projects.remove(i);
					return true;	
				} else {
					Log.i(CLASSTAG, "Project " + name + " couldn't be deleted.");
					return false;
				}
			}
		Log.i(CLASSTAG, "Project " + name + " couldn't be found and was not deleted.");
		return false;
	}

	// get the current project - should be null, if not set
	public static Project getCurrentProject() {
		return currentProject;
	}
	
	// get the current project - should be null, if not set
	public static List<Project> getAllProjects() {
		return projects;
	}

	// is the current project set?
	public static boolean isCurrentProjectSet() {
		return (!(currentProject == null));
	}
	
	/**
	 * Gets a Server-Object by URL.
	 *
	 * @param url the url
	 * @return the server
	 */
	public static Server getServer(String url) {
		for (Server s : servers) {
			if (s.getUrl().equals(url)) {
				return s;
			}
		}
		return servers.get(0);
	}

	/**
	 * Gets the server-list.
	 *
	 * @return the servers
	 */
	public static ArrayList<Server> getServers() {
		return servers;
	}

	/**
	 * Sets the server-list.
	 *
	 * @param servers the new servers
	 */
	private static void setServers(ArrayList<Server> servers) {
		ProjectHandler.servers = servers;
	}

	/**
	 * Adds a server to RAM and DB.
	 *
	 * @param context the context
	 * @param newServer the new server
	 */
	public static void addServer(Context context, Server newServer) {
		DBAdapter dbAdapter = new DBAdapter(context);
		// add server address to list + file of wms or wfs list + file
		dbAdapter.insertServerIntoDB(newServer);
		servers.add(newServer);
	}
	
	/**
	 * Deletes a server from RAM and DB.
	 *
	 * @param context the context
	 * @param newServer the new server
	 */
	public static void deleteServer(Context context, Server newServer) {
		DBAdapter dbAdapter = new DBAdapter(context);
		// add server address to list + file of wms or wfs list + file
		dbAdapter.deleteServerFromDB(newServer.getUrl());
		servers.remove(newServer);
	}

	/**
	 * Gets the project.
	 *
	 * @param projectID the project id
	 * @return the project
	 */
	public static Project getProject(long projectID) {
		for (int i = 0; i < projects.size(); i++) {
			if (projects.get(i).getProjectID() == projectID)	{
				// abbruch, da gefunden
				return projects.get(i);
			}
		}
		return null;
	}
}
