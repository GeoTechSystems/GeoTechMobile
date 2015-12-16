/*
Copyright 2006 Jerry Huxtable

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

/*
 * This file was semi-automatically converted from the public-domain USGS PROJ source.
 */
package com.jhlabs.map.proj;

import com.jhlabs.map.*;
import com.vividsolutions.jts.geom.Coordinate;

public class PutninsP4Projection extends Projection {

	protected double C_x;
	protected double C_y;

	public PutninsP4Projection() {
		C_x = 0.874038744;
		C_y = 3.883251825;
	}

	public Coordinate project(double lplam, double lpphi, Coordinate xy) {
		lpphi = MapMath.asin(0.883883476 * Math.sin(lpphi));
		xy.x = C_x * lplam * Math.cos(lpphi);
		xy.x /= Math.cos(lpphi *= 0.333333333333333);
		xy.y = C_y * Math.sin(lpphi);
		return xy;
	}

	public Coordinate projectInverse(double xyx, double xyy, Coordinate lp) {
		lp.y = MapMath.asin(xyy / C_y);
		lp.x = xyx * Math.cos(lp.y) / C_x;
		lp.y *= 3.;
		lp.x /= Math.cos(lp.y);
		lp.y = MapMath.asin(1.13137085 * Math.sin(lp.y));
		return lp;
	}

	/**
	 * Returns true if this projection is equal area
	 */
	public boolean isEqualArea() {
		return true;
	}

	public boolean hasInverse() {
		return true;
	}

	public String toString() {
		return "Putnins P4";
	}

}
