/**
 * Tests connection to server
 *  
 * @author Torsten Hoch
 * @author tubatubsen
 */

package de.geotech.systems.connectionTest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import de.geotech.systems.R;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;

public class TestConnectionTask extends AsyncTask<String, Integer, Boolean> {
	private ProgressDialog progress;
	private ServerResponseHandler handler;
	private String exception;
	private Context context;
	private String testURL;

	public TestConnectionTask(Context context) {
		this.context = context;
	}

	@Override
	protected void onPreExecute() {
		progress = new ProgressDialog(context);
		progress.setProgress(0);
		progress.setTitle(R.string.testconnectiontask_dialog_title);
		progress.setMessage(context.getString(R.string.testconnectiontask_dialog_message));
		progress.show();
		handler = new ServerResponseHandler();
		exception = null;
	}

	@Override
	protected Boolean doInBackground(String... params) {
		boolean isValid = true;
		try {
			// Setting url and parser
			String addressToTest = (params[0]);
			addressToTest += "?REQUEST=getCapabilities";
			testURL = addressToTest;
			URL url = new URL(addressToTest);
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser parser = spf.newSAXParser();
			XMLReader reader = parser.getXMLReader();
			// Setting handler and parsing
			reader.setContentHandler(handler);
			reader.parse(new InputSource(url.openStream()));
		} catch (MalformedURLException e) {
			isValid = false;
			exception = e.getLocalizedMessage()
					+ context.getString(R.string.testconnectiontask_MalformedURLException);
		} catch (ParserConfigurationException e) {
			isValid = false;
			exception = e.getLocalizedMessage()
					+ context.getString(R.string.testconnectiontask_ParserConfigurationException);
		} catch (SAXException e) {
			isValid = false;
			exception = e.getLocalizedMessage()
					+ context.getString(R.string.testconnectiontask_SAXException);
		} catch (IOException e) {
			isValid = false;
			exception = context.getString(R.string.testconnectiontask_IOException)
					+ e.getLocalizedMessage();
		} catch (Exception e) {
			isValid = false;
			exception = e.getLocalizedMessage();
		}

		if (isValid)
			isValid = handler.isValidServer();
		return isValid;
	}

	@Override
	protected void onPostExecute(Boolean result) {
		progress.cancel();
		AlertDialog.Builder alert = new AlertDialog.Builder(context);
		alert.setTitle(R.string.testconnectiontask_dialog_title);
		alert.setNeutralButton(R.string.close, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		if (result.booleanValue()) {
			if (handler.isWFS()) {
				// Connection success
				alert.setMessage(context.getString(R.string.testconnectiontask_ready_dialog_part1) 
						+ context.getString(R.string.testconnectiontask_ready_dialog_part2)
						+ testURL 
						+ context.getString(R.string.testconnectiontask_ready_dialog_part3)
						+ handler.getWFSIdent().wfsType + " ("
						+ handler.getWFSIdent().wfsVersion + "): "
						+ handler.getWFSIdent().wfsTitle + "\n\n -->"
						+ handler.getWFSIdent().wfsAbstract + "\n"
						+ context.getString(R.string.testconnectiontask_ready_dialog_part4) 
						+ context.getString(R.string.testconnectiontask_ready_dialog_part5)
						+ handler.getProvider().name 
						+ context.getString(R.string.testconnectiontask_ready_dialog_part6)
						+ handler.getProvider().individual
						+ context.getString(R.string.testconnectiontask_ready_dialog_part7)
						+ handler.getProvider().position 
						+ context.getString(R.string.testconnectiontask_ready_dialog_part8)
						+ handler.getProvider().city 
						+ context.getString(R.string.testconnectiontask_ready_dialog_part9)
						+ handler.getProvider().country);
			} else {
				alert.setMessage(context.getString(R.string.testconnectiontask_ready_dialog_part1)
						+ context.getString(R.string.testconnectiontask_ready_dialog_part2)
						+ testURL
						+ context.getString(R.string.testconnectiontask_ready_dialog_part3)
						+ handler.getWMSIdent().wmsType + " ("
						+ handler.getWMSIdent().wmsVersion + "): "
						+ handler.getWMSIdent().wmsTitle + "\n\n -->"
						+ handler.getWMSIdent().wmsAbstract + "\n"
						+ context.getString(R.string.testconnectiontask_ready_dialog_part4)
						+ context.getString(R.string.testconnectiontask_ready_dialog_part5)
						+ handler.getWMSIdent().organization
						+ context.getString(R.string.testconnectiontask_ready_dialog_part6)
						+ handler.getWMSIdent().person 
						+ context.getString(R.string.testconnectiontask_ready_dialog_part7)
						+ handler.getWMSIdent().position 
						+ context.getString(R.string.testconnectiontask_ready_dialog_part8)
						+ handler.getWMSIdent().city 
						+ context.getString(R.string.testconnectiontask_ready_dialog_part9)
						+ handler.getWMSIdent().country);
			}
		} else {
			// Connection error
			String msg = context.getString(R.string.testconnectiontask_ready_dialog_errormsg1);
			if (exception != null)
				msg += context.getString(R.string.testconnectiontask_ready_dialog_errormsg2) + exception + "\n\n";
			msg += context.getString(R.string.testconnectiontask_ready_dialog_errormsg3);
			alert.setMessage(msg);
		}
		alert.show();
	}
}