//***************************************************************************//
// Erstellt von: Simon Laun                                                  //
// Letzter Stand: 09.04.2015												 //
//																			 //
// Beschreibung:															 //
// Die Klasse Kriging stellt die Funktionalitaeten fuer das Kriging bereit. 	 //
// Sie dient als Oberklasse fuer verschiedene Krigign-Verfahren.				 //
//***************************************************************************//

package de.geotech.systems.kriging;

import java.util.List;

import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;


public class Kriging {
	
	String variogrammTyp;
	double distanceData[][], calcPointDistance[], nugget, sill, range;
	int numOfPoints;
	RealMatrix A ,B, weightVector;
	List<DataPoint> points;
	DataPoint calcPoint ;
	
	public Kriging(double nugget, double sill, double range, String variogrmmTyp, List<DataPoint> points, DataPoint calcPoint) {
		this.nugget = nugget;
		this.sill = sill;
		this.range = range;
		this.variogrammTyp = variogrmmTyp;
		this.points = points;
		this.calcPoint = calcPoint;
	}
	
	//Berechnung der Distanzen zwischen den Informationspunkten; Berechnung der Distanzen zwischen den Informationspunkten und dem Interpolationspunkt
	public void calculateDistances() {
		
		numOfPoints = points.size();
		distanceData = new double[numOfPoints][numOfPoints];
		calcPointDistance = new double[numOfPoints];

		//Berechnung der Distanzen zwischen den Informationspunkten
		for (int i = 0; i < numOfPoints; i++) {
			for (int j = 0; j < numOfPoints; j++) {

				distanceData[i][j] = Math.sqrt(Math.pow(points.get(i).xCoordinate - points.get(j).xCoordinate, 2) + Math.pow(points.get(i).yCoordinate - points.get(j).yCoordinate, 2));
			}
		}
			
		//Berechnung der Distanzen zwischen den Informationspunkten und dem Interpolationspunkt
		for (int i = 0; i < numOfPoints; i++)
		{
			calcPointDistance[i] = Math.sqrt(Math.pow(calcPoint.xCoordinate - points.get(i).xCoordinate, 2) + Math.pow(calcPoint.yCoordinate - points.get(i).yCoordinate, 2));
		}
	}

	//Berechung der Semivarianz in Abhaengigkeit einer spezifischen Distanz
	protected double solveFunction(double x) {
		switch (variogrammTyp) {
		case "Gauss":
			return nugget
					+sill-sill*Math.exp(-3*Math.pow(x,2)/Math.pow(range,2))
					-nugget+nugget*Math.exp(-3*Math.pow(x,2)/Math.pow(range,2));
		case "Exp":
			return nugget+sill-sill*Math.exp(-3*x/range)-nugget+nugget*Math.exp(-3*x/range);
		}
		return 0;
	}
	
	//Berechnung des Gewichtsvektor
	public void calculateWeightVector() {
		
		weightVector = new LUDecomposition(A).getSolver().getInverse().multiply(B);
	}
	
	//Berechnung des Zustandswertes (Gebaeudezustand) des Interpolationspunktes
	public double calculateIntValue() {
		double value = 0;
		
		for(int i = 0; i<numOfPoints; i++) {
			value = value + weightVector.getEntry(i, 0) * points.get(i).data;
		}
			
		return value;
	}
}
