package org.noise_planet.noisemodelling.pathfinder.profilebuilder;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.math.Vector2D;
import org.noise_planet.noisemodelling.pathfinder.utils.geometry.RTreeUtils;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.BridgeService.PropagationType;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.triangulate.quadedge.Vertex;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * Small helper utilities extracted from {@code ProfileBuilder} to centralize
 * line-splitting and obstacle query logic so {@code ProfileBuilder} can
 * delegate responsibilities and remain focused on orchestration.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Split long source-&gt;receiver segments into shorter pieces for stable
 *       spatial-index queries and for accurate interpolation of Z values.</li>
 *   <li>Query obstacle/service spatial indexes (processed walls) and dispatch
 *       intersection handling to the appropriate service (building, wall,
 *       bridge, ground) to produce profile cut points.</li>
 *   <li>Collect cut points discovered by obstacle handlers and append them
 *       to the provided {@link CutProfile}.</li>
 *   <li>Adjust Z coordinates of bridge cut points through interpolation based on
 *       surrounding valid points for accurate acoustic propagation calculations.</li>
 * </ul>
 *
 * <p>Design notes: methods in this utility are stateless and operate only on
 * their inputs. They require that service arguments (for example
 * {@link ProcessedWallService#getProcessedRtree()} and related indexes) have been
 * prepared by the caller (indexed) before invocation.</p>
 */
public final class ProfileUtils {
    private ProfileUtils() {}

    /**
     * Splits a segment between two coordinates into smaller LineSegments whose length
     * does not exceed the specified maximum length.
     *
     * <p>The method preserves and interpolates Z coordinates linearly when
     * creating intermediate segment endpoints. If the original segment length
     * is less than {@code maxLineLength}, a single segment equal to the full
     * line is returned.</p>
     *
     * @param c0 segment start coordinate (may contain z)
     * @param c1 segment end coordinate (may contain z)
     * @param maxLineLength maximum allowed length per returned segment
     * @return list of LineSegment parts covering the full segment in order
     */
    public static List<LineSegment> splitToSegments(Coordinate c0, Coordinate c1, double maxLineLength) {
        List<LineSegment> segments = new ArrayList<>();
    LineSegment fullLine = new LineSegment(c0, c1);
    double l = c0.distance(c1);
        if(l < maxLineLength) {
            segments.add(fullLine);
        }
        else {
            double frac = maxLineLength / l;
            for(int i = 0; i < l / maxLineLength; i++) {
                Coordinate p0 = fullLine.pointAlong(i * frac);
                p0.z = c0.z + (c1.z - c0.z) * i * frac;
                Coordinate p1 = fullLine.pointAlong(Math.min((i + 1) * frac, 1.0));
                p1.z = c0.z + (c1.z - c0.z) * Math.min((i + 1) * frac, 1.0);
                segments.add(new LineSegment(p0, p1));
            }
        }
        return segments;
    }

    private static final double MILLIMETER = 1.0;
    private static final double LEFT_SIDE = Math.PI / 2;

    /**
     * Determines if an intersection point represents an entry into a polygon facet
     * based on the propagation line direction and polygon facet orientation.
     *
     * <p>The method creates a perpendicular vector to the facet line and checks
     * if moving slightly in the exterior direction brings the point closer to
     * the propagation source, indicating an entry intersection.</p>
     *
     * @param intersection the intersection point to check
     * @param propagationLine the line segment representing sound propagation
     * @param polygonFacetLine the line segment representing the polygon facet
     * @return true if the intersection represents an entry, false otherwise
     */
    public static boolean isIntersectionEntry(Coordinate intersection, LineSegment propagationLine, LineSegment polygonFacetLine) {
        Vector2D facetVector = Vector2D.create(polygonFacetLine.p0, polygonFacetLine.p1);
        Vector2D exteriorVector = facetVector.rotate(LEFT_SIDE).normalize().multiply(MILLIMETER);
        Coordinate exteriorPoint = exteriorVector.add(Vector2D.create(intersection)).toCoordinate();
        return  exteriorPoint.distance(propagationLine.p0) < intersection.distance(propagationLine.p0);
    }
    
    /**
     * Determines if an intersection point represents an entry into a wall
     * based on the propagation line direction and wall orientation.
     *
     * @param intersection the intersection point to check
     * @param propagationLine the line segment representing sound propagation
     * @param wall the wall object containing the line segment
     * @return true if the intersection represents an entry, false otherwise
     */
    public static boolean isIntersectionEntry(Coordinate intersection, LineSegment propagationLine, Wall wall) {
        LineSegment polygonFaceLine = wall.getLineSegment();
        return isIntersectionEntry(intersection, propagationLine, polygonFaceLine);
    }

}
