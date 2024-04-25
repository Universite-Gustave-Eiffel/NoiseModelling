package org.noise_planet.noisemodelling.jdbc;

import org.h2gis.api.EmptyProgressVisitor;
import org.h2gis.functions.factory.H2GISDBFactory;
import org.h2gis.utilities.JDBCUtilities;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.noise_planet.noisemodelling.pathfinder.IComputeRaysOut;
import org.noise_planet.noisemodelling.pathfinder.RootProgressVisitor;
import org.noise_planet.noisemodelling.propagation.ComputeRaysOutAttenuation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

public class LTPointNoiseMapFactoryTest {

    static Logger LOGGER = LoggerFactory.getLogger(LTPointNoiseMapFactoryTest.class);

    private Connection connection;

    @Before
    public void tearUp() throws Exception {
        connection = JDBCUtilities.wrapConnection(H2GISDBFactory.createSpatialDataBase(LTPointNoiseMapFactoryTest.class.getSimpleName(), true, ""));
    }

    @After
    public void tearDown() throws Exception {
        if(connection != null) {
            connection.close();
        }
    }

    @Test
    public void testPointDynamic() throws Exception {
        try (Statement st = connection.createStatement()) {
            st.execute("CREATE TABLE BUILDINGS(pk serial  PRIMARY KEY, the_geom geometry, height real)");
            st.execute("drop table if exists LW_DYNAMIC;");
            st.execute("create table LW_DYNAMIC(IT integer, PK integer, the_geom GEOMETRY(POINTZ), Hz63 double precision, Hz125 double precision, Hz250 double precision, Hz500 double precision, Hz1000 double precision, Hz2000 double precision, Hz4000 double precision, Hz8000 double precision);");
            st.execute("INSERT INTO LW_DYNAMIC(IT , PK,the_geom,Hz63, Hz125, Hz250, Hz500, Hz1000,Hz2000, Hz4000, Hz8000) " +
                    "VALUES (1,1,'POINTZ (223915.72 6757290.22 2.0)',90,90,90,90,90,90,90,90);");
            st.execute("INSERT INTO LW_DYNAMIC(IT , PK,the_geom,Hz63, Hz125, Hz250, Hz500, Hz1000,Hz2000, Hz4000, Hz8000) " +
                    "VALUES (2,1,'POINTZ (223915.72 6757290.22 2.0)',90,90,90,90,90,90,90,90);");
            st.execute("INSERT INTO LW_DYNAMIC(IT , PK,the_geom,Hz63, Hz125, Hz250, Hz500, Hz1000,Hz2000, Hz4000, Hz8000) " +
                    "VALUES (3,1,'POINTZ (223915.72 6757290.22 2.0)',90,90,90,90,90,90,90,90);");
            st.execute("INSERT INTO LW_DYNAMIC(IT , PK,the_geom,Hz63, Hz125, Hz250, Hz500, Hz1000,Hz2000, Hz4000, Hz8000) " +
                    "VALUES (1,2,'POINTZ (223915.72 6757390.22 2.0)',90,90,90,90,90,90,90,90);");
            st.execute("INSERT INTO LW_DYNAMIC(IT , PK,the_geom,Hz63, Hz125, Hz250, Hz500, Hz1000,Hz2000, Hz4000, Hz8000) " +
                    "VALUES (2,5,'POINTZ (223915.72 6757390.22 2.0)',90,90,90,90,90,90,90,90);");

            st.execute("create table receivers(id serial PRIMARY KEY, the_geom GEOMETRY(POINTZ));\n" +
                    "insert into receivers(the_geom) values ('POINTZ (223915.72 6757490.22 2.0)');" +
                    "insert into receivers(the_geom) values ('POINTZ (223925.72 6757480.22 2.0)');");

            PointNoiseMap pointNoiseMap = new PointNoiseMap("BUILDINGS", "LW_DYNAMIC", "RECEIVERS");
            pointNoiseMap.setComputeHorizontalDiffraction(false);
            pointNoiseMap.setType("T");
            pointNoiseMap.setComputeVerticalDiffraction(true);
            pointNoiseMap.setSoundReflectionOrder(0);
            pointNoiseMap.setReceiverHasAbsoluteZCoordinates(false);
            pointNoiseMap.setMaximumPropagationDistance(10000);
            pointNoiseMap.setSourceHasAbsoluteZCoordinates(false);
            pointNoiseMap.setHeightField("HEIGHT");



            LTConfig ltConfig = new LTConfig();
            ltConfig.setExportRaysMethod(LTConfig.ExportRaysMethods.TO_MEMORY);
            LTPointNoiseMapFactory ltPointNoiseMapFactory = new LTPointNoiseMapFactory(connection, ltConfig);


            pointNoiseMap.setPropagationProcessDataFactory(ltPointNoiseMapFactory);
            pointNoiseMap.setComputeRaysOutFactory(ltPointNoiseMapFactory);

            pointNoiseMap.initialize(connection, new EmptyProgressVisitor());
            Set<Long> receivers = new HashSet<>();
            pointNoiseMap.setThreadCount(1);
            pointNoiseMap.setGridDim(1);
            RootProgressVisitor progressVisitor = new RootProgressVisitor(pointNoiseMap.getGridDim() * pointNoiseMap.getGridDim(), true, 5);
            for(int i=0; i < pointNoiseMap.getGridDim(); i++) {
                for(int j=0; j < pointNoiseMap.getGridDim(); j++) {
                    IComputeRaysOut out = pointNoiseMap.evaluateCell(connection, i, j, progressVisitor, receivers);
                    if(out instanceof LTComputeRaysOut) {
                        LTComputeRaysOut rout = (LTComputeRaysOut) out;

                        ComputeRaysOutAttenuation.VerticeSL sl = rout.ltData.lTLevels.pop();
                     //   assertEquals(1, sl.receiverId);
                      //  assertEquals(73.3, sl.value[0], 1);
                        sl = rout.ltData.lTLevels.pop();
                   //     assertEquals(2, sl.receiverId);
                     //   assertEquals(53.3, sl.value[0], 1);
                      //  assertTrue(rout.ltData.lTLevels.isEmpty());


                    } else {
                        throw new IllegalStateException();
                    }
                }
            }
        }
    }


}