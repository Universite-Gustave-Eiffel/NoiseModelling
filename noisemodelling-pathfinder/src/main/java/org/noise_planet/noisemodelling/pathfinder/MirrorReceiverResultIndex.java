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
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.index.kdtree.KdNode;
import org.locationtech.jts.index.kdtree.KdNodeVisitor;
import org.locationtech.jts.index.kdtree.KdTree;

import java.util.ArrayList;
import java.util.List;

public class MirrorReceiverResultIndex {
    KdTree mirrorReceiverTree;
    public static final int DEFAULT_MIRROR_RECEIVER_CAPACITY = 50000;
    private int mirrorReceiverCapacity = DEFAULT_MIRROR_RECEIVER_CAPACITY;
    private Coordinate receiverCoordinate;
    private List<ProfileBuilder.Wall> buildWalls;

    public MirrorReceiverResultIndex(List<ProfileBuilder.Wall> buildWalls, Coordinate receiverCoordinates, int reflectionOrder) {
        this.receiverCoordinate = receiverCoordinates;
        this.buildWalls = buildWalls;
        mirrorReceiverTree = new KdTree();
        int pushed = 0;
        ArrayList<MirrorReceiverResult> parentsToProcess = new ArrayList<>();
        for(int currentDepth = 0; currentDepth < reflectionOrder; currentDepth++) {
            if(currentDepth == 0) {
                parentsToProcess.add(null);
            }
            ArrayList<MirrorReceiverResult> nextParentsToProcess = new ArrayList<>();
            for(MirrorReceiverResult parent : parentsToProcess) {
                for (ProfileBuilder.Wall wall : buildWalls) {
                    if (parent != null && wall.getProcessedWallIndex() == parent.getWall().getProcessedWallIndex()) {
                        continue;
                    }
                    //Calculate the coordinate of projection
                    Coordinate proj = wall.getLineSegment().project(receiverCoordinates);
                    Coordinate rcvMirror = new Coordinate(2 * proj.x - receiverCoordinates.x,
                            2 * proj.y - receiverCoordinates.y, receiverCoordinates.z);
                    MirrorReceiverResult receiverResult = new MirrorReceiverResult(rcvMirror, parent, wall,
                            wall.getOriginId(), wall.getType());
                    mirrorReceiverTree.insert(rcvMirror, receiverResult);
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

    List<MirrorReceiverResult> findCloseMirrorReceivers(Coordinate sourcePosition,
                                                        double maximumDistanceFromSourceReceiver,
                                                        double maximumPropagationDistance) {
        Envelope env = new Envelope(receiverCoordinate);
        env.expandToInclude(sourcePosition);
        env.expandBy(maximumPropagationDistance);
        ReceiverImageVisitor receiverImageVisitor = new ReceiverImageVisitor(buildWalls, sourcePosition,
                receiverCoordinate, maximumDistanceFromSourceReceiver, maximumPropagationDistance);
        mirrorReceiverTree.query(env, receiverImageVisitor);
        return receiverImageVisitor.result;
    }

    private static class ReceiverImageVisitor implements KdNodeVisitor {
        List<MirrorReceiverResult> result = new ArrayList<>();
        List<ProfileBuilder.Wall> buildWalls;
        Coordinate source;
        Coordinate receiver;
        LineSegment sourceReceiverSegment;
        double maximumDistanceFromSegment;
        double maximumPropagationDistance;

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
        public void visit(KdNode node) {
            // try to excluded walls without taking into account the topography and other factors

            MirrorReceiverResult receiverImage = (MirrorReceiverResult)node.getData();
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
                    }
                    if (reflectionPoint != null) {
                        reflectionPoint = li.getIntersection(0);
                        double fraction = currentWallLineSegment.segmentFraction(reflectionPoint);
                        double wallReflectionPointZ = currentWallLineSegment.p0.z +
                                fraction * (currentWallLineSegment.p1.z - currentWallLineSegment.p0.z);
                        double propagationFraction = srcMirrRcvLine.segmentFraction(reflectionPoint);
                        double propagationReflectionPointZ =  srcMirrRcvLine.p0.z +
                                propagationFraction * (srcMirrRcvLine.p1.z - srcMirrRcvLine.p0.z);
                        if(propagationReflectionPointZ > wallReflectionPointZ) {
                            // The receiver image is not visible because the wall is not tall enough
                            return;
                        }
                    }
                    // Check if other surface of this wall obstruct the view
                    //Check if another wall is masking the current
                    for (ProfileBuilder.Wall otherWall : currentWall.getObstacle().getWalls()) {
                        LineSegment otherWallSegment = otherWall.getLineSegment();
                        li = new RobustLineIntersector();
                        li.computeIntersection(otherWall.p0, otherWall.p1,
                                srcMirrRcvLine.p0, srcMirrRcvLine.p1);
                        if (li.hasIntersection()) {
                            Coordinate otherReflectionPoint = li.getIntersection(0);
                            double fraction = otherWallSegment.segmentFraction(otherReflectionPoint);
                            double wallReflectionPointZ = otherWallSegment.p0.z +
                                    fraction * (otherWallSegment.p1.z - otherWallSegment.p0.z);
                            double propagationFraction = srcMirrRcvLine.segmentFraction(otherReflectionPoint);
                            double propagationReflectionPointZ =  srcMirrRcvLine.p0.z +
                                    propagationFraction * (srcMirrRcvLine.p1.z - srcMirrRcvLine.p0.z);
                            if(propagationReflectionPointZ <= wallReflectionPointZ) {
                                // This wall is obstructing the view of the propagation line (other wall too tall)
                                return;
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
