/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.pathfinder.path;

import org.locationtech.jts.algorithm.LineIntersector;
import org.locationtech.jts.algorithm.RobustLineIntersector;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.index.ItemVisitor;
import org.locationtech.jts.triangulate.quadedge.Vertex;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.Wall;

import java.util.ArrayList;
import java.util.List;


public class MirrorReceiverVisitor implements ItemVisitor {
    List<MirrorReceiver> result = new ArrayList<>();
    List<Wall> buildWalls;

    public Coordinate getReceiver() {
        return receiver;
    }

    public Coordinate getSource() {
        return source;
    }

    Coordinate source;
    Coordinate receiver;
    LineSegment sourceReceiverSegment;
    double maximumDistanceFromSegment;
    double maximumPropagationDistance;
    int visitedNode = 0;

    public MirrorReceiverVisitor(List<Wall> buildWalls, Coordinate source, Coordinate receiver,
                                 double maximumDistanceFromSegment,
                                 double maximumPropagationDistance) {
        this.buildWalls = buildWalls;
        this.source = source;
        this.receiver = receiver;
        this.sourceReceiverSegment = new LineSegment(source, receiver);
        this.maximumDistanceFromSegment = maximumDistanceFromSegment;
        this.maximumPropagationDistance = maximumPropagationDistance;
    }
    public MirrorReceiverVisitor(Coordinate source, Coordinate receiver) {

        this.source = source;
        this.receiver = receiver;
    }


    public void visitItemNoWall(Object item) {
        visitedNode++;

        MirrorReceiver receiverImage = (MirrorReceiver) item;

        // Add mirrored positions to the respective lists
        source = receiverImage.getReceiverPos();
        receiver = receiverImage.getParentMirror().getReceiverPos();
    }


    /**
     *
     * @param item the index item to be visited
     */
    @Override
    public void visitItem(Object item) {
        visitedNode++;
        // try to excluded walls without taking into account the topography and other factors

        MirrorReceiver receiverImage = (MirrorReceiver) item;
        // Check propagation distance
        if(receiverImage.getReceiverPos().distance3D(source) < maximumPropagationDistance) {
            // Check distance of walls
            MirrorReceiver currentReceiverImage = receiverImage;
            Coordinate reflectionPoint = source;
            while (currentReceiverImage != null) {
                final Wall currentWall = currentReceiverImage.getWall();
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
                for (Wall otherWall : currentWall.getObstacle().getWalls()) {
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
