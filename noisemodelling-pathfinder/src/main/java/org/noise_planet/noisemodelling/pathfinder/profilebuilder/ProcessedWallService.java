package org.noise_planet.noisemodelling.pathfinder.profilebuilder;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.index.strtree.STRtree;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.ProfileBuilder.IntersectionType;
import org.noise_planet.noisemodelling.pathfinder.utils.geometry.RTreeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.ArrayList;
import java.util.List;

/**
 * Sink that owns processed walls (facets) and their spatial index.
 *
 * <p>This class isolates processed-wall logic from raw-wall ingestion and
 * centralizes two responsibilities:
 * <ul>
 *   <li>store the sequential list of processed wall facets (internal list),</li>
 *   <li>maintain an STRtree spatial index referencing that list for fast
 *       spatial queries.</li>
 * </ul>
 *
 * <p>Important notes for callers:
 * <ul>
 *   <li>Lifecycle: callers add facets via {@link #addProcessedWall(Wall)} (or
 *       its overload) during preprocessing. After all inserts the caller must
 *       call {@link #buildProcessedWallRtree()} to finalize the internal STRtree
 *       before issuing queries (eg. {@link #getWallsOnPath} or
 *       {@link #getWallsIn}). Querying the R-tree before calling
 *       {@code buildProcessedWallRtree()} may return incomplete results or
 *       behave unexpectedly.</li>
 *   <li>Index mapping: the STRtree stores the integer index of the wall in the
 *       internal list. Code that relies on the processed-wall index should not
 *       modify the internal list order after inserts or assume indices are
 *       stable across re-builds unless documented.</li>
 *   <li>Thread-safety: this class is not thread-safe for concurrent writers
 *       and readers. Typical usage is: add features → call
 *       {@link #buildProcessedWallRtree()} → perform read-only queries from
 *       multiple threads.</li>
 * </ul>
 */
public class ProcessedWallService implements FrequencyInitializable{
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessedWallService.class);
    private final List<Wall> processedWalls = new ArrayList<>();
    private final STRtree processedRtree;
    private final GeometryFactory geometryFactory;

    public ProcessedWallService(int nodeCapacity, GeometryFactory geometryFactory) {
        this.processedRtree = new STRtree(nodeCapacity);
        this.geometryFactory = geometryFactory != null ? geometryFactory : GeometryFactoryProvider.SHARED;
    }

    
    public ProcessedWallService(int nodeCapacity) {
        this(nodeCapacity, GeometryFactoryProvider.SHARED);
    }


    public ProcessedWallService() {
        this(5, GeometryFactoryProvider.SHARED);
    }

    public void addProcessedWall(Wall w, GeometryFactory factory) {
        // Add a processed wall facet to the internal list and insert a reference
        // into the STRtree. The STRtree stores the integer index pointing to
        // the element in {@link #processedWalls} so callers may record the
        // returned index (processedWalls.size()-1) as the facet identifier.
        processedWalls.add(w);
        processedRtree.insert(w.getLineSegment().toGeometry(factory).getEnvelopeInternal(), processedWalls.size() - 1);
    }

    public void addProcessedWall(Wall w) {
        addProcessedWall(w, this.geometryFactory);
    }

    public List<Wall> getProcessedWalls() {
    // Returns a read-only view for callers. Note that the underlying
    // list is mutable while preprocessing is ongoing; callers performing
    // long-lived references to indices should avoid modifying the list
    // afterwards.
    return new LoggingUnmodifiableList<>(processedWalls, "ProcessedWallService.processedWalls");
    }

    public STRtree getProcessedRtree() {
    // Return the spatial index. Ensure {@link #buildProcessedWallRtree}
    // has been called after all inserts to guarantee consistent query
    // behaviour.
    return processedRtree;
    }

    public void buildProcessedWallRtree() {
    // Finalize the internal STRtree. Call this once after all
    // {@link #addProcessedWall} calls are complete and before using the
    // index for queries.
    processedRtree.build();
    }

    public void getWallsOnPath(Coordinate p1, Coordinate p2, BuildingIntersectionPathVisitor visitor, double maxLineLength) {
        try {
            List<LineSegment> lines = ProfileUtils.splitToSegments(p1, p2, maxLineLength);
            for (LineSegment segment : lines) {
                visitor.setIntersectionLine(segment);
                Envelope pathEnv = new Envelope(segment.p0, segment.p1);
                this.processedRtree.query(pathEnv, visitor);
            }
        } catch (IllegalStateException ex) {
            // Ignore
        }
    }
    
    /**
     * Return processed walls that intersect the given envelope and are of type BUILDING or WALL.
     *
     * @param env query envelope
     * @return list of processed walls intersecting the envelope
     */
    public List<Wall> getWallsIn(Envelope env) {
        List<Wall> list = new ArrayList<>();
        List<Integer> indexes = RTreeUtils.query(getProcessedRtree(), env);
        for (int i : indexes) {
            Wall w = getProcessedWalls().get(i);
            if (w.getType() == IntersectionType.BUILDING || w.getType() == IntersectionType.WALL) {
                list.add(w);
            }
        }
        return list;
    }
    
    /**
     * Initialize frequency-dependent arrays on raw and processed walls managed by this service.
     * @param exactFrequencyArray list of exact frequency values used to compute alphas
     */
    @Override
    public void initializeFrequencyDependentData(List<Double> exactFrequencyArray) {
        if (exactFrequencyArray == null) {
            LOGGER.warn("ProcessedWallService.initializeFrequencyDependentData: exactFrequencyArray is null");
            return;
        }
        LOGGER.debug("ProcessedWallService.initializeFrequencyDependentData: called with exactFrequencyArray.size={}", exactFrequencyArray.size());
        for (Wall w : processedWalls) {
            w.initialize(exactFrequencyArray);
        }
    }

    /**
     * Compute a hash code representing the current state of this ProcessedWallService.
     * The hash is based on the number of processed walls.
     * 
     * @return Hash code representing the service state
     */
    @Override
    public int hashCode() {
        return Objects.hash(processedWalls);
    }
}
