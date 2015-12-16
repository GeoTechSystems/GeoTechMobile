package de.geotech.systems.drawing;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Klasse ist Immutable.Hat nur Read-Only-Methoden
 * @author svenweisker
 *
 */
public class BBOX {

	private final Coordinate upperLeft, lowerRight;

	/**
	 * Default Konstruktor. Initialisiert Coordinate upperLeft,lowerRight mit x=0 und y=0
	 */
	public BBOX(){
		upperLeft = new Coordinate(0, 0);
		lowerRight= new Coordinate(0, 0);
	}
	
	
	public BBOX(Coordinate upperLeft, Coordinate lowerRight) {
		this.upperLeft = new Coordinate(upperLeft);
		this.lowerRight = new Coordinate(lowerRight);
	}
	
	
	public Coordinate getUpperLeft(){
		return new Coordinate(upperLeft);
	}
	
	public Coordinate getLowerRight(){
		return new Coordinate(lowerRight);
	}
}
