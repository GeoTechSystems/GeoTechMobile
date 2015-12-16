//***************************************************************************//
// Erstellt von: Simon Laun                                                  //
// Letzter Stand: 09.04.2015												 //
//																			 //
// Beschreibung:															 //
// Die Klasse GaussFunction stellt die Gaussfunktion sowie die 				 //
// entsprechenden partiellen Ableitungen fuer das gauss sche Variogramm 		 //
// bereit. Die Klasse ist eine Realisierung des Interface 					 //
// ParametricUnivariateFunction.											 //
//***************************************************************************//

package de.geotech.systems.kriging;

import org.apache.commons.math3.analysis.ParametricUnivariateFunction;


public class GaussFunction implements ParametricUnivariateFunction {

	@Override
	public double[] gradient(double x, double... parameters) {
		double[] gradientVector = new double[3];

		//partielle Ableitungen		
		gradientVector[0] = Math.exp(-3*Math.pow(x,2)/Math.pow(parameters[2],2));
		gradientVector[1] = 1-Math.exp(-3*Math.pow(x,2)/Math.pow(parameters[2],2));
		gradientVector[2] = -6*parameters[1]*Math.pow(x,2)*Math.exp(-3*Math.pow(x,2)/Math.pow(parameters[2],2))/Math.pow(parameters[2],3)
							+6*parameters[0]*Math.pow(x,2)*Math.exp(-3*Math.pow(x,2)/Math.pow(parameters[2],2))/Math.pow(parameters[2],3);

		return gradientVector;
	}
	
	@Override
	public double value(double x, double... parameters) {
		
		//Funktion fuer das exponentielle Modell mit Nuggeteffekt
		return parameters[0]
				+parameters[1]-parameters[1]*Math.exp(-3*Math.pow(x,2)/Math.pow(parameters[2],2))
				-parameters[0]+parameters[0]*Math.exp(-3*Math.pow(x,2)/Math.pow(parameters[2],2));
	}
}
