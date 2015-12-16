package de.geotech.systems.drawing;

import com.vividsolutions.jts.geom.Coordinate;

import de.geotech.systems.geometry.Pixel;

/**
 * Diese Klasse soll als Singleton implementiert sein. Kann aber jederzeit geaendert werden
 * 
 * @author svenweisker TODO: Bin mit Namen von Variablen und Methoden nicht zufrieden !!!
 */
public final class MapScreen extends Screen {

	private static final MapScreen INSTANCE = new MapScreen();
	private BBOX bboxMap;
	private BBOX bboxFeatures;

	/**
	 * privater Konstruktor damit keine weiter Instanze erschaffen werden kann
	 */
	private MapScreen() {
		bboxMap = new BBOX();
		bboxFeatures = new BBOX();
	}

	/**
	 * Fabrik-Methode um die absicht einer Singleton-Klasse deutlich zu machen
	 * 
	 * @return
	 */
	public static MapScreen getInstance() {
		return INSTANCE;
	}

	public void setBBOXMap(Coordinate upperLeft) {
		Coordinate lowerRight = pixelToCoordinate((float) width, (float) height);
		bboxMap = new BBOX(upperLeft, lowerRight);
	}

	/**
	 * Gets the upper left pixel of the screen as georeferenced Coordinate.
	 * 
	 * @return the upper left Coordinate
	 */
	public Coordinate getMapUpperLeft() {
		return bboxMap.getUpperLeft();
	}

	/**
	 * Gets the lower right pixel of the screen as georeferenced Coordinate.
	 * 
	 * @return the lower right Coordinate
	 */
	public Coordinate getMapLowerRight() {
		return pixelToCoordinate((float) width, (float) height);
		//return bboxMap.getLowerRight() funktioniert leider nicht. Bildschirm ruckelt und Map
		// wird noch mehr verzerrt!
	}

	public void setBBOXFeature(Coordinate upperLeft, Coordinate lowerRight) {
		bboxFeatures = new BBOX(upperLeft, lowerRight);
	}

	public Coordinate getFeatureUpperLeft() {
		return bboxFeatures.getUpperLeft();
	}

	public Coordinate getFeatureLowerRight() {
		return bboxFeatures.getLowerRight();
	}

	/**
	 * Get the center of the screen as georeferenced Coordinate.
	 * 
	 * @return the center Coordinate
	 */
	public Coordinate getMapCenter() {
		return pixelToCoordinate((float) width / 2f, (float) height / 2f);
	}
	
	// Returns distance between touched position (first pointer) and a given Coordinate
	public float touchedDistance(Coordinate c) {
		Pixel p = coordinateToPixel(c);
		Pixel t = new Pixel(firstPointerX, firstPointerY);
		return p.distance(t);
	}

	// Calculates pixel values from coordinates
	public Pixel coordinateToPixel(Coordinate coordinate) {
		return coordinateToPixel(coordinate.x, coordinate.y);
	}

	// Calculates pixel values from coordinates
	public Pixel coordinateToPixel(double x, double y) {
		return new Pixel((float) ((x - bboxMap.getUpperLeft().x) * scale), (float) ((bboxMap.getUpperLeft().y - y) * scale));
	}

	// Calculates coordinates from pixel values
	public Coordinate pixelToCoordinate(Pixel p) {
		return new Coordinate((double) (bboxMap.getUpperLeft().x + p.getCol() / scale),
				(double) (bboxMap.getUpperLeft().y - p.getRow() / scale));
	}

	// Calculates coordinates from pixel values
	public Coordinate pixelToCoordinate(float c, float r) {
		return new Coordinate((double) (bboxMap.getUpperLeft().x + c / scale), (double) (bboxMap.getUpperLeft().y - r / scale));
	}
}
