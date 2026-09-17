/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */
package org.noise_planet.noisemodelling.propagation;

import org.locationtech.jts.algorithm.Angle;
import org.locationtech.jts.geom.Coordinate;

import java.util.Arrays;

/**
 * Discrete favourable probability implementation
 */
public class DiscreteFavourableProbability implements FavourableProbability {
    // Favourable probability for each direction
    public static final double[] DEFAULT_WIND_ROSE = new double[]{0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5};

    private final double angleSection;
    final double[] windRose;

    public DiscreteFavourableProbability() {
        this(DEFAULT_WIND_ROSE);
    }

    public DiscreteFavourableProbability(double[] windRose) {
        this.windRose = windRose;
        this.angleSection = (2 * Math.PI) / windRose.length;
    }

    /**
     * The north slice is the last array index not the first one
     * Ex for slice width of 20°:
     *      - The first column 20° contain winds between 10 to 30 °
     *      - The last column 360° contains winds between 350° to 360° and 0 to 10°
     * get the rose index to search the mean occurrence p of favourable conditions in the direction of the angle:
     * @return rose index
     */
    public int getRoseIndex(double angle) {
        // Angle from cos -1 sin 0
        double angleRad = -(angle - Math.PI);
        // Offset angle by PI / 2 (North),
        // the north slice ranges is [PI / 2 + angle_section / 2; PI / 2 - angle_section / 2]
        angleRad -= (Math.PI / 2 - angleSection / 2);
        // Fix out of bounds angle 0-2Pi
        if(angleRad < 0) {
            angleRad += Math.PI * 2;
        }
        int index = (int)(angleRad / angleSection) - 1;
        if(index < 0) {
            index = windRose.length - 1;
        }
        return index;
    }

    /**
     * get the rose index to search the mean occurrence p of favourable conditions in the direction of the path (S,R):
     * @param receiver
     * @param source
     * @return rose index
     */
    public int getRoseIndex(Coordinate receiver, Coordinate source) {
        return getRoseIndex(Angle.angle(receiver, source));
    }

    @Override
    public double getFavourableProbability(double angle) {
        return windRose[getRoseIndex(angle)];
    }

    @Override
    public String getFavourableProbabilitySettings() {
        return String.join(", ", Arrays.stream(windRose).mapToObj(String::valueOf).toArray(String[]::new));
    }
}
