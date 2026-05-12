/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.jdbc.output;

import org.noise_planet.noisemodelling.pathfinder.PathFinder;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutPointSource;

import java.util.Objects;

/**
 * Unique identifier for one discretized source point in the maxError bookkeeping.
 * We cannot use the coordinate alone because distinct source points may legitimately
 * share the same position while belonging to different source identifiers.
 * A line source will create multiple source points with the same primary key and index but with different coordinates.
 */
final class SourcePointKey {
    final long sourcePk;
    final int sourceIndex;
    final long x;
    final long y;
    final long z;

    SourcePointKey(PathFinder.SourcePointInfo sourcePointInfo) {
        this.sourcePk = sourcePointInfo.sourcePk;
        this.sourceIndex = sourcePointInfo.sourceIndex;
        this.x = Double.doubleToLongBits(sourcePointInfo.position.x);
        this.y = Double.doubleToLongBits(sourcePointInfo.position.y);
        this.z = Double.doubleToLongBits(sourcePointInfo.position.z);
    }

    SourcePointKey(CutPointSource sourcePoint) {
        this.sourcePk = sourcePoint.sourcePk;
        this.sourceIndex = sourcePoint.id;
        this.x = Double.doubleToLongBits(sourcePoint.coordinate.x);
        this.y = Double.doubleToLongBits(sourcePoint.coordinate.y);
        this.z = Double.doubleToLongBits(sourcePoint.coordinate.z);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SourcePointKey)) return false;
        SourcePointKey that = (SourcePointKey) o;
        return sourcePk == that.sourcePk && sourceIndex == that.sourceIndex &&
                x == that.x && y == that.y && z == that.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourcePk, sourceIndex, x, y, z);
    }
}
