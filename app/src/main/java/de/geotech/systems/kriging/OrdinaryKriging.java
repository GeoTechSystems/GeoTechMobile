//***************************************************************************//
// Erstellt von: Simon Laun                                                  //
// Letzter Stand: 09.04.2015												 //
//																			 //
// Beschreibung:															 //
// Die Klasse OrdinaryKriging ist von der Klasse Kriging abgeleitet. 		 //
// Zusaetzlich zu den Funktionaltitaeten der Klasse Kriging, bietet diese 	 //
// Klasse eine Funktion zum aufstellen der Komponenten des Krigesystems fuer  //
// das Ordinary Kriging.													 //
//***************************************************************************//

package de.geotech.systems.kriging;

import java.util.List;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;


public class OrdinaryKriging extends Kriging{
	
	public OrdinaryKriging(double nugget, double sill, double range,
			String variogrmmTyp, List<DataPoint> points, DataPoint calcPoint) {
		super(nugget, sill, range, variogrmmTyp, points, calcPoint);
		// TODO Auto-generated constructor stub
	}
	
	//Ermittlung des Krige-Systems
	void calculateKrigeSystem() {
		
		double[][] semiVarianceData = new double[numOfPoints+1][numOfPoints+1];
		double[][] calcSemiVarianceData = new double[numOfPoints+1][1];
		
		//Ermittlung der Krige-Matrix
		for (int i = 0; i <= numOfPoints; i++) {
			for (int j = 0; j <= numOfPoints; j++) {
				if(i < numOfPoints && j < numOfPoints) {
					semiVarianceData[i][j] = solveFunction(distanceData[i][j]);
				}
				else if((i < numOfPoints && j == numOfPoints)) {
					semiVarianceData[i][j] = 1;
				}
				else if (i == numOfPoints && j < numOfPoints) {
					semiVarianceData[i][j] = 1;
				}
				else if(i == numOfPoints && j == numOfPoints) {
					semiVarianceData[i][j] = 0;
				}
			}	
		}
		
		//Ermittlung des Vektors mit den Semivarianzen in Abhaengigkeit der Distanzen zwischen Informationspunkten und Interpolationspunkt
		for (int i = 0; i <= numOfPoints; i++) {
			if(i < numOfPoints) {
				calcSemiVarianceData[i][0] = solveFunction(calcPointDistance[i]);
			}
			else if(i == numOfPoints) {
				calcSemiVarianceData[i][0] = 1;
			}
		}
		
		A = new Array2DRowRealMatrix(semiVarianceData);
		B = new Array2DRowRealMatrix(calcSemiVarianceData);
	}
}
