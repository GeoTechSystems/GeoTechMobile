/**
 * Overlay for Markers of Features
 * 
 * @author Torsten Hoch - tohoch@uos.de
 */
package unused;

import java.util.ArrayList;
import java.util.ListIterator;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.Log;

import com.vividsolutions.jts.geom.Coordinate;

import de.geotech.systems.R;
import de.geotech.systems.features.Feature;

public class WFSFeatureMarkerOverlay extends Overlay {
	// the standard BMP to draw
	private final static int standardBMP = R.drawable.pin_red;
	// the classtag
	private static final String CLASSTAG = "WFSFeatureMarkerOverlay";
	// the standard corrections for the standard bmp
	private static final int STANDARD_CORRECTION_X = 1;
	private static final int STANDARD_CORRECTION_Y = 4;
	
	// all Features to draw in a container
	private ArrayList<ArrayList<Feature>> featureContainer;
	// the context
	private Context context;
	// code for BMP
	private int rCodeForBMP;
	// how many pixel to correct in x for the BMP
	private int xPixelCorrection;
	// how many pixel to correct in y for the BMP	
	private int yPixelCorrection;

	/**
	 * Instantiates a new WFS feature overlay.
	 *
	 * @param newContext the new context
	 * @param newFeatureContainer the new feature container
	 * @param newRCodeForBMP the new r code for bmp
	 * @param xPixelCorrection the x pixel correction
	 * @param yPixelCorrection the y pixel correction
	 */
	public WFSFeatureMarkerOverlay(Context newContext, ArrayList<ArrayList<Feature>> newFeatureContainer, int newRCodeForBMP, int xPixelCorrection, int yPixelCorrection) {
		super(newContext);
		this.context = newContext;
		this.featureContainer = newFeatureContainer;
		this.rCodeForBMP = newRCodeForBMP;
		this.xPixelCorrection = xPixelCorrection;
		this.yPixelCorrection = yPixelCorrection;
	}
	
	/**
	 * Instantiates a new WFS feature overlay.
	 *
	 * @param newContext the new context
	 * @param newFeatureContainer the new feature container
	 */
	public WFSFeatureMarkerOverlay(Context newContext, ArrayList<ArrayList<Feature>> newFeatureContainer) {
		this(newContext, newFeatureContainer, standardBMP, STANDARD_CORRECTION_X, STANDARD_CORRECTION_Y);
	}

	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		Log.i(CLASSTAG, "Overlay for all WFS Layers is drawn!!!");
		// is it a shadow overlay
		Feature currentFeature;
		Bitmap bmp;
		Coordinate firstCoord;
		GeoPoint geoPoint;
		android.graphics.Point graphicsPoint;
		if (!shadow) {
			for (int i = 0; i < featureContainer.size(); i++) {
				ListIterator<Feature> iterContainer = featureContainer.get(i).listIterator();
				while (iterContainer.hasNext()) {
					currentFeature = iterContainer.next();
					// the bitmap to be drawn at the coordinate
					bmp = BitmapFactory.decodeResource(context.getResources(), getBMPString(currentFeature.getColor()));
					firstCoord = currentFeature.getCoordinatesFromLinestring()[0];
					geoPoint = new GeoPoint(firstCoord.y, firstCoord.x);
					graphicsPoint = new android.graphics.Point();
					mapView.getProjection().toMapPixels(geoPoint, graphicsPoint);
					int x = (graphicsPoint.x - (bmp.getWidth() / 2 + xPixelCorrection));
					int y = (graphicsPoint.y - bmp.getHeight() + yPixelCorrection);
					canvas.drawBitmap(bmp, x, y, null);
				}
			}
		}
	}
	
	/**
	 * Gets the BMP string.
	 *
	 * @param currentFeature the current feature
	 * @return the BMP string
	 */
	private int getBMPString(int color) {
		switch (color) {
		case Color.RED:
			standardCorrection();
			return R.drawable.pin_red;
		case Color.BLACK:
			standardCorrection();
			return R.drawable.pin_black;
		case Color.BLUE:
			standardCorrection();
			return R.drawable.pin_blue;
		case Color.CYAN:
			standardCorrection();
			return R.drawable.pin_cyan;
		case Color.DKGRAY:
			standardCorrection();
			return R.drawable.pin_dkgray;
		case Color.GRAY:
			standardCorrection();
			return R.drawable.pin_gray;
		case Color.GREEN:
			standardCorrection();
			return R.drawable.pin_green;
		case Color.LTGRAY:
			standardCorrection();
			return R.drawable.pin_ltgray;
		case Color.MAGENTA:
			standardCorrection();
			return R.drawable.pin_magenta;
		case Color.WHITE:
			standardCorrection();
			return R.drawable.pin_white;
		case Color.YELLOW:
			standardCorrection();
			return R.drawable.pin_yellow;
		case Color.TRANSPARENT:
			standardCorrection();
			return R.drawable.pin_transparent;
		default:
			standardCorrection();
			return R.drawable.pin_gray;
		}
	}
	
	public void standardCorrection() {
		setxPixelCorrection(STANDARD_CORRECTION_X); 
		setyPixelCorrection(STANDARD_CORRECTION_Y);
	}
	
	public int getxPixelCorrection() {
		return xPixelCorrection;
	}

	public void setxPixelCorrection(int xPixelCorrection) {
		this.xPixelCorrection = xPixelCorrection;
	}
	
	public int getyPixelCorrection() {
		return yPixelCorrection;
	}

	public void setyPixelCorrection(int yPixelCorrection) {
		this.yPixelCorrection = yPixelCorrection;
	}
	
}
