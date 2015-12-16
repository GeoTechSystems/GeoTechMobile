//***************************************************************************//
// Erstellt von: Simon Laun                                                  //
// Letzter Stand: 09.04.2015												 //
//																			 //
// Beschreibung:															 //
// Die Klasse LagPointPair dient der paarweisen Zuordnung von zwei Objekten  //
// des Typs DataPoint. Die Zuordnung erfolgt in Abhaengigkeit des 			 //
// entsprechenden Lags.														 //
//***************************************************************************//

package de.geotech.systems.kriging;

public class LagPointPair {
	double distance;
	int data1, data2;
	
	public LagPointPair(int d1, int d2, double dist)
	{
		data1 = d1;
		data2 = d2;
		distance = dist;
	}
}
