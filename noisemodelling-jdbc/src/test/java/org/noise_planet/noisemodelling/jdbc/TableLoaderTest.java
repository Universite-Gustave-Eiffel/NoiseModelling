/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */
package org.noise_planet.noisemodelling.jdbc;

import org.h2gis.api.EmptyProgressVisitor;
import org.h2gis.functions.factory.H2GISDBFactory;
import org.h2gis.functions.io.dbf.DBFRead;
import org.h2gis.functions.io.shp.SHPRead;
import org.h2gis.utilities.JDBCUtilities;
import org.h2gis.utilities.SpatialResultSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.LineString;
import org.noise_planet.noisemodelling.emission.LineSource;
import org.noise_planet.noisemodelling.emission.railway.RailWayParameters;
import org.noise_planet.noisemodelling.jdbc.input.DefaultTableLoader;
import org.noise_planet.noisemodelling.jdbc.input.SceneDatabaseInputSettings;
import org.noise_planet.noisemodelling.jdbc.railway.RailWayLWGeom;
import org.noise_planet.noisemodelling.jdbc.railway.RailWayLWIterator;
import org.noise_planet.noisemodelling.jdbc.utils.CellIndex;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.ProfileBuilder;
import org.noise_planet.noisemodelling.pathfinder.utils.AcousticIndicatorsFunctions;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.noise_planet.noisemodelling.pathfinder.utils.AcousticIndicatorsFunctions.*;

public class TableLoaderTest {

    private Connection connection;
    public static double[] aWeightingArray = Arrays.stream(
            asOctaveBands(ProfileBuilder.DEFAULT_FREQUENCIES_A_WEIGHTING_THIRD_OCTAVE)).
            mapToDouble(value -> value).toArray();

    @BeforeEach
    public void tearUp() throws Exception {
        connection = JDBCUtilities.wrapConnection(H2GISDBFactory.createSpatialDataBase(TableLoaderTest.class.getSimpleName(), true, ""));
    }

    @AfterEach
    public void tearDown() throws Exception {
        if(connection != null) {
            connection.close();
        }
    }

    @Test
    public void testNoiseEmissionRailWay() throws SQLException, IOException {
        SHPRead.importTable(connection, TableLoaderTest.class.getResource("RailTrack.shp").getFile());
        DBFRead.importTable(connection, TableLoaderTest.class.getResource("RailTrain.dbf").getFile());
        int expectedNumberOfRows;
        try(ResultSet rs = connection.createStatement().executeQuery("SELECT COUNT(*) FROM RAILTRACK")) {
            assertTrue(rs.next());
            expectedNumberOfRows = rs.getInt(1);
        }
        RailWayLWIterator railWayLWIterator = new RailWayLWIterator(connection,"RAILTRACK", "RAILTRAIN","RailwayVehiclesCnossos.json","RailwayTrainsets.json", "RailwayCnossosSNCF_2021.json", "RailwayPlatforms.json");

        int numberOfRows = 0;
        while (railWayLWIterator.hasNext()) {
            RailWayLWGeom v = railWayLWIterator.next();
            assertNotNull(v);
            numberOfRows++;
        }
        assertEquals(expectedNumberOfRows, numberOfRows);
    }

    @Test
    public void testNoiseEmissionRailWayTwoGeoms() throws SQLException, IOException {
        SHPRead.importTable(connection, TableLoaderTest.class.getResource("RailTrack.shp").getFile());
        DBFRead.importTable(connection, TableLoaderTest.class.getResource("RailTrain.dbf").getFile());

        // Test with two track only
        connection.createStatement().execute("DELETE FROM RAILTRACK WHERE PK NOT IN (SELECT PK FROM RAILTRACK LIMIT 2)");

        int expectedNumberOfRows;
        try(ResultSet rs = connection.createStatement().executeQuery("SELECT COUNT(*) FROM RAILTRACK")) {
            assertTrue(rs.next());
            expectedNumberOfRows = rs.getInt(1);
        }
        RailWayLWIterator railWayLWIterator = new RailWayLWIterator(connection,"RAILTRACK", "RAILTRAIN");
        int numberOfRows = 0;
        while (railWayLWIterator.hasNext()) {
            RailWayLWGeom v = railWayLWIterator.next();
            assertNotNull(v);
            numberOfRows++;
        }
        assertEquals(expectedNumberOfRows, numberOfRows);
    }
//

    @Test
    public void testNoiseEmissionRailWaySingleGeom() throws SQLException, IOException {
        SHPRead.importTable(connection, TableLoaderTest.class.getResource("RailTrack.shp").getFile());
        DBFRead.importTable(connection, TableLoaderTest.class.getResource("RailTrain.dbf").getFile());

        // Test with two track only
        connection.createStatement().execute("DELETE FROM RAILTRACK WHERE PK NOT IN (SELECT PK FROM RAILTRACK LIMIT 1)");

        int expectedNumberOfRows;
        try(ResultSet rs = connection.createStatement().executeQuery("SELECT COUNT(*) FROM RAILTRACK")) {
            assertTrue(rs.next());
            expectedNumberOfRows = rs.getInt(1);
        }
        RailWayLWIterator railWayLWIterator = new RailWayLWIterator(connection,"RAILTRACK", "RAILTRAIN");
        int numberOfRows = 0;
        while (railWayLWIterator.hasNext()) {
            RailWayLWGeom v = railWayLWIterator.next();
            assertNotNull(v);
            numberOfRows++;
        }
        assertEquals(expectedNumberOfRows, numberOfRows);
    }


    @Test
    public void testNoiseEmissionRailWaySingleGeomSingleTrain() throws SQLException, IOException {
        SHPRead.importTable(connection, TableLoaderTest.class.getResource("RailTrack.shp").getFile());
        DBFRead.importTable(connection, TableLoaderTest.class.getResource("RailTrain.dbf").getFile());

        // Test with two track only
        connection.createStatement().execute("DELETE FROM RAILTRACK WHERE PK NOT IN (SELECT PK FROM RAILTRACK LIMIT 1)");
        connection.createStatement().execute("DELETE FROM RAILTRAIN WHERE PK NOT IN (SELECT R1.PK FROM RAILTRAIN R1, RAILTRACK R2 WHERE r1.IDSECTION = R2.IDSECTION LIMIT 1)");

        int expectedNumberOfRows;
        try(ResultSet rs = connection.createStatement().executeQuery("SELECT COUNT(*) FROM RAILTRACK")) {
            assertTrue(rs.next());
            expectedNumberOfRows = rs.getInt(1);
        }
        RailWayLWIterator railWayLWIterator = new RailWayLWIterator(connection,"RAILTRACK", "RAILTRAIN");
        int numberOfRows = 0;
        while (railWayLWIterator.hasNext()) {
            RailWayLWGeom v = railWayLWIterator.next();
            assertNotNull(v);
            numberOfRows++;
        }
        assertEquals(expectedNumberOfRows, numberOfRows);
    }

    @Test
    public void testNoiseEmissionRailWay_OC5() throws SQLException, IOException {
        SHPRead.importTable(connection, TableLoaderTest.class.getResource("Test/OC/RailTrack.shp").getFile());
        DBFRead.importTable(connection, TableLoaderTest.class.getResource("Test/OC/RailTrain.dbf").getFile());

        RailWayLWIterator railWayLWIterator = new RailWayLWIterator(connection,"RAILTRACK", "RAILTRAIN");
        RailWayLWGeom v = railWayLWIterator.next();
        assertNotNull(v);
        v.setNbTrack(2);
        RailWayParameters railWayLW = v.getRailWayLW();
        List<LineString> geometries = v.getRailWayLWGeometry();

        v = railWayLWIterator.next();
        assertFalse(railWayLWIterator.hasNext());

    }

    @Test
    public void testNoiseEmissionRailWay_BM() throws SQLException, IOException {
        double[] dBA = new double[]{-30,-26.2,-22.5,-19.1,-16.1,-13.4,-10.9,-8.6,-6.6,-4.8,-3.2,-1.9,-0.8,0,0.6,1,1.2,1.3,1.2,1,0.5,-0.1,-1.1,-2.5};

        SHPRead.importTable(connection, TableLoaderTest.class.getResource("Test/BM/RailTrack.shp").getFile());
        DBFRead.importTable(connection, TableLoaderTest.class.getResource("Test/BM/RailTrain.dbf").getFile());

        HashMap<String, double[]> Resultats = new HashMap<>();

        RailWayLWIterator railWayLWIterator = new RailWayLWIterator(connection,"RAILTRACK", "RAILTRAIN","RailwayVehiclesCnossos.json","RailwayTrainsets.json", "RailwayCnossosSNCF_2021.json", "RailwayPlatforms.json");
        double resD,resE,resN;

        while (railWayLWIterator.hasNext()) {
            RailWayLWGeom v = railWayLWIterator.next();

            RailWayParameters railWayLW = v.getRailWayLWDay();
            double[] lW = new double[24];
            Arrays.fill(lW, -99.00);

            if (!railWayLW.getRailwaySourceList().isEmpty()){
                for (Map.Entry<String, LineSource> railwaySourceEntry : railWayLW.getRailwaySourceList().entrySet()) {
                    double[]  lW1 = railwaySourceEntry.getValue().getlW();
                    lW = AcousticIndicatorsFunctions.sumDbArray(lW, lW1);
                }
            }

            double[] LWA = sumArray(lW, dBA);
            resD = sumDbArray(LWA);

            railWayLW = v.getRailWayLWEvening();
            Arrays.fill(lW, -99.00);
            if (!railWayLW.getRailwaySourceList().isEmpty()){
                for (Map.Entry<String, LineSource> railwaySourceEntry : railWayLW.getRailwaySourceList().entrySet()) {
                    double[]  lW1 = railwaySourceEntry.getValue().getlW();
                    lW = AcousticIndicatorsFunctions.sumDbArray(lW, lW1);
                }
            }
            LWA = sumArray(lW, dBA);
            resE = sumDbArray(LWA);

            railWayLW = v.getRailWayLWNight();
            Arrays.fill(lW, -99.00);
            if (!railWayLW.getRailwaySourceList().isEmpty()){
                for (Map.Entry<String, LineSource> railwaySourceEntry : railWayLW.getRailwaySourceList().entrySet()) {
                    double[]  lW1 = railwaySourceEntry.getValue().getlW();
                    lW = AcousticIndicatorsFunctions.sumDbArray(lW, lW1);
                }
            }
            LWA = sumArray(lW, dBA);
            resN = sumDbArray(LWA);

            String idSection = v.getIdSection();

            Resultats.put(idSection,new double[]{resD, resE, resN});

        }
    }

    @Test
    public void testNoiseEmissionRailWay_Section556() throws SQLException, IOException {
        double[] dBA = new double[]{-30,-26.2,-22.5,-19.1,-16.1,-13.4,-10.9,-8.6,-6.6,-4.8,-3.2,-1.9,-0.8,0,0.6,1,1.2,1.3,1.2,1,0.5,-0.1,-1.1,-2.5};

        SHPRead.importTable(connection, TableLoaderTest.class.getResource("Test/556/RAIL_SECTIONS.shp").getFile());
        DBFRead.importTable(connection, TableLoaderTest.class.getResource("Test/556/RAIL_TRAFIC.dbf").getFile());

        HashMap<String, double[]> Resultats = new HashMap<>();

        RailWayLWIterator railWayLWIterator = new RailWayLWIterator(connection,"RAIL_SECTIONS", "RAIL_TRAFIC");

        double resD,resE,resN;

       // RailWayLWIterator.RailWayLWGeom v = railWayLWIterator.current();

        while (railWayLWIterator.hasNext()) {
            RailWayLWGeom v = railWayLWIterator.next();

            RailWayParameters railWayLW = v.getRailWayLWDay();

            double[] lW = new double[24];
            Arrays.fill(lW, -99.00);

            if (!railWayLW.getRailwaySourceList().isEmpty()){
                for (Map.Entry<String, LineSource> railwaySourceEntry : railWayLW.getRailwaySourceList().entrySet()) {
                    double[]  lW1 = railwaySourceEntry.getValue().getlW();
                    lW = AcousticIndicatorsFunctions.sumDbArray(lW, lW1);
                }
            }

            double[] LWA = sumArray(lW, dBA);
            resD = sumDbArray(LWA);

            railWayLW = v.getRailWayLWEvening();
            Arrays.fill(lW, -99.00);
            if (!railWayLW.getRailwaySourceList().isEmpty()){
                for (Map.Entry<String, LineSource> railwaySourceEntry : railWayLW.getRailwaySourceList().entrySet()) {
                    double[]  lW1 = railwaySourceEntry.getValue().getlW();
                    lW = AcousticIndicatorsFunctions.sumDbArray(lW, lW1);
                }
            }
            LWA = sumArray(lW, dBA);
            resE = sumDbArray(LWA);

            railWayLW = v.getRailWayLWNight();
            Arrays.fill(lW, -99.00);
            if (!railWayLW.getRailwaySourceList().isEmpty()){
                for (Map.Entry<String, LineSource> railwaySourceEntry : railWayLW.getRailwaySourceList().entrySet()) {
                    double[]  lW1 = railwaySourceEntry.getValue().getlW();
                    lW = AcousticIndicatorsFunctions.sumDbArray(lW, lW1);
                }
            }
            LWA = sumArray(lW, dBA);
            resN = sumDbArray(LWA);

            String idSection = v.getIdSection();

            Resultats.put(idSection,new double[]{resD, resE, resN});

        }
    }


    @Test
    public void testNoiseEmissionRailWayForPropa() throws SQLException, IOException {
        SHPRead.importTable(connection, TableLoaderTest.class.getResource("PropaRail/Rail_Section2.shp").getFile());
        DBFRead.importTable(connection, TableLoaderTest.class.getResource("PropaRail/Rail_Traffic.dbf").getFile());

        EmissionTableGenerator.makeTrainLWTable(connection, "Rail_Section2", "Rail_Traffic",
                "LW_RAILWAY", "HZ");

        // Get Class to compute LW
        RailWayLWIterator railWayLWIterator = new RailWayLWIterator(connection,"Rail_Section2", "Rail_Traffic");
        RailWayLWGeom v = railWayLWIterator.next();
        assertNotNull(v);
        List<LineString> geometries = v.getRailWayLWGeometry();
        assertEquals(geometries.size(),2);

        SHPRead.importTable(connection, TableLoaderTest.class.getResource("PropaRail/Recepteurs.shp").getFile());
        SHPRead.importTable(connection, TableLoaderTest.class.getResource("PropaRail/Buildings.shp").getFile());
        SHPRead.importTable(connection, TableLoaderTest.class.getResource("PropaRail/Rail_protect.shp").getFile());

        // ICI POUR CHANGER HAUTEUR ET G ECRAN
        connection.createStatement().execute("CREATE TABLE SCREENS AS SELECT ST_BUFFER(the_geom, 0.5, 'join=mitre endcap=flat') as the_geom, pk as pk, 3.0 as height, g as g FROM Rail_protect");

        // ICI HAUTEUR RECPTEUR
        connection.createStatement().execute("SELECT UpdateGeometrySRID('RECEPTEURS', 'THE_GEOM', 2154);");
        connection.createStatement().execute("SELECT UpdateGeometrySRID('LW_RAILWAY', 'THE_GEOM', 2154);");

        connection.createStatement().execute("UPDATE RECEPTEURS SET THE_GEOM = ST_UPDATEZ(THE_GEOM,4.0);");
        //connection.createStatement().execute("UPDATE LW_RAILWAY SET THE_GEOM = ST_SETSRID(ST_UPDATEZ(THE_GEOM,0.5),2154);");


        NoiseMapByReceiverMaker noiseMapByReceiverMaker = new NoiseMapByReceiverMaker("SCREENS", "LW_RAILWAY",
                "RECEPTEURS");

        NoiseMapDatabaseParameters parameters = noiseMapByReceiverMaker.getNoiseMapDatabaseParameters();

        noiseMapByReceiverMaker.setInputMode(SceneDatabaseInputSettings.INPUT_MODE.INPUT_MODE_LW_DEN);

        // Use train directivity functions instead of discrete directivity
        DefaultTableLoader defaultTableLoader = ((DefaultTableLoader) noiseMapByReceiverMaker.getPropagationProcessDataFactory());
        defaultTableLoader.insertTrainDirectivity();

        noiseMapByReceiverMaker.run(connection, new EmptyProgressVisitor());

        DefaultTableLoader loader = (DefaultTableLoader) noiseMapByReceiverMaker.getTableLoader();

        List<String> frequenciesFields = loader.frequencyArray.stream()
                .map(frequency -> noiseMapByReceiverMaker.getFrequencyFieldPrepend()+frequency)
                .collect(Collectors.toList());
        double[] expected = new double[]{20.92, 22.45, 28.20, 27.02, 29.60, 31.44, 33.84, 31.74, 31.20, 32.43, 34.63,
                38.16, 40.28, 40.38, 39.15, 33.14, 29.30, 26.11, 23.72, 17.31, 7.67, -6.51, -23.50, -47.91};
        try(ResultSet rs = connection.createStatement().executeQuery("SELECT * FROM "
                + parameters.receiversLevelTable +
                "  WHERE PERIOD='D' ORDER BY IDRECEIVER")) {
            assertTrue(rs.next());
            double[] receiversLevel = frequenciesFields.stream().mapToDouble(field -> {
                try {
                    return rs.getDouble(field);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }).toArray();
            assertArrayEquals(expected, receiversLevel, 0.01);
        }

    }


    @Test
    public void testReadFrequencies() throws SQLException, IOException {
        SHPRead.importTable(connection, TableLoaderTest.class.getResource("lw_roads.shp").getFile());
        SHPRead.importTable(connection, TableLoaderTest.class.getResource("buildings.shp").getFile());
        SHPRead.importTable(connection, TableLoaderTest.class.getResource("receivers.shp").getFile());

        try(Statement st = connection.createStatement()) {
            // drop all columns except 1000 Hz
            st.execute("ALTER TABLE lw_roads drop column LWD63");
            st.execute("ALTER TABLE lw_roads drop column LWD125");
            st.execute("ALTER TABLE lw_roads drop column LWD250");
            st.execute("ALTER TABLE lw_roads drop column LWD500");
            st.execute("ALTER TABLE lw_roads drop column LWD2000");
            st.execute("ALTER TABLE lw_roads drop column LWD4000");
            st.execute("ALTER TABLE lw_roads drop column LWD8000");
            st.execute("ALTER TABLE lw_roads drop column LWE63");
            st.execute("ALTER TABLE lw_roads drop column LWE125");
            st.execute("ALTER TABLE lw_roads drop column LWE250");
            st.execute("ALTER TABLE lw_roads drop column LWE500");
            st.execute("ALTER TABLE lw_roads drop column LWE1000");
            st.execute("ALTER TABLE lw_roads drop column LWE2000");
            st.execute("ALTER TABLE lw_roads drop column LWE4000");
            st.execute("ALTER TABLE lw_roads drop column LWE8000");
            st.execute("ALTER TABLE lw_roads drop column LWN63");
            st.execute("ALTER TABLE lw_roads drop column LWN125");
            st.execute("ALTER TABLE lw_roads drop column LWN250");
            st.execute("ALTER TABLE lw_roads drop column LWN500");
            st.execute("ALTER TABLE lw_roads drop column LWN1000");
            st.execute("ALTER TABLE lw_roads drop column LWN2000");
            st.execute("ALTER TABLE lw_roads drop column LWN4000");
            st.execute("ALTER TABLE lw_roads drop column LWN8000");
        }

        NoiseMapByReceiverMaker noiseMap = new NoiseMapByReceiverMaker("BUILDINGS",
                "LW_ROADS", "RECEIVERS");

        noiseMap.setFrequencyFieldPrepend("LW");

        noiseMap.initialize(connection);

        DefaultTableLoader tableLoader = (DefaultTableLoader)noiseMap.getTableLoader();

        assertEquals(1, tableLoader.frequencyArray.size());

        assertEquals(1000, (int) tableLoader.frequencyArray.get(0));
    }



    // Check regression of finding cell i,j that contains receivers
    @Test
    public void testRegression1() throws SQLException, IOException {
        SHPRead.importTable(connection, TableLoaderTest.class.getResource("regression1/lw_roads_fence.shp").getFile());
        SHPRead.importTable(connection, TableLoaderTest.class.getResource("regression1/bati_fence.shp").getFile());
        SHPRead.importTable(connection, TableLoaderTest.class.getResource("regression1/receivers.shp").getFile());

        // Count receivers
        int nbReceivers = 0;
        try(ResultSet rs = connection.createStatement().executeQuery("SELECT COUNT(*) CPT FROM RECEIVERS")) {
            assertTrue(rs.next());
            nbReceivers = rs.getInt(1);
        }

        NoiseMapByReceiverMaker noiseMapByReceiverMaker = new NoiseMapByReceiverMaker("BATI_FENCE",
                "LW_ROADS_FENCE",
                "RECEIVERS");

        noiseMapByReceiverMaker.initialize(connection);

        Map<CellIndex, Integer> populatedCells = noiseMapByReceiverMaker.searchPopulatedCells(connection);

        // Check if all receivers are found
        assertEquals(nbReceivers, populatedCells.values().stream().reduce(Integer::sum).orElse(0));
    }

<<<<<<< fixhRail

    /**
     * Test that HRAIL column is properly created in LW_RAILWAY output
     * and that the default value matches the default platform hRail (h2 = 0.18).
     */
    @Test
    public void testRailWithScreenBodyBarrier() throws SQLException, IOException {
        // --- Common setup ---
        SHPRead.importTable(connection, TableLoaderTest.class.getResource("PropaRail/Rail_Section2.shp").getFile());
        DBFRead.importTable(connection, TableLoaderTest.class.getResource("PropaRail/Rail_Traffic.dbf").getFile());

        EmissionTableGenerator.makeTrainLWTable(connection, "Rail_Section2", "Rail_Traffic",
                "LW_RAILWAY", "HZ");

        // Verify HRAIL column was created in LW_RAILWAY
        assertTrue(JDBCUtilities.hasField(connection, "LW_RAILWAY", "HRAIL"),
                "LW_RAILWAY should have HRAIL column");

        // Verify HRAIL value is the default platform hRail (h2 = 0.18)
        try (ResultSet rs = connection.createStatement().executeQuery(
                "SELECT HRAIL FROM LW_RAILWAY LIMIT 1")) {
            assertTrue(rs.next());
            assertEquals(0.18, rs.getDouble("HRAIL"), 0.001,
                    "Default HRAIL should be 0.18 (h2 = rail above ballast)");
        }
    }

=======
    @Test
    public void testRoadNoiseEmission() throws SQLException, IOException {
        try(Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS TRAFFIC_TEST (PK SERIAL, LV_D REAL, LV_SPD_D REAL, HGV_D REAL, HGV_SPD_D REAL, PVMT VARCHAR);\n"
                    + "INSERT INTO TRAFFIC_TEST(LV_D, LV_SPD_D, HGV_D, HGV_SPD_D, PVMT) VALUES (3747.23, 50, 479.02, 50, 'FR_R2');");
        }
        Map<String, Integer> sourceEmissionFieldsCache = new HashMap<>();
        double globalLevel = 0;
        try(SpatialResultSet rs = connection.createStatement().executeQuery("SELECT * FROM TRAFFIC_TEST").unwrap(SpatialResultSet.class)) {
            assertTrue(rs.next());
            // new double[][] {ld, le, ln}
            double[][] lw = EmissionTableGenerator.computeLw(rs, 1, sourceEmissionFieldsCache);
            double[] dbLw = AcousticIndicatorsFunctions.wToDb(lw[0]);
            double[] dbALw = AcousticIndicatorsFunctions.sumArray(dbLw, aWeightingArray);
            globalLevel = AcousticIndicatorsFunctions.sumArray(AcousticIndicatorsFunctions.dBToW(dbALw)); // add day value only
        }
        assertEquals(89.6, AcousticIndicatorsFunctions.wToDb(globalLevel), 0.1);
    }
>>>>>>> main
}