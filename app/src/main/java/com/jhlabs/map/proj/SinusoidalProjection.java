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

public class SinusoidalProjection extends PseudoCylindricalProjection {
	
	public Coordinate project(double lam, double phi, Coordinate xy) {
		xy.x = lam * Math.cos(phi);
		xy.y = phi;
		return xy;
	}

	public Coordinate projectInverse(double x, double y, Coordinate lp) {
		lp.x = x / Math.cos(y);
		lp.y = y;
		return lp;
	}

	public double getWidth(double y) {
		return MapMath.normalizeLongitude(Math.PI) * Math.cos(y);
	}

	public boolean hasInverse() {
		return true;
	}

	public String toString() {
		return "Sinusoidal";
	}

}