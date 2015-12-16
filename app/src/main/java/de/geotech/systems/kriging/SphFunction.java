//***************************************************************************//
// Erstellt von: Simon Laun                                                  //
// Letzter Stand: 09.04.2015												 //
//																			 //
// Beschreibung:															 //
// Die Klasse ExpFunction stellt die Exponentialfunktion sowie die 			 //
// entsprechenden partiellen Ableitungen fuer das exponentielle Variogramm 	 //
// bereit. Die Klasse ist eine Realisierung des Interface 					 //
// ParametricUnivariateFunction.											 //
//***************************************************************************//

package de.geotech.systems.kriging;

import org.apache.commons.math3.analysis.ParametricUnivariateFunction;


public class SphFunction implements ParametricUnivariateFunction {

    @Override
    public double[] gradient(double x, double... parameters) {

        double[] gradientVector = new double[3];

        //partielle Ableitungen
        gradientVector[0] = Math.pow(x,3)*Math.pow(parameters[2],3)/2-3*parameters[2]*x/2+1;
        gradientVector[1] = 3*x*parameters[1]*parameters[2]-Math.pow(x,3)*Math.pow(parameters[2],3)*parameters[1];
        gradientVector[2] = 3/2*parameters[0]*Math.pow(x,3)*Math.pow(parameters[2],2)-3*x*parameters[0]/2-3/2*Math.pow(x,3)*Math.pow(parameters[2],2)*Math.pow(parameters[1],2)+2*x*Math.pow(parameters[1],2)/2;
        return gradientVector;
    }

    @Override
    public double value(double x, double... parameters) {

        //Funktion fuer das sphaerische Modell mit Nuggeteffekt
        return 1/2*parameters[0]*Math.pow(x,3)*Math.pow(parameters[2],3)-3*parameters[0]*x*parameters[2]/2+parameters[0]-1/2*Math.pow(x,3)*Math.pow(parameters[2],3)*Math.pow(parameters[1],2)+3/2*x*parameters[2]*Math.pow(parameters[1],2);
    }

}
