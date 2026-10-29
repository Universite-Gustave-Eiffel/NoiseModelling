/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */
package org.noise_planet.noisemodelling.propagation;


import java.util.Arrays;
import java.util.Collection;

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

    public DiscreteFavourableProbability(Collection<Double> windRose) {
        this(windRose.stream().mapToDouble(Double::doubleValue).toArray());
    }

    /**
     * Create a discrete favourable probability with the same favourable probability for all directions
     * @param favourableProbability the favourable probability for all directions
     */
    public DiscreteFavourableProbability(double favourableProbability) {
        double[] allDirections = new double[DEFAULT_WIND_ROSE.length]; // Assuming 16 directions
        Arrays.fill(allDirections, favourableProbability);
        this(allDirections);
    }

    /**
     * Get the rose index holding the mean occurrence of favourable conditions in the given direction.
     * The rose is ordered clockwise starting just after north: index 0 covers the sector centred on
     * one sector width clockwise from north, and north itself is the last index.
     * With 16 sectors, index 0 covers [11.25°, 33.75°) and the last index covers
     * [348.75°, 360°) and [0°, 11.25°).
     * @param angle propagation angle in radians in [0, 2 * pi), 0 = north, increasing clockwise
     * @return rose index
     */
    public int getRoseIndex(double angle) {
        double angleRad = angle % (Math.PI * 2);
        if (angleRad < 0) {
            angleRad += Math.PI * 2;
        }
        int index = (int) Math.round(angleRad / angleSection) - 1;
        if (index < 0) {
            index = windRose.length - 1;
        } else if (index >= windRose.length) {
            index = 0;
        }
        return index;
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
