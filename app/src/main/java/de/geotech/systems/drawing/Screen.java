package de.geotech.systems.drawing;

import android.util.FloatMath;

public abstract class Screen {

	public final double SCALE = 241.23656351074038;
	// minimum and maximum zoom scales
	public final double MINIMUM_ZOOM_SCALE = 25d;
	public final double MAXIMUM_ZOOM_SCALE = 10000000d;

	// Map size
	public int width, height;

	// Scale - Zoom
	public double scale;

	// Helper for Calculation of Scale - Zoom
	public double oldScale;
	// Bounding Box of the Map
	public BBOX bbox;

	// touch coordinates of the tips on display
	public float firstPointerX, firstPointerY, movedPointerX, movedPointerY;

	// touch panning distance
	public float baseDist;

	// coordinate helpers
	public double focusX;
	public double focusY;

	/**
	 * Returns distance between first touched and moved position in pixel units. normal phytagoras a2 + b2 = c2
	 * 
	 * @return moved distance
	 */
	public float movedDistance() {
		float diffX = movedPointerX - firstPointerX;
		float diffY = movedPointerY - firstPointerY;
		float dist = FloatMath.sqrt(diffX * diffX + diffY * diffY);
		// Log.i(CLASSTAG, "Moved distance: " + dist);
		return dist;
	}
}
