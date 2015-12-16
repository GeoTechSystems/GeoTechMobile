//***************************************************************************//
// Erstellt von: Simon Laun                                                  //
// Letzter Stand: 09.04.2015												 //
//																			 //
// Beschreibung:															 //
// Die Klasse UniversalKriging ist von der Klasse Kriging abgeleitet. 		 //
// Zusaetzlich zu den Funktionaltitaeten der Klasse Kriging, bietet diese 	 //
// Klasse eine Funktion zum aufstellen der Komponenten des Krigesystems fuer  //
// das Universal Kriging.		 											 //
//***************************************************************************//

package de.geotech.systems.kriging;

import java.util.List;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;


public class UniversalKriging extends Kriging {

	public UniversalKriging(double nugget, double sill, double range,
			String variogrmmTyp, List<DataPoint> points, DataPoint calcPoint) {
		super(nugget, sill, range, variogrmmTyp, points, calcPoint);
		// TODO Auto-generated constructor stub
	}

	//Ermittlung des Krige-Systems
	public void calculateKrigeSystem() {
		
		double[][] semiVarianceData = new double[numOfPoints+3][numOfPoints+3];
		double[][] calcSemiVarianceData = new double[numOfPoints+3][1];
		
		//Ermittlung der Krige-Matrix
		for (int i = 0; i <= numOfPoints+2; i++) {
			for (int j = 0; j <= numOfPoints+2; j++) {
				if(i < numOfPoints && j < numOfPoints) {
					semiVarianceData[i][j] = solveFunction(distanceData[i][j]);
				}
				else if(j == numOfPoints && i < numOfPoints) {
					semiVarianceData[i][j] = points.get(i).xCoordinate;
				}
				else if(i == numOfPoints && j < numOfPoints) {
					semiVarianceData[i][j] = points.get(j).xCoordinate;
				}
				else if(i >= numOfPoints && j >= numOfPoints) {
					semiVarianceData[i][j] = 0;
				}
				else if(j == (numOfPoints+1) && i < numOfPoints) {
					semiVarianceData[i][j] = points.get(i).yCoordinate;
				}
				else if(i == (numOfPoints+1) && j < numOfPoints) {
					semiVarianceData[i][j] = points.get(j).yCoordinate;
				}
				else if(j == (numOfPoints+2) && i < numOfPoints) {
					semiVarianceData[i][j] = 1;
				}
				else if(i == (numOfPoints+2) && j < numOfPoints) {
					semiVarianceData[i][j] = 1;
				}
			}	
		}
		
		//Ermittlung des Vektors mit den Semivarianzen in Abhaengigkeit der Distanzen zwischen Informationspunkten und Interpolationspunkt
		for (int i = 0; i <= numOfPoints+2; i++) {
			if(i < numOfPoints) {
				calcSemiVarianceData[i][0] = solveFunction(calcPointDistance[i]);
			}
			else if(i == numOfPoints) {
				calcSemiVarianceData[i][0] = calcPoint.xCoordinate;
			}
			else if(i == (numOfPoints+1)) {
				calcSemiVarianceData[i][0] = calcPoint.yCoordinate;
			}
			else if(i == (numOfPoints+2)) {
				calcSemiVarianceData[i][0] = 1;
			}
		}
		A = new Array2DRowRealMatrix(semiVarianceData);
		B = new Array2DRowRealMatrix(calcSemiVarianceData);
	}
}
