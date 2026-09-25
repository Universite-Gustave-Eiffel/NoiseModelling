package org.noise_planet.noisemodelling.pathfinder;

import org.h2gis.api.ProgressVisitor;

/**
 * Interface for objects managing the PathFinderProcessor objects. An instance
 * that implement this interface is generated on each new computation cell area.
 * This non-thread safe instance is also used to store inter-thread information.
 */
public interface PathFinderProcessorManager {

    /**
     * Create a PathFinderProcessor object that will be processed by a single
     * thread (one thread per receiver) and will launch computations along the
     * path finding process.
     *
     * @param visitor Progression information, used to cancel processing too
     * @return PathFinderProcessor instance processed by a single thread
     */
     PathFinderProcessor subProcess(ProgressVisitor visitor);
}
