/**
 * Drawing Thread. Draws Features to Surface Canvas.
 * 
 * @author Sven Weisker (uucly@student.kit.edu) 
 * @author Torsten Hoch - tohoch@uos.de
 */
package de.geotech.systems.drawing;

import java.util.ArrayList;

import android.content.ContentValues;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.util.Log;
import android.view.SurfaceHolder;

import de.geotech.systems.editor.EditedFeatureDrawer;
import de.geotech.systems.features.Feature;
import de.geotech.systems.features.FeatureContainer;
import de.geotech.systems.projects.ProjectHandler;
import de.geotech.systems.wfs.WFSLayer;
import de.geotech.systems.wfs.WFSLayerAttributeTypes;

public class DrawingThread extends Thread {
	private static final String CLASSTAG = "DrawingThread";

	private SurfaceHolder surfaceHolder;
	private FeatureContainer featureContainer;
	private DrawingFeatureManager manager;
	private boolean draw;
	private boolean marker;
	private DrawingPanelView drawingPanel;

	/**
	 * Instantiates a new drawing thread.
	 *
	 * @param holder the holder
	 * @param panel the panel
	 * @param featureContainer the feature container
	 * @param marker the marker
	 */
	public DrawingThread(SurfaceHolder holder, DrawingPanelView newPanel, FeatureContainer featureContainer) {
		this.surfaceHolder = holder;
		this.featureContainer = featureContainer;
		this.manager = new DrawingFeatureManager(newPanel);
		this.drawingPanel = newPanel;
	}

	/**
	 * Running thread
	 */
	@Override
	public void run() {
		Canvas canvas = null;
		// Log.i(CLASSTAG, "Thread " + this.getId() + " with " + featureContainer.countAllFeatures() + " Features running from now...");
		setMarker();
		try {
			// Oberflaeche wird gelockt und an das lokale Objektvariable c uebergeben
			canvas = surfaceHolder.lockCanvas(null);
			if (canvas != null) {
				synchronized (surfaceHolder) {
					// Vor jedem neuen zeichnen der Feature, werden die Feature von der Oberflaeche geloescht
					// Falls gezeichnet werden soll, anderfalls c Oberflaeche wird geloescht
					canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
					if (draw) {
						if(drawingPanel.isInKrigingMode())
							manager.drawFeaturesForThread(canvas, featureContainer, false);
						else
							manager.drawFeaturesForThread(canvas, featureContainer, false);
					}
					// Log.i(CLASSTAG, "Alle " + featureContainer.countAllFeatures() + " Features im Thread " + this.getId() + " gemalt.");
				}
			}
		} catch (Exception e) {
			Log.e(CLASSTAG, "Something went wrong: \n" + e.getMessage().toString());
		} finally {
			// Canvas wird freigegeben und gezeigt
			if (canvas != null) {
				if (drawingPanel.isInEditingMode()) {
					drawingPanel.getEditFeatureToolbar().drawBar();
				}
				this.surfaceHolder.unlockCanvasAndPost(canvas);
			}
		}
	}

	/**
	 * Draw features or not..
	 *
	 * @param draw the draw
	 */
	public void drawFeatures(boolean draw) {
		this.draw = draw;
	}
	
	/**
	 * Sets the marker.
	 *
	 * @param marker the new marker
	 */
	private void setMarker() {
		if (ProjectHandler.isCurrentProjectSet()) {
			this.marker = ProjectHandler.getCurrentProject().isShowMarker();
		} else {
			this.marker = true;
		}
	}
	
	/**
	 * Sets the feature container.
	 *
	 * @param newFeatureContainer the new feature container
	 */
	public void setFeatureContainer(FeatureContainer newFeatureContainer) {
		this.featureContainer = newFeatureContainer;
	}

}
