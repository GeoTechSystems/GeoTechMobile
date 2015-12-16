/**
 * Class for activating the media modules
 * 
 * @author tubatubsen
 */

package de.geotech.systems.media;

import java.util.Calendar;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.format.DateFormat;

public class MediaCapture {
	
	static public final int CAPTURE_IMAGE = 104;
	
	static public Uri takePicture(Activity activity) {
		
		StorageHelper helper = new StorageHelper(activity);
		if (helper.isExternalStorageAvailable() && helper.isExternalStorageWriteable()) {
			
			String filename = new DateFormat().format("yyyyMMdd-hhmmss",Calendar.getInstance().getTime())+".jpg";
			
			Uri imageUri = helper.createImageDestinationUri(null, filename);
			
			Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
			activity.startActivityForResult(intent, CAPTURE_IMAGE);
			
			return imageUri;
		}
		
		return null;
	}

	//TODO: Umwandeln in byte[] jpeg
	
}
