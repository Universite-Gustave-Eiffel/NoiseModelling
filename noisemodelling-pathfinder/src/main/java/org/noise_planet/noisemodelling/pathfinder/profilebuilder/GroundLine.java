package org.noise_planet.noisemodelling.pathfinder.profilebuilder;

import org.locationtech.jts.geom.LineSegment;

public class GroundLine extends LineObstruction {

    public GroundLine(LineSegment line, int originId) {
        this.line = line;
        this.originId = originId;
    }
}
