/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.pathfinder;

import org.cts.crs.CRSException;
import org.cts.op.CoordinateOperationException;
import org.junit.Test;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.Building;
import org.noise_planet.noisemodelling.pathfinder.path.Scene;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutPoint;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutProfile;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.ProfileBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.*;

/**
 * Test class dedicated to {@link ProfileBuilder}.
 */
public class ProfileBuilderTest {

    /** JTS WKT reader. */
    private static final WKTReader READER = new WKTReader();
    /** Delta value. */
    private static final double DELTA = 1e-8;
    private Logger logger = LoggerFactory.getLogger(ProfileBuilderTest.class);

    /**
     * Test the building adding to a {@link ProfileBuilder}.
     * @throws ParseException JTS WKT parsing exception.
     */
    @Test
    public void buildingAddingTest() throws ParseException {
        ProfileBuilder profileBuilder = new ProfileBuilder(3, 3, 3, 2);
        profileBuilder.addBuilding(READER.read("POLYGON((1 1,5 1,5 5,1 5,1 1))"), 10, -1);
        profileBuilder.addBuilding(READER.read("POLYGON((10 10,15 10,15 15,10 15,10 10))"), 23, -1);
        profileBuilder.addBuilding(READER.read("POLYGON((6 8,8 10,8 4,6 8))"), 56, -1);

        profileBuilder.finishFeeding();

        List<Building> list = profileBuilder.getBuildings();
        assertEquals(3, list.size());
        assertEquals("POLYGON ((1 1, 5 1, 5 5, 1 5, 1 1))", list.get(0).getGeometry().toText());
        assertEquals(10, list.get(0).getGeometry().getCoordinate().z, 0);
        assertEquals("POLYGON ((10 10, 15 10, 15 15, 10 15, 10 10))", list.get(1).getGeometry().toText());
        assertEquals(23, list.get(1).getGeometry().getCoordinate().z, 0);
        assertEquals("POLYGON ((6 8, 8 10, 8 4, 6 8))", list.get(2).getGeometry().toText());
        assertEquals(56, list.get(2).getGeometry().getCoordinate().z, 0);
    }

    /**
     * Test the finish of {@link ProfileBuilder} feeding.
     * @throws ParseException JTS WKT parsing exception.
     */
    @Test
    public void finishBuildingFeedingTest() throws ParseException {
        ProfileBuilder profileBuilder = new ProfileBuilder(3, 3, 3, 2);
        profileBuilder.addBuilding(READER.read("POLYGON((1 1,5 1,5 5,1 5,1 1))"), 10);
        assertNotNull(profileBuilder.finishFeeding());
        profileBuilder.addBuilding(READER.read("POLYGON((10 10,15 10,15 15,10 15,10 10))"), 23);
        profileBuilder.addBuilding(READER.read("POLYGON((6 8,8 10,8 4,6 8))"), 56);

        List<Building> list = profileBuilder.getBuildings();
        assertEquals(1, list.size());
    }

    /**
     * Test the building cut profile generation.
     * @throws ParseException JTS WKT parsing exception.
     */
    @Test
    public void buildingCutProfileTest() throws ParseException {
        ProfileBuilder profileBuilder = new ProfileBuilder(3, 3, 3, 2);
        profileBuilder.addBuilding(READER.read("POLYGON((2 2 10, 1 3 15, 2 4 10, 3 3 12, 2 2 10))"));
        profileBuilder.addBuilding(READER.read("POLYGON((4.5 7, 4.5 8.5, 6.5 8.5, 4.5 7))"), 3.3);
        profileBuilder.addBuilding(READER.read("POLYGON((7 6, 10 6, 10 2, 7 2, 7 6))"), 5.6);
        profileBuilder.finishFeeding();

        CutProfile profile = profileBuilder.getProfile(new Coordinate(0, 1, 0.1), new Coordinate(8, 10, 0.3));
        List<CutPoint> pts = profile.getCutPoints();
        assertEquals(8, pts.size());
        assertEquals(0.0, pts.get(0).getCoordinate().x, DELTA);
        assertEquals(1.0, pts.get(0).getCoordinate().y, DELTA);
        assertEquals(0.1, pts.get(0).getCoordinate().z, DELTA);
        assertEquals(8.0, pts.get(7).getCoordinate().x, DELTA);
        assertEquals(10.0, pts.get(7).getCoordinate().y, DELTA);
        assertEquals(0.3, pts.get(7).getCoordinate().z, DELTA);
    }

    /**
     * Test the topographic adding to a {@link ProfileBuilder}.
     * @throws ParseException JTS WKT parsing exception.
     */
    @Test
    public void topoAddingTest() throws ParseException {
        ProfileBuilder profileBuilder = new ProfileBuilder(3, 3, 3, 2);
        profileBuilder.addTopographicLine((LineString) READER.read("LINESTRING (4 1 1.5, 5 7 1.0, 8 9 1.5)"));
        profileBuilder.addTopographicPoint(new Coordinate(7, 9, 2.5));
        profileBuilder.addTopographicPoint(new Coordinate(2, 4, 2.5));
        profileBuilder.addTopographicPoint(new Coordinate(6, 1, 3.0));
        profileBuilder.addTopographicPoint(new Coordinate(4, 4, 3.0));
        profileBuilder.addTopographicPoint(new Coordinate(2, 5, 3.0));
        profileBuilder.addTopographicPoint(new Coordinate(1, 9, 2.0));
        profileBuilder.addTopographicPoint(new Coordinate(8, 2, 2.0));
        profileBuilder.finishFeeding();

        assertEquals(11, profileBuilder.getTriangles().size());
    }

    /**
     * Test the finish of {@link ProfileBuilder} feeding.
     * @throws ParseException JTS WKT parsing exception.
     */
    @Test
    public void topoBuildingFeedingTest() throws ParseException {
        ProfileBuilder profileBuilder = new ProfileBuilder(3, 3, 3, 2);
        profileBuilder.addTopographicLine((LineString) READER.read("LINESTRING (4 1 1.5, 5 7 1.0, 8 9 1.5)"));
        profileBuilder.addTopographicPoint(new Coordinate(7, 9, 2.5));
        profileBuilder.addTopographicPoint(new Coordinate(2, 4, 2.5));
        profileBuilder.addTopographicPoint(new Coordinate(6, 1, 3.0));
        assertNotNull(profileBuilder.finishFeeding());
        profileBuilder.addTopographicPoint(new Coordinate(4, 4, 3.0));
        profileBuilder.addTopographicPoint(new Coordinate(2, 5, 3.0));
        profileBuilder.addTopographicPoint(new Coordinate(1, 9, 2.0));
        profileBuilder.addTopographicPoint(new Coordinate(8, 2, 2.0));

        assertEquals(4, profileBuilder.getTriangles().size());
    }


    /**
     * Test the topographic cut profile generation.
     * @throws ParseException JTS WKT parsing exception.
     */
    @Test
    public void topoCutProfileTest() throws ParseException {
        ProfileBuilder profileBuilder = new ProfileBuilder(3, 3, 3, 2);
        profileBuilder.addTopographicLine((LineString) READER.read("LINESTRING (4 1 1.5, 5 7 1.0, 8 9 1.5)"));
        profileBuilder.addTopographicPoint(new Coordinate(7, 9, 2.5));
        profileBuilder.addTopographicPoint(new Coordinate(2, 4, 2.5));
        profileBuilder.addTopographicPoint(new Coordinate(6, 1, 3.0));
        profileBuilder.addTopographicPoint(new Coordinate(4, 4, 3.0));
        profileBuilder.addTopographicPoint(new Coordinate(2, 5, 3.0));
        profileBuilder.addTopographicPoint(new Coordinate(1, 9, 2.0));
        profileBuilder.addTopographicPoint(new Coordinate(8, 2, 2.0));
        profileBuilder.finishFeeding();

        CutProfile profile = profileBuilder.getProfile(new Coordinate(0, 1, 0.1), new Coordinate(8, 10, 0.3));
        List<CutPoint> pts = profile.getCutPoints();
        assertEquals(10, pts.size());
        assertEquals(0.0, pts.get(0).getCoordinate().x, DELTA);
        assertEquals(1.0, pts.get(0).getCoordinate().y, DELTA);
        assertEquals(0.1, pts.get(0).getCoordinate().z, DELTA);
        assertEquals(8.0, pts.get(9).getCoordinate().x, DELTA);
        assertEquals(10.0, pts.get(9).getCoordinate().y, DELTA);
        assertEquals(0.3, pts.get(9).getCoordinate().z, DELTA);
    }

    /**
     * Test the ground adding to a {@link ProfileBuilder}.
     * @throws ParseException JTS WKT parsing exception.
     */
    @Test
    public void groundAddingTest() throws ParseException {
        ProfileBuilder profileBuilder = new ProfileBuilder(3, 3, 3, 2);
        profileBuilder.addGroundEffect(READER.read("POLYGON((-1 7, -0.5 8, 0 8.5, 1 9, 1.5 7, 2 6, 2.5 7, 3 9, 5.5 8.5, 7 7, 7 6, 5 5, 5 4, 4 2, 2 3, 1 5, 0 6, -1 7))"), 0.5);
        profileBuilder.addGroundEffect(READER.read("POLYGON((8 1, 7 2, 7 4.5, 8 5, 9 4.5, 10 3.5, 9.5 2, 8 1))"), 0.25);
        profileBuilder.finishFeeding();

        assertEquals(2, profileBuilder.getGroundEffects().size());
    }

    /**
     * Test the finish of {@link ProfileBuilder} feeding.
     * @throws ParseException JTS WKT parsing exception.
     */
    @Test
    public void groundBuildingFeedingTest() throws ParseException {
        ProfileBuilder profileBuilder = new ProfileBuilder(3, 3, 3, 2);
        profileBuilder.addGroundEffect(READER.read("POLYGON((-1 7, -0.5 8, 0 8.5, 1 9, 1.5 7, 2 6, 2.5 7, 3 9, 5.5 8.5, 7 7, 7 6, 5 5, 5 4, 4 2, 2 3, 1 5, 0 6, -1 7))"), 0.5);
        assertNotNull(profileBuilder.finishFeeding());
        profileBuilder.addGroundEffect(READER.read("POLYGON((8 1, 7 2, 7 4.5, 8 5, 9 4.5, 10 3.5, 9.5 2, 8 1))"), 0.25);

        assertEquals(1, profileBuilder.getGroundEffects().size());
    }

    /**
     * Test the ground cut profile generation.
     * @throws ParseException JTS WKT parsing exception.
     */
    @Test
    public void groundCutProfileTest() throws ParseException {
        ProfileBuilder profileBuilder = new ProfileBuilder(3, 3, 3, 2);
        profileBuilder.addGroundEffect(READER.read("POLYGON((-1 7, -0.5 8, 0 8.5, 1 9, 1.5 7, 2 6, 2.5 7, 3 9, 5.5 8.5, 7 7, 7 6, 5 5, 5 4, 4 2, 2 3, 1 5, 0 6, -1 7))"), 0.5);
        profileBuilder.addGroundEffect(READER.read("POLYGON((8 1, 7 2, 7 4.5, 8 5, 9 4.5, 10 3.5, 9.5 2, 8 1))"), 0.25);
        profileBuilder.finishFeeding();

        CutProfile profile = profileBuilder.getProfile(new Coordinate(0, 1, 0.1), new Coordinate(8, 10, 0.3));
        List<CutPoint> pts = profile.getCutPoints();
        assertEquals(4, pts.size());
        assertEquals(0.0, pts.get(0).getCoordinate().x, DELTA);
        assertEquals(1.0, pts.get(0).getCoordinate().y, DELTA);
        assertEquals(0.1, pts.get(0).getCoordinate().z, DELTA);
        assertEquals(8.0, pts.get(3).getCoordinate().x, DELTA);
        assertEquals(10.0, pts.get(3).getCoordinate().y, DELTA);
        assertEquals(0.3, pts.get(3).getCoordinate().z, DELTA);
    }



    /**
     * Test the cut profile generation.
     * @throws ParseException JTS WKT parsing exception.
     */
    @Test
    public void allCutProfileTest() throws Exception {
        ProfileBuilder profileBuilder = new ProfileBuilder(3, 3, 3, 2);

        profileBuilder.addBuilding(READER.read("POLYGON((2 2 10, 1 3 15, 2 4 10, 3 3 12, 2 2 10))"), 10);
        profileBuilder.addBuilding(READER.read("POLYGON((4.5 7, 4.5 8.5, 6.5 8.5, 4.5 7))"), 3.3);
        profileBuilder.addBuilding(READER.read("POLYGON((7 6, 10 6, 10 2, 7 2, 7 6))"), 5.6);

        profileBuilder.addTopographicLine((LineString) READER.read("LINESTRING (4 1 1.5, 5 7 1.0, 8 9 1.5)"));
        profileBuilder.addTopographicPoint(new Coordinate(7, 9, 2.5));
        profileBuilder.addTopographicPoint(new Coordinate(2, 4, 2.5));
        profileBuilder.addTopographicPoint(new Coordinate(6, 1, 3.0));
        profileBuilder.addTopographicPoint(new Coordinate(4, 4, 3.0));
        profileBuilder.addTopographicPoint(new Coordinate(2, 5, 3.0));
        profileBuilder.addTopographicPoint(new Coordinate(1, 9, 2.0));
        profileBuilder.addTopographicPoint(new Coordinate(8, 2, 2.0));

        profileBuilder.addGroundEffect(READER.read("POLYGON((-1 -1, -1 2, 2 2, 2 -1, -1 -1))"), 0.6);
        profileBuilder.addGroundEffect(READER.read("POLYGON((-1 7, -0.5 8, 0 8.5, 1 9, 1.5 7, 2 6, 2.5 7, 3 9, 5.5 8.5, 7 7, 7 6, 5 5, 5 4, 4 2, 2 3, 1 5, 0 6, -1 7))"), 0.5);
        profileBuilder.addGroundEffect(READER.read("POLYGON((8 1, 7 2, 7 4.5, 8 5, 9 4.5, 10 3.5, 9.5 2, 8 1))"), 0.25);
        profileBuilder.finishFeeding();

        CutProfile profile = profileBuilder.getProfile(new Coordinate(0, 1, 0.1), new Coordinate(8, 10, 0.3));

        List<CutPoint> pts = profile.getCutPoints();
        assertEquals(19, pts.size());
        assertEquals(0.0, pts.get(0).getCoordinate().x, DELTA);
        assertEquals(1.0, pts.get(0).getCoordinate().y, DELTA);
        assertEquals(0.1, pts.get(0).getCoordinate().z, DELTA);
        assertEquals(8.0, pts.get(18).getCoordinate().x, DELTA);
        assertEquals(10.0, pts.get(18).getCoordinate().y, DELTA);
        assertEquals(0.3, pts.get(18).getCoordinate().z, DELTA);

    }

    @Test
    public void testComplexTopographic() throws IOException, XMLStreamException, CRSException, CoordinateOperationException {
        ProfileBuilder profileBuilder = new ProfileBuilder(3, 3, 3, 2);

        // Generate a digital elevation model using Simplex Noise method
        long seed = 5289231824766894L;
        double width = 2000;
        double height = 2000;
        int xStepSize = 10;
        int yStepSize = 10;
        double minHeight = 50;
        double maxHeight = 350;
        double xOrigin = 222532;
        double yOrigin = 6758964;
        double frequency = 5;
        Envelope envDomain = new Envelope();
        for(int x = 0; x < (int)width; x += xStepSize) {
            for(int y = 0; y < (int)height; y += yStepSize) {
                double nx = x/width - 0.5, ny = y/height - 0.5;
                double z = minHeight + OpenSimplex2S.noise2(seed, nx * frequency, ny * frequency)
                        * (maxHeight - minHeight);
                Coordinate topoCoordinate = new Coordinate(x + xOrigin, y + yOrigin, z);
                envDomain.expandToInclude(topoCoordinate);
                profileBuilder.addTopographicPoint(topoCoordinate);
            }
        }
        profileBuilder.finishFeeding();
        double[][] testPointPositions = new double[][] {{0.1, 0.15,0.25,0.16},
                {0.5, 0.1,0.8,0.4},
                {0.1, 0.1,0.11,0.11},
                {0.1, 0.1,0.9,0.9},
                {0.5, 0.5,0.55,0.55},
                {-0.1, 0.5,1.1,0.5}};

        // Check found intersections

        int loops = 800;
        int startLoop = 50;
        long start = 0;
        for(int i = 0; i < loops; i++) {
            if(i == startLoop) {
                start = System.currentTimeMillis();
            }
            for(double[] testPoint : testPointPositions) {
                Coordinate cutStart = new Coordinate(envDomain.getMinX() + envDomain.getWidth() * testPoint[0], envDomain.getMinY() + envDomain.getHeight() * testPoint[1]);
                cutStart.setZ(profileBuilder.getZGround(new CutPoint(cutStart, ProfileBuilder.IntersectionType.TOPOGRAPHY, 0)));
                Coordinate cutEnd = new Coordinate(envDomain.getMinX() + envDomain.getWidth() * testPoint[2], envDomain.getMinY() + envDomain.getHeight() * testPoint[3]);
                cutEnd.setZ(profileBuilder.getZGround(new CutPoint(cutEnd, ProfileBuilder.IntersectionType.TOPOGRAPHY, 0)));
                profileBuilder.getProfile(cutStart, cutEnd, 0);
            }
        }
        logger.info(String.format(Locale.ROOT, "Building topography profile in average of %f ms", (double)(System.currentTimeMillis() - start) / (loops - startLoop)));

//        try(FileOutputStream outData = new FileOutputStream("target/testTopo.geojson")) {
//            GeoJSONDocument geoJSONDocument = new GeoJSONDocument(outData);
//            geoJSONDocument.setInputCRS("EPSG:2154");
//            geoJSONDocument.writeHeader();
//            geoJSONDocument.writeTopographic(profileBuilder.getTriangles(), profileBuilder.getVertices());
//            geoJSONDocument.writeFooter();
//        }
    }

    @Test
    public void testProfileTopographicGroundEffectWall() throws Exception {

        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder()
                //Ground effects
                .addGroundEffect(0.0, 50.0, -20.0, 80.0, 0.9)
                .addGroundEffect(50.0, 150.0, -20.0, 80.0, 0.5)
                .addGroundEffect(150.0, 225.0, -20.0, 80.0, 0.2)
                //Topography
                .addTopographicLine(0, 80, 0, 225, 80, 0)
                .addTopographicLine(225, 80, 0, 225, -20, 0)
                .addTopographicLine(225, -20, 0, 0, -20, 0)
                .addTopographicLine(0, -20, 0, 0, 80, 0)
                .addTopographicLine(120, -20, 0, 120, 80, 0)
                .addTopographicLine(185, -5, 10, 205, -5, 10)
                .addTopographicLine(205, -5, 10, 205, 75, 10)
                .addTopographicLine(205, 75, 10, 185, 75, 10)
                .addTopographicLine(185, 75, 10, 185, -5, 10)
                // Add building
                .addWall(new Coordinate[]{
                                new Coordinate(175, 50, 17),
                                new Coordinate(190, 10, 14)},
                        1)
                .finishFeeding();

        Coordinate receiver = new Coordinate(200, 50, 14);
        Coordinate source = new Coordinate(10, 10, 1);
        CutProfile cutProfile = profileBuilder.getProfile(source, receiver, 0);
        assertEquals(9, cutProfile.getCutPoints().size());
        assertEquals(0, cutProfile.getCutPoints().get(0).getCoordinate().distance3D(new Coordinate(10, 10, 1)), 0.001);
        assertEquals(0, cutProfile.getCutPoints().get(1).getCoordinate().distance3D(new Coordinate(50, 18.421, 0)), 0.001);
        assertEquals(0, cutProfile.getCutPoints().get(2).getCoordinate().distance3D(new Coordinate(50, 18.421, 0)), 0.001);
        assertEquals(0, cutProfile.getCutPoints().get(3).getCoordinate().distance3D(new Coordinate(120, 33.158, 0)), 0.001);
        assertEquals(0, cutProfile.getCutPoints().get(4).getCoordinate().distance3D(new Coordinate(150, 39.474, 4.616)), 0.001);
        assertEquals(0, cutProfile.getCutPoints().get(5).getCoordinate().distance3D(new Coordinate(150, 39.474, 4.616)), 0.001);
        assertEquals(0, cutProfile.getCutPoints().get(6).getCoordinate().distance3D(new Coordinate(176.83, 45.122, 16.634)), 0.001);
        assertEquals(0, cutProfile.getCutPoints().get(7).getCoordinate().distance3D(new Coordinate(185, 46.842, 10)), 0.001);
        assertEquals(0, cutProfile.getCutPoints().get(8).getCoordinate().distance3D(new Coordinate(200, 50, 14)), 0.001);
    }

    /*
     * CutProfile{pts=[
     * SOURCE (10.0,10.0,1.0) ; grd : 0.9 ; topoH : null ; buildH : 0.0 ; buildId : -1 ; alpha : [] ; ,
     * GROUND_EFFECT (50.0,18.421052631578945,0.0) ; grd : 0.5 ; topoH : null ; buildH : 0.0 ; buildId : -1 ; alpha : [] ; ,
     * TOPOGRAPHY (120.0,33.1578947368421,0.0) ; grd : 0.5 ; topoH : null ; buildH : 0.0 ; buildId : -1 ; alpha : [] ; ,
     * GROUND_EFFECT (150.0,39.473684210526315,4.615384615384616) ; grd : 0.2 ; topoH : null ; buildH : 0.0 ; buildId : -1 ; alpha : [] ; ,
     * WALL (176.82926829268294,45.1219512195122,16.634146341463413) ; grd : 0.2 ; topoH : null ; buildH : 0.0 ; buildId : -1 ; alpha : [] ; ,
     * TOPOGRAPHY (185.0,46.84210526315789,10.0) ; grd : 0.2 ; topoH : null ; buildH : 0.0 ; buildId : -1 ; alpha : [] ; ,
     * RECEIVER (200.0,50.0,14.0) ; grd : 0.2 ; topoH : null ; buildH : 0.0 ; buildId : -1 ; alpha : [] ; ]
     */


    @Test
    public void testRelativeSourceLineProjection() throws ParseException {
        ProfileBuilder profileBuilder = new ProfileBuilder();
        // left upper hill
        profileBuilder.addTopographicLine(7.78, 112.97, 0.3, 7.78, -1.0, 0.3);
        profileBuilder.addTopographicLine(9.78, 112.97, 1.5, 9.78, -1.0, 1.5);
        profileBuilder.addTopographicLine(11.84, 112.97, 2.7, 11.84, -1.0, 2.7);
        profileBuilder.addTopographicLine(13.78, 112.97, 3, 13.78, -1.0, 3);

        // right downward hill
        profileBuilder.addTopographicLine(23.94, 112.97, 3, 23.94, -1.0, 3);
        profileBuilder.addTopographicLine(24.82, 112.97, 2.7, 24.82, -1.0, 2.7);
        profileBuilder.addTopographicLine(26.88, 112.97, 1.5, 26.88, -1.0, 1.5);
        profileBuilder.addTopographicLine(28.84, 112.97, 0.3, 28.84, -1.0, 0.3);

        // flat zone
        profileBuilder.addTopographicLine(29.85, 112.97, 0.0, 29.85, -1.0, 0.0);
        profileBuilder.addTopographicLine(137.79, 112.97, 0.0, 137.79, -1.0, 0.0);
        profileBuilder.finishFeeding();
        Scene scene = new Scene(profileBuilder);
        WKTReader wktReader = new WKTReader();
        Geometry geometry = wktReader.read("MultiLineStringZ ((18.27972380180239753 -1.52672398417648481 0.05, 18.27972380180239753 113.47327601582351519 0.05))");
        scene.addSource(1L, geometry);
        PathFinder pathFinder = new PathFinder(scene);
        assertEquals(2, scene.sourceGeometries.get(0).getNumPoints());
        pathFinder.makeSourceRelativeZToAbsolute();
        // The source line should now be made of 4 points (2 points being created by the elevated DEM)
        assertEquals(4, scene.sourceGeometries.get(0).getNumPoints());
        double minZ = Arrays.stream(scene.sourceGeometries.get(0).getCoordinates())
                .map(coordinate -> coordinate.z).min(Double::compareTo).get();
        assertEquals(0.05, minZ, 1e-6);
        double maxZ = Arrays.stream(scene.sourceGeometries.get(0).getCoordinates())
                .map(coordinate -> coordinate.z).max(Double::compareTo).get();
        assertEquals(3.05, maxZ, 1e-6);
    }
}
