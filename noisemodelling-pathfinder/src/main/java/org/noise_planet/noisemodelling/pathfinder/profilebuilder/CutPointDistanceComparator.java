package org.noise_planet.noisemodelling.pathfinder.profilebuilder;

import org.locationtech.jts.geom.Coordinate;

import java.util.Comparator;

public class CutPointDistanceComparator implements Comparator<CutPoint> {
    private final Coordinate reference;

    public CutPointDistanceComparator(Coordinate reference) {
        this.reference = reference;
    }

    @Override
    public int compare(CutPoint o1, CutPoint o2) {
        return Double.compare(o1.coordinate.distance(reference), o2.coordinate.distance(reference));
    }
}
