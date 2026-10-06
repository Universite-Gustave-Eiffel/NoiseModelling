package org.noise_planet.noisemodelling.pathfinder.profilebuilder;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineSegment;

public class LineObstruction extends Obstruction {

    public LineSegment line;

    /** Id or index of the source building or topographic triangle. */
    public int originId;

    public int processedObstructionIndex;

    public LineSegment getLineSegment() {
        return line;
    }

    public Coordinate p0() {
        return line.p0;
    }

    public Coordinate p1() {
        return line.p1;
    }

    /**
     * Retrieve the id or index of the source building or topographic triangle.
     * @return Id or index of the source building or topographic triangle.
     */
    public int getOriginId() {
        return originId;
    }

    /**
     * @return Index of this wall in the ProfileBuild list
     */
    public int getProcessedObstructionIndex() {
        return processedObstructionIndex;
    }

    /**
     * @param processedObstructionIndex Index of this wall in the ProfileBuild list
     */
    public LineObstruction setProcessedObstructionIndex(int processedObstructionIndex) {
        this.processedObstructionIndex = processedObstructionIndex;
        return this;
    }
}
