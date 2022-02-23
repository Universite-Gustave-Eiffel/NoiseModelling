/**
 * NoiseMap is a scientific computation plugin for OrbisGIS developed in order to
 * evaluate the noise impact on urban mobility plans. This model is
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
 * Copyright (C) 2011-2012 IRSTV (FR CNRS 2488)
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
package org.noise_planet.noisemodelling.pathfinder;

import org.locationtech.jts.algorithm.LineIntersector;
import org.locationtech.jts.algorithm.RobustLineIntersector;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.index.ItemVisitor;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.math.Vector2D;
import org.locationtech.jts.triangulate.quadedge.Vertex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class MirrorReceiverResultIndex {
    private static final double DEFAULT_CIRCLE_POINT_ANGLE = Math.PI / 12;
    STRtree mirrorReceiverTree;
    public static final int DEFAULT_MIRROR_RECEIVER_CAPACITY = 50000;
    private int mirrorReceiverCapacity = DEFAULT_MIRROR_RECEIVER_CAPACITY;
    private final Coordinate receiverCoordinate;
    private final List<ProfileBuilder.Wall> buildWalls;
    private final double maximumDistanceFromWall;
    private final double maximumPropagationDistance;

    private static Coordinate projectPoint(Coordinate receiverImage, Coordinate wallCorner,
                                            double maximumPropagationDistance,
                                            double maximumDistanceFromWall) {
        Vector2D vectorP0 = new Vector2D(receiverImage, wallCorner);
        double distanceFromCorner = maximumPropagationDistance - receiverImage.distance(wallCorner);
        if(distanceFromCorner > maximumDistanceFromWall) {
            distanceFromCorner = maximumDistanceFromWall;
        }
        vectorP0 = vectorP0.normalize().multiply(distanceFromCorner);
        return new Coordinate(wallCorner.x + vectorP0.getX(), wallCorner.y + vectorP0.getY());
    }

    public static Polygon createWallReflectionVisibilityCone(Coordinate receiverImage, LineSegment wall,
                                                             double maximumPropagationDistance,
                                                             double maximumDistanceFromWall) {
        GeometryFactory factory = new GeometryFactory();
        if(wall.distance(receiverImage) > maximumPropagationDistance) {
            return factory.createPolygon();
        }
        Coordinate conePointP0 = projectPoint(receiverImage, wall.p0, maximumPropagationDistance,
                maximumDistanceFromWall);
        Coordinate conePointP1 = projectPoint(receiverImage, wall.p1, maximumPropagationDistance,
                maximumDistanceFromWall);

        // create circle segments between p0 and p1
        ArrayList<Coordinate> circleSegmentPoints = new ArrayList<>(Arrays.asList(conePointP0, conePointP1));

        // Insert a mid point
        int startCheckIndex = 0;
        while (true) {
            boolean newPoint = false;
            for(int i=startCheckIndex; i < circleSegmentPoints.size() - 1; i++) {
                Coordinate intermediatePoint0 = circleSegmentPoints.get(i);
                Coordinate intermediatePoint1 = circleSegmentPoints.get(i + 1);
                Vector2D rP0 = new Vector2D(receiverImage, intermediatePoint0);
                Vector2D rP1 = new Vector2D(receiverImage, intermediatePoint1);
                if(circleSegmentPoints.size() == 2 || Math.abs(rP0.angleTo(rP1)) > DEFAULT_CIRCLE_POINT_ANGLE) {
                    newPoint = true;
                    startCheckIndex = i;
                    // distance is too great create a new intermediate point in the circle
                    Vector2D midVector = rP0.rotate((rP1.angle() - rP0.angle()) / 2.0);
                    Vector2D translateVector = midVector.normalize().multiply(maximumPropagationDistance);
                    Coordinate midPointAtMaxDistance = new Coordinate(receiverImage.x + translateVector.getX(),
                            receiverImage.y + translateVector.getY());
                    double distanceFromCorner = wall.distance(midPointAtMaxDistance);
                    if(distanceFromCorner > maximumDistanceFromWall) {
                        translateVector = midVector.normalize()
                                .multiply(maximumPropagationDistance - (distanceFromCorner -  maximumDistanceFromWall));
                        midPointAtMaxDistance = new Coordinate(receiverImage.x + translateVector.getX(),
                            receiverImage.y + translateVector.getY());
                    }
                    circleSegmentPoints.add(i+1, midPointAtMaxDistance);
                    break;
                }
            }
            if(!newPoint) {
                break;
            }
        }

        Coordinate[] conePolygon = new Coordinate[circleSegmentPoints.size() + 3];
        conePolygon[0] = wall.p1;
        conePolygon[1] = wall.p0;
        conePolygon[conePolygon.length - 1] = wall.p1;
        for(int i = 0; i < circleSegmentPoints.size(); i++) {
            conePolygon[i + 2] = circleSegmentPoints.get(i);
        }
        return factory.createPolygon(conePolygon);
    }
    /**
     * Generate all image receivers from the provided list of walls
     * @param buildWalls
     * @param receiverCoordinates
     * @param reflectionOrder
     */
    public MirrorReceiverResultIndex(List<ProfileBuilder.Wall> buildWalls, Coordinate receiverCoordinates,
                                     int reflectionOrder, double maximumPropagationDistance,
                                     double maximumDistanceFromWall) {
        this.receiverCoordinate = receiverCoordinates;
        this.buildWalls = buildWalls;
        this.maximumDistanceFromWall = maximumDistanceFromWall;
        this.maximumPropagationDistance = maximumPropagationDistance;
        mirrorReceiverTree = new STRtree();
        int pushed = 0;
        ArrayList<MirrorReceiverResult> parentsToProcess = new ArrayList<>();
        for(int currentDepth = 0; currentDepth < reflectionOrder; currentDepth++) {
            if(currentDepth == 0) {
                parentsToProcess.add(null);
            }
            ArrayList<MirrorReceiverResult> nextParentsToProcess = new ArrayList<>();
            for(MirrorReceiverResult parent : parentsToProcess) {
                for (ProfileBuilder.Wall wall : buildWalls) {
                    Coordinate receiverImage;
                    if (parent != null) {
                        if(wall == parent.getWall()) {
                            continue;
                        } else {
                            receiverImage = parent.getReceiverPos();
                        }
                    } else {
                        receiverImage = receiverCoordinates;
                    }
                    //Calculate the coordinate of projection
                    Coordinate proj = wall.getLineSegment().project(receiverImage);
                    Coordinate rcvMirror = new Coordinate(2 * proj.x - receiverImage.x,
                            2 * proj.y - receiverImage.y, receiverImage.z);
                    MirrorReceiverResult receiverResult = new MirrorReceiverResult(rcvMirror, parent, wall,
                            wall.getOriginId(), wall.getType());
                    // create the visibility cone of this receiver image
                    Polygon imageReceiverVisibilityCone = createWallReflectionVisibilityCone(rcvMirror,
                            wall.getLineSegment(), maximumPropagationDistance, maximumDistanceFromWall);
                    mirrorReceiverTree.insert(imageReceiverVisibilityCone.getEnvelopeInternal(), receiverResult);
                    nextParentsToProcess.add(receiverResult);
                    pushed++;
                    if(pushed >= mirrorReceiverCapacity) {
                        return;
                    }
                }
            }
            parentsToProcess = nextParentsToProcess;
        }
    }

    public int getMirrorReceiverCapacity() {
        return mirrorReceiverCapacity;
    }

    public void setMirrorReceiverCapacity(int mirrorReceiverCapacity) {
        this.mirrorReceiverCapacity = mirrorReceiverCapacity;
    }

    List<MirrorReceiverResult> findCloseMirrorReceivers(Coordinate sourcePosition) {
        if(Double.isNaN(sourcePosition.z)) {
            throw new IllegalArgumentException("Not supported NaN z value");
        }
        Envelope env = new Envelope(sourcePosition);
        ReceiverImageVisitor receiverImageVisitor = new ReceiverImageVisitor(buildWalls, sourcePosition,
                receiverCoordinate, maximumDistanceFromWall, maximumPropagationDistance);
        mirrorReceiverTree.query(env, receiverImageVisitor);
        return receiverImageVisitor.result;
    }

    private static class ReceiverImageVisitor implements ItemVisitor {
        List<MirrorReceiverResult> result = new ArrayList<>();
        List<ProfileBuilder.Wall> buildWalls;
        Coordinate source;
        Coordinate receiver;
        LineSegment sourceReceiverSegment;
        double maximumDistanceFromSegment;
        double maximumPropagationDistance;
        int visitedNode = 0;

        public ReceiverImageVisitor(List<ProfileBuilder.Wall> buildWalls, Coordinate source, Coordinate receiver,
                                    double maximumDistanceFromSegment,
                                    double maximumPropagationDistance) {
            this.buildWalls = buildWalls;
            this.source = source;
            this.receiver = receiver;
            this.sourceReceiverSegment = new LineSegment(source, receiver);
            this.maximumDistanceFromSegment = maximumDistanceFromSegment;
            this.maximumPropagationDistance = maximumPropagationDistance;
        }

        @Override
        public void visitItem(Object item) {
            visitedNode++;
            // try to excluded walls without taking into account the topography and other factors

            MirrorReceiverResult receiverImage = (MirrorReceiverResult) item;
            // Check propagation distance
            if(receiverImage.getReceiverPos().distance3D(source) < maximumPropagationDistance) {
                // Check distance of walls
                MirrorReceiverResult currentReceiverImage = receiverImage;
                Coordinate reflectionPoint = source;
                while (currentReceiverImage != null) {
                    final ProfileBuilder.Wall currentWall = receiverImage.getWall();
                    final LineSegment currentWallLineSegment = currentWall.getLineSegment();
                    if (currentWallLineSegment.distance(sourceReceiverSegment) > maximumDistanceFromSegment) {
                        return;
                    }
                    // Check if reflection is placed on the wall segment
                    LineSegment srcMirrRcvLine = new LineSegment(currentReceiverImage.getReceiverPos(), reflectionPoint);
                    LineIntersector li = new RobustLineIntersector();
                    li.computeIntersection(currentWallLineSegment.p0, currentWallLineSegment.p1,
                            srcMirrRcvLine.p0, srcMirrRcvLine.p1);
                    if(!li.hasIntersection()) {
                        // No reflection on this wall
                        return;
                    } else {
                        reflectionPoint = li.getIntersection(0);
                        double wallReflectionPointZ = Vertex.interpolateZ(reflectionPoint, currentWallLineSegment.p0,
                                currentWallLineSegment.p1);
                        double propagationReflectionPointZ =  Vertex.interpolateZ(reflectionPoint, srcMirrRcvLine.p0,
                                srcMirrRcvLine.p1);
                        if(propagationReflectionPointZ > wallReflectionPointZ) {
                            // The receiver image is not visible because the wall is not tall enough
                            return;
                        }
                    }
                    // Check if other surface of this wall obstruct the view
                    //Check if another wall is masking the current
                    for (ProfileBuilder.Wall otherWall : currentWall.getObstacle().getWalls()) {
                        if(!otherWall.equals(currentWall)) {
                            LineSegment otherWallSegment = otherWall.getLineSegment();
                            li = new RobustLineIntersector();
                            li.computeIntersection(otherWall.p0, otherWall.p1, reflectionPoint, source);
                            if (li.hasIntersection()) {
                                Coordinate otherReflectionPoint = li.getIntersection(0);
                                double wallReflectionPointZ = Vertex.interpolateZ(otherReflectionPoint,
                                        otherWallSegment.p0, otherWallSegment.p1);
                                double propagationReflectionPointZ = Vertex.interpolateZ(otherReflectionPoint,
                                        srcMirrRcvLine.p0, srcMirrRcvLine.p1);
                                if (propagationReflectionPointZ <= wallReflectionPointZ) {
                                    // This wall is obstructing the view of the propagation line (other wall too tall)
                                    return;
                                }
                            }
                        }
                    }
                    currentReceiverImage = currentReceiverImage.getParentMirror();
                }
                // not rejected
                result.add(receiverImage);
            }
        }
    }
}
