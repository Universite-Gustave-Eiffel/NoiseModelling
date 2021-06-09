package org.noise_planet.noisemodelling.pathfinder;

import org.locationtech.jts.geom.Coordinate;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PointPath {

    // given by user
    public Coordinate coordinate; // coordinate (absolute)
    public double altitude; // altitude of relief (exact)
    public double gs;       // only if POINT_TYPE = SRCE or RECV, G coefficient right above the point
    public List<Double> alphaWall = Collections.unmodifiableList(Arrays.asList(0.1,0.1,0.1,0.1,0.1,0.1,0.1,0.1));; // only if POINT_TYPE = REFL, alpha coefficient
    public int buildingId; // only if POINT_TYPE = REFL
    public POINT_TYPE type; // type of point
    public enum POINT_TYPE {
        SRCE,
        REFL,
        DIFV,
        DIFH,
        RECV
    }

    /**
     * parameters given by user
     * @param coordinate
     * @param altitude
     * @param gs
     * @param alphaWall
     * @param buildingId
     * @param type
     */
    public PointPath(Coordinate coordinate, double altitude, double gs, List<Double> alphaWall, int buildingId, POINT_TYPE type) {
        this.coordinate = coordinate;
        this.altitude = altitude;
        this.gs = gs;
        this.alphaWall = alphaWall;
        this.buildingId = buildingId;
        this.type = type;
    }

    /**
     * parameters given by user
     * @param cutPoint CutPoint to use to generate the PointPath
     * @param defaultType Defaut point type to use if the cut point is nor a source, nor a receiver.
     */
    public PointPath(ProfileBuilder.CutPoint cutPoint, POINT_TYPE defaultType, double gs) {
        this.coordinate = cutPoint.getCoordinate();
        this.altitude = cutPoint.getTopoHeight() + cutPoint.getBuildingHeight();
        this.gs = gs;
        this.alphaWall = cutPoint.getWallAlpha();
        this.buildingId = cutPoint.getBuildingId();
        this.type = cutPoint.getType().toPointType(defaultType);
    }

    /**
     * parameters given by user
     * @param coordinate
     * @param altitude
     * @param gs
     * @param alphaWall
     * @param buildingId
     * @param type
     */
    public PointPath(Coordinate coordinate, double altitude, double gs, double[] alphaWall, int buildingId, POINT_TYPE type) {
        this.coordinate = coordinate;
        this.altitude = altitude;
        this.gs = gs;
        this.alphaWall = new ArrayList<>(alphaWall.length);
        for(double a : alphaWall) {
            this.alphaWall.add(a);
        }
        this.buildingId = buildingId;
        this.type = type;
    }

    public PointPath() {

    }


    /**
     * Writes the content of this object into <code>out</code>.
     * @param out the stream to write into
     * @throws java.io.IOException if an I/O-error occurs
     */
    public void writeStream( DataOutputStream out ) throws IOException {
        PropagationPath.writeCoordinate(out, coordinate);
        out.writeDouble(altitude);
        out.writeDouble(gs);
        out.writeShort(alphaWall.size());
        for (Double bandAlpha : alphaWall) {
            out.writeDouble(bandAlpha);
        }
        out.writeInt(buildingId);
        out.writeInt(type.ordinal());
    }

    /**
     * Reads the content of this object from <code>out</code>. All
     * properties should be set to their default value or to the value read
     * from the stream.
     * @param in the stream to read
     * @throws IOException if an I/O-error occurs
     */
    public void readStream( DataInputStream in ) throws IOException {
        coordinate = PropagationPath.readCoordinate(in);
        altitude = in.readDouble();
        gs = in.readDouble();
        int nbFreq = in.readShort();
        ArrayList<Double> readAlpha = new ArrayList<>(nbFreq);
        for (int j = 0; j< nbFreq; j++){
            readAlpha.add(in.readDouble());
        }
        this.alphaWall = readAlpha;
        buildingId = in.readInt();
        type = POINT_TYPE.values()[in.readInt()];
    }

    public void setType(POINT_TYPE type) {
        this.type =  type;
    }

    public void setBuildingId(int buildingId) {
        this.buildingId =  buildingId;
    }

    public void setAlphaWall(List<Double> alphaWall) {
        this.alphaWall = new ArrayList<>(alphaWall);
    }

    public int getBuildingId() {
        return buildingId;
    }


    public void setCoordinate(Coordinate coordinate) {
        this.coordinate =  coordinate;
    }
}
