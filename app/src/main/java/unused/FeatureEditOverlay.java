/**
 * Overlay for a Feature that is edited
 * 
 * @author Torsten Hoch - tohoch@uos.de
 */

package unused;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import de.geotech.systems.features.Feature;
import de.geotech.systems.geometry.Pixel;
import de.geotech.systems.wfs.WFSLayer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Paint.Style;
import android.util.Log;

public class FeatureEditOverlay extends Overlay {
	// the classtag
	private static final String CLASSTAG = "WFSFeatureEditorOverlay";
	// standard radius
	private static final float STANDARD_BIG_RADIUS = 5f;
	// standard radius
	private static final float STANDARD_SMALL_RADIUS = 1f;
	// integer for no color chosen
	private static final int STANDARD_COLOR = Color.CYAN;

	// all Features to draw in a container
	private Feature feature;
	// the context
	private Context context;
	// Upper left corner coordinate
	private Coordinate upperLeft;
	// the one color // Cyan: -16711681
	private int backgroundColor;
	// the second color - for a little dot in the middle of the feature
	private int secondColor;
	// Scale 
	private double scale;
	// the radius of drawed points
	private float radius;

	/**
	 * Instantiates a new WFS feature overlay.
	 *
	 * @param newContext the new context
	 * @param newFeature the new feature
	 * @param upperLeft the upper left
	 * @param scale the scale
	 * @param color the color
	 * @param radius the radius
	 */
	public FeatureEditOverlay(Context newContext, Feature newFeature, Coordinate upperLeft, double scale, int color, float radius) {
		super(newContext);
		this.context = newContext;
		this.feature = newFeature;
		this.upperLeft = upperLeft;
		this.scale = scale;
		this.backgroundColor = color;
		this.radius = radius;
	}

	/**
	 * Instantiates a new WFS feature overlay.
	 *
	 * @param newContext the new context
	 * @param newFeature the new feature
	 * @param upperLeft the upper left
	 * @param scale the scale
	 */
	public FeatureEditOverlay(Context newContext, Feature newFeature, Coordinate upperLeft, double scale) {
		this(newContext, newFeature, upperLeft, scale, STANDARD_COLOR, STANDARD_BIG_RADIUS);
	}

	@Override
	protected void draw(Canvas canvas, MapView mapView, boolean shadow) {
		if (feature != null) {
			// set the second color
			this.secondColor = feature.getColor();
			// new drawing element
			Paint paintBig = new Paint();
			paintBig.setColor(backgroundColor);
			paintBig.setAlpha(255);
			Paint paintSmall = new Paint();
			paintSmall.setColor(secondColor);
			paintSmall.setAlpha(255);			
			for (int i = 1; i <= feature.getCoordinatesFromLinestring().length; i++) {
				Coordinate coordinate = feature.getCoordinatesFromLinestring()[i - 1];
				GeoPoint geoPoint = new GeoPoint(coordinate.y, coordinate.x);
				android.graphics.Point graphicsPoint = new android.graphics.Point();
				graphicsPoint = mapView.getProjection().toMapPixels(geoPoint, graphicsPoint);
				canvas.drawCircle(graphicsPoint.x, graphicsPoint.y, radius, paintBig);
				canvas.drawCircle(graphicsPoint.x, graphicsPoint.y, STANDARD_SMALL_RADIUS, paintSmall);
			}
			switch (feature.getFeatureType()) {
			case WFSLayer.LAYER_TYPE_POINT:
				break;
			case WFSLayer.LAYER_TYPE_LINE:
				paintBig.setStyle(Style.STROKE);
				paintBig.setStrokeWidth(1.5f);
				drawLine((LineString) feature.getGeom(), paintBig, canvas);
				break;
			case WFSLayer.LAYER_TYPE_POLYGON:
				paintBig.setStyle(Style.FILL);
				drawPolygon((Polygon) feature.getGeom(), paintBig, canvas);
				break;
			default:
				Log.e(CLASSTAG, "Error at drawing layers: invalid layer type.");
			}
		}
	}

	// Draws point from ArrayList to canvas
	public void drawPoint(Point point, float r, Paint paint, Canvas c) {
		Pixel pixel = this.coordinateToPixel(point.getCoordinate());
		c.drawCircle(pixel.getCol(), pixel.getRow(), r, paint);
	}

	// Draws line from ArrayList to canvas
	private void drawLine(LineString ls, Paint p, Canvas c) {
		Coordinate[] coords = ls.getCoordinates();
		Path path = new Path();
		boolean first = true;
		for (int i = 0; i < coords.length; i++) {
			Pixel currentPixel = this.coordinateToPixel(coords[i]);
			if (first) {
				path.moveTo(currentPixel.getCol(), currentPixel.getRow());
				first = false;
			} else {
				path.lineTo(currentPixel.getCol(), currentPixel.getRow());
			}
		}
		c.drawPath(path, p);
	}

	// Draws polygon to canvas
	private void drawPolygon(Polygon pl, Paint p, Canvas c) {
		Path polygon = new Path();
		// draw shell
		Coordinate[] shell_coords = pl.getExteriorRing().getCoordinates();
		Path shell = new Path();
		boolean first = true;
		for (int i = 0; i < shell_coords.length; i++) {
			Pixel currentPixel = this.coordinateToPixel(shell_coords[i]);
			if (first) {
				shell.moveTo(currentPixel.getCol(), currentPixel.getRow());
				first = false;
			} else {
				shell.lineTo(currentPixel.getCol(), currentPixel.getRow());
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
				Pixel holePixel = this.coordinateToPixel(hole_coords[i]);
				if (hole_first) {
					hole.moveTo(holePixel.getCol(), holePixel.getRow());
					hole_first = false;
				} else {
					hole.lineTo(holePixel.getCol(), holePixel.getRow());
				}
			}
			hole.close();
			polygon.addPath(hole);
		}
		polygon.setFillType(Path.FillType.EVEN_ODD);
		c.drawPath(polygon, p);
	}

	// Calculates pixel values from coordinates
	public Pixel coordinateToPixel(Coordinate coordinate) {
		return coordinateToPixel(coordinate.x, coordinate.y);
	}

	// Calculates pixel values from coordinates
	public Pixel coordinateToPixel(double x, double y) {
		return new Pixel((float) ((x - upperLeft.x) * scale), 
				(float) ((upperLeft.y - y) * scale));
	}

}