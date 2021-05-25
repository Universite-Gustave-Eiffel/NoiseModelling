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
import org.locationtech.jts.math.Matrix;

/**
 * When providing Orientation to a sound source there is 2 cases
 * - Sound source is a point. The final orientation is the same, relative to the north and clock-wise to east.
 * - Sound source is a line. The final orientation is rotated by the line direction.
 * So 0 degrees point the end of the line segment.
 */
public class Orientation {
    public final float bearing;
    public final float inclination;
    public final float roll;

    /**
     * @param bearing     Orientation using degrees east of true north (0 is north, 90 is east)
     * @param inclination Vertical orientation in degrees. (0 flat, 90 vertical top, -90 vertical bottom)
     * @param roll        Longitudinal axis in degrees. A positive value lifts the left wing and lowers the right wing.
     */
    public Orientation(float bearing, float inclination, float roll) {
        this.bearing = (360 + bearing) % 360;
        this.inclination = Math.min(90, Math.max(-90, inclination));
        this.roll = (360 + roll) % 360;
    }

    @Override
    public String toString() {
        return "Orientation{" +
                "bearing=" + bearing +
                ", inclination=" + inclination +
                ", roll=" + roll +
                '}';
    }

    public Orientation rotate(Orientation inputAngle) {
        double[][] b = new double[][]{{Math.toRadians(bearing),0,0}, {0 , Math.toRadians(inclination), 0}, {0, 0 , Math.toRadians(roll)}};
        final double yaw = Math.toRadians(inputAngle.bearing);
        final double pitch = Math.toRadians(inputAngle.inclination);
        final double roll = Math.toRadians(inputAngle.roll);
        double[][] a = new double[][]{
                {Math.cos(yaw) * Math.cos(pitch), Math.cos(yaw) * Math.sin(pitch) * Math.sin(roll)
                        - Math.sin(yaw) * Math.cos(roll), Math.cos(yaw) * Math.sin(pitch) * Math.cos(roll)
                        + Math.sin(yaw) * Math.sin(roll)},
                {Math.sin(yaw) * Math.cos(pitch), Math.sin(yaw) * Math.sin(pitch) * Math.sin(roll)
                        + Math.cos(yaw) * Math.cos(roll), Math.sin(yaw) * Math.sin(pitch) * Math.cos(roll) - Math.cos(yaw) * Math.sin(roll)},
                {-Math.sin(pitch), Math.cos(pitch) * Math.sin(roll), Math.cos(pitch) * Math.cos(roll)}
        };
        RealMatrix matrixA = new Array2DRowRealMatrix(a);
        RealMatrix matrixB = new Array2DRowRealMatrix(b);
        RealMatrix res = matrixA.multiply(matrixB);
        return new Orientation((float) Math.toDegrees(res.getEntry(0,0)), (float) Math.toDegrees(res.getEntry(1,0)), (float) Math.toDegrees(res.getEntry(2,0)));
    }
}
