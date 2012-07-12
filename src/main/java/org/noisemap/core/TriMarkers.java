/**
 * NoiseMap is a scientific computation plugin for OrbisGIS to quickly evaluate the
 * noise impact on European action plans and urban mobility plans. This model is
 * based on the French standard method NMPB2008. It includes traffic-to-noise
 * sources evaluation and sound propagation processing.
 *
 * This version is developed at French IRSTV Institute and at IFSTTAR
 * (http://www.ifsttar.fr/) as part of the Eval-PDU project, funded by the
 * French Agence Nationale de la Recherche (ANR) under contract ANR-08-VILL-0005-01.
 *
 * Noisemap is distributed under GPL 3 license. Its reference contact is Judicaël
 * Picaut <judicael.picaut@ifsttar.fr>. It is maintained by Nicolas Fortin
 * as part of the "Atelier SIG" team of the IRSTV Institute <http://www.irstv.fr/>.
 *
 * Copyright (C) 2011 IFSTTAR
 * Copyright (C) 2011-1012 IRSTV (FR CNRS 2488)
 *
 * Noisemap is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * Noisemap is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Noisemap. If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please consult: <http://www.orbisgis.org/>
 * or contact directly:
 * info_at_ orbisgis.org
 */
package org.noisemap.core;

import com.vividsolutions.jts.algorithm.CGAlgorithms;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Triangle;

/**
 * Add the constraint of CCW orientation.
 * 
 * Used by TriangleContouring.
 * Store also three double values, one fore each vertices.
 * 
 * @author Nicolas Fortin
 */
public class TriMarkers extends Triangle {

	public TriMarkers() {
		super(new Coordinate(), new Coordinate(), new Coordinate());
		this.m1 = 0;
		this.m2 = 0;
		this.m3 = 0;
	}

        @Override
        public String toString() {
            return "TriMarkers{" + "p1=" + p0 + ", p2=" + p1 + ", p3=" + p2 + " m1=" + m1 + ", m2=" + m2 + ", m3=" + m3 + "}";
        }

	public TriMarkers(Coordinate p0, Coordinate p1, Coordinate p2, double m1,
			double m2, double m3) {
		super(p0, p1, p2);

		if (!CGAlgorithms.isCCW(this.getRing())) {
			this.setCoordinates(p2, p1, p0);
			this.m1 = m3;
			this.m3 = m1;
		} else {
			this.m1 = m1;
			this.m3 = m3;
		}
		this.m2 = m2;
	}

	public double m1, m2, m3;

	public void setMarkers(double m1, double m2, double m3) {
		this.m1 = m1;
		this.m2 = m2;
		this.m3 = m3;
	}

	public void setAll(Coordinate p0, Coordinate p1, Coordinate p2, double m1,
			double m2, double m3) {
		setCoordinates(p0, p1, p2);
		setMarkers(m1, m2, m3);
		if (!CGAlgorithms.isCCW(this.getRing())) {
			this.setCoordinates(p2, p1, p0);
			this.m1 = m3;
			this.m3 = m1;
		}
	}

	public double getMinMarker() {
		return getMinMarker((short) -1);
	}

	public double getMinMarker(short exception) {
		double minval = Double.POSITIVE_INFINITY;
		if (exception != 0) {
			minval = Math.min(minval, this.m1);
		}
		if (exception != 1) {
			minval = Math.min(minval, this.m2);
		}
		if (exception != 2) {
			minval = Math.min(minval, this.m3);
		}
		return minval;
	}

	public double getMaxMarker() {
		return getMaxMarker((short) -1);
	}

	public double getMaxMarker(short exception) {
		double maxval = Double.NEGATIVE_INFINITY;
		if (exception != 0) {
			maxval = Math.max(maxval, this.m1);
		}
		if (exception != 1) {
			maxval = Math.max(maxval, this.m2);
		}
		if (exception != 2) {
			maxval = Math.max(maxval, this.m3);
		}
		return maxval;
	}

	public void setCoordinates(Coordinate p0, Coordinate p1, Coordinate p2) {
		this.p0 = p0;
		this.p1 = p1;
		this.p2 = p2;
	}

	public Coordinate[] getRing() {
		Coordinate[] ring = { p0, p1, p2, p0 };
		return ring;
	}

	Coordinate getVertice(short idvert) {
		if (idvert == 0) {
			return this.p0;
		} else if (idvert == 1) {
			return this.p1;
		} else {
			return this.p2;
		}
	}

	double getMarker(short idvert) {
		if (idvert == 0) {
			return this.m1;
		} else if (idvert == 1) {
			return this.m2;
		} else {
			return this.m3;
		}
	}
}
