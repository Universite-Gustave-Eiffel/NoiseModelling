package org.noise_planet.noisemodelling.pathfinder;

import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test class dedicated to {@link ProfileBuilder}.
 */
public class ProfileBuilderTest {

    /** JTS WKT reader. */
    private static final WKTReader READER = new WKTReader();
    /** Delta value. */
    private static final double DELTA = 1e-8;

    /**
     * Test the building adding to a {@link ProfileBuilder}.
     * @throws ParseException JTS WKT parsing exception.
     */
    @Test
    public void buildingAddingTest() throws ParseException {
        ProfileBuilder profileBuilder = new ProfileBuilder(3, 3, 3, 2);
        profileBuilder.addBuilding(READER.read("POLYGON((1 1,5 1,5 5,1 5,1 1))"), 10);
        profileBuilder.addBuilding(READER.read("POLYGON((10 10,15 10,15 15,10 15,10 10))"), 23);
        profileBuilder.addBuilding(READER.read("POLYGON((6 8,8 10,8 4,6 8))"), 56);

        List<ProfileBuilder.Building> list = profileBuilder.getBuildings();
        assertEquals(3, list.size());
        assertEquals("POLYGON ((1 1, 5 1, 5 5, 1 5, 1 1))", list.get(0).getGeometry().toText());
        assertEquals(10, list.get(0).getHeight(), 0);
        assertEquals("POLYGON ((10 10, 15 10, 15 15, 10 15, 10 10))", list.get(1).getGeometry().toText());
        assertEquals(23, list.get(1).getHeight(), 0);
        assertEquals("POLYGON ((6 8, 8 10, 8 4, 6 8))", list.get(2).getGeometry().toText());
        assertEquals(56, list.get(2).getHeight(), 0);
    }

    /**
     * Test the finish of {@link ProfileBuilder} feeding.
     * @throws ParseException JTS WKT parsing exception.
     */
    @Test
    public void finishBuildingFeedingTest() throws ParseException {
        ProfileBuilder profileBuilder = new ProfileBuilder(3, 3, 3, 2);
        profileBuilder.addBuilding(READER.read("POLYGON((1 1,5 1,5 5,1 5,1 1))"), 10);
        assertTrue(profileBuilder.finishFeeding());
        profileBuilder.addBuilding(READER.read("POLYGON((10 10,15 10,15 15,10 15,10 10))"), 23);
        profileBuilder.addBuilding(READER.read("POLYGON((6 8,8 10,8 4,6 8))"), 56);

        List<ProfileBuilder.Building> list = profileBuilder.getBuildings();
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

        ProfileBuilder.CutProfile profile = profileBuilder.getProfile(new Coordinate(0, 1, 0.1), new Coordinate(8, 10, 0.3));
        List<ProfileBuilder.CutPoint> pts = profile.getCutPoints();
        assertEquals(4, pts.size());
        assertEquals(0.0, pts.get(0).getCoordinate().x, DELTA);
        assertEquals(1.0, pts.get(0).getCoordinate().y, DELTA);
        assertEquals(0.1, pts.get(0).getCoordinate().z, DELTA);
        assertEquals(1.411764705882353, pts.get(1).getCoordinate().x, DELTA);
        assertEquals(2.588235294117647, pts.get(1).getCoordinate().y, DELTA);
        assertEquals(3.5294117647058822, pts.get(1).getCoordinate().z, DELTA);
        assertEquals(2.3529411764705883, pts.get(2).getCoordinate().x, DELTA);
        assertEquals(3.6470588235294117, pts.get(2).getCoordinate().y, DELTA);
        assertEquals(6.117647058823529, pts.get(2).getCoordinate().z, DELTA);
        assertEquals(8.0, pts.get(3).getCoordinate().x, DELTA);
        assertEquals(10.0, pts.get(3).getCoordinate().y, DELTA);
        assertEquals(0.3, pts.get(3).getCoordinate().z, DELTA);
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
        assertTrue(profileBuilder.finishFeeding());
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

        ProfileBuilder.CutProfile profile = profileBuilder.getProfile(new Coordinate(0, 1, 0.1), new Coordinate(8, 10, 0.3));
        List<ProfileBuilder.CutPoint> pts = profile.getCutPoints();
        assertEquals(10, pts.size());
        assertEquals(0.0, pts.get(0).getCoordinate().x, DELTA);
        assertEquals(1.0, pts.get(0).getCoordinate().y, DELTA);
        assertEquals(0.1, pts.get(0).getCoordinate().z, DELTA);
        assertEquals(2.2857142857142856, pts.get(1).getCoordinate().x, DELTA);
        assertEquals(3.5714285714285716, pts.get(1).getCoordinate().y, DELTA);
        assertEquals(2.357142857142857, pts.get(1).getCoordinate().z, DELTA);
        assertEquals(8.0, pts.get(11).getCoordinate().x, DELTA);
        assertEquals(10.0, pts.get(11).getCoordinate().y, DELTA);
        assertEquals(0.3, pts.get(11).getCoordinate().z, DELTA);
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
        assertTrue(profileBuilder.finishFeeding());
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

        ProfileBuilder.CutProfile profile = profileBuilder.getProfile(new Coordinate(0, 1, 0.1), new Coordinate(8, 10, 0.3));
        List<ProfileBuilder.CutPoint> pts = profile.getCutPoints();
        assertEquals(4, pts.size());
        assertEquals(0.0, pts.get(0).getCoordinate().x, DELTA);
        assertEquals(1.0, pts.get(0).getCoordinate().y, DELTA);
        assertEquals(0.1, pts.get(0).getCoordinate().z, DELTA);
        assertEquals(1.92, pts.get(1).getCoordinate().x, DELTA);
        assertEquals(3.16, pts.get(1).getCoordinate().y, DELTA);
        assertEquals(Double.NaN, pts.get(1).getCoordinate().z, DELTA);
        assertEquals(6.11764705882353, pts.get(2).getCoordinate().x, DELTA);
        assertEquals(7.88235294117647, pts.get(2).getCoordinate().y, DELTA);
        assertEquals(Double.NaN, pts.get(2).getCoordinate().z, DELTA);
        assertEquals(8.0, pts.get(3).getCoordinate().x, DELTA);
        assertEquals(10.0, pts.get(3).getCoordinate().y, DELTA);
        assertEquals(0.3, pts.get(3).getCoordinate().z, DELTA);
    }



    /**
     * Test the cut profile generation.
     * @throws ParseException JTS WKT parsing exception.
     */
    @Test
    public void allCutProfileTest() throws ParseException {
        ProfileBuilder profileBuilder = new ProfileBuilder(3, 3, 3, 2);

        profileBuilder.addBuilding(READER.read("POLYGON((2 2 10, 1 3 15, 2 4 10, 3 3 12, 2 2 10))"));
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

        ProfileBuilder.CutProfile profile = profileBuilder.getProfile(new Coordinate(0, 1, 0.1), new Coordinate(8, 10, 0.3));
        List<ProfileBuilder.CutPoint> pts = profile.getCutPoints();
        assertEquals(16, pts.size());
        assertEquals(0.0, pts.get(0).getCoordinate().x, DELTA);
        assertEquals(1.0, pts.get(0).getCoordinate().y, DELTA);
        assertEquals(0.1, pts.get(0).getCoordinate().z, DELTA);
        assertEquals(8.0, pts.get(15).getCoordinate().x, DELTA);
        assertEquals(10.0, pts.get(15).getCoordinate().y, DELTA);
        assertEquals(0.3, pts.get(15).getCoordinate().z, DELTA);

        for(ProfileBuilder.CutPoint cut : pts) {
            System.out.println(cut);
            assert !Double.isNaN(cut.getCoordinate().z);
        }
    }

    //TODO source on ground effect
}
