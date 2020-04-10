/**
 * NoiseModelling is a free and open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 *
 * This version is developed by Université Gustave Eiffel and CNRS
 * <http://noise-planet.org/noisemodelling.html>
 * as part of:
 * the Eval-PDU project (ANR-08-VILL-0005) 2008-2011, funded by the Agence Nationale de la Recherche (French)
 * the CENSE project (ANR-16-CE22-0012) 2017-2021, funded by the Agence Nationale de la Recherche (French)
 * the Nature4cities (N4C) project, funded by European Union’s Horizon 2020 research and innovation programme under grant agreement No 730468
 *
 * Noisemap is distributed under GPL 3 license.
 *
 * Contact: contact@noise-planet.org
 *
 * Copyright (C) 2011-2012 IRSTV (FR CNRS 2488) and Ifsttar
 * Copyright (C) 2013-2019 Ifsttar and CNRS
 * Copyright (C) 2020 Université Gustave Eiffel and CNRS
 */


package org.noise_planet.noisemodelling.wps

import groovy.sql.Sql
import org.h2gis.functions.io.shp.SHPRead
import org.h2gis.functions.spatial.crs.ST_SetSRID
import org.h2gis.functions.spatial.crs.ST_Transform
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.GeometryFactory
import org.noise_planet.noisemodelling.wps.Receivers.Building_Grid
import org.noise_planet.noisemodelling.wps.Receivers.Delaunay_Grid
import org.noise_planet.noisemodelling.wps.Receivers.Random_Grid

class TestReceivers extends JdbcTestCase {

    void testBuildingGrid() {
        def sql = new Sql(connection)

        SHPRead.readShape(connection, TestReceivers.getResource("buildings.shp").getPath())
        sql.execute("CREATE SPATIAL INDEX ON BUILDINGS(THE_GEOM)")
        sql.execute("CREATE INDEX bheight ON BUILDINGS(height)")

        SHPRead.readShape(connection, TestReceivers.getResource("roads.shp").getPath())

        new Building_Grid().exec(connection,  ["tableBuilding" : "BUILDINGS",
                                               "delta" : 5,
                                                "height" : 6,
                                               "sourcesTableName" : "ROADS",
                                               "fenceTableName" : "BUILDINGS"])


        def receivers_in_buildings = sql.firstRow("SELECT COUNT(*) from receivers r, buildings b where r.the_geom && b.the_geom and st_intersects(r.the_geom, b.the_geom) and ST_Z(r.the_geom) < b.height ")[0] as Integer
        assertEquals(0, receivers_in_buildings)

        sql.execute("CREATE SPATIAL INDEX ON RECEIVERS(the_geom)")
        sql.execute("CREATE INDEX ON RECEIVERS(build_pk)")

        // check effective distance between receivers

        def average_receiver_min_distance = sql.firstRow("SELECT AVG((select ST_DISTANCE(R.THE_GEOM, RR.THE_GEOM) dist from receivers rr where rr.build_pk = r.build_pk and r.pk != rr.pk ORDER BY ST_DISTANCE(R.THE_GEOM, RR.THE_GEOM) LIMIT 1)) avgdist from receivers r")[0] as Double

        //SHPWrite.exportTable(connection, "target/receivers.shp", "RECEIVERS")
        //SHPWrite.exportTable(connection, "target/receivers_line.shp", "TMP_SCREENS_MERGE")
        assertEquals(5, average_receiver_min_distance, 0.6)

    }

    void testBuildingGridWithPop() {
        def sql = new Sql(connection)

        SHPRead.readShape(connection, TestReceivers.getResource("buildings.shp").getPath(), "BUILDINGS_NOPOP")
        sql.execute("DROP TABLE IF EXISTS BUILDINGS")
        sql.execute("CREATE TABLE BUILDINGS(pk serial, the_geom geometry, height double, pop double) AS SELECT pk, the_geom, height, ST_AREA(THE_GEOM) / 15 as pop from buildings_nopop")

        SHPRead.readShape(connection, TestReceivers.getResource("roads.shp").getPath())

        new Building_Grid().exec(connection,  ["tableBuilding" : "BUILDINGS",
                                               "delta" : 5,
                                               "height" : 6,
                                               "sourcesTableName" : "ROADS",
                                               "fenceTableName" : "BUILDINGS"])

        //SHPWrite.exportTable(connection, "target/receivers.shp", "RECEIVERS")
        //SHPWrite.exportTable(connection, "target/buildings.shp", "BUILDINGS")

        def receivers_pop = sql.firstRow("SELECT sum(pop) from receivers")[0] as Double

        def buildings_pop = sql.firstRow("SELECT sum(pop) from buildings where pk in (select distinct build_pk from receivers)")[0] as Double

        assertEquals(0, buildings_pop - receivers_pop, 0.1);
    }
    void testBuildingGridFence() {
        def sql = new Sql(connection)

        SHPRead.readShape(connection, TestReceivers.getResource("buildings.shp").getPath())
        sql.execute("CREATE SPATIAL INDEX ON BUILDINGS(THE_GEOM)")
        sql.execute("CREATE INDEX bheight ON BUILDINGS(height)")

        GeometryFactory f = new GeometryFactory();
        def g = f.toGeometry(new Envelope(223556.5, 223765.7,6758256.91, 6758576.3))
        def gFence = ST_Transform.ST_Transform(connection, ST_SetSRID.setSRID(g, 2154), 4326)
        new Building_Grid().exec(connection,  ["tableBuilding" : "BUILDINGS",
                                               "delta" : 5,
                                               "height" : 6,
                                               "fence" : gFence])

        assertTrue(sql.firstRow("SELECT count(*) cpt from receivers")[0] > 0)

        def receivers_pop = sql.firstRow("SELECT count(*) cpt from receivers r where not ST_Intersects(r.the_geom, ST_GeomFromText('"+g.toString()+"'))")[0] as Integer

        assertEquals(0, receivers_pop);
    }

    public void testDelaunayGrid() {
        def sql = new Sql(connection)

        SHPRead.readShape(connection, TestReceivers.getResource("buildings.shp").getPath())
        SHPRead.readShape(connection, TestReceivers.getResource("roads.shp").getPath())
        sql.execute("CREATE SPATIAL INDEX ON BUILDINGS(THE_GEOM)")
        sql.execute("CREATE SPATIAL INDEX ON ROADS(THE_GEOM)")

        new Delaunay_Grid().exec(connection, ["buildingTableName" : "BUILDINGS",
        "sourcesTableName" : "ROADS"]);

        sql.execute("CREATE SPATIAL INDEX ON RECEIVERS(THE_GEOM)")

        // Check if index and geoms is corresponding
        def res = sql.firstRow("SELECT MAX((SELECT ST_DISTANCE(T.THE_GEOM, R.THE_GEOM) D FROM RECEIVERS R WHERE R.PK = T.PK_1)) D1," +
                " MAX((SELECT ST_DISTANCE(T.THE_GEOM, R.THE_GEOM) D FROM RECEIVERS R WHERE R.PK = T.PK_2)) D2," +
                " MAX((SELECT ST_DISTANCE(T.THE_GEOM, R.THE_GEOM) D FROM RECEIVERS R WHERE R.PK = T.PK_3)) D3 FROM TRIANGLES T");
        def max_dist_a = res[0] as Double
        def max_dist_b = res[1] as Double
        def max_dist_c = res[2] as Double
        assertEquals(0.0, max_dist_a, 1e-6d);
        assertEquals(0.0, max_dist_b, 1e-6d);
        assertEquals(0.0, max_dist_c, 1e-6d);
    }

    public void testRandomGrid() {

        def sql = new Sql(connection)

        SHPRead.readShape(connection, TestReceivers.getResource("buildings.shp").getPath())
        SHPRead.readShape(connection, TestReceivers.getResource("roads.shp").getPath())

        new Random_Grid().exec(connection,  ["buildingTableName" : "BUILDINGS",
                                             "sourcesTableName" : "ROADS",
                                             "nReceivers" : 200])

        assertTrue(200 >= (sql.firstRow("SELECT COUNT(*) CPT FROM RECEIVERS")[0] as Integer))
    }
}
