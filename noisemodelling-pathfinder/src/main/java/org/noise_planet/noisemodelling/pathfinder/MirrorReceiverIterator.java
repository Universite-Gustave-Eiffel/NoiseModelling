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

import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineSegment;

import java.util.*;

/**
 * Iterator through mirrored receiver
 * TODO Use Binary Space Partitioning in order to optimise wall orientation filter.
 * @author Nicolas Fortin
 */
public class MirrorReceiverIterator implements Iterator<MirrorReceiverResult> {
    private final Coordinate receiverCoord;
    private final List<ProfileBuilder.Wall> nearBuildingsWalls;
    // Wall stack
    private CrossTableIterator wallIdentifierIt;
    private MirrorReceiverResult current = null;
    private final int maxDepth;

    private MirrorReceiverIterator(Coordinate receiverCoord, List<ProfileBuilder.Wall> nearBuildingsWalls, int maxDepth) {
        this.receiverCoord = receiverCoord;
        this.nearBuildingsWalls = nearBuildingsWalls;
        this.wallIdentifierIt = new CrossTableIterator(maxDepth, nearBuildingsWalls.size());
        this.maxDepth = maxDepth;
        fetchNext();
    }

    @Override
    public boolean hasNext() {
        return current != null;
    }

    private void fetchNext() {
        if(!wallIdentifierIt.hasNext()) {
            current = null;
        }
        MirrorReceiverResult next = null;
        while(wallIdentifierIt.hasNext()) {
            List<Integer> currentWall = wallIdentifierIt.next();
            MirrorReceiverResult parent = fetchParent(current, currentWall);
            int wallId = currentWall.get(currentWall.size() - 1);
            ProfileBuilder.Wall wall = nearBuildingsWalls.get(wallId);
            //Counter ClockWise test. Walls vertices are CCW oriented.
            //This help to test if a wall could see a point or another wall
            //If the triangle formed by two point of the wall + the receiver is CCW then the wall is oriented toward the point.
            boolean isCCW;
            Coordinate receiverIm;
            if (parent == null) { //If it is the first depth wall
                isCCW = MirrorReceiverIterator.wallPointTest(wall.getLine(), receiverCoord);
                receiverIm = receiverCoord;
            } else {
                //Call wall visibility test
                receiverIm = parent.getReceiverPos();
                isCCW = MirrorReceiverIterator.wallWallTest(nearBuildingsWalls.get(parent.getWallId()).getLine(), wall.getLine())
                         && MirrorReceiverIterator.wallPointTest(wall.getLine(), receiverCoord);
            }
            if (isCCW) {
                Coordinate intersectionPt = wall.getLine().project(receiverIm);
                Coordinate mirrored = new Coordinate(2 * intersectionPt.x
                        - receiverIm.x, 2 * intersectionPt.y
                        - receiverIm.y, receiverIm.z);
                next = new MirrorReceiverResult(mirrored,
                        parent, wallId, wall.getOriginId(), wall.getType());
                break;
            }
            // MirrorReceiverResult has not been found with this wall
            // Do not fetch sub-reflections
            if(currentWall.size() < maxDepth) {
                wallIdentifierIt.skipLevel();
            }
        }
        current = next;
    }

    private static MirrorReceiverResult fetchParent(MirrorReceiverResult lastOne, List<Integer> currentWall) {
        if(currentWall.size() == 1) {
            return null;
        } else {
            Deque<MirrorReceiverResult> parents = new ArrayDeque<>();
            MirrorReceiverResult cursor = lastOne;
            while(cursor != null) {
                parents.addFirst(cursor);
                cursor = cursor.getParentMirror();
            }
            int walli = 0;
            for(MirrorReceiverResult parent : parents) {
                if(parent.getWallId() != currentWall.get(walli++)) {
                    break;
                } else {
                    cursor = parent;
                }
            }
            return cursor;
        }
    }

    @Override
    public MirrorReceiverResult next() {
        MirrorReceiverResult retValue = current;
        fetchNext();
        return retValue;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Not supported");
    }

    /**
     * Occlusion test on two walls. Segments are CW oriented.
     *
     * @param wall1
     * @param wall2
     * @return True if the walls are face to face
     */
    public static boolean wallWallTest(LineSegment wall1, LineSegment wall2) {
        return ((Orientation.isCCW(new Coordinate[]{wall1.getCoordinate(0), wall1.getCoordinate(1), wall2.getCoordinate(0), wall1.getCoordinate(0)}) || Orientation.isCCW(new Coordinate[]{wall1.getCoordinate(0), wall1.getCoordinate(1), wall2.getCoordinate(1), wall1.getCoordinate(0)})) && (Orientation.isCCW(new Coordinate[]{wall2.getCoordinate(0), wall2.getCoordinate(1), wall1.getCoordinate(0), wall2.getCoordinate(0)}) || Orientation.isCCW(new Coordinate[]{wall2.getCoordinate(0), wall2.getCoordinate(1), wall1.getCoordinate(1), wall2.getCoordinate(0)})));
    }

    /**
     * Occlusion test on two walls. Segments are CW oriented.
     *
     * @param wall1
     * @param pt
     * @return True if the wall is oriented to the point
     */
    public static boolean wallPointTest(LineSegment wall1, Coordinate pt) {
        return Orientation.isCCW(new Coordinate[]{wall1.getCoordinate(0), wall1.getCoordinate(1), pt, wall1.getCoordinate(0)});
    }

    public static final class It implements Iterable<MirrorReceiverResult> {
        private final Coordinate receiverCoord;
        private final List<ProfileBuilder.Wall> nearBuildingsWalls;
        private final int maxDepth;

        public It(Coordinate receiverCoord, List<ProfileBuilder.Wall> nearBuildingsWalls, int maxDepth) {
            this.receiverCoord = receiverCoord;
            this.nearBuildingsWalls = nearBuildingsWalls;
            this.maxDepth = maxDepth;
        }

        @Override
        public java.util.Iterator<MirrorReceiverResult> iterator() {
            return new MirrorReceiverIterator(receiverCoord, nearBuildingsWalls,
                    maxDepth);
        }
    }

    /**
     * Iterates over wall identifier.
     */
    public static final class CrossTableIterator implements Iterator<List<Integer>> {
        private final int maxDepth;
        private final int wallCount;
        private List<Integer> current;
        private int lastDepth = 1;

        public CrossTableIterator(int maxDepth, int wallCount) {
            this.maxDepth = maxDepth;
            this.wallCount = wallCount;
            current = new ArrayList<>();
            if(wallCount > 0) {
                current.add(0);
            }
        }

        public void setNext(List<Integer> next, int lastDepth) {
            this.lastDepth = lastDepth;
            this.current = next;
        }

        /**
         * Skip last returned wall id (do not process sub-walls)
         */
        public void skipLevel() {
            if(!current.isEmpty() && lastDepth < current.size()) {
                while (lastDepth < current.size()) {
                    // remove skipped sub-walls
                    current.remove(lastDepth);
                }
                if(current.size() < lastDepth && wallCount > 1) {
                    current.add(nextVal(-1, current.get(current.size() - 1)));
                } else {
                    do {
                        current.set(current.size() - 1, nextVal(current.get(current.size() - 1),
                                current.size() > 1 ? current.get(current.size() - 2) : -1) );
                        if(current.get(current.size() - 1) >= wallCount) {
                            current.remove(current.size() - 1);
                        } else {
                            break;
                        }
                    } while(!current.isEmpty());
                }
            }
        }

        @Override
        public boolean hasNext() {
            return !current.isEmpty();
        }

        private static int nextVal(int current, int skip) {
            if(current + 1 != skip) {
                return current + 1;
            } else {
                return current + 2;
            }
        }

        @Override
        public List<Integer> next() {
            List<Integer> currentIndex = new ArrayList<>(current);
            lastDepth = current.size();
            // Go to next value
            if(current.size() < maxDepth && wallCount > 1) {
                current.add(nextVal(-1, current.get(current.size() - 1)));
            } else {
                do {
                    current.set(current.size() - 1, nextVal(current.get(current.size() - 1),
                            current.size() > 1 ? current.get(current.size() - 2) : -1) );
                    if(current.get(current.size() - 1) >= wallCount) {
                        current.remove(current.size() - 1);
                    } else {
                        break;
                    }
                } while(!current.isEmpty());
            }
            return currentIndex;
        }

        @Override
        public void remove() {

        }
    }
}
