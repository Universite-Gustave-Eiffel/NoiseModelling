/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */


package org.noise_planet.noisemodelling.pathfinder;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.*;
import org.noise_planet.noisemodelling.pathfinder.utils.geometry.CurvedProfileGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class CurvedProfileTest {


    @Test
    public void curvedProfile() {
        // Test the implementation with some dummy data
        List<CutPoint> flatProfile = new ArrayList<>();
        flatProfile.add(new CutPointSource(new Coordinate(0, 0, 4)));
        flatProfile.add(new CutPoint(new Coordinate(500, 500, 1)));
        flatProfile.add(new CutPoint(new Coordinate(1000, 1000, 1)));
        flatProfile.add(new CutPoint(new Coordinate(1500, 1500, 1)));
        flatProfile.add(new CutPointReceiver(new Coordinate(2000, 2000, 4)));

        flatProfile.get(0).setZGround(0);
        flatProfile.get(1).setZGround(0);
        flatProfile.get(2).setZGround(0);
        flatProfile.get(3).setZGround(0);
        flatProfile.get(4).setZGround(0);

        double distance = flatProfile.get(0).getCoordinate().distance(flatProfile.get(4).getCoordinate());
        List<CutPoint> curvedProfile = CurvedProfileGenerator.applyTransformation(flatProfile);
        assertInstanceOf(CutPointSource.class, curvedProfile.get(0));
        assertInstanceOf(CutPointReceiver.class, curvedProfile.get(curvedProfile.size() - 1));
        assertEquals(2828.427, distance, 1e-3);
        assertEquals(0, new Coordinate(0, 0, 4).distance3D(curvedProfile.get(0).getCoordinate()), 1e-6);
        assertEquals(0, new Coordinate(2000, 2000, 4).distance3D(curvedProfile.get(curvedProfile.size() - 1).getCoordinate()), 1e-6);
        assertEquals(-15.57, curvedProfile.get(1).getCoordinate().z, 1e-2);
        assertEquals(-21.09, curvedProfile.get(2).getCoordinate().z, 1e-2);
        assertEquals(-15.57, curvedProfile.get(3).getCoordinate().z, 1e-2);
        assertEquals(-16.57, curvedProfile.get(1).zGround, 1e-2);
        assertEquals(-22.09, curvedProfile.get(2).zGround, 1e-2);
        assertEquals(-16.57, curvedProfile.get(3).zGround, 1e-2);
    }

    @Test
    public void testTC28CurvedProfile() {

        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();

        // Add building
        builder.addBuilding(new Coordinate[]{
                        new Coordinate(113, 10, 0),
                        new Coordinate(127, 16, 0),
                        new Coordinate(102, 70, 0),
                        new Coordinate(88, 64, 0)}, 6, -1)

                .addBuilding(new Coordinate[]{
                        new Coordinate(176, 19, 0),
                        new Coordinate(164, 88, 0),
                        new Coordinate(184, 91, 0),
                        new Coordinate(196, 22, 0)}, 10, -1)

                .addBuilding(new Coordinate[]{
                        new Coordinate(250, 70, 0),
                        new Coordinate(250, 180, 0),
                        new Coordinate(270, 180, 0),
                        new Coordinate(270, 70, 0)}, 14, -1)

                .addBuilding(new Coordinate[]{
                        new Coordinate(332, 32, 0),
                        new Coordinate(348, 126, 0),
                        new Coordinate(361, 108, 0),
                        new Coordinate(349, 44, 0)}, 10, -1)

                .addBuilding(new Coordinate[]{
                        new Coordinate(400, 5, 0),
                        new Coordinate(400, 85, 0),
                        new Coordinate(415, 85, 0),
                        new Coordinate(415, 5, 0)}, 9, -1)

                .addBuilding(new Coordinate[]{
                        new Coordinate(444, 47, 0),
                        new Coordinate(436, 136, 0),
                        new Coordinate(516, 143, 0),
                        new Coordinate(521, 89, 0),
                        new Coordinate(506, 87, 0),
                        new Coordinate(502, 127, 0),
                        new Coordinate(452, 123, 0),
                        new Coordinate(459, 48, 0)}, 12, -1)

                .addBuilding(new Coordinate[]{
                        new Coordinate(773, 12, 0),
                        new Coordinate(728, 90, 0),
                        new Coordinate(741, 98, 0),
                        new Coordinate(786, 20, 0)}, 14, -1)

                .addBuilding(new Coordinate[]{
                        new Coordinate(972, 82, 0),
                        new Coordinate(979, 121, 0),
                        new Coordinate(993, 118, 0),
                        new Coordinate(986, 79, 0)}, 8, -1)
                .addGroundEffect(-11, 1011, -300, 300,0.5);


        builder.finishFeeding();
        Coordinate source = new Coordinate(0, 50, 4);
        Coordinate receiver = new Coordinate(1000, 100, 1);
        CutProfile profile = builder.getProfile(source, receiver);
        List<CutPoint> curvedProfile = CurvedProfileGenerator.applyTransformation(profile.getCutPoints());
        CutProfile curvedCutProfile = new CutProfile((CutPointSource) curvedProfile.get(0),
                (CutPointReceiver) curvedProfile.get(curvedProfile.size() - 1));
        curvedCutProfile.cutPoints = new ArrayList<>(curvedProfile);
        // Check new convex hull


    }

}
