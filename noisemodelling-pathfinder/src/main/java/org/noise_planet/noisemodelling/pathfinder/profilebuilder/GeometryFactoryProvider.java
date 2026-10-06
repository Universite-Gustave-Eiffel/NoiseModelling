package org.noise_planet.noisemodelling.pathfinder.profilebuilder;

import org.locationtech.jts.geom.GeometryFactory;

/**
 * Central provider for a shared GeometryFactory instance.
 *
 * This class allows the codebase to consistently reuse a single
 * GeometryFactory where appropriate. Individual classes can still
 * accept custom factories via constructors when needed.
 */
public final class GeometryFactoryProvider {
    /** Shared GeometryFactory instance. */
    public static final GeometryFactory SHARED = new GeometryFactory();

    private GeometryFactoryProvider() {}
}
