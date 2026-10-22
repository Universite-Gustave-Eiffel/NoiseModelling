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
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutPoint;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutPointBridgeWall;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutProfile;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.ProfileBuilder;
import org.noise_planet.noisemodelling.propagation.cnossos.CnossosPath;
import org.noise_planet.noisemodelling.propagation.cnossos.CnossosPathBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    /**
     * Source on the deck, receiver below AND beside it (off-axis, inside the bridge footprint):
     * the deck's lateral ("OTHER") edge pivot is found by a shortest-path search along the edge,
     * so the resulting source -&gt; pivot -&gt; receiver path is not collinear with the straight
     * source/receiver line. The CutProfile's 2D-unfolded distance ({@code d}) therefore no longer
     * matches the true source/receiver distance ({@code dc}), and the CNOSSOS delta must be
     * computed from {@code dc}.
     */
    @Test
    public void sourceOnDeckToReceiverBelowAndBesideUsesTrueDirectDistance() {
        ProfileBuilder withDeck = flatGround();
        addDeck(withDeck);
        withDeck.finishFeeding();

        Coordinate src = new Coordinate(30, 20, 10.6); // on the deck
        Coordinate rcv = new Coordinate(70, 22, 2.0);  // below and beside the deck

        List<CnossosPath> withDeckPaths = paths(withDeck, src, rcv);
        assertFalse(withDeckPaths.isEmpty(), "a valid path is expected");

        double dc = src.distance3D(rcv);
        boolean foundPathUsingTrueDirectDistance = false;
        for (CnossosPath path : withDeckPaths) {
            assertEquals(dc, path.getSRSegment().dc, 1e-6,
                    "dc must always be the true source-receiver distance");
            assertTrue(Math.abs(path.getSRSegment().d - dc) > 0.5,
                    "this off-axis geometry should make the 2D-unfolded distance diverge from the true one");

            CutPoint pivotPoint = path.getCutPoints().get(1);
            assertTrue(pivotPoint instanceof CutPointBridgeWall
                            && ((CutPointBridgeWall) pivotPoint).getWallDirection() == CutPointBridgeWall.WallDirection.OTHER,
                    "expected the deck's off-axis (OTHER) edge to be the pivot for this scenario");

            Coordinate pivot = pivotPoint.getCoordinate();
            double expectedDeltaUsingDc = src.distance3D(pivot) + path.e + pivot.distance3D(rcv) - dc;
            if (Math.abs(expectedDeltaUsingDc - path.delta) < 1e-6) {
                foundPathUsingTrueDirectDistance = true;
            }
        }
        assertTrue(foundPathUsingTrueDirectDistance,
                "at least one path's delta must be computed from the true direct distance dc, "
                        + "not the invalid 2D-unfolded distance d");
    }

    /**
     * Regression for a real-world bug report (Protivin, CZ, overpass, EPSG:5514): a receiver
     * sitting almost exactly under a long, many-point bridge deck's centerline got a higher level
     * than an unshielded reference receiver, as if the deck were not there. Two distinct bugs
     * combined to cause this:
     *
     * <ul>
     *   <li>the favourable-conditions ("curved path") delta formula in
     *   {@code CnossosPathBuilder} assumed the diffraction pivot lies on the straight,
     *   2D-unfolded source-receiver line - invalid for the bridge's off-axis edge pivot, which
     *   collapsed delta toward zero (no shielding) or inflated it to exactly {@code d} (an
     *   unrelated, nonsensical value) depending on source position;</li>
     *   <li>{@code CutProfile#getConvexHullIndices} could drop that same pivot from the convex
     *   hull entirely once the favourable-conditions curvature correction pushed its (invalid)
     *   2D height below the hull line at longer range, silently turning the path into an
     *   unobstructed direct one.</li>
     * </ul>
     *
     * <p>Fixed by always treating the bridge's off-axis (OTHER) edge pivot as a required
     * diffraction point, and always deriving its delta from the true 3D direct distance.
     */
    @Test
    public void sourceOnDeckToReceiverUnderTheAxisOfALongBridgeIsShielded() {
        // Real BRIDGE_PK=1 deck points (PK8..PK45) from the Protivin CZ bug-report dataset.
        double[][] rows = {
            {-770895.605,-1137437.618,392.697}, {-770901.754,-1137445.504,392.740},
            {-770907.903,-1137453.390,392.784}, {-770914.053,-1137461.276,392.827},
            {-770920.202,-1137469.162,392.871}, {-770926.351,-1137477.047,392.914},
            {-770932.500,-1137484.933,392.958}, {-770938.649,-1137492.819,393.001},
            {-770944.799,-1137500.705,393.045}, {-770950.948,-1137508.591,393.088},
            {-770957.097,-1137516.477,393.131}, {-770963.246,-1137524.363,393.175},
            {-770969.395,-1137532.249,393.218}, {-770975.545,-1137540.135,393.262},
            {-770981.694,-1137548.020,393.305}, {-770987.843,-1137555.906,393.349},
            {-770993.992,-1137563.792,393.392}, {-771000.142,-1137571.678,393.436},
            {-771006.291,-1137579.564,393.479}, {-771012.440,-1137587.450,393.522},
            {-771018.589,-1137595.336,393.566}, {-771024.738,-1137603.222,393.609},
            {-771030.888,-1137611.108,393.653}, {-771037.037,-1137618.993,393.696},
            {-771043.186,-1137626.879,393.740}, {-771049.335,-1137634.765,393.783},
            {-771055.484,-1137642.651,393.827}, {-771061.634,-1137650.537,393.870},
            {-771067.783,-1137658.423,393.914}, {-771073.932,-1137666.309,393.957},
            {-771080.081,-1137674.195,394.000}, {-771086.230,-1137682.081,394.044},
            {-771092.380,-1137689.966,394.087}, {-771098.529,-1137697.852,394.131},
            {-771104.678,-1137705.738,394.174}, {-771110.827,-1137713.624,394.218},
            {-771116.976,-1137721.510,394.261}, {-771121.542,-1137727.365,394.293},
        };
        double groundZ = 387.0; // deck underside ~393.15, ~6 m clearance, matching the bug report
        ProfileBuilder pb = new ProfileBuilder();
        for (double x = -771300; x <= -770800; x += 50) {
            for (double y = -1137900; y <= -1137400; y += 50) {
                pb.addTopographicPoint(new Coordinate(x, y, groundZ));
            }
        }
        List<BridgePoint> bridgePoints = new ArrayList<>();
        for (long i = 0; i < rows.length; i++) {
            bridgePoints.add(new BridgePoint.Builder(i, 1L, new Coordinate(rows[(int) i][0], rows[(int) i][1], rows[(int) i][2]))
                    .withHeightType(Scene.HeightType.ABSOLUTE)
                    .withDeckThickness(0.5)
                    .withWidth(6.5, 6.5)
                    .withBarrierHeight(0.0, 0.0)
                    .withPosition(BridgePoint.Position.CENTER)
                    .withGirderType(Bridge.GirderType.STEEL_BOX)
                    .withSlabType(Bridge.SlabType.CONCRETE)
                    .build());
        }
        pb.addBridge(new Bridge.Builder(bridgePoints)
                .setPrimaryKey(1L)
                .setGirderType(Bridge.GirderType.STEEL_BOX)
                .setSlabType(Bridge.SlabType.CONCRETE)
                .build());
        pb.finishFeeding();

        // receiver "28" from the bug report: Z is a relative height above ground in the real
        // pipeline, so absolute = groundZ + relative. Its horizontal offset from the deck
        // centerline is ~0.02 m (effectively exactly under the axis).
        Coordinate rcv = new Coordinate(-771029.769, -1137609.705, groundZ + 2.0);

        // A nearby source (real PK30, ~1.8 m along the deck from the receiver) and a distant one
        // (real PK44, ~110 m away) - the convex-hull-drop half of the bug only showed up at range.
        Coordinate nearSrc = new Coordinate(rows[22][0], rows[22][1], rows[22][2] + 0.6);
        Coordinate farSrc = new Coordinate(rows[36][0], rows[36][1], rows[36][2] + 0.6);

        for (Coordinate src : new Coordinate[]{nearSrc, farSrc}) {
            CutProfile profile = pb.getProfile(src, rcv);
            List<CnossosPath> cpaths = CnossosPathBuilder.computeCnossosPathsFromCutProfile(profile, false, FREQ, 0.0);
            assertFalse(cpaths.isEmpty(), "a valid path is expected for src=" + src);

            double dc = src.distance3D(rcv);
            for (CnossosPath p : cpaths) {
                CutPointBridgeWall pivot = null;
                for (CutPoint cp : p.getCutPoints()) {
                    if (cp instanceof CutPointBridgeWall) {
                        pivot = (CutPointBridgeWall) cp;
                    }
                }
                assertTrue(pivot != null && pivot.getWallDirection() == CutPointBridgeWall.WallDirection.OTHER,
                        "expected the deck's off-axis edge to still be a diffraction pivot for src=" + src);

                double d = p.getSRSegment().d;
                assertTrue(p.delta > 0.3,
                        "delta collapsed toward zero (lost shielding) for src=" + src + ": delta=" + p.delta);
                assertTrue(Math.abs(p.delta - d) > 1.0,
                        "delta blew up to exactly the invalid 2D-unfolded distance d for src=" + src
                                + ": delta=" + p.delta + ", d=" + d);
                assertTrue(Math.abs(d - dc) > 0.1,
                        "this near-axis geometry should still make d diverge from the true distance dc for src=" + src);
            }
        }
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
