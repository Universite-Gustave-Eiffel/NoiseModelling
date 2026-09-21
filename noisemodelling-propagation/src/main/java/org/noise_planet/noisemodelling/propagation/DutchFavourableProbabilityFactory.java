/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */
package org.noise_planet.noisemodelling.propagation;

/**
 * Get the fraction of favourable conditions for conditions in the Netherlands.
 */
public class DutchFavourableProbabilityFactory {

    /**
     * Returns a favourable probability generator for the specified period.
     *
     * @param generatorName the generator identifier
     * @return the favourable probability generator or null if the generator is unknown
     */
    public static FavourableProbability getFavourableProbabilityGenerator(String generatorName) {
        return switch (generatorName) {
            case "DutchD" -> new DProbabilityGenerator("DutchD");
            case "DutchE" -> new ENProbabilityGenerator("DutchE");
            case "DutchN" -> new ENProbabilityGenerator("DutchN");
            default -> null;
        };
    }

    /**
     * DutchFavourableProbability is using another convention for the angle than the standard polar angle
     * @param angle the angle in radians
     * @return the rotated angle in radians
     */
    public static double rotateAngle(double angle) {
        double theta1 = -Math.PI / 2.0 - angle;

        // Normalize the angle to be within [-PI, PI]
        if (theta1 <= -Math.PI) theta1 += 2 * Math.PI;
        if (theta1 > Math.PI) theta1 -= 2 * Math.PI;

        return theta1;
    }

    public static class DProbabilityGenerator implements FavourableProbability {
        String name;

        public DProbabilityGenerator(String name) {
            this.name = name;
        }

        @Override
        public double getFavourableProbability(double propagationAngle) {
            double zeta = Math.toDegrees(DutchFavourableProbabilityFactory.rotateAngle(propagationAngle));
            zeta = Math.toRadians(zeta + 35.);
            return 0.34 - 0.1 * Math.sin(zeta) + 0.045 * Math.pow(Math.sin(zeta), 2);
        }

        @Override
        public String getFavourableProbabilitySettings() {
            return name;
        }
    }
    public static class ENProbabilityGenerator implements FavourableProbability {
        String name;

        public ENProbabilityGenerator(String name) {
            this.name = name;
        }

        @Override
        public double getFavourableProbability(double propagationAngle) {
            double zeta = Math.toDegrees(DutchFavourableProbabilityFactory.rotateAngle(propagationAngle));
            zeta = Math.toRadians(zeta + 60.);
            return 0.40 - 0.1 * Math.sin(zeta) + 0.035 * Math.pow(Math.sin(zeta), 2);
        }

        @Override
        public String getFavourableProbabilitySettings() {
            return name;
        }
    }
}
