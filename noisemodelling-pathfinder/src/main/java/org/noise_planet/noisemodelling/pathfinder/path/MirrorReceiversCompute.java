/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.pathfinder.path;

import org.locationtech.jts.algorithm.Intersection;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.math.Vector2D;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.Wall;

import java.util.ArrayList;
import java.util.List;

public class MirrorReceiversCompute {
    private static final double DEFAULT_CIRCLE_POINT_ANGLE = Math.PI / 24;
    STRtree mirrorReceiverTree;
    public static final int DEFAULT_MIRROR_RECEIVER_CAPACITY = 50000;
    private int mirrorReceiverCapacity = DEFAULT_MIRROR_RECEIVER_CAPACITY;
    private final Coordinate receiverCoordinate;
    private final List<Wall> buildWalls;
    private final double maximumDistanceFromWall;
    private final double maximumPropagationDistance;
    int numberOfImageReceivers = 0;


    /**
     *
     * @param receiverImage
     * @param wall
     * @param maximumPropagationDistance
     * @param maximumDistanceFromWall
     * @return
     */
    public static Polygon createWallReflectionVisibilityCone(Coordinate receiverImage, LineSegment wall,
                                                             double maximumPropagationDistance,
                                                             double maximumDistanceFromWall) {
        double distanceMin = wall.distance(receiverImage);

        GeometryFactory factory = new GeometryFactory();
        if(distanceMin > maximumPropagationDistance) {
            return factory.createPolygon();
        }
        ArrayList<Coordinate> circleSegmentPoints = new ArrayList<>();

        Vector2D rP0 = new Vector2D(receiverImage, wall.p0).normalize();
        Vector2D rP1 = new Vector2D(receiverImage, wall.p1).normalize();
        double angleSign = rP0.angleTo(rP1) >= 0 ? 1 : -1;
        int numberOfStep = Math.max(1, (int)(Math.abs(rP0.angleTo(rP1)) / DEFAULT_CIRCLE_POINT_ANGLE));
        Coordinate lastWallIntersectionPoint = new Coordinate();
        for(int angleStep = 0 ; angleStep <= numberOfStep; angleStep++) {
            Vector2D newPointTranslationVector = rP0.rotate(DEFAULT_CIRCLE_POINT_ANGLE * angleSign * angleStep);
            if(angleStep == numberOfStep) {
                newPointTranslationVector = rP1;
            } else if(angleStep == 0) {
                newPointTranslationVector = rP0;
            }
            Coordinate newPoint = newPointTranslationVector.translate(receiverImage);
            Coordinate wallIntersectionPoint = Intersection.intersection(wall.p0, wall.p1, receiverImage, newPoint);
            if(wallIntersectionPoint != null) {
                double wallIntersectionPointDistance = wallIntersectionPoint.distance(receiverImage);
                if (wallIntersectionPointDistance < maximumPropagationDistance) {
                    double vectorLength = Math.min(wallIntersectionPointDistance + maximumDistanceFromWall, maximumPropagationDistance);
                    newPoint = newPointTranslationVector.multiply(vectorLength).translate(receiverImage);
                    if (circleSegmentPoints.isEmpty()) {
                        circleSegmentPoints.add(wallIntersectionPoint);
                    }
                    lastWallIntersectionPoint = wallIntersectionPoint;
                    circleSegmentPoints.add(newPoint);
                }
            }
        }
        if(!circleSegmentPoints.isEmpty()) {
            circleSegmentPoints.add(lastWallIntersectionPoint);
            circleSegmentPoints.add(circleSegmentPoints.get(0));
            Coordinate[] conePolygon = circleSegmentPoints.toArray(new Coordinate[0]);
            return factory.createPolygon(conePolygon);
        } else {
            return factory.createPolygon();
        }
    }
    /**
     * Generate all image receivers from the provided list of walls
     * @param buildWalls
     * @param receiverCoordinates
     * @param reflectionOrder
     */
    public MirrorReceiversCompute(List<Wall> buildWalls, Coordinate receiverCoordinates,
                                  int reflectionOrder, double maximumPropagationDistance,
                                  double maximumDistanceFromWall) {
        this.receiverCoordinate = receiverCoordinates;
        this.buildWalls = buildWalls;
        this.maximumDistanceFromWall = maximumDistanceFromWall;
        this.maximumPropagationDistance = maximumPropagationDistance;
        mirrorReceiverTree = new STRtree();
        ArrayList<MirrorReceiver> parentsToProcess = new ArrayList<>();
        for(int currentDepth = 0; currentDepth < reflectionOrder; currentDepth++) {
            if(currentDepth == 0) {
                parentsToProcess.add(null);
            }
            ArrayList<MirrorReceiver> nextParentsToProcess = new ArrayList<>();
            for(MirrorReceiver parent : parentsToProcess) {
                for (Wall wall : buildWalls) {
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
                    if(wall.getLineSegment().distance(rcvMirror) > maximumPropagationDistance) {
                        // wall is too far from the receiver image, there is no receiver image
                        continue;
                    }
                    MirrorReceiver receiverResult = new MirrorReceiver(rcvMirror, parent, wall,
                            wall.getOriginId(), wall.getType());
                    // create the visibility cone of this receiver image
                    Polygon imageReceiverVisibilityCone = createWallReflectionVisibilityCone(rcvMirror,
                            wall.getLineSegment(), maximumPropagationDistance, maximumDistanceFromWall);
                    mirrorReceiverTree.insert(imageReceiverVisibilityCone.getEnvelopeInternal(), receiverResult);
                    nextParentsToProcess.add(receiverResult);
                    numberOfImageReceivers++;
                    if(numberOfImageReceivers >= mirrorReceiverCapacity) {
                        return;
                    }
                }
            }
            parentsToProcess = nextParentsToProcess;
        }
        mirrorReceiverTree.build();
    }

    public int getMirrorReceiverCapacity() {
        return mirrorReceiverCapacity;
    }

    public void setMirrorReceiverCapacity(int mirrorReceiverCapacity) {
        this.mirrorReceiverCapacity = mirrorReceiverCapacity;
    }


    /**
     *
     * @param sourcePosition
     * @return
     */
    public List<MirrorReceiver> findCloseMirrorReceivers(Coordinate sourcePosition) {
        if(Double.isNaN(sourcePosition.z)) {
            throw new IllegalArgumentException("Not supported NaN z value");
        }
        Envelope env = new Envelope(sourcePosition);
        MirrorReceiverVisitor mirrorReceiverVisitor = new MirrorReceiverVisitor(buildWalls, sourcePosition,
                receiverCoordinate, maximumDistanceFromWall, maximumPropagationDistance);
        mirrorReceiverTree.query(env, mirrorReceiverVisitor);
        return mirrorReceiverVisitor.result;
    }


}
