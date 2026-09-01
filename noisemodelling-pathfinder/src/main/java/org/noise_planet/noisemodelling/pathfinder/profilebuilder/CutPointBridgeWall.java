/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */
package org.noise_planet.noisemodelling.pathfinder.profilebuilder;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineSegment;

import java.util.List;

@JsonTypeName("CutPointBridgeWall")
public class CutPointBridgeWall  extends CutPointWall {
    
    public enum WallDirection {
        UPWARD,
        DOWNWARD,
        OTHER
    }

    private WallDirection wallDirection = WallDirection.UPWARD;
    private boolean mirrorRelax;
    private double bridgeHeight;
    private long nextBridgePk = -1;
    private double nextBridgeHeight = Double.NaN;

    public void setWallDirection(WallDirection wallDirection){
        this.wallDirection = wallDirection;
    }

    public boolean getMirrorRelax() {
        return mirrorRelax;
    }

    public void setMirrorRelax(boolean mirrorRelax) {
        this.mirrorRelax = mirrorRelax;
    }

    public double getBridgeHeight() {
        return bridgeHeight;
    }

    public void setBridgeHeight(double bridgeHeight) {
        this.bridgeHeight = bridgeHeight;
    }


    /** Database primary key value of the obstacle */
    private Long wallPk = null;

    /**
     * Empty constructor for deserialization
     */
    public CutPointBridgeWall() {
        super();
        this.wallDirection = WallDirection.UPWARD;
    }

    public CutPointBridgeWall(int processedWallIndex, Coordinate intersection, LineSegment wallSegment, List<Double> wallAlpha, WallDirection wallDirection, long bridgePk) {
        super(processedWallIndex, intersection, wallSegment, wallAlpha);
        this.wallDirection = wallDirection;
        this.wallPk = bridgePk;
    }
    
    public CutPointBridgeWall(int processedWallIndex, Coordinate intersection, LineSegment wallSegment, List<Double> wallAlpha, long bridgePk) {
        super(processedWallIndex, intersection, wallSegment, wallAlpha);
        this.wallDirection = WallDirection.UPWARD;
        this.wallPk = bridgePk;
    }

    public WallDirection getWallDirection() {
        return wallDirection;
    }

    public Long getWallPk() {
        return wallPk;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("bridgePk")
    public Long getBridgePk(){
        return wallPk;
    }

    @JsonProperty("bridgePk")
    public void setBridgePk(long pk) {
        setPk(pk);
    }
    
    @JsonProperty("nextBridgePk")
    public Long getNextBridgePk() {
        return nextBridgePk;
    }

    public void setNextBridgePk(Long pk) {
        this.nextBridgePk = pk;
    }

    @JsonProperty("nextBridgeHeight")
    public double getNextBridgeHeight() {
        return nextBridgeHeight;
    }

    public void setNextBridgeHeight(double height) {
        this.nextBridgeHeight = height;
    }
    /**
     *
     * @param pk External primary key value, will be updated if {@literal >=} 0
     * @return this
     */
    public CutPointBridgeWall setPk(long pk) {
        if(pk >= 0) {
            this.wallPk = pk;
        }
        return this;
    }

    public void modifyIntersectionHeight(Bridge bridge){
        double newZ = 0.0;
        if (this.wallDirection == WallDirection.UPWARD) {
            newZ = bridge.getDeckHeightAtPoint(coordinate) + bridge.getBarrierHeightAtPoint(coordinate);
        } else {
            newZ = bridge.getDeckHeightAtPoint(coordinate) - bridge.getDeckThicknessAtPoint(coordinate);
        }
        setBridgeHeight(bridge.getDeckHeightAtPoint(coordinate));
        modifyIntersectionHeight(newZ);
    }

    
    public void modifyIntersectionHeight(double z){
        this.coordinate = new Coordinate(coordinate.x, coordinate.y, z);
        this.wall.p0 = new Coordinate(wall.p0.x, wall.p0.y, z);
        this.wall.p1 = new Coordinate(wall.p1.x, wall.p1.y, z);
    }

    /** @return the enter/exit classification of this bridge wall cut point. */
    public INTERSECTION_TYPE getIntersectionType() {
        return intersectionType;
    }

    /** @param intersectionType the enter/exit classification of this bridge wall cut point. */
    public void setIntersectionType(INTERSECTION_TYPE intersectionType) {
        this.intersectionType = intersectionType;
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
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        CutPointBridgeWall that = (CutPointBridgeWall) o;

        if (getBridgePk() != that.getBridgePk()) return false;
        if (mirrorRelax != that.mirrorRelax) return false;
        if (Double.compare(that.bridgeHeight, bridgeHeight) != 0) return false;
        if (nextBridgePk != that.nextBridgePk) return false;
        if (Double.compare(that.nextBridgeHeight, nextBridgeHeight) != 0) return false;
        if (wallDirection != that.wallDirection) return false;
        if (!getCoordinate().equals3D(that.getCoordinate())) return false;
        return true;
    }
    
    @Override
    public int hashCode() {
        int result = 17;
        // include coordinate (x,y,z) if available
        if (getCoordinate() != null) {
            long xBits = Double.doubleToLongBits(getCoordinate().x);
            long yBits = Double.doubleToLongBits(getCoordinate().y);
            long zBits = Double.doubleToLongBits(getCoordinate().z);
            result = 31 * result + (int) (xBits ^ (xBits >>> 32));
            result = 31 * result + (int) (yBits ^ (yBits >>> 32));
            result = 31 * result + (int) (zBits ^ (zBits >>> 32));
        }
        // include bridge primary key
        long pk = (getBridgePk() != null) ? getBridgePk() : -1L;
        result = 31 * result + (int) (pk ^ (pk >>> 32));
        // include wall direction
        result = 31 * result + (wallDirection != null ? wallDirection.ordinal() : 0);
        return result;
    }
}
