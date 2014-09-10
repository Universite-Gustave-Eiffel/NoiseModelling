/**
 *
 * jDelaunay is a library dedicated to the processing of Delaunay and constrained
 * Delaunay triangulations from PSLG inputs.
 *
 * This library is developed at French IRSTV institute as part of the AvuPur and Eval-PDU project,
 * funded by the French Agence Nationale de la Recherche (ANR) under contract
 * ANR-07-VULN-01 and ANR-08-VILL-0005-01 .
 *
 * jDelaunay is distributed under GPL 3 license. It is produced by the "Atelier SIG" team of
 * the IRSTV Institute <http://www.irstv.fr/> CNRS FR 2488.
 *
 * Copyright (C) 2010-2012 IRSTV FR CNRS 2488
 *
 * jDelaunay is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * jDelaunay is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * jDelaunay. If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please consult: <http://www.orbisgis.org/>
 * or contact directly:
 * info_at_ orbisgis.org
 */
package org.orbisgis.noisemap.core;

import org.jdelaunay.delaunay.error.DelaunayError;
import org.jdelaunay.delaunay.evaluator.InsertionEvaluator;
import org.jdelaunay.delaunay.evaluator.TriangleQuality;
import org.jdelaunay.delaunay.geometries.DPoint;
import org.jdelaunay.delaunay.geometries.DTriangle;
import org.jdelaunay.delaunay.geometries.Element;

/**
 *  Triangle quality evaluation. Better results than {@link org.jdelaunay.delaunay.evaluator.SkinnyEvaluator}
 *  @see "Bank, Randolph E., PLTMG: A Software Package for Solving Elliptic Partial Differential Equations, User's Guide 6.0,
 *  Society for Industrial and Applied Mathematics, Philadelphia, PA, 1990."
 * @author Nicolas Fortin
 */
public class TriangleConstraint extends TriangleQuality {
    private double maxArea = 0;

    /**
     * Default constructor. Default quality without area constraint.
     */
    public TriangleConstraint() {
    }

    /**
     * @param maxArea Triangle will be exploded if area is lower than this value.
     */
    public TriangleConstraint(double maxArea) {
        this.maxArea = maxArea;
    }

    @Override
    public boolean evaluate(DTriangle dTriangle) {
        if(super.evaluate (dTriangle) && maxArea != 0) {
            return true;
        } else {
            // Check area
            try {
                Element explodedTriangle = dTriangle.getCircumCenterContainer();
                return explodedTriangle instanceof DTriangle && ((DTriangle) explodedTriangle).getArea() > maxArea;
            } catch (DelaunayError err) {
                return false;
            }
        }
    }
}