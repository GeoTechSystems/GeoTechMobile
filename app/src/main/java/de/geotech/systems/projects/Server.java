/**
 * The Class Server.
 * 
 * @author Torsten Hoch - tohoch@uos.de
 */

package de.geotech.systems.projects;

public class Server {
	private long dbID;
	private String serverName;
	private String revisedURL;
	private String url;
	private String username;
	private String password;
	private boolean authenticate;

	/**
	 * Instantiates a new server.
	 *
	 * @param url the url
	 */
	public Server(String url) {
		this(url, "", "", "");
	}

	/**
	 * Instantiates a new server.
	 *
	 * @param url the url
	 * @param serverName the name
	 */
	public Server(String url, String serverName) {
		this(url, "", "", serverName);
	}

	/**
	 * Instantiates a new server.
	 *
	 * @param url the url
	 * @param username the username
	 * @param password the password
	 */
	public Server(String url, String username, String password) {
		this(url, username, password, "");
	}

	/**
	 * Instantiates a new server.
	 * Attribut Name wird bisher nicht genutzt, d.h. es wird nicht in die DB 
	 * geschrieben oder sonstwas
	 * 
	 * @param url the url
	 * @param username the username
	 * @param password the password
	 * @param serverName the name
	 */
	public Server(String url, String username, String password, String serverName) {
		this.serverName = serverName;
		this.url = url;
		this.revisedURL = this.reviseUrl(url);
		if (username != null && !username.equals("")) {
			this.username = username;
			this.password = password;
			this.authenticate = true;
		} else {
			this.username = "";
			this.password = "";
			this.authenticate = false;
		}
	}

	/**
	 * Checks if is authenticate.
	 *
	 * @return true, if is authenticate
	 */
	public boolean isAuthenticate() {
		if (!authenticate) {
			if (!username.equals("")) {
				authenticate = true;
			}
		}
		return authenticate;
	}

	/**
	 * Sets the authenticate.
	 *
	 * @param authenticate the new authenticate
	 */
	public void setAuthenticate(boolean authenticate) {
		this.authenticate = authenticate;
	}

	/**
	 * Gets the password.
	 *
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Sets the password.
	 *
	 * @param password the new password
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Gets the revised url.
	 *
	 * @return the revised url
	 */
	public String getRevisedURL() {
		return revisedURL;
	}

	/**
	 * Gets the url.
	 *
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Gets the username.
	 *
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Sets the username.
	 *
	 * @param username the new username
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Gets the name.
	 * 
	 * @return the name
	 */
	public String getName() {
		return serverName;
	}

	/**
	 * Sets the name.
	 * 
	 * @param name the new name
	 */
	public void setName(String name) {
		this.serverName = name;
	}

	/**
	 * Revise url.
	 *
	 * @param url the url
	 * @return the revised url
	 */
	private String reviseUrl(String url) {
		url = url.trim();
		if (!url.startsWith("http")) {
			url = "http://" + url;
		}
		return url;
	}
	
	public long getDBID() {
		return dbID;
	}

	public void setDBID(long newDBID) {
		this.dbID = newDBID;
	}
	
}
