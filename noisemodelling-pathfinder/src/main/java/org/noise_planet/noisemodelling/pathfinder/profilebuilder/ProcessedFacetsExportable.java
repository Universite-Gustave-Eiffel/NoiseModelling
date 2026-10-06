package org.noise_planet.noisemodelling.pathfinder.profilebuilder;

import org.locationtech.jts.geom.GeometryFactory;

/**
 * Contract for services that can export their processed facets (walls/edges)
 * into a {@link ProcessedWallService}.
 *
 * <p>Provides a two-argument method (factory + destination). A convenience
 * single-argument default method calls the two-argument form using the
 * shared GeometryFactory provider so implementors don't need to provide both
 * overloads.</p>
 */
public interface ProcessedFacetsExportable {
    /**
     * Export processed facets into the provided sink using the supplied
     * GeometryFactory for temporary geometries.
     */
    void exportFacetsToProcessedWalls(ProcessedWallService processedWallService, GeometryFactory factory);

    /**
     * Convenience overload using the shared GeometryFactory.
     */
    default void exportFacetsToProcessedWalls(ProcessedWallService processedWallService) {
        exportFacetsToProcessedWalls(processedWallService, GeometryFactoryProvider.SHARED);
    }
}
