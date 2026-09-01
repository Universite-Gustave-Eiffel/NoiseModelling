/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */
package org.noise_planet.noisemodelling.propagation;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.noise_planet.noisemodelling.pathfinder.path.Scene;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.Bridge;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.BridgePoint;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutProfile;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.ProfileBuilder;
import org.noise_planet.noisemodelling.propagation.cnossos.CnossosPath;
import org.noise_planet.noisemodelling.propagation.cnossos.CnossosPathBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that a bridge deck present in the {@link CutProfile} flows through the CNOSSOS path
 * builder without error and that a source on the deck yields a diffracting path (the deck edge
 * acts as a barrier), whereas the same source/receiver with no deck gives a plain direct path.
 *
 * <p>v0 models the deck edge as a generic barrier. A dedicated bridge attenuation model is a
 * later iteration.
 */
public class BridgePropagationTest {

    private static final List<Double> FREQ = Arrays.asList(
            50.0, 63.0, 80.0, 100.0, 125.0, 160.0, 200.0, 250.0, 315.0, 400.0, 500.0, 630.0,
            800.0, 1000.0, 1250.0, 1600.0, 2000.0, 2500.0, 3150.0, 4000.0, 5000.0, 6300.0,
            8000.0, 10000.0);

    private static ProfileBuilder flatGround() {
        ProfileBuilder pb = new ProfileBuilder();
        for (int x = -20; x <= 120; x += 35) {
            for (int y = -20; y <= 120; y += 35) {
                pb.addTopographicPoint(new Coordinate(x, y, 0.0));
            }
        }
        return pb;
    }

    private static void addDeck(ProfileBuilder pb) {
        List<Coordinate> pts = Arrays.asList(
                new Coordinate(0, 20, 10),
                new Coordinate(50, 20, 10),
                new Coordinate(100, 20, 10));
        List<BridgePoint> bridgePoints = new ArrayList<>();
        for (long i = 0; i < pts.size(); i++) {
            bridgePoints.add(new BridgePoint.Builder(i, 100L, pts.get((int) i))
                    .withHeightType(Scene.HeightType.ABSOLUTE)
                    .withDeckThickness(0.5)
                    .withWidth(5.0, 5.0)
                    .withBarrierHeight(1.0, 1.0)
                    .withPosition(BridgePoint.Position.CENTER)
                    .withGirderType(Bridge.GirderType.STEEL_BOX)
                    .withSlabType(Bridge.SlabType.STEEL)
                    .build());
        }
        pb.addBridge(new Bridge.Builder(bridgePoints)
                .setPrimaryKey(100L)
                .setGirderType(Bridge.GirderType.STEEL_BOX)
                .setSlabType(Bridge.SlabType.STEEL)
                .build());
    }

    private static List<CnossosPath> paths(ProfileBuilder pb, Coordinate src, Coordinate rcv) {
        CutProfile profile = pb.getProfile(src, rcv);
        return CnossosPathBuilder.computeCnossosPathsFromCutProfile(profile, false, FREQ, 0.0);
    }

    @Test
    public void sourceOnDeckProducesADiffractingPathWithoutError() {
        ProfileBuilder withDeck = flatGround();
        addDeck(withDeck);
        withDeck.finishFeeding();

        Coordinate src = new Coordinate(50, 20, 10.6); // on the deck
        Coordinate rcv = new Coordinate(50, 70, 2.0);  // on the ground, past the deck

        List<CnossosPath> withDeckPaths = paths(withDeck, src, rcv);
        assertFalse(withDeckPaths.isEmpty(), "a valid path is expected");
        int maxPoints = withDeckPaths.stream().mapToInt(p -> p.getPointList().size()).max().orElse(0);
        assertTrue(maxPoints > 2,
                "the deck edge should introduce at least one diffraction point (got " + maxPoints + " points)");
    }

    @Test
    public void sameGeometryWithoutDeckIsADirectPath() {
        ProfileBuilder noDeck = flatGround();
        noDeck.finishFeeding();

        Coordinate src = new Coordinate(50, 20, 10.6);
        Coordinate rcv = new Coordinate(50, 70, 2.0);

        List<CnossosPath> noDeckPaths = paths(noDeck, src, rcv);
        assertFalse(noDeckPaths.isEmpty());
        int maxPoints = noDeckPaths.stream().mapToInt(p -> p.getPointList().size()).max().orElse(0);
        assertTrue(maxPoints <= 2, "no deck : plain source-receiver path expected (got " + maxPoints + " points)");
    }
}
