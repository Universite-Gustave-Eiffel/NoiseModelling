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
import java.util.Collections;
import java.util.List;
import java.util.OptionalDouble;
import java.util.concurrent.atomic.AtomicInteger;

public class Building extends Obstruction {
    /** Building coordinates. */
    Polygon poly;

    /** Is the building definition valid? */
    boolean isValid;

    /** Primary key of the building in the database. */
    long primaryKey;

    /**
     * Main constructor. setting Alphas version and or g version
     * @param poly   {@link Geometry} of the building.
     * @param alphas Absorption coefficients.
     * @param key Primary key of the building in the database.
     */
    public Building(Polygon poly, List<Double> alphas, double g, long key) {
        this.poly = poly;
        // Fix clock wise orientation of the polygon and inner holes
        this.poly.normalize();
        this.primaryKey = key;

        if (!alphas.isEmpty()) {
            setAlpha(alphas);
        }
        if (!Double.isNaN(g)) {
            setG(g);
        }

        isValid = validateZCoordinates();
    }

    /**
     * Main constructor. setting Alphas version
     * @param poly   {@link Geometry} footprint of the building.
     * @param alphas Absorption coefficients.
     * @param key Primary key of the building in the database.
     */
    public Building(Polygon poly, List<Double> alphas, long key) {
        this(poly, alphas, Double.NaN, key);
    }

    /**
     * Main constructor. setting g version
     * @param poly   {@link Geometry} footprint of the building.
     * @param g G value.
     * @param key Primary key of the building in the database.
     */
    public Building(Polygon poly, double g, long key) {
        this(poly, Collections.emptyList(), g, key);
    }

    /**
     * Test if all vertices in the building polygon coordinates have a valid Z value (not NaN)
     */
    private boolean validateZCoordinates() {
        return Arrays.stream(this.poly.getCoordinates()).noneMatch(coord -> Double.isNaN(coord.getZ()));
    }

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

    public double getAverageZ() {
        return Arrays.stream(poly.getCoordinates()).mapToDouble(Coordinate::getZ).average().orElse(0.0);
    }
}