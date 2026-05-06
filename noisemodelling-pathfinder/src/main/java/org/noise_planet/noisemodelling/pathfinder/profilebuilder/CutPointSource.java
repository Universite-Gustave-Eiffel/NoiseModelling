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
import org.locationtech.jts.geom.Coordinate;
import org.noise_planet.noisemodelling.pathfinder.PathFinder;
import org.noise_planet.noisemodelling.pathfinder.utils.geometry.Orientation;

public class CutPointSource  extends CutPoint {

    /**
     * External identifier of the source (from table primary key)
     */
    public long sourcePk = -1;

    public CutPointSource() {
    }

    public CutPointSource(Coordinate location) {
        super(location);
    }

    public CutPointSource(Coordinate coordinate, double li) {
        super(coordinate);
        this.li = li;
    }

    /**
     * Generate default point source without information on DEM (source at 0.05 above ground level)
     * @param sourcePointInfo
     */
    public CutPointSource(PathFinder.SourcePointInfo sourcePointInfo) {
        super(sourcePointInfo.position, sourcePointInfo.position.z - 0.05, 0);
        this.sourcePk = sourcePointInfo.getSourcePk();
        this.li = sourcePointInfo.li;
        this.orientation = sourcePointInfo.orientation;
        this.id = sourcePointInfo.sourceIndex;
    }

    public CutPointSource(CutPoint src) {
        super(src);
    }

    /** Source line subdivision length (1.0 means a point is representing 1 meter of line sound source) */
    public double li = 1.0;

    /**
     * Orientation of the point (should be about the source or receiver point)
     * The orientation is related to the directivity associated to the object
     */
    public Orientation orientation = new Orientation();

    /**
     * Index in the subdomain
     */
    @JsonIgnore
    public int id = -1;

    @Override
    public String toString() {
        return "CutPointSource{" +
                "\nsourcePk=" + sourcePk +
                "\n, li=" + li +
                "\n, orientation=" + orientation +
                "\n, id=" + id +
                "\n, coordinate=" + coordinate +
                "\n, zGround=" + zGround +
                "\n, groundCoefficient=" + groundCoefficient +
                "\n}\n";
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        CutPointSource that = (CutPointSource) o;
        return sourcePk == that.sourcePk && id == that.id;
    }

    @Override
    public int hashCode() {
        int result = Long.hashCode(sourcePk);
        result = 31 * result + id;
        return result;
    }
}
