/**
 * NoiseMap is a scientific computation plugin for OrbisGIS developed in order to
 * evaluate the noise impact on urban mobility plans. This model is
 * based on the French standard method NMPB2008. It includes traffic-to-noise
 * sources evaluation and sound propagation processing.
 *
 * This version is developed at French IRSTV Institute and at IFSTTAR
 * (http://www.ifsttar.fr/) as part of the Eval-PDU project, funded by the
 * French Agence Nationale de la Recherche (ANR) under contract ANR-08-VILL-0005-01.
 *
 * Noisemap is distributed under GPL 3 license. Its reference contact is Judicaël
 * Picaut <judicael.picaut@ifsttar.fr>. It is maintained by Nicolas Fortin
 * as part of the "Atelier SIG" team of the IRSTV Institute <http://www.irstv.fr/>.
 *
 * Copyright (C) 2011 IFSTTAR
 * Copyright (C) 2011-2012 IRSTV (FR CNRS 2488)
 *
 * Noisemap is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * Noisemap is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Noisemap. If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please consult: <http://www.orbisgis.org/>
 * or contact directly:
 * info_at_ orbisgis.org
 */
package org.noise_planet.noisemodelling.pathfinder;

import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKTWriter;
import org.locationtech.jts.operation.buffer.BufferParameters;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestWallReflection {

    public static int pushBuildingToWalls(ProfileBuilder.Building building, int index, List<ProfileBuilder.Wall> wallList) {
        ArrayList<ProfileBuilder.Wall> wallsOfBuilding = new ArrayList<>();
        Coordinate[] coords = building.getGeometry().getCoordinates();
        for (int i = 0; i < coords.length - 1; i++) {
            LineSegment lineSegment = new LineSegment(coords[i], coords[i + 1]);
            ProfileBuilder.Wall w = new ProfileBuilder.Wall(lineSegment, index, ProfileBuilder.IntersectionType.BUILDING);
            w.setProcessedWallIndex(i);
            wallsOfBuilding.add(w);
        }
        building.setWalls(wallsOfBuilding);
        wallList.addAll(wallsOfBuilding);
        return coords.length;
    }

    @Test
    public void testWideWall() {

        List<ProfileBuilder.Wall> buildWalls = new ArrayList<>();
        Coordinate cA = new Coordinate(50, 100, 5);
        Coordinate cB = new Coordinate(150, 100, 5);
        buildWalls.add(new ProfileBuilder.Wall(cA, cB, 0, ProfileBuilder.IntersectionType.WALL));

        Polygon polygon = MirrorReceiverResultIndex.createWallReflectionVisibilityCone(
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
//        ComputeCnossosRays.splitLineStringIntoPoints(pathReceiver, 0.5 ,pts);
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
