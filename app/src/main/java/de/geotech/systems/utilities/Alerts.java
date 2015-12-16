package de.geotech.systems.utilities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class Alerts {

	/** Shows an alert with one button */
	public static AlertDialog.Builder errorMessage(Context context, String title, String message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		if(title != null) {
			builder.setTitle(title);
		}
		if(message != null) {
			builder.setMessage(message);
		}
		builder.setCancelable(false);
		builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   dialog.cancel();
		           }
	       		});
		return builder;
	}

}
