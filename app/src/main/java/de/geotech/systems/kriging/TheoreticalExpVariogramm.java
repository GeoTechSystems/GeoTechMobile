//***************************************************************************//
// Erstellt von: Simon Laun                                                  //
// Letzter Stand: 09.04.2015												 //
//																			 //
// Beschreibung:															 //
// Die Klasse TheoreticalExpVariogramm fuehrt eine Anpassung, der in der 	 //
// Klasse ExpFunction breitgestellten Funktion, an das experimentelle  		 //
// Variogramm durch. Des Weiteren erfolgt hierdurch eine Ermittlung der 	 //
// Unbekannten (Sill, Nugget, Range) des theoretischen Variogramms.			 //
//***************************************************************************//

package de.geotech.systems.kriging;

//Ab 3.4 Commons Math kann LevenberMarquardt so ersetzt werden
/*import java.util.ArrayList;
import java.util.Collection;
import java.util.List;*/

import org.apache.commons.math3.optimization.fitting.CurveFitter;
import org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.stat.regression.SimpleRegression;

//Ab 3.4 Commons Math kann LevenberMarquardt so ersetzt werden
/*import org.apache.commons.math3.fitting.AbstractCurveFitter;
import org.apache.commons.math3.fitting.SimpleCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;*/

@SuppressWarnings("deprecation")
public class TheoreticalExpVariogramm {
	
	String typ = "Exp";
	double lagCenter[], semiVariances[], nugget, sill, range;
	
	public TheoreticalExpVariogramm(double[] lagCenter, double[] semiVariances) {
		
		this.lagCenter = lagCenter;
		this.semiVariances = semiVariances;
	}

	//Ermittlung von Nugget, Sill und Range
	public void calculateNuggetSillRange() {
		LevenbergMarquardtOptimizer optimizer = new LevenbergMarquardtOptimizer();
		CurveFitter<ExpFunction> fitter = new CurveFitter<ExpFunction>(optimizer);
		
		//Ab 3.4 Commons Math kann LevenberMarquardt so ersetzt werden
		//List<WeightedObservedPoint> observations = new ArrayList<WeightedObservedPoint>();
		
		//Hinzufuegen von Suetzpunkten, an welche die Funktion angepasst wird
		for(int i=0; i<lagCenter.length; i++) {
			fitter.addObservedPoint(lagCenter[i], semiVariances[i]);
			
			//Ab 3.4 Commons Math kann LevenberMarquardt so ersetzt werden
			//WeightedObservedPoint temp = new WeightedObservedPoint(1, lagCenter[i], semiVariances[i]);
			//observations.add(temp);
		}
		
		ExpFunction function = new ExpFunction();
		
		//Ermittlung von Startwerten fuer die Regression
		double[] initialGuess = calculateInitialGuess();
		
		//Ab 3.4 Commons Math kann LevenberMarquardt so ersetzt werden
		//SimpleCurveFitter fit = SimpleCurveFitter.create(function, initialGuess);
		//double[] result = fit.fit(observations);
		
		//Prozessierung der Regression mittels Levenberg Marquardt
		double[] result = fitter.fit(100, function, initialGuess);
		
		nugget = result[0];
		sill = result[1];
		range = result[2];	
	}

	//Ermittlung von Startwerten fuer die Regression
	private double[] calculateInitialGuess() {
		
		double[][] data = new double[lagCenter.length][2];
		double[] initialGuess = new double[3];
		
		//Zusammenstellen der Daten fuer eine lineare Regression
		for(int i=0; i<lagCenter.length; i++)
		{
			data[i][0] = lagCenter[i];
			data[i][1] = semiVariances[i];
		}
		
		SimpleRegression regression = new SimpleRegression();
		regression.addData(data);
		
		//Nugget = Y-Wert an der Stelle X=0; Sill = Y-Wert an der Stelle X=Zentrum des letzten Lag; 
		initialGuess[0] = regression.predict(0);
		initialGuess[1] = regression.predict(lagCenter[lagCenter.length-1]);
		initialGuess[2] = lagCenter[lagCenter.length-1];
		
		return initialGuess;
	}
}
