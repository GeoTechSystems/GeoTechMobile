/**
 * Class for storing the captured media
 * 
 * @author tubatubsen
 */


package de.geotech.systems.media;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;

public class StorageHelper {

	private Context context;
	private boolean externalStorageAvailable = false;
	private boolean externalStorageWriteable = false;
	
	
	public StorageHelper(Context context)
	{
		this.context = context;
		init();
	}
	
	void init()
	{
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
		    // We can read and write the media
		    externalStorageAvailable = externalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
		    // We can only read the media
		    externalStorageAvailable = true;
		    externalStorageWriteable = false;
		} else {
		    // Something else is wrong. It may be one of many other states, but all we need
		    //  to know is we can neither read nor write
		    externalStorageAvailable = externalStorageWriteable = false;
		}
		
		
	}
	
	public boolean isExternalStorageAvailable() {
		return externalStorageAvailable;
	}

	public boolean isExternalStorageWriteable() {
		return externalStorageWriteable;
	}

	public Uri createImageDestinationUri(String bucket, String filename)
	{
		if (!this.isExternalStorageAvailable())
		{
			return null; 
		}
		if (!this.isExternalStorageWriteable())
		{
			return null; 
		}
		
		Uri result = null;
		
		String path = Environment.DIRECTORY_PICTURES;		
		if (bucket!=null && !bucket.equals(""))
		{
			path = path +"/" + bucket;
		}
		File picturePath = context.getExternalFilesDir(path);
		if (picturePath==null)
		{
			picturePath = Environment.getExternalStoragePublicDirectory( path );
		}
		File file = new File(picturePath, filename );
		result = Uri.fromFile(file);
		return result;
	}
//	
//	public boolean saveJPEGBuffer(String bucket, String filename, byte[] jpeg, MediaScannerConnection.OnScanCompletedListener scanCompleteListener)
//	{
//		if (!this.isExternalStorageAvailable())
//		{
//			return false; 
//		}
//		if (!this.isExternalStorageWriteable())
//		{
//			return false; 
//		}
//		
//		boolean result = false;
//		
//		String path = Environment.DIRECTORY_PICTURES;		
//		if (bucket!=null && !bucket.equals(""))
//		{
//			path = path +"/" + bucket;
//		}
//		File picturePath = context.getExternalFilesDir(path);
//		File file = new File(picturePath, filename );
//		OutputStream os;
//		try {
//			os = new FileOutputStream(file);
//		    os.write(jpeg);
//		    os.close();		    
//			MediaScannerConnection.scanFile(context,new String[] { file.toString() }, null,scanCompleteListener);
//			result = true;	        
//		} catch (FileNotFoundException e) {
//			
//		} catch (IOException e) {
//			
//		}
//
//		
//
//		return result;
//	}
//	public Uri saveJPEG(String bucket, String filename, Bitmap bitmap, boolean savePublic)
//	{
//		if (!this.isExternalStorageAvailable())
//		{
//			return null; 
//		}
//		if (!this.isExternalStorageWriteable())
//		{
//			return null; 
//		}
//		
//		Uri result = null;
//		
//		String path = Environment.DIRECTORY_PICTURES;		
//		if (bucket!=null && !bucket.equals(""))
//		{
//			path = path +"/" + bucket;
//		}
//		File picturePath = null;
//		if (savePublic)
//		{
//			picturePath = Environment.getExternalStoragePublicDirectory(path);
//			picturePath.mkdir();
//		}
//		else
//		{
//			picturePath = context.getExternalFilesDir(path);
//			picturePath.mkdir();
//		}
//		File file = new File(picturePath, filename );
//		OutputStream os;
//		try {
//			os = new FileOutputStream(file);
//			bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);		    
//		    os.close();
//		    MediaScannerConnection.scanFile(context,new String[] { file.toString() }, null,null);
//			result = Uri.fromFile(file);				
//			
//		} catch (FileNotFoundException e) {
//			
//		} catch (IOException e) {
//			
//		}
//
//		return result;
//		
//	}
//	public Uri saveJPEG(String bucket, String filename, byte[] jpeg)
//	{
//		if (!this.isExternalStorageAvailable())
//		{
//			return null; 
//		}
//		if (!this.isExternalStorageWriteable())
//		{
//			return null; 
//		}
//		
//		Uri result = null;
//		
//		String path = Environment.DIRECTORY_PICTURES;		
//		if (bucket!=null && !bucket.equals(""))
//		{
//			path = path +"/" + bucket;
//		}
//		File picturePath = context.getExternalFilesDir(path);
//		File file = new File(picturePath, filename );
//		OutputStream os;
//		try {
//			os = new FileOutputStream(file);
//		    os.write(jpeg);
//		    os.close();		    			
//			result = Uri.fromFile(file);	        
//		} catch (FileNotFoundException e) {
//			
//		} catch (IOException e) {
//			
//		}
//
//		
//
//		return result;
//	}
//
//	public boolean addressesDirectory(Uri data)
//	{
//		String type = context.getContentResolver().getType(data);
//		boolean isDirectory = type.startsWith("vnd.android.cursor.dir/");
//		return isDirectory;
//	}
//	
//	public boolean addressesSingleItem(Uri data)
//	{
//	   try
//	   {
//	     long id = ContentUris.parseId(data);
//	     return id>=0;
//	    }
//		catch (UnsupportedOperationException e)
//		{
//		  return false;
//		}
//		catch (NumberFormatException e)
//		{
//		  return false;
//		}
//	}
//

	
}