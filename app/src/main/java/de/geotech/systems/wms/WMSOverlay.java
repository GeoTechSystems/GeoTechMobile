/**
 * Overlay to display and manage WMS Overlays.
 * 
 * @author Mathias Menninghaus
 * @author Torsten Hoch
 */

package de.geotech.systems.wms;

import java.util.ArrayList;

import com.vividsolutions.jts.geom.Coordinate;

import de.geotech.systems.drawing.DrawingPanelView;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;

public class WMSOverlay implements SleepableOverlayInterface {
	private static final String CLASSTAG = "WMSOverlay";
	private static final Coordinate ORIGIN = new Coordinate(0, 0);

	private Paint semitransparent;
	private ArrayList<WMSLoader<String>> loaderList;
	private WMSProgressAnimationManager loadManager;
	private double previousZoomLevel = -1;
	private InvalidationHandler invalidationHandler;
	private DrawingPanelView view;
	private boolean sleeps;

	/**
	 * Instantiate a new WMSOverlay.
	 * 
	 * @param loadManager to display if the overlay loads some parts
	 * @param drawingPanel father of this overlay
	 */
	public WMSOverlay(WMSProgressAnimationManager loadManager,
			DrawingPanelView view) {
		this.loaderList = new ArrayList<WMSLoader<String>>();
		this.loadManager = loadManager;
		this.view = view;
		this.semitransparent = new Paint();
		this.invalidationHandler = new InvalidationHandler();
	}

	@Override
	public void makeSleeping() {
		this.sleeps = true;

	}

	@Override
	public void makeAwake() {
		this.sleeps = false;
	}
	
	/**
	 * Add a new baseURL to load parts from. The first added WMS fill be
	 * displayed on the bottom.
	 * 
	 * @param getMapBaseURL {@link WMSUtils.getMapBaseURL}
	 */
	public void addLoader(String getMapBaseURL) {
		this.loaderList.add(new WMSLoader<String>(getMapBaseURL,
				invalidationHandler));
	}

	// Remove all previously added baseURLS
	public void clear() {
		for (WMSLoader<String> l : loaderList) {
			l.stopLoading();
		}
		loaderList.clear();
	}

	// Draw Parts using the currently displayed Position and all getMapBaseUrls.
	public void draw(Canvas canvas) {
		if (this.sleeps){
			return;
		}
		if (this.previousZoomLevel != view.getZoomLevel()) {
			this.stopLoading();
			this.previousZoomLevel = view.getZoomLevel();
		}
		// calculate distance to last center
		final int[] dist = WMSUtils.pixelDistance(ORIGIN,
				view.getMapScreen().pixelToCoordinate(view.getLeft(), view.getTop()),view);
		// so much parts we will need to load
		final int partsX = (view.getWidth() / WMSUtils.WIDTH) + 1;
		final int partsY = (view.getHeight() / WMSUtils.HEIGHT) + 1;
		// coordinates of the first part in ScreenPixels
		final int startX = (Math.abs(dist[WMSUtils.X]) % WMSUtils.WIDTH)
				* (dist[WMSUtils.X] < 0 ? (-1) : 1) - WMSUtils.WIDTH;
		final int startY = (Math.abs(dist[WMSUtils.Y]) % WMSUtils.HEIGHT)
				* (dist[WMSUtils.Y] < 0 ? (-1) : 1) - WMSUtils.HEIGHT;
		// coordinates of the last part in ScreenPixels
		final int endX = startX + partsX * WMSUtils.WIDTH;
		final int endY = startY + partsY * WMSUtils.HEIGHT;
		// identifier for the first part
		final int startIdentX = (dist[WMSUtils.X] * -1) / WMSUtils.WIDTH - 1;
		final int startIdentY = dist[WMSUtils.Y] / WMSUtils.HEIGHT + 1;
		Bitmap map;
		String key;
//		int k = 0;
		// Load parts for every seen part from top-left to bottom-right.
		for (int y = startY, identY = startIdentY; y <= endY; y += WMSUtils.HEIGHT, identY--) {
			for (int x = startX, identX = startIdentX; x <= endX; x += WMSUtils.WIDTH, identX++) {

//				 k++;
//				 String ident = "(" + view.getZoomLevel() + ") | " + identX
//				 + " | " + identY;
//				 Paint rectangle = new Paint();
//				 if((identX+identY)%2==0)rectangle.setColor(Color.LTGRAY);
//				 else rectangle.setColor(Color.GRAY);
//				 rectangle.setAlpha(100);
//				 rectangle.setStyle(Paint.Style.FILL);
//				 rectangle.setStrokeWidth(3);
//				 Rect r = new Rect(x, y, x + WMSUtils.WIDTH, y
//				 + WMSUtils.HEIGHT);
//				 canvas.drawRect(r, rectangle);

				// key to identify the part for a wmsLoader definite
				key = view.getZoomLevel() + "," + identX + "," + identY;
				// Load part from every WMSLoader.
				for (WMSLoader<String> layerName : loaderList) {
					map = layerName.loadMap(key, x, y, view);
					if (map != null) {
						canvas.drawBitmap(map, x, y, semitransparent);
					}
				}

//				 rectangle.setStrokeWidth(1);
//				 rectangle.setColor(Color.BLACK);
//				 rectangle.setAlpha(255);
//				 rectangle.setStyle(Paint.Style.FILL);
//				 rectangle.setTextSize(15);
//				 canvas.drawText(ident, x + 40, y + WMSUtils.HALFHEIGHT,
//				 rectangle);

			}
		}
		// Log.d(DT, "displayed rectangles: "+k);
		// Log.d(DT, "parts: "+partsX +" "+partsY);
		// Log.d(DT, "startPx: "+startX +" "+startY);
		// Log.d(DT, "startId: "+startIdentX+" "+startIdentY);
	}

	// Should be called if the Overlay is no longer visible.
	public void onPause() {
		stopLoading();
	}

	/**
	 * Set the transparency of the WMSOverlay
	 * 
	 * @param transparency alpha value [0..255]
	 */
	public void setTransparency(int transparency) {
		semitransparent.setAlpha(transparency);
	}

	private void stopLoading() {
		for (WMSLoader<String> layerName : loaderList) {
			layerName.stopLoading();
		}
	}

	// Handler to handle Threads of the WMSLoader
	private class InvalidationHandler extends Handler {
		
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case WMSLoader.START:
				loadManager.start();
				break;
			case WMSLoader.LOADSUCCESS:
				// loadManager.stop();
				WMSOverlay.this.view.invalidate();
				break;
			case WMSLoader.LOADFAIL:
				// loadManager.stop();
				break;
			case WMSLoader.STOP:
				loadManager.stop();
			}
			super.handleMessage(msg);
		}
	}

}
