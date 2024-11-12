/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.pathfinder.profilebuilder;

import org.apache.commons.math3.geometry.Vector;
import org.apache.commons.math3.geometry.euclidean.threed.Plane;
import org.locationtech.jts.algorithm.RectangleLineIntersector;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.prep.PreparedLineString;
import org.locationtech.jts.index.ItemVisitor;
import org.locationtech.jts.math.Vector2D;

import java.util.*;

import static org.noise_planet.noisemodelling.pathfinder.PathFinder.cutRoofPointsWithPlane;


public final class WallIntersectionPathVisitor implements ItemVisitor {
    Set<Integer> itemProcessed = new HashSet<>();
    List<Wall> walls;
    Coordinate p1;
    Coordinate p2;
    boolean left;
    LineSegment p1Top2;
    PreparedLineString seg;
    Set<Integer> wallsInIntersection = new HashSet<>();
    ProfileBuilder profileBuilder;
    Plane cutPlane;
    List<Coordinate> input;
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    public WallIntersectionPathVisitor(Coordinate p1, Coordinate p2,boolean left, ProfileBuilder profileBuilder,
                                       List<Coordinate> input, Plane cutPlane) {
        this.profileBuilder = profileBuilder;
        this.input = input;
        this.cutPlane = cutPlane;
        this.walls = profileBuilder.getWalls();
        this.p1 = p1;
        this.p2 = p2;
        this.left = left;
        seg = new PreparedLineString(GEOMETRY_FACTORY.createLineString(new Coordinate[]{p1, p2}));
    }

    /**
     *
     * @param item the index item to be visited
     */
    @Override
    public void visitItem(Object item) {
        int id = (Integer) item;
        if(!itemProcessed.contains(id)) {
            itemProcessed.add(id);
            final Wall w = walls.get(id-1);
            RectangleLineIntersector rect = new RectangleLineIntersector(w.getLine().getEnvelopeInternal());
            if (rect.intersects(p1, p2) && seg.intersects(w.getLine())) {
                addItem(id);
            }
        }
    }

    /**
     *
     * @param id
     */
    public void addItem(int id) {
        if (wallsInIntersection.contains(id)) {
            return;
        }
        final LineSegment originalWall = profileBuilder.getWall(id-1).getLineSegment();
        // Create the diffraction point outside of the wall segment
        // Diffraction point must not intersect with wall
        Vector2D translationVector = new Vector2D(originalWall.p0, originalWall.p1).normalize()
                .multiply(ProfileBuilder.wideAngleTranslationEpsilon);
        Coordinate extendedP0 = new Coordinate(originalWall.p0.x - translationVector.getX(),
                originalWall.p0.y - translationVector.getY(), originalWall.p0.z);
        Coordinate extendedP1 = new Coordinate(originalWall.p1.x + translationVector.getX(),
                originalWall.p1.y + translationVector.getY(), originalWall.p1.z);
        List<Coordinate> roofPoints = Arrays.asList(extendedP0, extendedP1);
        // Create a cut of the building volume
        roofPoints = cutRoofPointsWithPlane(cutPlane, roofPoints);
        if (!roofPoints.isEmpty()) {
            input.addAll(roofPoints);
            wallsInIntersection.add(id);
            // Stop iterating bounding boxes
            throw new IllegalStateException();
        }
    }

}
