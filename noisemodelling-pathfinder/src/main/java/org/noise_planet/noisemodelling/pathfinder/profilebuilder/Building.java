/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.pathfinder.profilebuilder;

import org.locationtech.jts.geom.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Double.isNaN;


public class Building extends Obstruction {
    /** Building coordinates. */
    Polygon poly;

    /** Input relative height of the building. Can be NaN if the relative height is not defined */
    final double relativeHeight;

    /** Does the building coordinates all have Z values */
    boolean hasValidZCoordinates = false;

    /** Is the building definition valid */
    boolean isValid;

    /** Minimum Z ground under building contour */
    double minimumZDEM = Double.NaN;

    /** Primary key of the building in the database. */
    long primaryKey = -1;

    /**
     * Main constructor. setting Alphas version
     * @param poly   {@link Geometry} footprint of the building.
     * @param relativeHeight Height of the building.
     * @param alphas Absorption coefficients.
     * @param key Primary key of the building in the database.
     */
    public Building(Polygon poly, double relativeHeight, List<Double> alphas, long key) {
        this.poly = poly;
        // Fix clock wise orientation of the polygon and inner holes
        this.poly.normalize();
        this.relativeHeight = relativeHeight;
        setAlpha(alphas);
        this.primaryKey = key;

        this.hasValidZCoordinates = this.validateZCoordinates();
        this.isValid = (hasValidZCoordinates || !Double.isNaN(relativeHeight));
    }

    /**
     * Main constructor. setting g version
     * @param poly   {@link Geometry} footprint of the building.
     * @param relativeHeight Height of the building.
     * @param g G value.
     * @param key Primary key of the building in the database.
     */
    public Building(Polygon poly, double relativeHeight, double g, long key) {
        this.poly = poly;
        // Fix clock wise orientation of the polygon and inner holes
        if(this.poly != null) {
            this.poly.normalize();
        }
        this.relativeHeight = relativeHeight;
        setG(g);
        this.primaryKey = key;

        this.hasValidZCoordinates = this.validateZCoordinates();
        this.isValid = (hasValidZCoordinates || !Double.isNaN(relativeHeight));
    }



    /**
     * Forces poly to have Z coordinates.
     * If _any_ point in the polygon doesn't have a valid Z, forces all polygon Z values to 0.0, valid Z are lost in this operation
     */
    public void force3D() {
        if (hasValidZCoordinates) {
            return; // already 3D
        }
        forceZeroZ();
    }

    /**
     * Forces all points in the polygon to have 0.0 z value
     */
    public void forceZeroZ() {
        GeometryFactory f = new GeometryFactory();

        LinearRing shell2D = poly.getExteriorRing();
        Coordinate[] newCoordinate = new Coordinate[shell2D.getNumPoints()];
        for (int idCoordinate=0;idCoordinate<newCoordinate.length;idCoordinate++) {
            newCoordinate[idCoordinate] = new Coordinate(shell2D.getCoordinateN(idCoordinate).getX(),
                    shell2D.getCoordinateN(idCoordinate).getY(), 0.0);
        }

        LinearRing shell3D = f.createLinearRing(newCoordinate);

        LinearRing[] holes = new LinearRing[poly.getNumInteriorRing()];
        for (int idHole=0;idHole<holes.length;idHole++){
            LinearRing lr2D = poly.getInteriorRingN(idHole);
            newCoordinate = new Coordinate[lr2D.getNumPoints()];
            for (int idCoordinate=0;idCoordinate<newCoordinate.length;idCoordinate++) {
                newCoordinate[idCoordinate] = new Coordinate(lr2D.getCoordinateN(idCoordinate).getX(),
                        lr2D.getCoordinateN(idCoordinate).getY(), 0.0);
            }
            holes[idHole]=f.createLinearRing(newCoordinate);
        }

        this.poly = f.createPolygon(shell3D, holes);
    }


    /**
     * Test if all vertex in the building polygon coordinates have a valid Z value (not NaN)
     */
    private boolean validateZCoordinates() {
        return Arrays.stream(this.poly.getCoordinates()).allMatch(coord -> !Double.isNaN(coord.getZ()));
    }

    /**
     * get Height from Building
     * @return height
     */
    public double getRelativeHeight() { return relativeHeight; }


    /**
     * Retrieve the building footprint.
     * @return The building footprint.
     */
    public Polygon getGeometry() {
        return poly;
    }


    /**
     * Retrieve the primary key of the building in the database. If there is no primary key, returns -1.
     * @return The primary key of the building in the database or -1.
     */
    public long getPrimaryKey() {
        return primaryKey;
    }


    /**
     * Compute all polygon points Z (absolute altitude) based on defined relativeHeight and topo if it exists
     * Erases all previous Z values
     * @param profileBuilder
     * @return
     */
    public void applyRelativeHeightAndTopo(ProfileBuilder profileBuilder) {
        for (Coordinate coordinate : this.poly.getCoordinates()) {
            double zTopo = profileBuilder.getZGround(coordinate);
            coordinate.setZ(zTopo + this.relativeHeight);
        }
        this.hasValidZCoordinates = this.validateZCoordinates();
    }

    /**
     * Compute minimum Z ground under the building contour
     * @param profileBuilder
     * @return
     */
    public double updateZTopo(ProfileBuilder profileBuilder) {
        Coordinate[] coordinates = poly.getBoundary().getCoordinates();
        double minZ = Double.MAX_VALUE;
        AtomicInteger triangleHint = new AtomicInteger(-1);
        for (int i = 0; i < coordinates.length-1; i++) {
            minZ = Math.min(minZ, profileBuilder.getZGround(coordinates[i], triangleHint));
        }
        minimumZDEM = minZ;
        return minimumZDEM;
    }

    public double getAverageZ() {
        if (hasValidZCoordinates) {
            return Arrays.stream(poly.getCoordinates()).mapToDouble(Coordinate::getZ).average().getAsDouble();
        } else {
            return minimumZDEM + relativeHeight;
        }
    }
}