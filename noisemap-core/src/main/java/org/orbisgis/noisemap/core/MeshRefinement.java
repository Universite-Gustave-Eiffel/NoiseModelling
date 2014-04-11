package org.orbisgis.noisemap.core;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.index.strtree.STRtree;
import org.jdelaunay.delaunay.evaluator.InsertionEvaluator;
import org.jdelaunay.delaunay.geometries.DPoint;
import org.jdelaunay.delaunay.geometries.DTriangle;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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
    private STRtree rTree;
    private GeometryFactory factory = new GeometryFactory();

    public MeshRefinement(double maxArea, double minArea, double targetQuality, MeshBuilder meshBuilder) {
        this.maxArea = maxArea;
        this.minArea = minArea;
        this.targetQuality = targetQuality;
        int id=0;
        LinkedList<MeshBuilder.PolygonWithHeight> areas = meshBuilder.getPolygonWithHeight();
        int itemCount = areas.size();
        rTree = new STRtree(itemCount);
        for(MeshBuilder.PolygonWithHeight poly : areas) {
            rTree.insert(poly.getGeometry().getEnvelopeInternal(), poly.getGeometry());
        }
    }

    private boolean isPointInBuilding(Coordinate pt) {
        Envelope queryEnv = new Envelope(pt);
        queryEnv.expandBy(1);
        for(Object polyObj : rTree.query(queryEnv)) {
            if(polyObj instanceof Geometry) {
                if(((Geometry) polyObj).contains(factory.createPoint(pt))) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean evaluate(DTriangle dTriangle) {
        // Do not refine in buildings area
        if(isPointInBuilding(dTriangle.getCircumCenter())) {
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
