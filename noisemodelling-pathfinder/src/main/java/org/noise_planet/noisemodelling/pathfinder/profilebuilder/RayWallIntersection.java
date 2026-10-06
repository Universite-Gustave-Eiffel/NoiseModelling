/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */
package org.noise_planet.noisemodelling.pathfinder.profilebuilder;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineSegment;

import java.util.Objects;

/**
 * Value object describing the intersection between an acoustic ray and a processed wall.
 */
public class RayWallIntersection {
    private final Coordinate intersection;
    private final LineSegment ray;
    private final Wall processedWall;
    private final int wallIndex;

    public RayWallIntersection(LineSegment ray, ProcessedWallService processedWallService, int wallIndex) {
      this.wallIndex = wallIndex;
      this.processedWall = processedWallService.getProcessedWalls().get(wallIndex);
      this.ray = ray;
      this.intersection = ray.intersection(processedWall.getLineSegment());
    }

    public boolean hasValidIntersection() {
        return intersection != null;
    }

    public void setZonWall(){
        final Coordinate wp0 = processedWall.p0();
        final Coordinate wp1 = processedWall.p1();
        if (!Double.isNaN(wp0.z) && !Double.isNaN(wp1.z)) {
            if (Double.compare(wp0.z, wp1.z) == 0) {
                intersection.z = wp0.z;
            } else {
                LineSegment wallSegment = processedWall.getLineSegment();
                double ratio = wallSegment.p0.distance(intersection) / wallSegment.getLength();
                intersection.z = wp0.z + ratio * (wp1.z - wp0.z);
            }
        }
    }

    public ProfileBuilder.IntersectionType getType(){
      return processedWall.getType();
    }

    public Coordinate getIntersection() {
        return intersection;
    }

    public LineSegment getRay() {
        return ray;
    }

    public Wall getProcessedWall() {
        return processedWall;
    }

    public int getWallIndex() {
        return wallIndex;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        RayWallIntersection that = (RayWallIntersection) obj;

        return wallIndex == that.wallIndex
                && Objects.equals(intersection, that.intersection)
                && Objects.equals(ray, that.ray)
                && Objects.equals(processedWall, that.processedWall);
    }

    @Override
    public int hashCode() {
        return Objects.hash(intersection, ray, processedWall, wallIndex);
    }

    @Override
    public String toString() {
        return "RayWallIntersection{" +
                "intersection=" + intersection +
                ", ray=" + ray +
                ", processedWall=" + processedWall +
                ", wallIndex=" + wallIndex +
                '}';
    }
}