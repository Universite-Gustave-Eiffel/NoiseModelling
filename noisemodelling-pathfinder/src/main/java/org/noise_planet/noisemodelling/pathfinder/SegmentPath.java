package org.noise_planet.noisemodelling.pathfinder;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.math.Vector3D;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class SegmentPath {

    //  given by user
    public double gPath;          // G coefficient for the considered path segment
    public Vector3D vector3D;     // mean Plane for the considered path segment
    public Coordinate pInit;     // init point to compute the mean Plane

    // computed in AugmentedSegments
    public int idPtStart;               //start point indice for the considered path segment
    public int idPtFinal;               //final point indice for the considered path segment

    public Double gPathPrime = null;    //Gpath prime , calculated from Gpath and geometry
    public Double gw = null;
    public Double gm = null;
    public Double zs = null;
    public Double zr = null;
    public Double zsPrime = null;
    public Double zrPrime = null;
    public Double testForm = null;
    public Double testFormPrime = null;

    public Double dPath; // pass by points
    public Double d ; // direct ray between source and receiver
    public Double dc; // direct ray sensible to meteorological conditions (can be curve) between source and receiver
    public Double dp; // distance on mean plane between source and receiver
    public Double eLength = 0.0; // distance between first and last diffraction point
    public Double delta;


    public void setDelta(Double delta) {
        this.delta = delta;
    }

    public Double getDelta() {
        return delta;
    }

    /**
     * @param gPath
     */

    public SegmentPath(double gPath, Vector3D vector3D, Coordinate pInit) {
        this.gPath = gPath;
        this.vector3D = vector3D;
        this.pInit = pInit;
    }

    public SegmentPath() {
    }


    /**
     * Writes the content of this object into <code>out</code>.
     * @param out the stream to write into
     * @throws java.io.IOException if an I/O-error occurs
     */
    public void writeStream( DataOutputStream out ) throws IOException {
        out.writeDouble(gPath);
        PropagationPath.writeVector(out, vector3D);
        PropagationPath.writeCoordinate(out, pInit);
    }

    /**
     * Reads the content of this object from <code>out</code>. All
     * properties should be set to their default value or to the value read
     * from the stream.
     * @param in the stream to read
     * @throws IOException if an I/O-error occurs
     */
    public void readStream( DataInputStream in ) throws IOException {
        gPath = in.readDouble();
        vector3D = PropagationPath.readVector(in);
        pInit = PropagationPath.readCoordinate(in);
    }


    public void setGw(double g) {
        this.gw = g;
    }

    public void setGm(double g) {
        this.gm = g;
    }

    public Double getgPathPrime(PropagationPath path) {
        if(gPathPrime == null) {
            path.computeAugmentedSegments();
        }
        return gPathPrime;
    }

    public Double getGw() {
        return gw;
    }

    public Double getGm() {
        return gm;
    }

    public Double getZs(PropagationPath path, SegmentPath segmentPath) {
        if(zs == null) {
            zs = path.computeZs(segmentPath);
        }
        return zs;
    }

    public Double getZr(PropagationPath path, SegmentPath segmentPath) {
        if(zr == null) {
            zr = path.computeZr(segmentPath);
        }
        return zr;
    }

    public Double getZsPrime(PropagationPath path, SegmentPath segmentPath) {
        if(zsPrime == null) {
            zsPrime = path.computeZsPrime(segmentPath);
        }
        return zsPrime;
    }

    public Double getZrPrime(PropagationPath path, SegmentPath segmentPath) {
        if(zrPrime == null) {
            zrPrime = path.computeZrPrime(segmentPath);
        }
        return zrPrime;
    }
}
