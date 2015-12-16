/**
 * Class AddLayerActivity represent the user interface for selecting a server
 * (WFS or WMS) and choosing the preferred layers.
 * 
 * @author Sven Weisker (uucly@student.kit.edu)
 * @author Paul Vincent Kuper (kuper@kit.edu)
 * @author tubatubsen
 * @author Torsten Hoch
 */

package de.geotech.systems.layers;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import de.geotech.systems.R;
import de.geotech.systems.connectionTest.TestConnectionTask;
import de.geotech.systems.database.DBAdapter;
import de.geotech.systems.projects.Server;
import de.geotech.systems.utilities.Functions;

public class AddLayerActivity extends Activity implements OnClickListener,
RadioGroup.OnCheckedChangeListener {
	private static final String CLASSTAG = "AddLayerActivity";
	private static final String WMSONSTRING = "de.geotech.systems.wmson";
	private static final int SERVEREINGABE_DIALOG = 1;

	private String serveradresse = "";
	private ServerSpinner serverLists = null;
	private Button addNewServer;
	private Button checkConnection;
	private RadioButton wmsRadioButton;
	private RadioButton wfsRadioButton;
	private RadioGroup radioGroup;

	// if WMS or not (-> WFS)
	private boolean wmsOn = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.addlayeractivity_load_screen);
		SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		// wms oder wfs momentan?
		wmsOn = prefs.getBoolean(WMSONSTRING, false);
		// neuer spinner für server liste
		serverLists = new ServerSpinner(this, wmsOn, R.id.addLayeractivity_layer_table_layout);
		// "Add new server" - Button
		addNewServer = (Button) findViewById(R.id.addLayerActivity_add_new_Server_Button);
		addNewServer.setOnClickListener(this);
		// "Check connection" - Button
		checkConnection = (Button) findViewById(R.id.addLayerActivity_button_checkconnection);
		checkConnection.setOnClickListener(this);
		// Radiobuttons zur auswahl ob wms oder wfs
		wfsRadioButton = (RadioButton) findViewById(R.id.addLayerActivity_radio_wfs);
		wmsRadioButton = (RadioButton) findViewById(R.id.addLayerActivity_radio_wms);
		wfsRadioButton.setChecked(!wmsOn);
		wmsRadioButton.setChecked(wmsOn);
		// radiogroup dafür, um das ganze einfacher zu handeln
		radioGroup = (RadioGroup) findViewById(R.id.addLayerActivity_radioGroup);
		radioGroup.setOnCheckedChangeListener(this);
		// spinner starten
		serverLists.loadSpinner();
	}

	// onClick methods for the different buttons
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.addLayerActivity_add_new_Server_Button:
			serverundpassworteingabe();
			break;

		case R.id.addLayerActivity_button_checkconnection:
			testWFSConnection();
			break;
		}
	}

	private void serverundpassworteingabe() {
		this.showDialog(SERVEREINGABE_DIALOG);
	}

	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId) {
		// wenn geklickt wurde, dann ist status fuer wmsOn auf jeden
		// fall andersherum
		wmsOn = (!wmsOn);
		// dementsprechend alles neu setzen
		wfsRadioButton.setChecked(!wmsOn);
		wmsRadioButton.setChecked(wmsOn);
		serverLists.setWMSOn(wmsOn);
		SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		prefs.edit().putBoolean(WMSONSTRING, wmsOn).commit();
		// spinner dementsprechend neu laden
		serverLists.loadSpinner();
	}

	@Override
	public void onBackPressed() {
		Intent returnToMain = new Intent();
		setResult(RESULT_CANCELED, returnToMain);
		finish();
	}

	// Private method to test wfs connection.
	private void testWFSConnection() {
		TestConnectionTask test = new TestConnectionTask(this);
		if (!wmsOn) {
			test.execute(Functions.reviseWfsUrl(serverLists
					.getCurrentServerAddress()));
		} else if (wmsOn) {
			test.execute(Functions.reviseWmsUrl(serverLists
					.getCurrentServerAddress()));
		}

	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = null;
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.newserver_dialog, null);

		final TextView serverNameText = (TextView) layout.findViewById(R.id.newserver_dialog_servername_text);
		final EditText serverName = (EditText) layout.findViewById(R.id.newserver_dialog_servername_edittext);
		final TextView serverURLText = (TextView) layout.findViewById(R.id.newserver_dialog_serverurl_text);
		final EditText serverURL = (EditText) layout.findViewById(R.id.newserver_dialog_serverurl_edittext);
		final TextView optpara = (TextView) layout.findViewById(R.id.newserver_dialog_optionaleparameter);
		final TextView userNameText = (TextView) layout.findViewById(R.id.newserver_dialog_username_text);
		final EditText userName = (EditText) layout.findViewById(R.id.newserver_dialog_username_edittext);
		final TextView passwortText = (TextView) layout.findViewById(R.id.newserver_dialog_passwort_text);
		final EditText passwort = (EditText) layout.findViewById(R.id.newserver_dialog_passwort_edittext);
		
		builder.setView(layout);
		builder.setCancelable(true);
		// OK Button (im Dialog)
		builder.setPositiveButton(R.string.ok,
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				serveradresse = serverURL.getText().toString();
				// wenn server schon in der liste: nix machen, sonst eintragen
				if (!serverLists.getServerList().contains(serveradresse)) {
					serverLists.addNEWServer(new Server(serveradresse,
							userName.getText().toString(), passwort.getText().toString(),
							serverName.getText().toString()));
				}
				// spinner neu laden mit aktuellem server
				serverLists.loadSpinner();
			}
		});
		// Cancel - Button (im Dialog)
		builder.setNegativeButton(R.string.cancel,
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.cancel();
			}
		});
		// Erzeugt die Anzeige des Dialogs
		dialog = builder.create();
		return dialog;
	}

	//Alte OnClick für case R.id.addLayerActivity_add_new_Server_Button:
	
	// AlertDialog.Builder builder = new
	// AlertDialog.Builder(v.getContext());
	// // erzeugt einen EditText Feld zur Eingabe der Serveradresse
	// final EditText serverURL = new
	// EditText(addNewServer.getContext());
	// final EditText serverUserName = new
	// EditText(addNewServer.getContext());
	// final EditText serverPW = new
	// EditText(addNewServer.getContext());
	// final TextView userNameText = new
	// TextView(addNewServer.getContext());
	// final TextView pwText = new TextView(addNewServer.getContext());
	// // TODO Uebersezungen
	// userNameText.setText("User Name: (optional)");
	// pwText.setText("Password: (optional)");
	// builder.setTitle(R.string.addLayerAcitivty_popup_text_title);
	// builder.setMessage(R.string.addLayerActivity_popup_text_uri);
	// builder.setView(serverURL);
	// builder.setView(userNameText);
	// builder.setView(serverUserName);
	// builder.setView(pwText);
	// builder.setView(serverPW);
	// // OK Button (im Dialog)
	// builder.setPositiveButton(R.string.ok, new
	// DialogInterface.OnClickListener() {
	// public void onClick(DialogInterface dialog, int whichButton) {
	// serveradresse = serverURL.getText().toString();
	// // wenn wms
	// // if (wmsOn) {
	// // // wenn server schon in der liste: nix machen; sonst eintragen
	// // if (!serverLists.getServerList().contains(
	// // serveradresse)) {
	// // serverLists.addServerAddress(serveradresse);
	// // }
	// // } else {
	// // WFS
	// if (!serverLists.getServerList().contains(serveradresse)) {
	// serverLists.addServerAddress(new Server
	// (serverURL.getText().toString(),
	// serverUserName.getText().toString(),
	// serverPW.getText().toString()));
	// // }
	// }
	// // spinner neu laden mit aktuellem server
	// serverLists.loadSpinner();
	// }
	// });
	// // Cancel - Button (im Dialog)
	// builder.setNegativeButton(R.string.cancel, new
	// DialogInterface.OnClickListener() {
	// public void onClick(DialogInterface dialog, int whichButton) {
	// dialog.cancel();
	// }
	// });
	// // Erzeugt die Anzeige des Dialogs
	// AlertDialog dialog = builder.create();
	// dialog.show();
}