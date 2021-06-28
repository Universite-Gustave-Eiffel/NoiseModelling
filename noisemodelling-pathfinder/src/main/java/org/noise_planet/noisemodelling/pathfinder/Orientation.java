/**
 * NoiseMap is a scientific computation plugin for OrbisGIS developed in order to
 * evaluate the noise impact on urban mobility plans. This model is
 * based on the French standard method NMPB2008. It includes traffic-to-noise
 * sources evaluation and sound propagation processing.
 * <p>
 * This version is developed at French IRSTV Institute and at IFSTTAR
 * (http://www.ifsttar.fr/) as part of the Eval-PDU project, funded by the
 * French Agence Nationale de la Recherche (ANR) under contract ANR-08-VILL-0005-01.
 * <p>
 * Noisemap is distributed under GPL 3 license. Its reference contact is Judicaël
 * Picaut <judicael.picaut@ifsttar.fr>. It is maintained by Nicolas Fortin
 * as part of the "Atelier SIG" team of the IRSTV Institute <http://www.irstv.fr/>.
 * <p>
 * Copyright (C) 2011 IFSTTAR
 * Copyright (C) 2011-2012 IRSTV (FR CNRS 2488)
 * <p>
 * Noisemap is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p>
 * Noisemap is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * Noisemap. If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * For more information, please consult: <http://www.orbisgis.org/>
 * or contact directly:
 * info_at_ orbisgis.org
 */
package org.noise_planet.noisemodelling.pathfinder;


import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.locationtech.jts.math.Vector3D;

import java.util.Objects;

/**
 * When providing Orientation to a sound source there is 2 cases
 * - Sound source is a point. The final orientation is the same, relative to the north and clock-wise to east.
 * - Sound source is a line. The final orientation is rotated by the line direction.
 * So 0 degrees point the end of the line segment.
 */
public class Orientation {
    public final float yaw;
    public final float pitch;
    public final float roll;

    /**
     * @param yaw     Orientation using degrees east of true north (0 is north, 90 is east)
     * @param pitch Vertical orientation in degrees. (0 flat, 90 vertical top, -90 vertical bottom)
     * @param roll        Longitudinal axis in degrees. A positive value lifts the left wing and lowers the right wing.
     */
    public Orientation(float yaw, float pitch, float roll) {
        this.yaw = (360 + yaw) % 360;
        this.pitch = Math.min(90, Math.max(-90, pitch));
        this.roll = (360 + roll) % 360;
    }

    @Override
    public String toString() {
        return "Orientation{" +
                "yaw=" + yaw +
                ", pitch=" + pitch +
                ", roll=" + roll +
                '}';
    }

    /**
     * Rotate the vector by the provided orientation and return the result vector orientation
     * @param orientation Rotation to apply
     * @param vector Vector to rotate
     * @return New vector orientation
     */
    public static Vector3D rotate(Orientation orientation, Vector3D vector) {
        return rotate(orientation, vector, false);
    }

    /**
     * Rotate the vector by the provided orientation and return the result vector orientation
     * @param orientation Rotation to apply
     * @param vector Vector to rotate
     * @param inverse True to inverse rotation
     * @return New vector orientation
     */
    public static Vector3D rotate(Orientation orientation, Vector3D vector, boolean inverse) {
        // Coordinate system of the orientation is Y+ North X+ East
        // Y+ must be yaw = 0
        // X+ must be yaw = 90
        double[] b = new double[]{vector.getY(), vector.getX(), vector.getZ()};
        final double yaw = Math.toRadians(orientation.yaw);
        final double pitch = Math.toRadians(orientation.pitch);
        final double roll = Math.toRadians(orientation.roll);
        final double c1 = Math.cos(yaw);
        final double s1 = Math.sin(yaw);
        final double c2 = Math.cos(- pitch);
        final double s2 = Math.sin(- pitch);
        final double c3 = Math.cos(roll);
        final double s3 = Math.sin(roll);
        // https://en.wikipedia.org/wiki/Euler_angles#Rotation_matrix
        double[][] a = new double[][]{
                {c1 * c2, c1 * s2 * s3 - s1 * c3, c1 * s2 * c3 + s1 * s3},
                {s1 * c2, s1 * s2 * s3 + c1 * c3, s1 * s2 * c3 - c1 * s3},
                {-s2, c2 * s3, c2 * c3}
        };
        RealMatrix matrixA = new Array2DRowRealMatrix(a);
        if(inverse) {
            matrixA = matrixA.transpose();
        }
        RealMatrix matrixB = new Array2DRowRealMatrix(b);
        RealMatrix res = matrixA.multiply(matrixB);
        return new Vector3D(res.getEntry(0, 0),
                res.getEntry(1, 0),
                res.getEntry(2, 0));
    }

    public static Orientation fromVector(Vector3D vector, float roll) {
        double newYaw = Math.atan2(vector.getY(), vector.getX());
        double newPitch = Math.asin(vector.getZ());
        return new Orientation((float) Math.toDegrees(newYaw), (float) Math.toDegrees(newPitch), roll);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Orientation that = (Orientation) o;
        return Float.compare(that.yaw, yaw) == 0 &&
                Float.compare(that.pitch, pitch) == 0 &&
                Float.compare(that.roll, roll) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(yaw, pitch, roll);
    }
}
