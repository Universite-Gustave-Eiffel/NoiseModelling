package org.noise_planet.noisemodelling.jdbc;

import org.cts.crs.CRSException;
import org.cts.op.CoordinateOperationException;
import org.h2gis.api.EmptyProgressVisitor;
import org.h2gis.api.ProgressVisitor;
import org.h2gis.functions.factory.H2GISDBFactory;
import org.h2gis.functions.io.dbf.DBFRead;
import org.h2gis.functions.io.shp.SHPDriverFunction;
import org.h2gis.functions.io.shp.SHPRead;
import org.h2gis.functions.spatial.convert.ST_Force3D;
import org.h2gis.functions.spatial.edit.ST_UpdateZ;
import org.h2gis.utilities.JDBCUtilities;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.noise_planet.noisemodelling.emission.RailWayLW;
import org.noise_planet.noisemodelling.pathfinder.IComputeRaysOut;
import org.noise_planet.noisemodelling.pathfinder.ProfileBuilder;
import org.noise_planet.noisemodelling.pathfinder.RootProgressVisitor;
import org.noise_planet.noisemodelling.pathfinder.utils.KMLDocument;
import org.noise_planet.noisemodelling.propagation.ComputeRaysOutAttenuation;
import org.noise_planet.noisemodelling.propagation.PropagationProcessPathData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static org.junit.Assert.*;

public class PropaTrainTest {

    static Logger LOGGER = LoggerFactory.getLogger(PropaTrainTest.class);

    private Connection connection;

    @Before
    public void tearUp() throws Exception {
        connection = JDBCUtilities.wrapConnection(H2GISDBFactory.createSpatialDataBase(PropaTrainTest.class.getSimpleName(), true, ""));
    }

    @After
    public void tearDown() throws Exception {
        if(connection != null) {
            connection.close();
        }
    }

    public static void importFiles(Connection connection) throws IOException, SQLException {

        SHPRead.importTable(connection, PropaTrainTest.class.getResource("PropaRail/RAIL_SECTIONS.shp").getFile());
        DBFRead.importTable(connection, PropaTrainTest.class.getResource("PropaRail/Rail_Traffic.dbf").getFile());
        SHPRead.importTable(connection, PropaTrainTest.class.getResource("PropaRail/RECEPTEURS.shp").getFile());

        SHPRead.importTable(connection, PropaTrainTest.class.getResource("PropaRail/Buildings.shp").getFile());
        SHPRead.importTable(connection, PropaTrainTest.class.getResource("PropaRail/RAIL_PROTECT.shp").getFile());


        SHPRead.importTable(connection, PropaTrainTest.class.getResource("PropaRail/DEM.shp").getFile());
        SHPRead.importTable(connection, PropaTrainTest.class.getResource("PropaRail/LANDCOVER_G0.shp").getFile());
        SHPRead.importTable(connection, PropaTrainTest.class.getResource("PropaRail/LANDCOVER_G1.shp").getFile());


    }

    public static void computeLW(Connection connection) throws IOException, SQLException {

        // drop table LW_RAILWAY if exists and the create and prepare the table
        connection.createStatement().execute("drop table if exists LW_RAILWAY;");

        // Build and execute queries
        StringBuilder createTableQuery = new StringBuilder("create table LW_RAILWAY (ID_SECTION int," +
                " the_geom GEOMETRY, DIR_ID int");
        StringBuilder insertIntoQuery = new StringBuilder("INSERT INTO LW_RAILWAY(ID_SECTION, the_geom," +
                " DIR_ID");
        StringBuilder insertIntoValuesQuery = new StringBuilder("?,?,?");
        for(int thirdOctave : PropagationProcessPathData.DEFAULT_FREQUENCIES_THIRD_OCTAVE) {
            createTableQuery.append(", LWD");
            createTableQuery.append(thirdOctave);
            createTableQuery.append(" double precision");
            insertIntoQuery.append(", LWD");
            insertIntoQuery.append(thirdOctave);
            insertIntoValuesQuery.append(", ?");
        }
        for(int thirdOctave : PropagationProcessPathData.DEFAULT_FREQUENCIES_THIRD_OCTAVE) {
            createTableQuery.append(", LWE");
            createTableQuery.append(thirdOctave);
            createTableQuery.append(" double precision");
            insertIntoQuery.append(", LWE");
            insertIntoQuery.append(thirdOctave);
            insertIntoValuesQuery.append(", ?");
        }
        for(int thirdOctave : PropagationProcessPathData.DEFAULT_FREQUENCIES_THIRD_OCTAVE) {
            createTableQuery.append(", LWN");
            createTableQuery.append(thirdOctave);
            createTableQuery.append(" double precision");
            insertIntoQuery.append(", LWN");
            insertIntoQuery.append(thirdOctave);
            insertIntoValuesQuery.append(", ?");
        }
        createTableQuery.append(")");
        insertIntoQuery.append(") VALUES (");
        insertIntoQuery.append(insertIntoValuesQuery);
        insertIntoQuery.append(")");
        connection.createStatement().execute(createTableQuery.toString());

        // --------------------------------------
        // Start calculation and fill the table
        // --------------------------------------

        // Get Class to compute LW
        LDENConfig ldenConfig = new LDENConfig(LDENConfig.INPUT_MODE.INPUT_MODE_RAILWAY_FLOW);
        ldenConfig.setPropagationProcessPathData(new PropagationProcessPathData());
        ldenConfig.setCoefficientVersion(2);
        ldenConfig.setExportRays(true);
        RailWayLWIterator railWayLWIterator = new RailWayLWIterator(connection,"Rail_SectionS", "Rail_Traffic", ldenConfig);

        while (railWayLWIterator.hasNext()) {
            RailWayLWIterator.RailWayLWGeom railWayLWGeom = railWayLWIterator.next();

            RailWayLW railWayLWDay = railWayLWGeom.getRailWayLWDay();
            RailWayLW railWayLWEvening = railWayLWGeom.getRailWayLWEvening();
            RailWayLW railWayLWNight = railWayLWGeom.getRailWayLWNight();
            List<LineString> geometries = railWayLWGeom.getRailWayLWGeometry();

            int pk = railWayLWGeom.getPK();
            double[] LWDay = new double[0];
            double[] LWEvening = new double[0];
            double[] LWNight = new double[0];
            double heightSource = 0;
            int directivityId = 0;
            for (int iSource = 0; iSource < 6; iSource++) {
                switch (iSource) {
                    case 0:
                        LWDay = railWayLWDay.getLWRolling();
                        LWEvening = railWayLWEvening.getLWRolling();
                        LWNight = railWayLWNight.getLWRolling();
                        heightSource = 0.5;
                        directivityId = 1;
                        break;
                    case 1:
                        LWDay = railWayLWDay.getLWTractionA();
                        LWEvening = railWayLWEvening.getLWTractionA();
                        LWNight = railWayLWNight.getLWTractionA();
                        heightSource = 0.5;
                        directivityId = 2;
                        break;
                    case 2:
                        LWDay = railWayLWDay.getLWTractionB();
                        LWEvening = railWayLWEvening.getLWTractionB();
                        LWNight = railWayLWNight.getLWTractionB();
                        heightSource = 4;
                        directivityId = 3;
                        break;
                    case 3:
                        LWDay = railWayLWDay.getLWAerodynamicA();
                        LWEvening = railWayLWEvening.getLWAerodynamicA();
                        LWNight = railWayLWNight.getLWAerodynamicA();
                        heightSource = 0.5;
                        directivityId = 4;
                        break;
                    case 4:
                        LWDay = railWayLWDay.getLWAerodynamicB();
                        LWEvening = railWayLWEvening.getLWAerodynamicB();
                        LWNight = railWayLWNight.getLWAerodynamicB();
                        heightSource = 4;
                        directivityId = 5;
                        break;
                    case 5:
                        LWDay = railWayLWDay.getLWBridge();
                        LWEvening = railWayLWEvening.getLWBridge();
                        LWNight = railWayLWNight.getLWBridge();
                        heightSource = 0.5;
                        directivityId = 6;
                        break;
                }
                PreparedStatement ps = connection.prepareStatement(insertIntoQuery.toString());
                for (Geometry trackGeometry : geometries) {

                    Geometry sourceGeometry = ST_UpdateZ.updateZ(ST_Force3D.force3D(trackGeometry), heightSource).copy() ;

                    int cursor = 1;
                    ps.setInt(cursor++, pk);
                    ps.setObject(cursor++, sourceGeometry);
                    ps.setInt(cursor++, directivityId);
                    for (double v : LWDay) {
                        ps.setDouble(cursor++, v);
                    }
                    for (double v : LWEvening) {
                        ps.setDouble(cursor++, v);
                    }
                    for (double v : LWNight) {
                        ps.setDouble(cursor++, v);
                    }
                    ps.addBatch();
                }
                ps.executeBatch();
            }

        }

        // small check
        railWayLWIterator = new RailWayLWIterator(connection,"Rail_Sections", "Rail_Traffic", ldenConfig);
        RailWayLWIterator.RailWayLWGeom v = railWayLWIterator.next();
        assertNotNull(v);
        List<LineString> geometries = v.getRailWayLWGeometry();
        assertEquals(geometries.size(),2);

        // Add primary key and srid to the LW table
        connection.createStatement().execute("ALTER TABLE  LW_RAILWAY  ADD PK INT AUTO_INCREMENT PRIMARY KEY;");
        connection.createStatement().execute("SELECT UpdateGeometrySRID('LW_RAILWAY', 'THE_GEOM', 2154);");
    }


    @Test
    public void testNoiseEmissionRailWayForPropa() throws SQLException, IOException {

        importFiles(connection);
        computeLW(connection);

        List<String> configs = Arrays.asList("F0", "F1", "F2", "F3", "C0","C1","C2","C3","C4","C5","C6");

        //List<String> configs = Arrays.asList("C3","C4");


        for (String configName : configs) {
            System.out.println(configName);

            // ecran
            double screenHeight = 3.0;
            if (configName.equals("C1")) screenHeight = 2.0;

            double G = 0;
            if (configName.equals("C3")) G = 1;

            double screenDepth = 0.5;
            if (configName.equals("C2")) screenDepth = 1;

            // Receivers
            String rcvName = "RECEPTEURS";
            double rcvHeight = 1.2;
            if (configName.equals("F3") ||
                    configName.equals("C6")) rcvHeight = 4.0;


            // dem
            boolean dem = false;
            if (configName.equals("F0") ||
                    configName.equals("F1") ||
                    configName.equals("F3") ||
                    configName.equals("C0") ||
                    configName.equals("C1") ||
                    configName.equals("C2") ||
                    configName.equals("C3") ||
                    configName.equals("C4") ||
                    configName.equals("C5") ||
                    configName.equals("C6")) dem = true;

            // ground
            String landcover  = "G0";
            if (configName.equals("F0") ||
                    configName.equals("F2") ||
                    configName.equals("F3") ||
                    configName.equals("C0") ||
                    configName.equals("C1") ||
                    configName.equals("C2") ||
                    configName.equals("C3") ||
                    configName.equals("C4") ||
                    configName.equals("C5") ||
                    configName.equals("C6")) landcover = "G1";

            // Config
            int orderRef = 0;
            if (configName.equals("C3") ||
                    configName.equals("C4")) orderRef = 1;

            boolean dif = true;
            if (configName.equals("C5")) dif = false;

            // screen
            String buildingTable  = "SCREENS";
            if (configName.equals("F0") || configName.equals("F1") || configName.equals("F2") || configName.equals("F3")) buildingTable = "BUILDINGS";
            connection.createStatement().execute("DROP TABLE SCREENS IF EXISTS");
            connection.createStatement().execute("CREATE TABLE SCREENS AS SELECT ST_BUFFER(the_geom, "+screenDepth+", 'join=mitre endcap=flat') as the_geom, pk as pk, "+screenHeight+" as height, "+G+" as g FROM Rail_protect");

            // RECEIVERS
            int nbReceivers = 0;
            try(ResultSet rs = connection.createStatement().executeQuery("SELECT COUNT(*) CPT FROM "+rcvName+"")) {
                assertTrue(rs.next());
                nbReceivers = rs.getInt(1);
            }
            connection.createStatement().execute("SELECT UpdateGeometrySRID('"+rcvName+"', 'THE_GEOM', 2154);");
            connection.createStatement().execute("ALTER TABLE "+rcvName+" ALTER COLUMN THE_GEOM TYPE geometry(POINTZ, 2154) USING ST_UPDATEZ(THE_GEOM, "+rcvHeight+")");

            // Config
            LDENConfig ldenConfig = new LDENConfig(LDENConfig.INPUT_MODE.INPUT_MODE_LW_DEN);
            ldenConfig.setComputeLDay(true);
            ldenConfig.setComputeLEvening(false);
            ldenConfig.setComputeLNight(false);
            ldenConfig.setComputeLDEN(false);
            ldenConfig.setExportRays(true);

            LDENPointNoiseMapFactory factory = new LDENPointNoiseMapFactory(connection, ldenConfig);
            factory.setKeepRays(true);

            PointNoiseMap pointNoiseMap = new PointNoiseMap(""+buildingTable+"", "LW_RAILWAY",""+rcvName+"");
            pointNoiseMap.setComputeRaysOutFactory(factory);
            pointNoiseMap.setPropagationProcessDataFactory(factory);
            pointNoiseMap.setGs(1);
            pointNoiseMap.setMaximumPropagationDistance(800.0);
            pointNoiseMap.setMaximumReflectionDistance(500.0);
            pointNoiseMap.setComputeHorizontalDiffraction(false);
            pointNoiseMap.setComputeVerticalDiffraction(dif);
            pointNoiseMap.setSoundReflectionOrder(orderRef);
            pointNoiseMap.setMaximumError(0.0);

            System.out.println("configName:" + configName);
            System.out.println("landcover:" + landcover);
            System.out.println("dem:" + dem);
            System.out.println("orderRef:" + orderRef);
            System.out.println("buildingTable:" + buildingTable);

            PropagationProcessPathData environmentalData = new PropagationProcessPathData(false);
            double[] DEFAULT_WIND_ROSE = new double[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            environmentalData.setWindRose(DEFAULT_WIND_ROSE);
            pointNoiseMap.setPropagationProcessPathData(environmentalData);

            if (dem) pointNoiseMap.setDemTable("DEM");
            if (landcover.equals("G0")) pointNoiseMap.setSoilTableName("LANDCOVER_G0");
            if (landcover.equals("G1")) pointNoiseMap.setSoilTableName("LANDCOVER_G1");

            // Calcul
            Set<Long> receivers = new HashSet<>();
            try {
                RootProgressVisitor progressLogger = new RootProgressVisitor(1, false, 1);
                pointNoiseMap.initialize(connection, new EmptyProgressVisitor());
                factory.start();
                Map<PointNoiseMap.CellIndex, Integer> cells = pointNoiseMap.searchPopulatedCells(connection);
                ProgressVisitor progressVisitor = progressLogger.subProcess(cells.size());
                // Iterate over computation areas
                for(PointNoiseMap.CellIndex cellIndex : new TreeSet<>(cells.keySet())) {
                    // Run ray propagation
                    IComputeRaysOut out = pointNoiseMap.evaluateCell(connection, cellIndex.getLatitudeIndex(), cellIndex.getLongitudeIndex(), progressVisitor, receivers);
                    if (out instanceof ComputeRaysOutAttenuation) {
                        ComputeRaysOutAttenuation cellStorage = (ComputeRaysOutAttenuation) out;
                        exportScene(String.format(Locale.ROOT,"target/scene_%d_%d.kml", cellIndex.getLatitudeIndex(), cellIndex.getLongitudeIndex()), cellStorage.inputData.profileBuilder, cellStorage);
                    }
                }
            }finally {
                factory.stop();
            }
            connection.commit();

            // Check if computation ok (numer of receivers)
            assertEquals(nbReceivers, receivers.size());

            // Check if results OK
            //testResults(connection, config)
            connection.createStatement().execute("DROP TABLE RESULTS IF EXISTS;");
            connection.createStatement().execute("CREATE TABLE RESULTS AS SELECT R.the_geom the_geom, R.PK pk, laeq laeq FROM "+ ldenConfig.lDayTable + " LVL, "+rcvName+" R WHERE LVL.IDRECEIVER = R.PK");

            SHPDriverFunction shpDriver = new SHPDriverFunction();
            shpDriver.exportTable(connection, "RESULTS", new File("target/Results_"+configName+".shp"), true, new EmptyProgressVisitor());
            shpDriver.exportTable(connection, ""+rcvName+"", new File("target/RECEPTEURS_"+configName+".shp"), true, new EmptyProgressVisitor());
            shpDriver.exportTable(connection, "LW_RAILWAY", new File("target/LW_RAILWAY_"+configName+".shp"), true, new EmptyProgressVisitor());

        }
        connection.close();

    }

    public void testRestults(Connection connection, String config){
        // ICI A MODIFIER
        try(ResultSet rs = connection.createStatement().executeQuery("SELECT PK,laeq FROM LDAY_GEOM LVL, RECEPTEURS R WHERE LVL.IDRECEIVER = R.PK ORDER BY PK")) {
            //try(ResultSet rs = connection.createStatement().executeQuery("SELECT PK,PK2,laeq FROM "+ ldenConfig.lDayTable + " LVL, RECEPTEUR2 R WHERE LVL.IDRECEIVER = R.PK2 ORDER BY PK2")) {
            /*assertTrue(rs.next());
            assertEquals(47.60, rs.getDouble(1), 2.0);
            assertTrue(rs.next());
            assertEquals(56.58, rs.getDouble(1), 2.0);
            assertTrue(rs.next());
            assertEquals(56.58, rs.getDouble(1), 2.0);
            assertTrue(rs.next());
            assertEquals(56.58, rs.getDouble(1), 2.0);
            assertTrue(rs.next());
            assertEquals(56.58, rs.getDouble(1), 2.0);
            assertTrue(rs.next());
            assertEquals(56.58, rs.getDouble(1), 2.0);
            assertTrue(rs.next());
            assertEquals(56.58, rs.getDouble(1), 2.0);
            assertTrue(rs.next());
            assertEquals(56.58, rs.getDouble(1), 2.0);
            assertTrue(rs.next());
            assertEquals(56.58, rs.getDouble(1), 2.0);
            assertTrue(rs.next());
            assertEquals(56.58, rs.getDouble(1), 2.0);
            assertTrue(rs.next());
            assertEquals(56.58, rs.getDouble(1), 2.0);*/
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }


    public static void exportScene(String name, ProfileBuilder builder, ComputeRaysOutAttenuation result) throws IOException {
        try {
            //List<PropagationPath> propagationPaths = new ArrayList<>();
            //propagationPaths.addAll(((LDENComputeRaysOut) result).ldenData.rays);
            FileOutputStream outData = new FileOutputStream(name);
            KMLDocument kmlDocument = new KMLDocument(outData);
            kmlDocument.setInputCRS("EPSG:2154");
            kmlDocument.writeHeader();
            if(builder != null) {
                kmlDocument.writeTopographic(builder.getTriangles(), builder.getVertices());
            }
            if(result != null) {
              //  kmlDocument.writeRays(propagationPaths);
            }
            if(builder != null) {
                kmlDocument.writeBuildings(builder);
            }
            if(result != null) {
                kmlDocument.writeProfile(builder.getProfile(result.getInputData().sourceGeometries.get(0).getCoordinate(),result.getInputData().receivers.get(0)));
            }

            kmlDocument.writeFooter();
        } catch (XMLStreamException | CoordinateOperationException | CRSException ex) {
            throw new IOException(ex);
        }
    }
}