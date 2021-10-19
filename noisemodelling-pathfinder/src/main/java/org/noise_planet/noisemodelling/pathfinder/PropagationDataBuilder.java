package org.noise_planet.noisemodelling.pathfinder;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

public class PropagationDataBuilder {
    private static final GeometryFactory FACTORY = new GeometryFactory();

    private final CnossosPropagationData data;

    public PropagationDataBuilder(ProfileBuilder profileBuilder) {
        data = new CnossosPropagationData(profileBuilder);
    }

    public PropagationDataBuilder addSource(double x, double y, double z) {
        data.addSource(FACTORY.createPoint(new Coordinate(x, y, z)));
        return this;
    }

    public PropagationDataBuilder addSource(Geometry geom) {
        data.addSource(geom);
        return this;
    }

    public PropagationDataBuilder addReceiver(double x, double y, double z) {
        data.addReceiver(new Coordinate(x, y, z));
        return this;
    }

    public PropagationDataBuilder vEdgeDiff(boolean hDiff) {
        data.setComputeHorizontalDiffraction(hDiff);
        return this;
    }

    public PropagationDataBuilder hEdgeDiff(boolean vDiff) {
        data.setComputeVerticalDiffraction(vDiff);
        return this;
    }

    public PropagationDataBuilder setGs(double gs) {
        data.setGs(gs);
        return this;
    }

    public CnossosPropagationData build() {
        return data;
    }
}
