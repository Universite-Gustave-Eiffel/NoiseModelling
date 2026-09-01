package org.noise_planet.noisemodelling.pathfinder.profilebuilder;

/**
 * Interface for services that can compute or update elevations using ProfileBuilder.
 */
public interface ElevationComputable {
    /**
     * Compute/update elevations for the service using data exposed by the ProfileBuilder.
     */
    void computeElevations(ProfileBuilder profileBuilder);
}
