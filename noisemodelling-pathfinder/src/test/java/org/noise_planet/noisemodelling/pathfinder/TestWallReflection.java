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

import org.h2.tools.Csv;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.locationtech.jts.operation.buffer.BufferParameters;

import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

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
        CnossosPropagationData inputData = new CnossosPropagationData(profileBuilder);
        inputData.addReceiver(new Coordinate(599093.85,646227.90, 4));
        inputData.addSource(factory.createPoint(new Coordinate(599095.21, 646283.77, 1)));
        inputData.setComputeHorizontalDiffraction(false);
        inputData.setComputeVerticalDiffraction(false);
        inputData.maxRefDist = 80;
        inputData.maxSrcDist = 180;
        inputData.setReflexionOrder(2);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(inputData);
        computeRays.setThreadCount(1);


        Coordinate receiver = inputData.receivers.get(0);
        Envelope receiverPropagationEnvelope = new Envelope(receiver);
        receiverPropagationEnvelope.expandBy(inputData.maxSrcDist);
        List<ProfileBuilder.Wall> buildWalls = inputData.profileBuilder.getWallsIn(receiverPropagationEnvelope);
        MirrorReceiverResultIndex receiverMirrorIndex = new MirrorReceiverResultIndex(buildWalls, receiver,
                inputData.reflexionOrder, inputData.maxSrcDist, inputData.maxRefDist);


        List<PropagationPath> propagationPaths = computeRays.computeReflexion(receiver,
                inputData.sourceGeometries.get(0).getCoordinate(), false,
                new Orientation(), receiverMirrorIndex);

        // Only one second order reflexion propagation path must be found
        assertEquals(1, propagationPaths.size());


//        int idPath = 1;
//        for(PropagationPath path : propagationPaths) {
//            try {
//                try (FileWriter fileWriter = new FileWriter(String.format(Locale.ROOT, "target/testVisibilityPath_%03d.geojson", idPath))) {
//                    fileWriter.write(path.asGeom()));
//                }
//            } catch (IOException ex) {
//                //ignore
//            }
//            idPath++;
//        }

        // Keep only mirror receivers potentially visible from the source
        List<MirrorReceiverResult> mirrorResults = receiverMirrorIndex.findCloseMirrorReceivers(inputData.
                sourceGeometries.get(0).getCoordinate());
        assertEquals(18, mirrorResults.size());

        // // Or Take all reflections
        //        List<MirrorReceiverResult> mirrorResults = new ArrayList<>();
        //        var lst = receiverMirrorIndex.mirrorReceiverTree.query(new Envelope(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
        //                Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
        //        for(var item : lst) {
        //            if(item instanceof MirrorReceiverResult) {
        //                mirrorResults.add((MirrorReceiverResult) item);
        //            }
        //        }
        //        try {
        //            try (FileWriter fileWriter = new FileWriter("target/testVisibilityCone.csv")) {
        //                StringBuilder sb = new StringBuilder();
        //                receiverMirrorIndex.exportVisibility(sb, inputData.maxSrcDist, inputData.maxRefDist,
        //                        0, mirrorResults, true);
        //                fileWriter.write(sb.toString());
        //            }
        //        } catch (IOException ex) {
        //            //ignore
        //        }
        //




    }

}
