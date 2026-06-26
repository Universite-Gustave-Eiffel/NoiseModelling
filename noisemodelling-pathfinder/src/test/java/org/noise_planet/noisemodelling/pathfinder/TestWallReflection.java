/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */
package org.noise_planet.noisemodelling.pathfinder;

import org.h2.tools.Csv;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.noise_planet.noisemodelling.pathfinder.path.MirrorReceiver;
import org.noise_planet.noisemodelling.pathfinder.path.MirrorReceiversCompute;
import org.noise_planet.noisemodelling.pathfinder.path.Scene;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutPoint;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutPointReceiver;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutPointReflection;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutPointSource;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutPointTopography;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutProfile;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.ProfileBuilder;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.Wall;
import org.noise_planet.noisemodelling.pathfinder.utils.geometry.Orientation;

import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestWallReflection {

    @Test
    public void testCloseReflectionBeforeReceiverEvenWithPointAfterReflection() {
        CutProfile cutProfile = new CutProfile();
        cutProfile.setProfileType(CutProfile.PROFILE_TYPE.REFLECTION);
        cutProfile.cutPoints.add(new CutPointSource(new Coordinate(11.49, 0.0, 2.0)));

        CutPoint reflectionBasePoint = new CutPoint(new Coordinate(1.0, 0.0, 2.0), 0.0, 0.0);
        cutProfile.cutPoints.add(new CutPointReflection(reflectionBasePoint,
                new LineSegment(new Coordinate(1.0, -2.5, 0.0), new Coordinate(1.0, 2.5, 5.0)),
                Collections.emptyList()));

        // This point stands for a later event on the path (for example a diffraction-related point).
        cutProfile.cutPoints.add(new CutPointTopography(new Coordinate(1.2, 0.0, 3.0)));
        cutProfile.cutPoints.add(new CutPointReceiver(new Coordinate(1.49, 0.0, 2.0)));

        assertTrue(cutProfile.hasCloseReflectionBeforeReceiver(0.5),
                "The last reflection before the receiver should trigger filtering even if another point follows it.");
    }

    @Test
    public void testReflectionOutsideReceiverWallDistanceThreshold() {
        CutProfile cutProfile = new CutProfile();
        cutProfile.setProfileType(CutProfile.PROFILE_TYPE.REFLECTION);
        cutProfile.cutPoints.add(new CutPointSource(new Coordinate(11.51, 0.0, 2.0)));

        CutPoint reflectionBasePoint = new CutPoint(new Coordinate(1.0, 0.0, 2.0), 0.0, 0.0);
        cutProfile.cutPoints.add(new CutPointReflection(reflectionBasePoint,
                new LineSegment(new Coordinate(1.0, -2.5, 0.0), new Coordinate(1.0, 2.5, 5.0)),
                Collections.emptyList()));

        cutProfile.cutPoints.add(new CutPointReceiver(new Coordinate(1.50, 0.0, 2.0)));

        assertFalse(cutProfile.hasCloseReflectionBeforeReceiver(0.5),
                "The optional filter must only reject reflections with a receiver-to-wall distance strictly below 0.5 m.");
    }

    @Test
    public void testWideWall() {
        Coordinate cA = new Coordinate(50, 100, 5);
        Coordinate cB = new Coordinate(150, 100, 5);

        Polygon polygon = MirrorReceiversCompute.createWallReflectionVisibilityCone(
                new Coordinate(100, 50, 0.1),
                new LineSegment(cA, cB), 100, 100);

        GeometryFactory factory = new GeometryFactory();
        assertTrue(polygon.intersects(factory.createPoint(new Coordinate(100, 145, 0))));
    }

    @Test
    public void testNReflexion() throws ParseException, IOException, SQLException {
        GeometryFactory factory = new GeometryFactory();

        //Create profile builder
        ProfileBuilder profileBuilder = new ProfileBuilder();
        Csv csv = new Csv();
        WKTReader wktReader = new WKTReader();
        try(ResultSet rs = csv.read(new FileReader(
                TestWallReflection.class.getResource("testNReflexionBuildings.csv").getFile()),
                new String[]{"geom", "id"})) {
            assertTrue(rs.next()); //skip column name
            while(rs.next()) {
                profileBuilder.addBuilding(wktReader.read(rs.getString(1)), 10, rs.getInt(2));
            }
        }
        profileBuilder.finishFeeding();
        assertEquals(5, profileBuilder.getBuildingCount());
        Scene inputData = new Scene(profileBuilder);
        inputData.addReceiver(new Coordinate(599093.85,646227.90, 4));
        inputData.addSource(factory.createPoint(new Coordinate(599095.21, 646283.77, 1)));
        inputData.setComputeHorizontalDiffraction(false);
        inputData.setComputeVerticalDiffraction(false);
        inputData.maxRefDist = 80;
        inputData.maxSrcDist = 180;
        inputData.setReflexionOrder(2);
        PathFinder computeRays = new PathFinder(inputData);
        computeRays.setThreadCount(1);


        Coordinate receiver = inputData.receivers.get(0);
        Envelope receiverPropagationEnvelope = new Envelope(receiver);
        receiverPropagationEnvelope.expandBy(inputData.maxSrcDist);
        List<Wall> buildWalls = inputData.profileBuilder.getWallsIn(receiverPropagationEnvelope);
        MirrorReceiversCompute receiverMirrorIndex = new MirrorReceiversCompute(buildWalls, receiver,
                inputData.reflexionOrder, inputData.maxSrcDist, inputData.maxRefDist);

        // Keep only mirror receivers potentially visible from the source(and its parents)
        List<MirrorReceiver> mirrorResults = receiverMirrorIndex.findCloseMirrorReceivers(inputData.
                sourceGeometries.get(0).getCoordinate());

        assertEquals(4, mirrorResults.size());

        DefaultCutPlaneVisitor defaultCutPlaneVisitor = new DefaultCutPlaneVisitor(true, inputData);

        computeRays.computeReflexion(new PathFinder.ReceiverPointInfo(1, 1, receiver),
                new PathFinder.SourcePointInfo(1, 1, inputData.sourceGeometries.get(0).getCoordinate(), 1.0,
                new Orientation()), receiverMirrorIndex, defaultCutPlaneVisitor, CutPlaneVisitor.PathSearchStrategy.CONTINUE);

        List<CutProfile> profiles = new ArrayList<>(defaultCutPlaneVisitor.cutProfiles);
        // Only one second order reflexion propagation path must be found
        assertEquals(1, profiles.size());

        // Check expected values for the propagation path
        CutProfile firstPath = profiles.get(0);
        // S->Ref->Ref->R
        assertEquals(4, firstPath.cutPoints.size());
        var it = firstPath.cutPoints.iterator();
        assertTrue(it.hasNext());
        CutPoint current = it.next();
        assertInstanceOf(CutPointSource.class, current);
        PathFinderTest.assert3DCoordinateEquals ("Source not equal",
                inputData.sourceGeometries.get(0).getCoordinate(),
                current.coordinate, 1e-12);
        current = it.next();
        assertInstanceOf(CutPointReflection.class, current);
        PathFinderTest.assert3DCoordinateEquals("",
                new Coordinate(599102.81, 646245.83, 2.9), current.coordinate, 0.01);
        current = it.next();
        assertInstanceOf(CutPointReflection.class, current);
        PathFinderTest.assert3DCoordinateEquals("",
                new Coordinate(599092.38, 646235.61, 3.61), current.coordinate, 0.01);
        current = it.next();
        assertInstanceOf(CutPointReceiver.class, current);
    }


    @Test
    public void testNReflexionWithDem() throws ParseException, IOException, SQLException {
        GeometryFactory factory = new GeometryFactory();

        //Create profile builder
        ProfileBuilder profileBuilder = new ProfileBuilder();
        Csv csv = new Csv();
        WKTReader wktReader = new WKTReader();
        try(ResultSet rs = csv.read(new FileReader(
                        TestWallReflection.class.getResource("testNReflexionBuildings.csv").getFile()),
                new String[]{"geom", "id"})) {
            assertTrue(rs.next()); //skip column name
            while(rs.next()) {
                profileBuilder.addBuilding(wktReader.read(rs.getString(1)), 10, rs.getInt(2));
            }
        }
        profileBuilder.addTopographicPoint(new Coordinate(598962.08,646370.83,500.00));
        profileBuilder.addTopographicPoint(new Coordinate(599252.92,646370.11,500.00));
        profileBuilder.addTopographicPoint(new Coordinate(599254.37,646100.19,500.00));
        profileBuilder.addTopographicPoint(new Coordinate(598913.00,646104.52,500.00));
        profileBuilder.finishFeeding();
        assertEquals(5, profileBuilder.getBuildingCount());
        Scene inputData = new Scene(profileBuilder);
        inputData.addReceiver(new Coordinate(599093.85,646227.90, 504));
        inputData.addSource(factory.createPoint(new Coordinate(599095.21, 646283.77, 501)));
        inputData.setComputeHorizontalDiffraction(false);
        inputData.setComputeVerticalDiffraction(false);
        inputData.maxRefDist = 80;
        inputData.maxSrcDist = 180;
        inputData.setReflexionOrder(2);
        PathFinder computeRays = new PathFinder(inputData);
        computeRays.setThreadCount(1);


        Coordinate receiver = inputData.receivers.get(0);
        Envelope receiverPropagationEnvelope = new Envelope(receiver);
        receiverPropagationEnvelope.expandBy(inputData.maxSrcDist);
        List<Wall> buildWalls = inputData.profileBuilder.getWallsIn(receiverPropagationEnvelope);
        MirrorReceiversCompute receiverMirrorIndex = new MirrorReceiversCompute(buildWalls, receiver,
                inputData.reflexionOrder, inputData.maxSrcDist, inputData.maxRefDist);

        // Keep only mirror receivers potentially visible from the source(and its parents)
        List<MirrorReceiver> mirrorResults = receiverMirrorIndex.findCloseMirrorReceivers(inputData.
                sourceGeometries.get(0).getCoordinate());

        assertEquals(4, mirrorResults.size());

        DefaultCutPlaneVisitor defaultCutPlaneVisitor = new DefaultCutPlaneVisitor(true, inputData);

        computeRays.computeReflexion(new PathFinder.ReceiverPointInfo(1, 1, receiver),
                new PathFinder.SourcePointInfo(1, 1, inputData.sourceGeometries.get(0).getCoordinate(), 1.0,
                        new Orientation()), receiverMirrorIndex, defaultCutPlaneVisitor, CutPlaneVisitor.PathSearchStrategy.CONTINUE);

        List<CutProfile> profiles = new ArrayList<>(defaultCutPlaneVisitor.cutProfiles);
        // Only one second order reflexion propagation path must be found
        assertEquals(1, profiles.size());

        // Check expected values for the propagation path
        CutProfile firstPath = profiles.get(0);
        // S->Ref->Ref->R
        assertEquals(4, firstPath.cutPoints.size());
        var it = firstPath.cutPoints.iterator();
        assertTrue(it.hasNext());
        CutPoint current = it.next();
        assertInstanceOf(CutPointSource.class, current);
        PathFinderTest.assert3DCoordinateEquals ("Source not equal",
                inputData.sourceGeometries.get(0).getCoordinate(),
                current.coordinate, 1e-12);
        current = it.next();
        assertInstanceOf(CutPointReflection.class, current);
        PathFinderTest.assert3DCoordinateEquals("",
                new Coordinate(599102.81, 646245.83, 502.9), current.coordinate, 0.01);
        current = it.next();
        assertInstanceOf(CutPointReflection.class, current);
        PathFinderTest.assert3DCoordinateEquals("",
                new Coordinate(599092.38, 646235.61, 503.61), current.coordinate, 0.01);
        current = it.next();
        assertInstanceOf(CutPointReceiver.class, current);
    }
}
