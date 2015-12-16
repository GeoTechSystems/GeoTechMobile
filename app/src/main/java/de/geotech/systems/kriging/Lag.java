//***************************************************************************//
// Erstellt von: Simon Laun                                                  //
// Letzter Stand: 09.04.2015												 //
//																			 //
// Beschreibung:															 //
// Die Klasse Lag ist Zustaendig fuer die Datenhaltung der Lags. Sie enthaelt	 //
// alle relevanten Informationen eines Lags. Unteranderem gehoeren hierzu die //
// entsprechenden Punktpaare (Gebaeudepaare).								 //
//***************************************************************************//

package de.geotech.systems.kriging;

import java.util.ArrayList;
import java.util.List;


public class Lag {

	double lagStart, lagEnd, lagCenter, semiVariance;
	List<LagPointPair> pairs = new ArrayList<LagPointPair>();

	public double sumQuadraticDifferences() {
		double value = 0;
		
		for(int i=0; i<pairs.size(); i++)
		{
			value += Math.pow(pairs.get(i).data1-pairs.get(i).data2,2);
		}
		return value;
	}
}
