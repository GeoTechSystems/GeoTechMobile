/**
 * Besitzt statische Methoden um Point,Line und Polygone zu zeichnen
 *  
 * @author Sven Weisker (uucly@student.kit.edu)
 */

package de.geotech.systems.drawing;

import java.util.ArrayList;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Paint.Style;
import android.util.Log;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import de.geotech.systems.R;
import de.geotech.systems.features.Feature;
import de.geotech.systems.features.FeatureContainer;
import de.geotech.systems.geometry.Pixel;

public class DrawingFeatureManager {

	private static final String CLASSTAG = "DrawingFeatureManager";
	private DrawingPanelView panel;
	private Canvas canvas;
	private FeatureContainer featureContainer;
	private Paint paint;
	private boolean marker;
	private int lastColor;
	private Bitmap lastBMP;

	/**
	 * Instantiates a new drawing feature manager.
	 * 
	 * @param newPanel
	 *            the new panel
	 */
	public DrawingFeatureManager(DrawingPanelView newPanel) {
		this.panel = newPanel;
		this.lastBMP = BitmapFactory.decodeResource(panel.getContext().getResources(), R.drawable.pin_red);
		this.lastColor = Color.RED;
		this.paint = new Paint();
		this.paint.setColor(lastColor);
	}

	/**
	 * Draws features on canvas for a thread.
	 * 
	 * @param newCanvas
	 *            the new canvas
	 * @param newFeatureContainer
	 *            the new feature container
	 * @param marker
	 *            the marker
	 */
	public void drawFeaturesForThread(Canvas newCanvas, FeatureContainer newFeatureContainer, boolean marker) {
		this.featureContainer = newFeatureContainer;
		this.canvas = newCanvas;
		this.marker = marker;
		this.paint(canvas);
	}

	/**
	 * Paints all Features in Container on canvas.
	 * 
	 * @param canvas
	 *            the canvas
	 */
	private void paint(Canvas canvas) {
		paint.setStyle(Style.FILL);
		paint.setAlpha(255);
		ArrayList<Feature> tempList = featureContainer.getAllPolygonFeatures();
		Feature feature;
		for (int j = 0, k = tempList.size(); j < k; j++) {
			feature = tempList.get(j);
			paint.setColor(feature.getColor());
			drawPolygon((Polygon) feature.getGeom(), paint, canvas, marker, panel);
		}
		if (marker) {
			drawPolygonMarker(tempList);
		}
		paint.setStyle(Style.STROKE);
		paint.setStrokeWidth(2f);
		paint.setAlpha(255);
		tempList = featureContainer.getAllLineFeatures();
		for (int j = 0, k = tempList.size(); j < k; j++) {
			feature = tempList.get(j);
			paint.setColor(feature.getColor());
			drawLine((LineString) feature.getGeom(), paint, canvas, marker, panel);
		}
		paint.setStyle(Style.FILL);
		paint.setAlpha(255);
		tempList = featureContainer.getAllPointFeatures();
		
		
		//Hier werden Punkte aus dem Layer gezeichnet************************+
		for (int j = 0, k = tempList.size(); j < k; j++) {
			feature = tempList.get(j);
			paint.setColor(feature.getColor());
			drawPoint((Point) feature.getGeom(), 10f, paint, canvas, marker, panel);
		}
	}
	
	

	private void drawPolygonMarker(ArrayList<Feature> tempList) {
		Feature feature;
		Pixel pixel;
		for (int j = 0; j < tempList.size(); j++) {
			feature = tempList.get(j);
			pixel = panel.getMapScreen().coordinateToPixel(((Polygon) feature.getGeom()).getExteriorRing().getCoordinates()[0]);
			if (feature.getColor() != this.lastColor) {
				this.lastBMP = BitmapFactory.decodeResource(panel.getContext().getResources(), getBMPString(feature.getColor()));
				this.lastColor = feature.getColor();
			}
			canvas.drawBitmap(lastBMP, pixel.getCol() - (lastBMP.getWidth() / 2 + 1), pixel.getRow() - lastBMP.getHeight() + 4,
					null);
		}
	}

	/**
	 * Gets the BMP string.
	 * 
	 * @param currentFeature
	 *            the current feature
	 * @return the BMP string
	 */
	private int getBMPString(int color) {
		switch (color) {
		case Color.RED:
			return R.drawable.pin_red;
		case Color.BLACK:
			return R.drawable.pin_black;
		case Color.BLUE:
			return R.drawable.pin_blue;
		case Color.CYAN:
			return R.drawable.pin_cyan;
		case Color.DKGRAY:
			return R.drawable.pin_dkgray;
		case Color.GRAY:
			return R.drawable.pin_gray;
		case Color.GREEN:
			return R.drawable.pin_green;
		case Color.LTGRAY:
			return R.drawable.pin_ltgray;
		case Color.MAGENTA:
			return R.drawable.pin_magenta;
		case Color.WHITE:
			return R.drawable.pin_white;
		case Color.YELLOW:
			return R.drawable.pin_yellow;
		case Color.TRANSPARENT:
			return R.drawable.pin_transparent;
		default:
			return R.drawable.pin_gray;
		}
	}

	// Draws point from ArrayList to canvas
	public void drawPoint(Point point, float radius, Paint paint, Canvas c, boolean marker, DrawingPanelView panel) {
		Pixel pixel = panel.getMapScreen().coordinateToPixel(point.getCoordinate());
		c.drawCircle(pixel.getCol(), pixel.getRow(), radius, paint);
		if (marker) {
			if (paint.getColor() != this.lastColor) {
				this.lastBMP = BitmapFactory.decodeResource(panel.getContext().getResources(), getBMPString(paint.getColor()));
				this.lastColor = paint.getColor();
			}
			c.drawBitmap(lastBMP, pixel.getCol() - (lastBMP.getWidth() / 2 + 1), pixel.getRow() - lastBMP.getHeight() + 4, null);
		}
	}

	// Draws line from ArrayList to canvas
	public void drawLine(LineString ls, Paint p, Canvas c, boolean marker, DrawingPanelView panel) {
		Coordinate[] coords = ls.getCoordinates();
		Path path = new Path();
		boolean first = true;
		Pixel pixel;
		for (int i = 0; i < coords.length; i++) {
			pixel = panel.getMapScreen().coordinateToPixel(coords[i]);
			if (first) {
				path.moveTo(pixel.getCol(), pixel.getRow());
				first = false;
			} else {
				path.lineTo(pixel.getCol(), pixel.getRow());
			}
		}
		c.drawPath(path, p);
		if (marker) {
			pixel = panel.getMapScreen().coordinateToPixel(coords[0]);
			if (paint.getColor() != this.lastColor) {
				this.lastBMP = BitmapFactory.decodeResource(panel.getContext().getResources(), getBMPString(paint.getColor()));
				this.lastColor = paint.getColor();
			}
			c.drawBitmap(lastBMP, pixel.getCol() - (lastBMP.getWidth() / 2 + 1), pixel.getRow() - lastBMP.getHeight() + 4, null);
		}
	}

	// Draws polygon to canvas
	public void drawPolygon(Polygon pl, Paint p, Canvas c, boolean marker, DrawingPanelView panel) {
		Path polygon = new Path();
		Pixel pixel = null;
		// draw shell
		Coordinate[] shell_coords = pl.getExteriorRing().getCoordinates();
		Path shell = new Path();
		boolean first = true;
		for (int i = 0; i < shell_coords.length; i++) {
			pixel = panel.getMapScreen().coordinateToPixel(shell_coords[i]);
			if (first) {
				shell.moveTo(pixel.getCol(), pixel.getRow());
				first = false;
			} else {
				shell.lineTo(pixel.getCol(), pixel.getRow());
			}
		}
		shell.close();
		polygon.addPath(shell);
		// draw holes
		for (int j = 0; j < pl.getNumInteriorRing(); j++) {
			Coordinate[] hole_coords = pl.getInteriorRingN(j).getCoordinates();
			Path hole = new Path();
			boolean hole_first = true;
			for (int i = 0; i < hole_coords.length; i++) {
				pixel = panel.getMapScreen().coordinateToPixel(hole_coords[i]);
				if (hole_first) {
					hole.moveTo(pixel.getCol(), pixel.getRow());
					hole_first = false;
				} else {
					hole.lineTo(pixel.getCol(), pixel.getRow());
				}
			}
			hole.close();
			polygon.addPath(hole);
		}
		polygon.setFillType(Path.FillType.EVEN_ODD);
		c.drawPath(polygon, p);
	}

}
