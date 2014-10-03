package org.orbisgis.noisemap.core;

import com.vividsolutions.jts.algorithm.CGAlgorithms;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineSegment;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

/**
 * Iterator through mirrored receiver
 * @author Nicolas Fortin
 */
public class MirrorReceiverIterator implements Iterator<MirrorReceiverResult> {
    private final Coordinate receiverCoord;
    private final List<FastObstructionTest.Wall> nearBuildingsWalls;
    private final LineSegment srcReceiver;
    private final double distanceLimitation;
    private final double sourceDistanceLimitation;
    // Wall stack
    private CrossTableIterator wallIdentifierIt;
    private MirrorReceiverResult parent = null;
    private MirrorReceiverResult current = null;

    private MirrorReceiverIterator(Coordinate receiverCoord, List<FastObstructionTest.Wall> nearBuildingsWalls,
                                  LineSegment srcReceiver, double distanceLimitation, int maxDepth) {
        this.receiverCoord = receiverCoord;
        this.nearBuildingsWalls = nearBuildingsWalls;
        this.srcReceiver = srcReceiver;
        this.distanceLimitation = distanceLimitation;
        this.wallIdentifierIt = new CrossTableIterator(maxDepth, nearBuildingsWalls.size());
        this.sourceDistanceLimitation = 200;
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
        while(wallIdentifierIt.hasNext()) {
            List<Integer> currentWall = wallIdentifierIt.next();
            wallIdentifierIt.setSkipLevel(true);
            int wallId = currentWall.get(currentWall.size() - 1);
            FastObstructionTest.Wall wall = nearBuildingsWalls.get(wallId);
            //Counter ClockWise test. Walls vertices are CCW oriented.
            //This help to test if a wall could see a point or another wall
            //If the triangle formed by two point of the wall + the receiver is CCW then the wall is oriented toward the point.
            boolean isCCW;
            if (parent == null) { //If it is the first depth wall
                isCCW = MirrorReceiverIterator.wallPointTest(wall, receiverCoord);
            } else {
                //Call wall visibility test
                isCCW = MirrorReceiverIterator.wallWallTest(nearBuildingsWalls.get(parent.getWallId()), wall);
            }
            if (isCCW) {
                Coordinate intersectionPt = wall.project(receiverCoord);
                if (wall.distance(srcReceiver) < distanceLimitation) // Test maximum distance constraint
                {
                    Coordinate mirrored = new Coordinate(2 * intersectionPt.x
                            - receiverCoord.x, 2 * intersectionPt.y
                            - receiverCoord.y, receiverCoord.z);
                    if (srcReceiver.p0.distance(mirrored) < sourceDistanceLimitation) {
                        parent = fetchParent(current, currentWall);
                        current = new MirrorReceiverResult(mirrored,
                                parent , wallId, wall.getBuildingId());
                        wallIdentifierIt.setSkipLevel(false);
                        break;
                    }
                }
            }
        }
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
     * Occlusion test on two walls. Segments are CCW oriented.
     *
     * @param wall1
     * @param wall2
     * @return True if the walls are face to face
     */
    public static boolean wallWallTest(LineSegment wall1, LineSegment wall2) {
        return ((CGAlgorithms.isCCW(new Coordinate[]{wall1.getCoordinate(0), wall1.getCoordinate(1), wall2.getCoordinate(0), wall1.getCoordinate(0)}) || CGAlgorithms.isCCW(new Coordinate[]{wall1.getCoordinate(0), wall1.getCoordinate(1), wall2.getCoordinate(1), wall1.getCoordinate(0)})) && (CGAlgorithms.isCCW(new Coordinate[]{wall2.getCoordinate(0), wall2.getCoordinate(1), wall1.getCoordinate(0), wall2.getCoordinate(0)}) || CGAlgorithms.isCCW(new Coordinate[]{wall2.getCoordinate(0), wall2.getCoordinate(1), wall1.getCoordinate(1), wall2.getCoordinate(0)})));
    }

    /**
     * Occlusion test on two walls. Segments are CCW oriented.
     *
     * @param wall1
     * @param pt
     * @return True if the wall is oriented to the point
     */
    public static boolean wallPointTest(LineSegment wall1, Coordinate pt) {
        return CGAlgorithms.isCCW(new Coordinate[]{wall1.getCoordinate(0), wall1.getCoordinate(1), pt, wall1.getCoordinate(0)});
    }

    public static class It implements Iterable<MirrorReceiverResult> {
        private final Coordinate receiverCoord;
        private final List<FastObstructionTest.Wall> nearBuildingsWalls;
        private final LineSegment srcReceiver;
        private final double distanceLimitation;
        private final int maxDepth;

        public It(Coordinate receiverCoord, List<FastObstructionTest.Wall> nearBuildingsWalls,
                  LineSegment srcReceiver, double distanceLimitation, int maxDepth) {
            this.receiverCoord = receiverCoord;
            this.nearBuildingsWalls = nearBuildingsWalls;
            this.srcReceiver = srcReceiver;
            this.distanceLimitation = distanceLimitation;
            this.maxDepth = maxDepth;
        }

        @Override
        public java.util.Iterator<MirrorReceiverResult> iterator() {
            return new MirrorReceiverIterator(receiverCoord, nearBuildingsWalls, srcReceiver, distanceLimitation, maxDepth);
        }
    }

    /**
     * Iterates over wall identifier.
     */
    public static class CrossTableIterator implements Iterator<List<Integer>> {
        private final int maxDepth;
        private final int wallCount;
        private List<Integer> current;
        private boolean skipLevel = false;

        public CrossTableIterator(int maxDepth, int wallCount) {
            this.maxDepth = maxDepth;
            this.wallCount = wallCount;
            current = new ArrayList<>();
            if(wallCount > 0) {
                current.add(0);
            }
        }

        public void setSkipLevel(boolean skipLevel) {
            this.skipLevel = skipLevel;
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
            // Go to next value
            if(current.size() < maxDepth && !skipLevel) {
                skipLevel = false;
                current.add(nextVal(-1, current.get(current.size() - 1)));
            } else {
                skipLevel = false;
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
