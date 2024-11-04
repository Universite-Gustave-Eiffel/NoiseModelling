/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.pathfinder.profilebuilder;

import org.apache.commons.math3.geometry.euclidean.threed.Plane;
import org.locationtech.jts.algorithm.RectangleLineIntersector;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.prep.PreparedLineString;
import org.locationtech.jts.index.ItemVisitor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.noise_planet.noisemodelling.pathfinder.PathFinder.cutRoofPointsWithPlane;


public final class BuildingIntersectionPathVisitor implements ItemVisitor {
    Set<Integer> itemProcessed = new HashSet<>();
    List<Building> buildings;
    Coordinate p1;
    Coordinate p2;
    PreparedLineString seg;
    Set<Integer> buildingsInIntersection;
    ProfileBuilder profileBuilder;
    Plane cutPlane;
    List<Coordinate> input;
    boolean foundIntersection = false;
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    public BuildingIntersectionPathVisitor(List<Building> buildings, Coordinate p1,
                                           Coordinate p2, ProfileBuilder profileBuilder, List<Coordinate> input, Set<Integer> buildingsInIntersection, Plane cutPlane) {
        this.profileBuilder = profileBuilder;
        this.input = input;
        this.buildingsInIntersection = buildingsInIntersection;
        this.cutPlane = cutPlane;
        this.buildings = buildings;
        this.p1 = p1;
        this.p2 = p2;
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
            final Building b = buildings.get(id - 1);
            RectangleLineIntersector rect = new RectangleLineIntersector(b.getGeometry().getEnvelopeInternal());
            if (rect.intersects(p1, p2) && seg.intersects(b.getGeometry())) {
                addItem(id);
            }
        }
    }


    /**
     *
     * @param id
     */
    public void addItem(int id) {
        if (buildingsInIntersection.contains(id)) {
            return;
        }
        List<Coordinate> roofPoints = profileBuilder.getPrecomputedWideAnglePoints(id);
        // Create a cut of the building volume
        roofPoints = cutRoofPointsWithPlane(cutPlane, roofPoints);
        if (!roofPoints.isEmpty()) {
            input.addAll(roofPoints.subList(0, roofPoints.size() - 1));
            buildingsInIntersection.add(id);
            foundIntersection = true;
            // Stop iterating bounding boxes
            throw new IllegalStateException();
        }
    }

    /**
     *
     * @return
     */
    public boolean doContinue() {
        return !foundIntersection;
    }
}