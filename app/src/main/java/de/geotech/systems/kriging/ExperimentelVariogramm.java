

package de.geotech.systems.kriging;

import java.util.ArrayList;
import java.util.List;


public class ExperimentelVariogramm {

	//Laenge eines Lag (lagWidth); Maximale Entefernung zwschen zwei Informationspunkte (maxDist)
	double lagWidth, maxDist;
	
	//Anzahl der Lag
	int numLags = 12;
	
	List<Double> distances = new ArrayList<Double>();
	List<LagPointPair> pairs = new ArrayList<LagPointPair>();
	List<Lag> lags = new ArrayList<Lag>();
	
	//Bezug der Punkte innerhalb des Einzugsbereich des Interpolationspunkt (Maximale Anzahl 100)
	public List<DataPoint> getPointsForBufferedVariogramm(DataPoint intPoint, List<DataPoint> valuePoints, int buffer) {
		
		List<DataPoint> requiredPoints = new ArrayList<DataPoint>();
		List<Double> distances = new ArrayList<Double>();
		
		//Ermittlung der Informationspunkte, welche im Einzugsbereich des Interpolationspunkt liegen
		for(int i=0; i<valuePoints.size(); i++) {
			double distance = Math.sqrt(Math.pow(intPoint.xCoordinate-valuePoints.get(i).xCoordinate, 2)+Math.pow(intPoint.yCoordinate-valuePoints.get(i).yCoordinate, 2));
			if(distance <= buffer) {
				requiredPoints.add(valuePoints.get(i));
				distances.add(distance);
			}
		}
		
		//Wenn die Anzahl der Informationspunkte ueber 100 betraegt
		int n = requiredPoints.size();
		if (n>100) {
			//Berechnung eines neuen Buffer in abhaengigkeit von der Flaeche und der Anzahl an Informationspunkten
			double areaBuffer = Math.PI*Math.pow(buffer, 2);
			double areaNewBuffer = 200*areaBuffer/requiredPoints.size();
			int newBuffer = (int) (Math.sqrt(areaNewBuffer/Math.PI));
			
			//Wenn der neue Buffer kleiner ist als der alte
			if (newBuffer<buffer) {
				
				requiredPoints.clear();
				distances.clear();
				
				//Ermittlung der Informationspunkte, welche im Einzugsbereich des Interpolationspunkt liegen
				for (int i = 0; i < valuePoints.size(); i++) {
					double distance = Math.sqrt(Math.pow(intPoint.xCoordinate - valuePoints.get(i).xCoordinate, 2) + Math.pow(intPoint.yCoordinate - valuePoints.get(i).yCoordinate, 2));
					if (distance <= newBuffer) {
						requiredPoints.add(valuePoints.get(i));
						distances.add(distance);
					}
				}
				
				//Bubbelsort
				n = requiredPoints.size();
				if (n > 100) {
					do {
						int newn = 1;
						for (int i = 0; i < n - 1; ++i) {
							if (distances.get(i) > distances.get(i + 1)) {
								double temp1 = distances.get(i);
								double temp2 = distances.get(i + 1);

								distances.set(i, temp2);
								distances.set(i + 1, temp1);

								DataPoint temp3 = requiredPoints.get(i);
								DataPoint temp4 = requiredPoints.get(i + 1);

								requiredPoints.set(i, temp4);
								requiredPoints.set(i + 1, temp3);

								newn = i + 1;
							}
						}
						n = newn;
					} while (n > 1);
					//Die 100 Informationspunkte, die am naechesten zum Interpolationspunkt liegen werden zurueckgegeben (wegen Laufzeit)
					return requiredPoints.subList(0, 100);
				} else
					return requiredPoints;
				
			} else {
				//Bubblesort
				do {
					int newn = 1;
					for (int i = 0; i < n - 1; ++i) {
						if (distances.get(i) > distances.get(i + 1)) {
							double temp1 = distances.get(i);
							double temp2 = distances.get(i + 1);

							distances.set(i, temp2);
							distances.set(i + 1, temp1);

							DataPoint temp3 = requiredPoints.get(i);
							DataPoint temp4 = requiredPoints.get(i + 1);

							requiredPoints.set(i, temp4);
							requiredPoints.set(i + 1, temp3);

							newn = i + 1;
						}
					}
					n = newn;
				} while (n > 1);
				//Die 100 Informationspunkte, die am naechesten zum Interpolationspunkt liegen werden zurueckgegeben (wegen Laufzeit)
				return requiredPoints.subList(0, 100);
			}
			
		}
		
		return requiredPoints;
	}
	
	//Ermittlung der Maximalen Distanz zwischen zwei Informationspunkten; Bildung von Punktpaaren (LagPointPair)
	void calculateMaxDistance (List<DataPoint> points) {
		
		for(int i=0; i<points.size(); i++)
		{
			for(int j=i+1; j<points.size(); j++)
			{
				double distance = Math.sqrt(Math.pow(points.get(i).xCoordinate-points.get(j).xCoordinate, 2)+Math.pow(points.get(i).yCoordinate-points.get(j).yCoordinate, 2));
				distances.add(distance);
				
				pairs.add(new LagPointPair(points.get(i).data, points.get(j).data, distance));
			}
		}
		
		for(int i=0; i<distances.size(); i++) {
			if(distances.get(i)>maxDist)
				maxDist = distances.get(i);
		}
	}

	//Ermittlung der Laenge eines Lag
	public void calculateLagWidth() {
		
		double max = Math.ceil(maxDist);
		lagWidth = max/numLags;
	}

	//Erzeugen der Lag; Setzen von Sart und Ende eines Lag; Zuordnug der Informationspunktpaare zu dem entsprechenden Lag
	public void calculateLags() {
		
		for(int i=0 ; i<numLags; i++){
			lags.add(new Lag());
		}
		
		//Setzen von Start und Ende
		double start = 0, end = 0;
		for(int i=0 ; i<numLags; i++){
			end += lagWidth;
			lags.get(i).lagStart = start;
			lags.get(i).lagEnd = end;
			lags.get(i).lagCenter= start+(lagWidth/2);
			start = end;
		}
		
		//Zuordnug der Informationspunktpaare
		for (int i=0; i<pairs.size(); i++)
		{
			for(int j=0 ; j<numLags; j++){
				//Wenn die Entfernung zwischen den beiden Punktparen innerhalb des Lag liegt wird das Paar dem Lag hinzugefuegt
				if(pairs.get(i).distance>lags.get(j).lagStart && pairs.get(i).distance<lags.get(j).lagEnd)
				{
					lags.get(j).pairs.add(pairs.get(i));
					break;
				}
			}
		}
	}

	//Ermittlung der Semivarianz fuer jedes Lag
	public void calculateSemiVariances() {
		
		for(int i=0 ; i<numLags; i++){
			//Ermittlung der Anzahl an Informationspunktpaaren
			double numberOfPairs = (double) lags.get(i).pairs.size();
			
			//Berechnung der summierten, quadrierten Differenzen der Zustandswerte (Gebauudezustaunde), der Informationspunktpaare innerhalb eines Lag
			double sumQuadraticDifferences = lags.get(i).sumQuadraticDifferences();
			
			//Berechnung der Semivarianz eines Lag
			double semiVariance = 1/(2*numberOfPairs)*sumQuadraticDifferences;
			
			lags.get(i).semiVariance = semiVariance;
			//System.out.println(lags.get(i).semiVariance);
		}
	}

	//Bezug aller Lag-Zentren (x-Achse)
	public double[] getLagCenters() {
		
		double[] centers = new double[numLags];
		
		for(int i=0 ; i<numLags; i++){
			centers[i] = lags.get(i).lagCenter;
		}
		
		return centers;
	}

	//Bezug aller Lag-Semivarianzen
	public double[] getLagSemiVariances() {

		double[] semiVariances = new double[numLags];
		
		for(int i=0 ; i<numLags; i++){
			semiVariances[i] = lags.get(i).semiVariance;
		}
		
		return semiVariances;
	}	
}
