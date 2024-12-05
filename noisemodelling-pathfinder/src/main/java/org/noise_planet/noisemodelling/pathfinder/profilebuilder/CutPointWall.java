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
import org.locationtech.jts.geom.LineSegment;

public class CutPointWall  extends CutPoint {
    /**
     * x,y,z coordinates of the top segment of the wall that intersects the vertical cut plane
     * z is altitude
     */
    public LineSegment wall;

    /**
     * Unique external identifier of the wall. Could be the primary key of the related building in the database
     */
    public long wallPrimaryKey;

    /**
     * Obstacle index in the subdomain
     * @see ProfileBuilder#processedWalls
     */
    @JsonIgnore
    public int obstacleIndex = -1;

}
