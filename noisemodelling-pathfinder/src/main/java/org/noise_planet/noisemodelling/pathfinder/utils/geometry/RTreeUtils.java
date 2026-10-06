package org.noise_planet.noisemodelling.pathfinder.utils.geometry;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.index.strtree.STRtree;

import java.util.Collections;
import java.util.List;

/**
 * Small utility wrapper to centralize unchecked casts from STRtree.query(...) to typed lists.
 */
public final class RTreeUtils {
    private RTreeUtils() {}

    @SuppressWarnings("unchecked")
    public static <T> List<T> query(STRtree tree, Envelope env) {
        if (tree == null) return Collections.emptyList();
        List<?> res = tree.query(env);
        return (List<T>) res;
    }
}
