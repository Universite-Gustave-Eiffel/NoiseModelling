/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */
package org.noise_planet.noisemodelling.pathfinder;

import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.Polygon;
import org.noise_planet.noisemodelling.pathfinder.path.MirrorReceiversCompute;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.ProfileBuilder;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.Wall;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class TestWallReflection {

    /*public static int pushBuildingToWalls(Building building, int index, List<Wall> wallList) {
        ArrayList<Wall> wallsOfBuilding = new ArrayList<>();
        Coordinate[] coords = building.getGeometry().getCoordinates();
        for (int i = 0; i < coords.length - 1; i++) {
            LineSegment lineSegment = new LineSegment(coords[i], coords[i + 1]);
            Wall w = new Wall(lineSegment, index, ProfileBuilder.IntersectionType.BUILDING);
            w.setProcessedWallIndex(i);
            wallsOfBuilding.add(w);
        }
        building.setWalls(wallsOfBuilding);
        wallList.addAll(wallsOfBuilding);
        return coords.length;
    }*/

    @Test
    public void testWideWall() {

        List<Wall> buildWalls = new ArrayList<>();
        Coordinate cA = new Coordinate(50, 100, 5);
        Coordinate cB = new Coordinate(150, 100, 5);
        buildWalls.add(new Wall(cA, cB, 0, ProfileBuilder.IntersectionType.WALL));

        Polygon polygon = MirrorReceiversCompute.createWallReflectionVisibilityCone(
                new Coordinate(100, 50, 0.1),
                new LineSegment(cA, cB), 100, 100);

        GeometryFactory factory = new GeometryFactory();
        assertTrue(polygon.intersects(factory.createPoint(new Coordinate(100, 145, 0))));
    }

//
//    @Test
//    public void testExportVisibilityCones() throws Exception {
//        double maxPropagationDistance = 30;
//        double maxPropagationDistanceFromWall = 9999;
//        int reflectionOrder = 4;
//
//        List<ProfileBuilder.Wall> buildWalls = new ArrayList<>();
//        Coordinate cA = new Coordinate(1, 1, 5);
//        Coordinate cB = new Coordinate(1, 8, 5);
//        Coordinate cC = new Coordinate(8, 8, 5);
//        Coordinate cD = new Coordinate(8, 5, 5);
//        Coordinate cE = new Coordinate(5, 5, 5);
//        Coordinate cF = new Coordinate(5, 1, 5);
//        Coordinate cG = new Coordinate(10, -5, 2.5);
//        Coordinate cH = new Coordinate(13, 8, 2.5);
//        Coordinate cI = new Coordinate(8, 9, 2.5);
//        Coordinate cJ = new Coordinate(12, 8, 2.5);
//        buildWalls.add(new ProfileBuilder.Wall(cE, cF, 0, ProfileBuilder.IntersectionType.WALL));
//        buildWalls.add(new ProfileBuilder.Wall(cG, cH, 2, ProfileBuilder.IntersectionType.WALL));
//        buildWalls.add(new ProfileBuilder.Wall(cI, cJ, 2, ProfileBuilder.IntersectionType.WALL));
//
//
//        GeometryFactory factory = new GeometryFactory();
//        List<Coordinate> pts = new ArrayList<>();
//        LineString pathReceiver = factory.createLineString(new Coordinate[] {
//                new Coordinate(5, -1, 0.1),
//                new Coordinate(7.8, 1.62, 0.1),
//                new Coordinate(8.06, 6.01, 0.1),
//                new Coordinate(4.73, 9.95)
//        });
//
//        CnossosPaths.splitLineStringIntoPoints(pathReceiver, 0.5 ,pts);
//
//        WKTWriter wktWriter = new WKTWriter();
//        try(FileWriter fileWriter = new FileWriter("target/testVisibilityCone.csv")) {
//            fileWriter.write("geom, type, time\n");
//            int t = 0;
//            for (Coordinate receiverCoordinates : pts) {
//                MirrorReceiverResultIndex mirrorReceiverResultIndex = new MirrorReceiverResultIndex(buildWalls, receiverCoordinates, reflectionOrder, maxPropagationDistance, maxPropagationDistanceFromWall);
//                List<MirrorReceiverResult> objs = (List<MirrorReceiverResult>) mirrorReceiverResultIndex.mirrorReceiverTree.query(new Envelope(new Coordinate(0, 0), new Coordinate(500, 500)));
//                for (MirrorReceiverResult res : objs) {
//                    Polygon visibilityCone = MirrorReceiverResultIndex.createWallReflectionVisibilityCone(res.getReceiverPos(), res.getWall().getLineSegment(), maxPropagationDistance, maxPropagationDistanceFromWall);
//                    fileWriter.write("\"");
//                    fileWriter.write(wktWriter.write(visibilityCone));
//                    fileWriter.write("\",0");
//                    fileWriter.write(","+t+"\n");
//                    fileWriter.write("\"");
//                    fileWriter.write(wktWriter.write(factory.createPoint(res.getReceiverPos()).buffer(0.1, 12, BufferParameters.CAP_ROUND)));
//                    fileWriter.write("\",4");
//                    fileWriter.write(","+t+"\n");
//                }
//                for (ProfileBuilder.Wall wall : buildWalls) {
//                    fileWriter.write("\"");
//                    fileWriter.write(wktWriter.write(factory.createLineString(new Coordinate[]{wall.p0, wall.p1}).buffer(0.05, 8, BufferParameters.CAP_SQUARE)));
//                    fileWriter.write("\",1");
//                    fileWriter.write(","+t+"\n");
//                }
//                fileWriter.write("\"");
//                fileWriter.write(wktWriter.write(factory.createPoint(receiverCoordinates).buffer(0.1, 12, BufferParameters.CAP_ROUND)));
//                fileWriter.write("\",2");
//                fileWriter.write(","+t+"\n");
//                t+=1;
//            }
//        }
//    }

}
