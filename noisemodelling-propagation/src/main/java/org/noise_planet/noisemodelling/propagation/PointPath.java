package org.noise_planet.noisemodelling.propagation;

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
    public List<Double> alphaWall = MeshBuilder.ALPHA_DEFAULT_VALUE; // only if POINT_TYPE = REFL, alpha coefficient
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

    /**
     * parameters given by user
     * @param coordinate
     * @param altitude
     * @param gs
     * @param alpha
     * @param buildingId
     * @param type
     */
    public PointPath(Coordinate coordinate, double altitude, double gs, double alpha, int buildingId, POINT_TYPE type) {
        this.coordinate = coordinate;
        this.altitude = altitude;
        this.gs = gs;
        this.alphaWall = Collections.nCopies(PropagationProcessPathData.freq_lvl.size(), alpha);
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
        for (int j = 0; j< PropagationProcessPathData.freq_lvl.size(); j++){
            out.writeDouble(alphaWall.get(j));
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
        ArrayList<Double> readAlpha = new ArrayList<>(PropagationProcessPathData.freq_lvl.size());
        for (int j = 0; j< PropagationProcessPathData.freq_lvl.size(); j++){
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
