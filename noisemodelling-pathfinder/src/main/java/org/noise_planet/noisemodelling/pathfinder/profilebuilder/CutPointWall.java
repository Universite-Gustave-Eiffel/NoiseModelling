/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */
package org.noise_planet.noisemodelling.pathfinder.profilebuilder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineSegment;

import java.util.Collections;
import java.util.List;

public class CutPointWall  extends CutPoint {
    /**
     * x,y,z coordinates of the top segment of the wall that intersects the vertical cut plane
     * z is altitude
     */
    public LineSegment wall;

    /** Wall absorption coefficient per frequency band.*/
    public List<Double> wallAlpha = Collections.emptyList();

    /**
     * Obstacle index in the subdomain
     * @see ProfileBuilder#processedWalls
     */
    @JsonIgnore
    public int processedWallIndex = -1;

    /**
     * Empty constructor for deserialization
     */
    public CutPointWall() {
    }

    public CutPointWall(int processedWallIndex, Coordinate intersection, LineSegment wallSegment, List<Double> wallAlpha) {
        this.wall = wallSegment;
        this.coordinate = intersection;
        this.processedWallIndex = processedWallIndex;
        this.wallAlpha = wallAlpha;
    }

    /**
     * Copy constructor
     * @param other Other instance
     */
    public CutPointWall(CutPointWall other) {
        super(other);
        this.wall = other.wall;
        this.wallAlpha = other.wallAlpha;
        this.processedWallIndex = other.processedWallIndex;
    }

    /**
     * @return Convert alpha values to a java array
     */
    public double[] alphaAsArray() {
        return wallAlpha.stream().mapToDouble(aDouble -> aDouble).toArray();
    }

    @Override
    public String toString() {
        return "CutPointWall{" +
                "groundCoefficient=" + groundCoefficient +
                ", zGround=" + zGround +
                ", coordinate=" + coordinate +
                ", processedWallIndex=" + processedWallIndex +
                ", wallAlpha=" + wallAlpha +
                ", wall=" + wall +
                '}';
    }
}
