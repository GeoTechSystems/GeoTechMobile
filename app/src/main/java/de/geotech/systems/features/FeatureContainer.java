/**
 * Container for all current drawn Features
 * 
 * @author Torsten Hoch - tohoch@uos.de
 */

package de.geotech.systems.features;

import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Envelope;

import de.geotech.systems.wfs.WFSLayer;

public class FeatureContainer {
	private static final String CLASSTAG = "FeatureContainer";
	private ArrayList<Feature> pointFeatures;
	private ArrayList<Feature> lineFeatures;
	private ArrayList<Feature> polygonFeatures;
	private Feature currentHiddenEditedFeature;

	/**
	 * Instantiates a new feature container.
	 */
	public FeatureContainer() {
		pointFeatures = new ArrayList<Feature>();
		lineFeatures = new ArrayList<Feature>();
		polygonFeatures = new ArrayList<Feature>();
	}

	/**
	 * Adds any feature with given type into its correct Geometry
	 * List (point, line, polygon).
	 *
	 * @param feature the feature
	 * @return true, if successful
	 */
	public boolean addFeature(Feature feature) {
		if (currentHiddenEditedFeature != null) {
			if (currentHiddenEditedFeature.getFeatureID() == feature.getFeatureID()
					&& currentHiddenEditedFeature.getWFSlayerID() == feature.getWFSlayerID()) {
				return false;
			} else {
				return addNow(feature);
			}
		} else {
			return addNow(feature);
		}
	}

	private boolean addNow(Feature feature) {
		switch (feature.getFeatureType()) {
		case WFSLayer.LAYER_TYPE_POINT:
			this.addPointFeature(feature);
			return true;
		case WFSLayer.LAYER_TYPE_LINE:
			this.addLineFeature(feature);
			return true;
		case WFSLayer.LAYER_TYPE_POLYGON:
			this.addPolygonFeature(feature);
			return true;
		default:
			return false;
		}
	}
	/**
	 * Adds the point feature.
	 *
	 * @param f the f
	 */
	private void addPointFeature(Feature f) {
		pointFeatures.add(f);
	}

	/**
	 * Adds the line feature.
	 *
	 * @param f the f
	 */
	private void addLineFeature(Feature f) {
		lineFeatures.add(f);
	}

	/**
	 * Adds the polygon feature.
	 *
	 * @param f the f
	 */
	private void addPolygonFeature(Feature f) {
		polygonFeatures.add(f);
	}

	/**
	 * Counts all features in the whole Container.
	 *
	 * @return the int
	 */
	public int countAllFeatures() {
		return pointFeatures.size() + lineFeatures.size() + polygonFeatures.size();

	}

	/**
	 * Count just points.
	 *
	 * @return the int
	 */
	public int countJustPoints() {
		return pointFeatures.size();

	}

	/**
	 * Count just lines.
	 *
	 * @return the int
	 */
	public int countJustLines() {
		return lineFeatures.size();

	}

	/**
	 * Count just polygons.
	 *
	 * @return the int
	 */
	public int countJustPolygons() {
		return polygonFeatures.size();

	}



	/**
	 * Clears all features.
	 */
	public void clear() {
		pointFeatures.clear();
		lineFeatures.clear();
		polygonFeatures.clear();
	}

	/**
	 * Adds a feature list.
	 *
	 * @param featureList the feature list
	 */
	public void addFeatureList(List<Feature> featureList) {
		for (Feature feature : featureList) {
			this.addFeature(feature);
		}
	}

	/**
	 * Gets all features in Container (every type).
	 *
	 * @return the all features
	 */
	public ArrayList<Feature> getAllFeatures() {
		ArrayList<Feature> combined = new ArrayList<Feature>();
		combined.addAll(this.pointFeatures);
		combined.addAll(this.lineFeatures);
		combined.addAll(this.polygonFeatures);
		return combined;
	}

	/**
	 * Checks if the whole Container is empty.
	 *
	 * @return true, if is empty
	 */
	public boolean isEmpty() {
		if (this.countAllFeatures() == 0) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Gets all point Features.
	 *
	 * @return the all points
	 */
	public ArrayList<Feature> getAllPointFeatures() {
		return this.pointFeatures;
	}

	/**
	 * Gets all line features.
	 *
	 * @return the all line features
	 */
	public ArrayList<Feature> getAllLineFeatures() {
		return this.lineFeatures;
	}

	/**
	 * Gets all polygon features.
	 *
	 * @return the all polygon features
	 */
	public ArrayList<Feature> getAllPolygonFeatures() {
		return this.polygonFeatures;
	}

	/**
	 * Gets the envelope of the outer bounds of the layer.
	 * STILL DUMMY - DOES NOTHING!
	 *
	 * @return the envelope
	 */
	public Envelope getEnvelope() {
		return new Envelope();
	}

	/**
	 * Hide edited feature from drawing and put it in temp.
	 *
	 * @param currentEditedFeature the current edited feature
	 * @return true, if successful
	 */
	public boolean hideEditFeature(Feature currentEditedFeature) {
		this.currentHiddenEditedFeature = currentEditedFeature;
		if (getAllFeatures().remove(currentHiddenEditedFeature)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Reset edited feature, get it from temp and put it back into container.
	 *
	 * @return true, if successful
	 */
	public void resetEditedFeature() {
		getAllFeatures().add(currentHiddenEditedFeature);
		this.currentHiddenEditedFeature = null;
	}

	public void updateEditedFeature(Feature currentEditedFeature) {
		getAllFeatures().add(currentEditedFeature);
		this.currentHiddenEditedFeature = null;
	}
	
}
