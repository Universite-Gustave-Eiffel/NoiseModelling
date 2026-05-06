package org.noise_planet.noisemodelling.pathfinder;

import org.h2gis.api.ProgressVisitor;

/**
 * Generate a non-thread safe instance of cut plane visitor where each receiver are processed one by one
 */
public interface CutPlaneVisitorFactory {

    /**
     * @param visitor Progression information, used to cancel processing too
     * @return CutPlaneVisitor instance processed by a single thread
     */
    CutPlaneVisitor subProcess(ProgressVisitor visitor);
}
