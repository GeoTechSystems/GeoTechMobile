package de.geotech.systems.features;

import java.util.List;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.Log;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import de.geotech.systems.editor.AddFeatureBar;
import de.geotech.systems.editor.EditFeatureBar;
import de.geotech.systems.geometry.Pixel;
import de.geotech.systems.projects.Project;
import de.geotech.systems.wfs.WFSLayer;

/**
 * @author Sven Weisker (uucly@student.kit.edu) Eigener Thread fuer das Zeichnen
 *         der Feature. In kombination mit der Indexstruktur wird es das
 *         Anzeigen und zeichnen der Feature sehr viel smoother machen. Oh yeah
 *         TODO: Alles. Erst der Anfang
 * 
 */
public class FeatureDrawingThread extends Thread {

	private Canvas canvas;
	private Paint paint;
	private Envelope envelope;
	private Project project;
	private AddFeatureBar addFeatureBar;
	private EditFeatureBar editFeatureBar;
	private Paint editorPoint;
	private Paint editorLine;
	private Paint editorPolygon;
	private GeometryFactory geometryFactory;
	private boolean lock;
	private boolean start = false;

	public FeatureDrawingThread(Canvas c) {
		super();
		this.canvas = c;
		paint = new Paint();
		// this.envelope = e;
		// this.project = p;
	}

	@Override
	public void run() {

			while (!lock){
				paint.setStyle(Style.STROKE);
			paint.setStrokeWidth(1.5f);
			paint.setColor(Color.BLUE);
			canvas.drawLine(50, 50, 100, 100, paint);
			}
		// loadFeature();
	};

	public void draw(Canvas c) {
		this.canvas = c;

		if(!start)
			{
				start();
				start = true;
			}
	}

	public boolean getLock() {
		return this.lock;
	}

	public void setLock(boolean lock) {
		this.lock = lock;
	}

	public boolean isStart(){
		this.start = true;
		return start;
	}
	/*
	 * private void loadFeature() { for (int i = 0; i <
	 * project.getWFSContainer().size(); i++) { WFSLayer currentLayer =
	 * project.getWFSContainer().get(i);
	 * currentLayer.getFeatureContainer().clear(); if (currentLayer.isActive())
	 * { List<Integer> featureIDS = currentLayer.getFeatureIDs(envelope); if
	 * (featureIDS.size() > 0) { currentLayer.loadFeatures(featureIDS);
	 * currentLayer.createGeometry(); } } } }
	 * 
	 * private void drawFeature() { for (int i = 0; i <
	 * project.getWFSContainer().size(); i++) { WFSLayer currentLayer =
	 * project.getWFSContainer().get(i); paint.setAlpha(255); // loadFeatures();
	 * switch (currentLayer.getTypeInt()) { case WFSLayer.LAYER_TYPE_POINT:
	 * paint.setStyle(Style.FILL); paint.setColor(currentLayer.getColor()); for
	 * (int j = 0; j < currentLayer.getFeatureContainer().size(); j++) { Feature
	 * f = currentLayer.getFeatureContainer().get(j); drawPoint((Point)
	 * f.getGeom(), 3f, paint, canvas);
	 * 
	 * } break; case WFSLayer.LAYER_TYPE_LINE: paint.setStyle(Style.STROKE);
	 * paint.setStrokeWidth(1.5f); paint.setColor(currentLayer.getColor()); for
	 * (int j = 0; j < currentLayer.getFeatureContainer().size(); j++) { Feature
	 * f = currentLayer.getFeatureContainer().get(j); drawLine((LineString)
	 * f.getGeom(), paint, canvas);
	 * 
	 * } break; case WFSLayer.LAYER_TYPE_POLYGON: paint.setStyle(Style.FILL);
	 * paint.setAlpha(255); paint.setColor(currentLayer.getColor()); for (int j
	 * = 0; j < currentLayer.getFeatureContainer().size(); j++) { Feature f =
	 * currentLayer.getFeatureContainer().get(j); drawPolygon((Polygon)
	 * f.getGeom(), paint, canvas);
	 * 
	 * } break; default: Log.e(CLASSTAG,
	 * "Error at drawing layers: invalid layer type."); }
	 * 
	 * // feature to be created if (addFeatureBar.getType() > -1) { switch
	 * (addFeatureBar.getType()) { case WFSLayer.LAYER_TYPE_POINT: if
	 * (addFeatureBar.getNewPoint() != null) {
	 * drawPoint(addFeatureBar.getNewPoint(), 7f, editorPoint, canvas); } break;
	 * case WFSLayer.LAYER_TYPE_LINE: // check if only one point set if
	 * (addFeatureBar.getFirst() != null && addFeatureBar.getSecond() == null) {
	 * drawPoint(geometryFactory.createPoint(addFeatureBar .getFirst()), 3f,
	 * editorPoint, canvas); } // draw line if more than one point is set if
	 * (addFeatureBar.getNewLine() != null) { LineString currentLine =
	 * addFeatureBar.getNewLine(); // draw line drawLine(currentLine,
	 * editorLine, canvas); // draw line nodes Coordinate[] currentCoords =
	 * currentLine .getCoordinates(); for (Coordinate c : currentCoords) {
	 * drawPoint(geometryFactory.createPoint(c), 3f, editorPoint, canvas); } }
	 * break; case WFSLayer.LAYER_TYPE_POLYGON: // check if only one point is
	 * set if (addFeatureBar.getFirst() != null && addFeatureBar.getSecond() ==
	 * null) { drawPoint(geometryFactory.createPoint(addFeatureBar .getFirst()),
	 * 3f, editorPoint, canvas); } // check if two points are set if
	 * (addFeatureBar.getFirst() != null && addFeatureBar.getSecond() != null) {
	 * // draw line drawLine( geometryFactory.createLineString(new Coordinate[]
	 * { addFeatureBar.getFirst(), addFeatureBar.getSecond() }), editorLine,
	 * canvas); // draw nodes
	 * drawPoint(geometryFactory.createPoint(addFeatureBar .getFirst()), 3f,
	 * editorPoint, canvas); drawPoint(geometryFactory.createPoint(addFeatureBar
	 * .getSecond()), 3f, editorPoint, canvas); } // draw polygon if more than
	 * two points are set if (addFeatureBar.getNewPolygon() != null) { Polygon
	 * currentPolygon = addFeatureBar.getNewPolygon(); // draw polygon
	 * drawPolygon(currentPolygon, editorPolygon, canvas); // draw polygon nodes
	 * Coordinate[] currentCoords = currentPolygon .getCoordinates(); for
	 * (Coordinate c : currentCoords) {
	 * drawPoint(geometryFactory.createPoint(c), 3f, editorPoint, canvas); } }
	 * break; } } } }
	 * 
	 * private void drawPoint(Point point, float r, Paint paint, Canvas c) {
	 * Pixel pixel = this.coordinateToPixel(point.getCoordinate());
	 * c.drawCircle(pixel.getCol(), pixel.getRow(), r, paint); }
	 * 
	 * private Pixel coordinateToPixel(Coordinate coordinate) { return
	 * coordinateToPixel(coordinate.x, coordinate.y); }
	 * 
	 * // Calculates pixel values from coordinates private Pixel
	 * coordinateToPixel(double x, double y) { return new Pixel((float) ((x -
	 * upperLeft.x) * scale), (float) ((upperLeft.y - y) * scale)); }
	 */
}
