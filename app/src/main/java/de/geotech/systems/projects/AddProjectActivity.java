/**
 * Erstellung eines neuen Projektes
 * 
 * @author sven weisker
 * @author Torsten Hoch
 */

package de.geotech.systems.projects;

import de.geotech.systems.R;
import de.geotech.systems.utilities.CRS;
import de.geotech.systems.utilities.Constants;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

public class AddProjectActivity extends Activity {
	private static final int DIALOG_EXISTS = 0;
	private static final int DIALOG_EXEPTION = 1;
	private AlertDialog.Builder builder = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final ScaleAnimation icon_animation = new ScaleAnimation(0.7f, 1f,
				0.7f, 1f, Animation.RELATIVE_TO_SELF, 0.5f,
				Animation.RELATIVE_TO_SELF, 0.5f);
		icon_animation.setDuration(300);
		setContentView(R.layout.project_manager_add);
		builder = new AlertDialog.Builder(this);
		final EditText name = (EditText) findViewById(R.id.edit_projects_add_name);
		final Spinner epsg = (Spinner) findViewById(R.id.spinner_projects_add_srs);
		ArrayAdapter<CRS> adapter = new ArrayAdapter<CRS>(this,
				android.R.layout.simple_spinner_item, Constants.getCRSList());
		epsg.setAdapter(adapter);
		final EditText desc = (EditText) findViewById(R.id.edit_projects_add_desc);
		Button add = (Button) findViewById(R.id.button_projects_add);
		add.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String proj_name = name.getText().toString();
				// Falls kein Projektname angegeben wurde, erscheint hier ein Pop-Up mit Button.
				if (proj_name.trim().equals("") || proj_name == null) {
					v.startAnimation(icon_animation);
					builder = new AlertDialog.Builder(v.getContext());
					builder.setMessage(getString(R.string.project_please_set_project_name));
					builder.setCancelable(false);
					builder.setNeutralButton(R.string.ok,
							new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int which) {
							dialog.cancel();
						}
					});
					builder.show();
				} else {
					CRS selectedCRS = (CRS) epsg.getSelectedItem();

					// projekct intern und in sql speichern
					if (ProjectHandler.addProject(v.getContext(), proj_name, desc.getEditableText().toString(), selectedCRS.getCode())) {
						Intent backToManager = new Intent();
						setResult(RESULT_OK, backToManager);
						finish();
					} else {
						showDialog(DIALOG_EXISTS);
					}
				}
			}
		});
	}

	protected AlertDialog onCreateDialog(int id) {
		AlertDialog dialog;

		switch (id) {
		case DIALOG_EXISTS:
			// TODO uebersetzungen
			builder.setMessage("This project already exists!");
			builder.setCancelable(false);
			builder.setPositiveButton(R.string.ok,
					new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			});
			dialog = builder.create();
			break;
		case DIALOG_EXEPTION:
			// TODO uebersetzungen
			builder.setMessage("File output exeption!");
			builder.setCancelable(false);
			builder.setPositiveButton(R.string.ok,
					new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			});
			dialog = builder.create();
			break;
		default:
			dialog = null;
		}
		return dialog;
	}

}
