//***************************************************************************//
// Erstellt von: Simon Laun                                                  //
// Letzter Stand: 09.04.2015												 //
//																			 //
// Beschreibung:															 //
// Die Klasse KrigingToolbar dient der Erzeugung einer Toolbar, mit welcher  //
// die Funktionen des Kriging auf mobielen Endgeraeten genutzt werden koennen. //
// Die Klasse kann als Hauptprogramm fuer das Kriging interpretiert werden.   //
//***************************************************************************//

package de.geotech.systems.kriging;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.AsyncTask;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import de.geotech.systems.R;
import de.geotech.systems.drawing.DrawingPanelView;
import de.geotech.systems.features.Feature;
import de.geotech.systems.main.LeftMenuIconLayout;
import de.geotech.systems.projects.ProjectHandler;
import de.geotech.systems.utilities.Alerts;
import de.geotech.systems.utilities.StatusInfo;
import de.geotech.systems.wfs.WFSLayer;


public class KrigingToolbar {
	
	private Context context;
	private DrawingPanelView drawingPanel;
	private LinearLayout toolbarLayout;
	private Spinner variogrammType;
	private Spinner krigingMethode;
	public ArrayList<Feature> featureContainer;
	
	private class CalculaterTask extends AsyncTask<Void, StatusInfo, Boolean> {

		ProgressDialog dialog;
		Context taskContext;
		
		//Initialisierung der Progressbar fuer die Berechnung
		public CalculaterTask() {
			taskContext = context;
			dialog = new ProgressDialog(taskContext);
			dialog.setCancelable(true);
			dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			    @Override
			    public void onCancel(DialogInterface dialog)
			    {
			         cancel(true);
			    }
			});
			
			dialog.setTitle("Berechnung");
			dialog.setMessage("Die Berechnung erfolgt. Bitte warten...");
			dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			dialog.setCanceledOnTouchOutside(false);
			dialog.setMax(countIntData());
		}
		
		@Override
		protected void onPreExecute() {
			dialog.show();
		}
		
		private int countIntData() {
			int counter = 0;
			
			if(!featureContainer.isEmpty()) {
				
				for(int i = 0; i<featureContainer.size(); i++) {
					
					ContentValues temp2 = featureContainer.get(i).getAttributes();
					if(temp2.getAsInteger("isvalue") == 0) 
						counter++;		
				}
			}
			return counter;
		}

		//Hauptprogramm
		@Override
		protected Boolean doInBackground(Void... params) {
	
			List<DataPoint> valuePoints = new ArrayList<DataPoint>();
			List<DataPoint> interpolationPoints = new ArrayList<DataPoint>();
			
			//Beziehen der Informations- sowie Interpolationspunkte
			for(int i = 0; i<featureContainer.size(); i++) {
				
				if (isCancelled()) break;

				DataPoint temp1 = new DataPoint();
				ContentValues temp2 = featureContainer.get(i).getAttributes();
			
				temp1.region = temp2.getAsString("area");
				temp1.date = temp2.getAsString("date");
				temp1.typ = temp2.getAsString("type");
				temp1.data = temp2.getAsInteger("realdata");
				temp1.intData = temp2.getAsInteger("estimationdata");
				temp1.isValuePoint = temp2.getAsInteger("isvalue");
				temp1.xCoordinate = Double.parseDouble(temp2.getAsString("xcoordinate"));
				temp1.yCoordinate = Double.parseDouble(temp2.getAsString("ycoordinate"));
				temp1.id = i;
				
				//Entscheidung zwischen Informationspunkt und Interpolationspunkt
				switch (temp1.isValuePoint) {
				case 0:
					interpolationPoints.add(temp1);
					break;
				
				case 1:
					valuePoints.add(temp1);
					break;
				}
			}
			
			//Beziehen des Variogramm-Modells und der Kriging-Methode
			String controlerVariogramm = variogrammType.getSelectedItem().toString();
			String controlerKriging = krigingMethode.getSelectedItem().toString();
			
			//Mindestanzahl der Informationspunkte
			int minRequiredPoints = 15;
			
			//Alle Anweisungen innerhalb der Schleife sollen fuer alle Interpolationspunkte durchgefuehrt werden
			for(int i = 0; i<interpolationPoints.size(); i++) {
				
				if (isCancelled()) break;
				
				//Radius (Einzugsbereich) um den jeweiligen Interpolationspunkt. Alle Informationspunkte innerhalb des Radius gehen in die Schaetzung des Interpolationspunkt ein.
				int buffer = 1000;
				
				ExperimentelVariogramm eVariogramm = new ExperimentelVariogramm();
				
				//Bezug der Punkte innerhalb des Einzugsbereich des Interpolationspunkt (Maximale Anzahl 100)
				List<DataPoint> requiredPoints = eVariogramm.getPointsForBufferedVariogramm(interpolationPoints.get(i), valuePoints, buffer);
				
				if(requiredPoints.size() >= minRequiredPoints) {
					
					//Ermittlung der Maximalen Distanz zwischen zwei Informationspunkten
					eVariogramm.calculateMaxDistance(requiredPoints);
					
					//Ermittlung der Laenge eines Lag
					eVariogramm.calculateLagWidth();
					
					//Erzeugen der Lag; Setzen von Sart und Ende eines Lag; Zuordnug der Informationspunktpaare zu dem entsprechenden Lag
					eVariogramm.calculateLags();
					
					//Ermittlung der Semivarianz fuer jedes Lag
					eVariogramm.calculateSemiVariances();
			
					//Steuerung ob exponentielles oder gauss'sche Variogramm
					switch (controlerVariogramm) {
					case "Sphaerisch":
						//Initialisierung des theoretischen exponentiellen Variogramms
						TheoreticalSphVariogramm sph = new TheoreticalSphVariogramm(eVariogramm.getLagCenters(), eVariogramm.getLagSemiVariances());

						//Try Catch Bolck fuer den Fall dass der Levenberg Marquardt Algorithmus keine Konvergenz erziehlt
						try {
							//Ermittlung von Nugget, Sill und Range
							sph.calculateNuggetSillRange();

							//Steuerung ob Universal Kriging oder Ordinary Kriging
							switch (controlerKriging) {
								case "Universal":
									UniversalKriging uKriging = new UniversalKriging(sph.nugget, sph.sill, sph.range, sph.typ, requiredPoints, interpolationPoints.get(i));

									//Berechnung der Distanzen zwischen den Informationspunkten; Berechnung der Distanzen zwischen den Informationspunkten und dem Interpolationspunkt
									uKriging.calculateDistances();

									//Ermittlung des Krige-Systems
									uKriging.calculateKrigeSystem();

									//Berechnung des Gewichtsvektor
									uKriging.calculateWeightVector();

									//Berechnung des Zustandswertes (Gebaeudezustand) des Interpolationspunktes
									interpolationPoints.get(i).intData = (int) Math.round(uKriging.calculateIntValue());
									break;
								case "Ordinary":
									OrdinaryKriging oKriging = new OrdinaryKriging(sph.nugget, sph.sill, sph.range, sph.typ, requiredPoints, interpolationPoints.get(i));

									//Berechnung der Distanzen zwischen den Informationspunkten; Berechnung der Distanzen zwischen den Informationspunkten und dem Interpolationspunkt
									oKriging.calculateDistances();

									//Ermittlung des Krige-Systems
									oKriging.calculateKrigeSystem();

									//Berechnung des Gewichtsvektor
									oKriging.calculateWeightVector();

									//Berechnung des Zustandswertes (Gebaeudezustand) des Interpolationspunktes
									interpolationPoints.get(i).intData = (int) Math.round(oKriging.calculateIntValue());

									break;
							}
						} catch (Exception e) {
							//Der Punkt wird als nicht klassifizierbar gekennzeichnet, wenn keine Konvergez erziehlt wird
							interpolationPoints.get(i).hasConvergence = false;
							interpolationPoints.get(i).intData=0;
						}
						break;
					case "Exponentiell":
						//Initialisierung des theoretischen exponentiellen Variogramms
						TheoreticalExpVariogramm exp = new TheoreticalExpVariogramm(eVariogramm.getLagCenters(), eVariogramm.getLagSemiVariances());

						//Try Catch Bolck fuer den Fall dass der Levenberg Marquardt Algorithmus keine Konvergenz erziehlt
						try {
							//Ermittlung von Nugget, Sill und Range
							exp.calculateNuggetSillRange();
							
							//Steuerung ob Universal Kriging oder Ordinary Kriging
							switch (controlerKriging) {
							case "Universal":
								UniversalKriging uKriging = new UniversalKriging(exp.nugget, exp.sill, exp.range, exp.typ, requiredPoints, interpolationPoints.get(i));
						
								//Berechnung der Distanzen zwischen den Informationspunkten; Berechnung der Distanzen zwischen den Informationspunkten und dem Interpolationspunkt
								uKriging.calculateDistances();
						
								//Ermittlung des Krige-Systems
								uKriging.calculateKrigeSystem();
						
								//Berechnung des Gewichtsvektor
								uKriging.calculateWeightVector();
						
								//Berechnung des Zustandswertes (Gebaeudezustand) des Interpolationspunktes
								interpolationPoints.get(i).intData = (int) Math.round(uKriging.calculateIntValue());
								break;
							case "Ordinary":
								OrdinaryKriging oKriging = new OrdinaryKriging(exp.nugget, exp.sill, exp.range, exp.typ, requiredPoints, interpolationPoints.get(i));
								
								//Berechnung der Distanzen zwischen den Informationspunkten; Berechnung der Distanzen zwischen den Informationspunkten und dem Interpolationspunkt
								oKriging.calculateDistances();
								
								//Ermittlung des Krige-Systems
								oKriging.calculateKrigeSystem();
								
								//Berechnung des Gewichtsvektor
								oKriging.calculateWeightVector();
								
								//Berechnung des Zustandswertes (Gebaeudezustand) des Interpolationspunktes
								interpolationPoints.get(i).intData = (int) Math.round(oKriging.calculateIntValue());
								
								break;	
							}
						} catch (Exception e) {
							//Der Punkt wird als nicht klassifizierbar gekennzeichnet, wenn keine Konvergez erziehlt wird
							interpolationPoints.get(i).hasConvergence = false;
							interpolationPoints.get(i).intData=0;
						}
						break;
					case "Gauss":
						//Initialisierung des theoretischen gauss'schen Variogramms
						TheoreticalGaussVariogramm gauss = new TheoreticalGaussVariogramm(eVariogramm.getLagCenters(), eVariogramm.getLagSemiVariances());
						
						//Try Catch-Bolck fuer den Fall, dass der Levenberg Marquardt Algorithmus keine Konvergenz erziehlt
						try {
							//Ermittlung von Nugget, Sill und Range
							gauss.calculateNuggetSillRange();
							
							//Steuerung ob exponentielles oder gauss'sche Variogramm
							switch (controlerKriging) {
							case "Universal":
								UniversalKriging uKriging = new UniversalKriging(gauss.nugget, gauss.sill, gauss.range, gauss.typ, requiredPoints, interpolationPoints.get(i));
						
								//Berechnung der Distanzen zwischen den Informationspunkten; Berechnung der Distanzen zwischen den Informationspunkten und dem Interpolationspunkt
								uKriging.calculateDistances();
						
								//Ermittlung des Krige-Systems
								uKriging.calculateKrigeSystem();
						
								//Berechnung des Gewichtsvektor
								uKriging.calculateWeightVector();
						
								//Berechnung des Zustandswertes (Gebaeudezustand) des Interpolationspunktes
								interpolationPoints.get(i).intData = (int) Math.round(uKriging.calculateIntValue());
								break;
							case "Ordinary":
								OrdinaryKriging oKriging = new OrdinaryKriging(gauss.nugget, gauss.sill, gauss.range, gauss.typ, requiredPoints, interpolationPoints.get(i));
								
								//Berechnung der Distanzen zwischen den Informationspunkten; Berechnung der Distanzen zwischen den Informationspunkten und dem Interpolationspunkt
								oKriging.calculateDistances();
								
								//Ermittlung des Krige-Systems
								oKriging.calculateKrigeSystem();
								
								//Berechnung des Gewichtsvektor
								oKriging.calculateWeightVector();
								
								//Berechnung des Zustandswertes (Gebaeudezustand) des Interpolationspunktes
								interpolationPoints.get(i).intData = (int) Math.round(oKriging.calculateIntValue());
								break;
							}
						} catch (Exception e) {
							//Der Punkt wird als nicht klassifizierbar gekennzeichnet, wenn keine Konvergez erziehlt wird
							interpolationPoints.get(i).hasConvergence = false;
							interpolationPoints.get(i).intData=0;
						}
						break;
					}

					//Abfangen der Moeglichkeit, dass einem Interpolationspunkt ein Zustandswert (Gebaeudezustand) kleiner 1 oder groesser 5 zugeordnet wird (wenn kleiner 1 dann 1 wenn groesser 5 dann 5)
					if(interpolationPoints.get(i).intData<=1)
						//Abfangen der Moeglichkeit, dass ein Inrerpolationspunkt als nich klassifizierbar gekennzeichnet wurde, da keine Konvergenz erziehlt wurde
						if(interpolationPoints.get(i).intData==0 && interpolationPoints.get(i).hasConvergence == false)
							interpolationPoints.get(i).intData=0;
						else
							interpolationPoints.get(i).intData=1;
					else if(interpolationPoints.get(i).intData>=5)
						interpolationPoints.get(i).intData=5;
				} else
					//Wenn zu wenig Informationspunkte vorliegen wird der Interpolationspunkt als nicht klassifizierbar gekennzeichnet
					interpolationPoints.get(i).intData=0;
				
				dialog.incrementProgressBy(1);
				//publishProgress((int) i*100/interpolationPoints.size());
			}
			for(int i = 0; i<interpolationPoints.size(); i++) {
				
				if (isCancelled()) break;
				
				ContentValues contentValues = featureContainer.get(interpolationPoints.get(i).id).getAttributes();
				contentValues.put("estimationdata", interpolationPoints.get(i).intData);
				featureContainer.get(interpolationPoints.get(i).id).setAttributes(contentValues);
				//Log.i("allo", featureContainer.get(interpolationPoints.get(i).id).getAttributes().getAsString("isvalue"));
				setColor(interpolationPoints.get(i).id);
			}
			return true;		
		}
		
		//Statusupdate fuer die Progressbar
		@Override
		protected void onProgressUpdate(StatusInfo... status) {
			int count = status[0].getStatus();
			dialog.setProgress(count);
			if (count < 0) {
				Alerts.errorMessage(taskContext, "Error!", status[0].getMessage()).show();
			}
		}
		
		//Anzeige wenn die Berechnung beendet ist
		@Override
		protected void onPostExecute(Boolean result) {
			dialog.cancel();
			Alerts.errorMessage(taskContext,"Message","Berechnung beendet").show();
		}
		
		//Setzen der spezifischen Farbe in abhaengigkeit des Interpolationsergebnisses
		protected void setColor(int id) {

			switch (featureContainer.get(id).getAttributes().getAsInteger("estimationdata")) {
			case 0:
				featureContainer.get(id).setColor(Color.rgb(149,136,121));
				break;
			case 1:
				featureContainer.get(id).setColor(Color.rgb(92,229,71));
				break;
			case 2:
				featureContainer.get(id).setColor(Color.rgb(255,239,61));
				break;
			case 3:
				featureContainer.get(id).setColor(Color.rgb(255,197,61));
				break;
			case 4:
				featureContainer.get(id).setColor(Color.rgb(255,166,61));
				break;
			case 5:
				featureContainer.get(id).setColor(Color.rgb(255,118,115));
				break;
			default: 
				break;
			}
		}
	}
	
	public KrigingToolbar(DrawingPanelView drawingPanel, LinearLayout toolbarLayout) {
	
		this.context = drawingPanel.getContext();
		this.drawingPanel = drawingPanel;
		this.toolbarLayout = toolbarLayout;
		this.toolbarLayout.setGravity(Gravity.CENTER);
		this.toolbarLayout.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
		this.toolbarLayout.setDividerPadding(5);
		this.toolbarLayout.setDividerDrawable(this.context.getResources().getDrawable(R.drawable.divider));
		this.variogrammType = new Spinner(context);
		this.krigingMethode = new Spinner(context);
	}
	
	//Oeffnen der Kriging-Toolbar
	public boolean openBar(){
		if(this.abort()) {
			return false;
		}
		
		if (toolbarLayout != null) {
			this.toolbarLayout.removeAllViews();
		}
		
		//Beziehen des featureContainers
		for (WFSLayer layer : ProjectHandler.getCurrentProject().getWFSContainer()) {
			if (layer.isActive()) {
				featureContainer = layer.getFeatureContainer();
			}
		}

		if(!featureContainer.get(0).getAttributes().containsKey("isvalue") || !featureContainer.get(0).getAttributes().containsKey("realdata")) {
			AlertDialog.Builder builder = new AlertDialog.Builder(context);

			builder.setMessage("The data used are the wrong structure")
					.setTitle("Error");

			builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			});

			AlertDialog dialog = builder.create();
			dialog.show();
			closeBar();
			return false;
		}

		//Setzen der Farben fuer die Informationspunkte
		setColorForInfoPoint();
		drawingPanel.setKrigingMode(true);
		
		//Hinzufuegen eines Schliesssen-Buttons zur Toolbar
		this.addCloseButton();
		
		//Hinzufuegen eines Spinners zur Toolbar (Variogramm-Modell)
		List<String> variogramm = new ArrayList<String>();
		variogramm.add("Exponentiell");
		variogramm.add("Gauss");
		variogramm.add("Sphaerisch");
		this.addSpinner(variogramm, variogrammType);
		
		//Hinzufuegen eines Spinners zur Toolbar (Kriging-Methode)
		List<String> kriging = new ArrayList<String>();
		kriging.add("Ordinary");
		kriging.add("Universal");
		this.addSpinner(kriging, krigingMethode);
		
		////Hinzufuegen eines Berechnungs-Buttons zur Toolbar
		this.addCalcButton();
		
		//Toolbar wird sichtbar
		toolbarLayout.setVisibility(View.VISIBLE);

		return true;
	}
	
	//Ueberpruefung des Status
	public boolean abort() {
		if (ProjectHandler.getCurrentProject() == null) {
			return true;
		} else if (ProjectHandler.getCurrentProject().getWFSContainer() == null) {
			return true;
		} else {
			boolean anyActiveLayer = false;
			for (WFSLayer layer : ProjectHandler.getCurrentProject().getWFSContainer()) {
				if (layer.isActive()) {
					anyActiveLayer = true;
				}
			}
			if (!anyActiveLayer) {
				return true;
			}
		}
		// wenn schon im KrigingMode, dann abbrechen
		if (drawingPanel.isInKrigingMode()) {
			return true;
		}
		return false;
	}
	
	//Schliessen der Toolbar
	public void closeBar() {
		this.toolbarLayout.setVisibility(View.GONE);
		this.drawingPanel.setKrigingMode(false);
		this.drawingPanel.reloadFeaturesAndDraw();
	}
	
	//Hinzufuegen eines Schliessen-Buttons zur Toolbar
	public void addCloseButton() {	
		
		toolbarLayout.addView(new LeftMenuIconLayout(context,
				context.getString(R.string.close), Color.BLACK,
				R.drawable.close_toolbar, 250, 10, new OnClickListener() {
			
			//Ausfuehren wenn Button betaetigt wird
			@Override
			public void onClick(View v) {
				drawingPanel.setKrigingMode(false);
				closeBar();
			}
		}));
	}
	
	//Hinzufuegen eines Berechnungs-Buttons zur Toolbar
	public void addCalcButton() {
		
		toolbarLayout.addView(new LeftMenuIconLayout(context,
				context.getString(R.string.calculate), Color.BLACK,
				R.drawable.calculate, 250, 10, new OnClickListener() {
			@Override
			
			//Ausfuehren wenn Button betaetigt wird
			public void onClick(View v) {
				if(!featureContainer.isEmpty()) {
					new CalculaterTask().execute();	
				}
			}
		}));	
	}
	
	//Hinzufuegen eines Spinners zur Toolbar
	public void addSpinner(List<String> list, Spinner spinner) {
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(context,R.layout.spinner_item_toolbar, list);
		dataAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_toolbar);
		
		spinner.setAdapter(dataAdapter);
		spinner.setMinimumHeight(100);
		
		spinner.setMinimumWidth(180);
		toolbarLayout.addView(spinner);
	}

	//Setzen der Farbe fuer die Informationspunkte entsprechend der Klassifizierung
	public void setColorForInfoPoint() {
		for(int i = 0; i<featureContainer.size(); i++) {
			if(featureContainer.get(i).getAttributes().getAsInteger("isvalue") == 1) {
				switch (featureContainer.get(i).getAttributes().getAsInteger("realdata")) {
				case 1:
					featureContainer.get(i).setColor(Color.rgb(30,220,0));
					break;
				case 2:
					featureContainer.get(i).setColor(Color.rgb(255,233,0));
					break;
				case 3:
					featureContainer.get(i).setColor(Color.rgb(255,179,0));
					break;
				case 4:
					featureContainer.get(i).setColor(Color.rgb(255,197,61));
					break;
				case 5:
					featureContainer.get(i).setColor(Color.rgb(255,33,27));
					break;
				default: 
					break;
				}
			}
		}
	}
}


