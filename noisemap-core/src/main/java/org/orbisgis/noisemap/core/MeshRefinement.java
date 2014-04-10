package org.orbisgis.noisemap.core;

import org.jdelaunay.delaunay.evaluator.InsertionEvaluator;
import org.jdelaunay.delaunay.geometries.DTriangle;

/**
 *  Triangle quality evaluation for noise map grid.
 *  @see "Bank, Randolph E., PLTMG: A Software Package for Solving Elliptic Partial Differential Equations, User's Guide 6.0,
 *  Society for Industrial and Applied Mathematics, Philadelphia, PA, 1990."
 * @author Nicolas Fortin
 */
public class MeshRefinement implements InsertionEvaluator {
    private final double maxArea;
    private final double minArea;
    private final double targetQuality;
    public static final double DEFAULT_QUALITY = 0.6;
    private static final double SQRT3 = Math.sqrt(3.);

    public MeshRefinement(double maxArea, double minArea, double targetQuality) {
        this.maxArea = maxArea;
        this.minArea = minArea;
        this.targetQuality = targetQuality;
    }

    @Override
    public boolean evaluate(DTriangle dTriangle) {
        // Do not refine in buildings area
        if(dTriangle.getProperty() != 0) {
            return false;
        }
        double area = dTriangle.getArea();
        return area >= minArea && (area > maxArea ||
                (4 * area * SQRT3) / (
                        Math.pow(dTriangle.getEdge(0).get2DLength(), 2) +
                        Math.pow(dTriangle.getEdge(1).get2DLength(), 2) +
                        Math.pow(dTriangle.getEdge(2).get2DLength(), 2)) < targetQuality);
    }
}
