package unused;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import android.graphics.drawable.Drawable;
import android.os.StrictMode;
import android.util.Log;

public class ManageMapTiles {
	private String baseUrl = 
			"http://map-service.heidelberg.de/hd_map/osiris?request=getmap&service=wms&version=1.1.1&styles=default&format=image/png&layers=Stadtplan&srs=EPSG:31467&";
	private double bb_maxx, bb_maxy, bb_minx, bb_miny;
	private HashMap<String,Drawable> imgCache;

	// Konstruktor: Initialisiere mapTiles
	public ManageMapTiles()	{
		// TODO: Übergabe eines Objekts, in dem alle Informationen zum Server gespeichert sind => Set baseUrl, bbox, ...
		this.imgCache=new HashMap<String, Drawable>();
	}

	public Drawable getMapTileFromUrl(double minx, double miny, double maxx, 
			double maxy, double size) {
		// Lade den druch die Parameter beschriebenen Kartenteil vom WMS
		// if (minx >= bb_minx && miny >= bb_miny && maxx <= bb_maxx && maxy <= bb_maxy)
		try {
			URL url=new URL(this.baseUrl + "width=" + String.valueOf(size) + "&height=" 
					+ String.valueOf(size) + "&bbox=" + String.valueOf(minx) + "," 
					+ String.valueOf(miny) + "," + String.valueOf(maxx) + "," 
					+ String.valueOf(maxy));
			// URL url = new URL("http://hiu.kit.edu/img/kit_logo_de_farbe_positiv.jpg");

			Log.d("theUrl", url.toString());

			// Lazy way: Erlaube Netzwerkoperationen im Main-Thread (temporär!)
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
			StrictMode.setThreadPolicy(policy);

			InputStream content = (InputStream) url.getContent();
			Drawable img = Drawable.createFromStream(content, "src");

			// Speichere Kartenstück in der HashMap, die ID wird durch die Koordinaten und die Größe gegeben.
			imgCache.put(String.valueOf(minx)+","+String.valueOf(miny)+","+String.valueOf(maxx)+","+String.valueOf(maxy)+','+String.valueOf(size), img);
			return img;
		}
		catch(MalformedURLException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		} 
		return null;
	}
	/*else
		{
			return null;
		}*/


	public Drawable getMapTileFromCache(double minx, double miny, double maxx, 
			double maxy, double size) throws IOException	{
		//Prüfe, ob sich der angeforderte Kartenteil bereits im Cache befindet
		Drawable img=imgCache.get(String.valueOf(minx) + "," + String.valueOf(miny) + "," 
				+ String.valueOf(maxx) + "," + String.valueOf(maxy) + ',' + String.valueOf(size));
		if (img != null) {			
			// Ja -> Gebe Bild aus Cache zurück
			return img;
		}
		else {
			// Nein -> Lade Bild nach
			return getMapTileFromUrl(minx, miny, maxx, maxy, size);
		}
	}

}
