package org.noise_planet.noisemodelling.emission.jdbc;

import org.h2gis.api.EmptyProgressVisitor;
import org.h2gis.api.ProgressVisitor;
import org.h2gis.functions.factory.H2GISDBFactory;
import org.h2gis.functions.io.geojson.GeoJsonRead;
import org.h2gis.functions.io.shp.SHPWrite;
import org.h2gis.utilities.SFSUtilities;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.noise_planet.noisemodelling.propagation.IComputeRaysOut;
import org.noise_planet.noisemodelling.propagation.RootProgressVisitor;
import org.noise_planet.noisemodelling.propagation.jdbc.PointNoiseMap;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CnossosBench {

    private Connection connection;

    @Before
    public void tearUp() throws Exception {
        connection = SFSUtilities.wrapConnection(H2GISDBFactory.createSpatialDataBase(LDENPointNoiseMapFactoryTest.class.getSimpleName(), true, ""));
    }

    @After
    public void tearDown() throws Exception {
        if(connection != null) {
            connection.close();
        }
    }

    @Test
    public void testReflectionOrder() throws SQLException, IOException {

        GeoJsonRead.readGeoJson(connection, CnossosBench.class.getResource("testrefl_buildings.geojson").getFile());

        Coordinate src = new Coordinate(-1.5587684512138364,
                        47.205498836592874);


        Coordinate receiver = new Coordinate(-1.5567058324813843,
                47.20571385749249);

        try(Statement st = connection.createStatement()) {
            st.execute("CREATE TABLE BUILDINGS(pk serial primary key, geom geometry, height double) as select null, ST_Transform(st_setsrid(the_geom,4326), 2154) geom, 4  from testrefl_buildings");
            st.execute(String.format(Locale.ROOT,"CREATE TABLE SRC(pk serial primary key, geom geometry, lwd63 double, lwd125 double, lwd250 double, lwd500 double, lwd1000 double, lwd2000 double, lwd4000 double, lwd8000 double,lwe63 double, lwe125 double, lwe250 double, lwe500 double, lwe1000 double, lwe2000 double, lwe4000 double, lwe8000 double, lwn63 double, lwn125 double, lwn250 double, lwn500 double, lwn1000 double, lwn2000 double, lwn4000 double, lwn8000 double) as select null, ST_Transform(st_setsrid(ST_MakePoint(%.5f, %.5f, 0.05),4326), 2154) the_geom, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80",src.x, src.y));

            st.execute(String.format(Locale.ROOT,"CREATE TABLE RECEIVERS(pk serial primary key, geom geometry) as select null, ST_Transform(st_setsrid(ST_MakePoint(%.5f, %.5f, 1.6),4326), 2154) the_geom",receiver.x, receiver.y));
        }

        LDENConfig ldenConfig = new LDENConfig(LDENConfig.INPUT_MODE.INPUT_MODE_LW_DEN);

        ldenConfig.setComputeLDay(true);
        ldenConfig.setComputeLEvening(false);
        ldenConfig.setComputeLNight(false);
        ldenConfig.setComputeLDEN(false);
        ldenConfig.setExportRays(true);
        ldenConfig.setMergeSources(true); // No idsource column

        LDENPointNoiseMapFactory factory = new LDENPointNoiseMapFactory(connection, ldenConfig);


        PointNoiseMap pointNoiseMap = new PointNoiseMap("BUILDINGS", "SRC",
                "RECEIVERS");

        pointNoiseMap.setComputeRaysOutFactory(factory);
        pointNoiseMap.setPropagationProcessDataFactory(factory);

        pointNoiseMap.setMaximumPropagationDistance(300.0);
        pointNoiseMap.setComputeHorizontalDiffraction(false);
        pointNoiseMap.setComputeVerticalDiffraction(false);

        RootProgressVisitor progressLogger = new RootProgressVisitor(1, true, 1);

        pointNoiseMap.initialize(connection, new EmptyProgressVisitor());

        pointNoiseMap.setGridDim(1); // force grid size

        StringBuilder sb = new StringBuilder();
        SHPWrite.exportTable(connection, "target/buildings.shp", "BUILDINGS");
        for(int refOrder = 3; refOrder < 4; refOrder++) {
            pointNoiseMap.setSoundReflectionOrder(refOrder);
            try {
                factory.start();
                pointNoiseMap.evaluateCell(connection, 0, 0, progressLogger, new HashSet<>());
            } finally {
                factory.stop();
            }
            // Check receiver values
            try(ResultSet rs = connection.createStatement().executeQuery("SELECT leq FROM " + ldenConfig.lDayTable)) {
                assertTrue(rs.next());
                sb.append(refOrder).append(",").append(rs.getDouble(1)).append("\n");
            }
            SHPWrite.exportTable(connection, "target/rays_"+refOrder+".shp", ldenConfig.raysTable);
        }
        System.out.print(sb.toString());

    }
}
