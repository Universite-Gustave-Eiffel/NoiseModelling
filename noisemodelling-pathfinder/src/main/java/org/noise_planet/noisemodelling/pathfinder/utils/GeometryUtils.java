package org.noise_planet.noisemodelling.pathfinder.utils;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.math.Vector3D;

public class GeometryUtils {
    public static Coordinate projectPointOnSegment(Coordinate P, Vector3D vector, Coordinate pInit) {
        Coordinate A = new Coordinate(pInit.x, pInit.y,pInit.z);
        Coordinate B = new Coordinate(vector.getX()+pInit.x, vector.getY()+pInit.y,vector.getZ()+pInit.z);

        return new Coordinate(A.x+(Vector3D.dot(A,P,A,B) / Vector3D.dot(A,B,A,B))*vector.getX(),
                A.y+(Vector3D.dot(A,P,A,B) / Vector3D.dot(A,B,A,B))*vector.getY(),
                A.z+(Vector3D.dot(A,P,A,B) / Vector3D.dot(A,B,A,B))*vector.getZ());
    }
    public static Coordinate projectPointOnVector(Coordinate P, Vector3D vector,Coordinate pInit) {
        Coordinate A = new Coordinate(pInit.x, pInit.y,pInit.z);
        Coordinate B = new Coordinate(vector.getX()+pInit.x, vector.getY()+pInit.y,vector.getZ()+pInit.z);
        return new Coordinate(A.x+(Vector3D.dot(A,P,A,B) / Vector3D.dot(A,B,A,B))*vector.getX(),
                A.y+(Vector3D.dot(A,P,A,B) / Vector3D.dot(A,B,A,B))*vector.getY(),
                A.z+(Vector3D.dot(A,P,A,B) / Vector3D.dot(A,B,A,B))*vector.getZ());
    }
    public static Coordinate projectPointOnLine(Coordinate c, double a, double b) {
        double x = (c.x-a*b+a*c.y)/(1+a*a);
        double y = b+a*(c.x-a*b+a*c.y)/(1+a*a);
        return new Coordinate(x, y);
    }
}
