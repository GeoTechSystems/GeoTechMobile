/**
 * NEU:
 * Klasse liest daten vom server und initialisiert die tabelle
 * 
 * Handles the parsing of the xml data from the 
 *         getCapabilitie-request
 *
 * @author Torsten Hoch
 */

package de.geotech.systems.layerTables;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TableRow;

import de.geotech.systems.R;
import de.geotech.systems.projects.Server;

public class LayerTableAsyncTask extends AsyncTask<String, TableRow, Boolean> {
	private static final String CLASSTAG = "LayerTableAsyncTask";

	// dialog showing the actual progress
	private ProgressDialog progress;
	// the context
	private Context context;
	// contenthandler for wms or wfs
	private ContentHandler handler;
	// tabelle
	private LayerTable layertable;
	// the error code
	private int errorCode = 0;
	// the server object
	private Server server;

	// constructor
	public LayerTableAsyncTask(LayerTable layerTable, ContentHandler handler, Server newServer) { 
		this.context = layerTable.getTable().getContext();
		this.handler = handler;
		this.layertable = layerTable;
		this.server = newServer;
	}

	// before execution
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		// fortschrittsdialog anzeigen
		this.progress = new ProgressDialog(this.context);
		this.progress.setProgress(0);
		this.progress.setTitle(R.string.buildTableTask_connecting);
		this.progress.setMessage(this.context.getString(R.string.buildTableTask_waiting));
		this.progress.show();
		this.progress.setCancelable(true);
		this.progress.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				cancel(true);
			}
		});
	}

	// parsen in background, after been executed
	@Override
	protected Boolean doInBackground(String... params) {
		// variable, um validen vorgang zu pruefen
		boolean isValid = true;
		// solange uebergebene url nicht leer
		if (params[0] != null) {
			// url wieder zusammenbauen
			String paramsURL = params[0];
			try {
				// URL-Objekt bauen aus dem in execute(...) uebrgebenen string
				URL url = new URL(paramsURL);
				Log.e(CLASSTAG, "URL ist beim senden: " + url.toString());
				// alles fuer das parsen vorbereiten
				SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
				SAXParser saxParser = saxParserFactory.newSAXParser();
				XMLReader xmlReader = saxParser.getXMLReader();
				// mit dem uebergebenen handler parsen
				xmlReader.setContentHandler(this.handler);
				InputStreamReader xmlStream;
				/// wenn username gesetzt authenticated abfrage
				if (server.getUsername() != null && !server.getUsername().equals("")) {
					Authenticator.setDefault(new Authenticator(){
						protected PasswordAuthentication getPasswordAuthentication() {
							return new PasswordAuthentication(server.getUsername(), server.getPassword().toCharArray());
						}});
					HttpURLConnection c = (HttpURLConnection) new URL(url.toString()).openConnection();
					c.setUseCaches(false);
					c.setConnectTimeout(5000);
					c.connect();
					xmlStream = new InputStreamReader(c.getInputStream());
				} else {
					// sonst nur normale abfrage
					URLConnection c = url.openConnection();
					c.setConnectTimeout(5000);
					xmlStream = new InputStreamReader(c.getInputStream());
				}
				BufferedReader buffered = new BufferedReader(xmlStream);
				xmlReader.parse(new InputSource(buffered));
				// TODO: FEHLER NOCH KORREKTER ABFANGEN, BITTE!! MIT AUSGABE!!!
			} catch (MalformedURLException e) {
				isValid = false;
				errorCode = 1;
				e.printStackTrace();
			} catch (IOException e) {
				isValid = false;
				errorCode = 2;
				e.printStackTrace();
			} catch (SAXException e) {
				isValid = false;
				errorCode = 0;
				e.printStackTrace();
			} catch (ParserConfigurationException e) {
				isValid = false;
				errorCode = 0;
				e.printStackTrace();
			} catch (RuntimeException re) {
				isValid = false;
				errorCode = 0;
				re.printStackTrace();
			}
			Log.i(CLASSTAG + "doInBackground", "Parsing successful!");
			return isValid;
		} else {
			isValid = false;
			Log.e(CLASSTAG + "doInBackground", "No URL in params!");
			return isValid;
		}
	}

	@Override
	protected void onPostExecute(Boolean result) {
		super.onPostExecute(result);
		if (result) {
			this.layertable.activateLayerTableBuilding(this.handler);
			// fortschrittsdialog beenden
			this.progress.cancel();
		} else {
			// TODO Uebersetzungen
			switch (errorCode) {
			case 0:
				this.progress.setTitle("Connection Error");
				this.progress.setMessage("Possible causes: \n"
						+ " - No existing WFS/WMS-Server at given Server-Adress \n" 
						+ " - No Internet Connection could be established. \n"
						+ "If the current URL is correct please check if your internet connection established.");
				break;
			case 1:
				this.progress.setTitle("URL Error");
				this.progress.setMessage("The inserted URL is malformed.\n Please reenter the Server details.");
				break;
			case 2:
				this.progress.setTitle("Connection Error");
				this.progress.setMessage("Possible causes: \n"
						+ " - No existing WFS/WMS-Server at given Server-Adress \n" 
						+ " - The WFS/WMS-Server is unavailable \n"
						+ " - The WFS/WMS-Server is password protected \n"
						+ " - Incorrect username or password at login");
				break;
			}
			// TODO Uebersetzungen etc.
			// bzw. Servertest hier einbauen!!!
			this.progress.setCanceledOnTouchOutside(true);
		}
	}

}
