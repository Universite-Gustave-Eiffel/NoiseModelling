/**
 * NoiseModelling is an open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 *
 * This version is developed by the DECIDE team from the Lab-STICC (CNRS) and by the Mixt Research Unit in Environmental Acoustics (Université Gustave Eiffel).
 * <http://noise-planet.org/noisemodelling.html>
 *
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 *
 * Contact: contact@noise-planet.org
 *
 */

package org.noise_planet.noisemodelling.wps

import groovy.sql.Sql
import org.h2gis.functions.io.shp.SHPRead
import org.junit.Test
import org.noise_planet.noisemodelling.wps.Geometric_Tools.Change_SRID
import org.noise_planet.noisemodelling.wps.Geometric_Tools.CleanBuildingsTable
import org.noise_planet.noisemodelling.wps.Geometric_Tools.Screen_to_building
import org.noise_planet.noisemodelling.wps.Geometric_Tools.Set_Height
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.SQLException
import java.sql.Statement

/**
 * Test parsing of zip file using H2GIS database
 */
class TestGeometricTools extends JdbcTestCase {
    Logger LOGGER = LoggerFactory.getLogger(TestGeometricTools.class)

    @Test
    void testChangeSRID1() {

        SHPRead.readShape(connection, TestGeometricTools.getResource("roads.shp").getPath())

        String res = new Change_SRID().exec(connection,
                ["newSRID": "2154",
                 "tableName": "roads"])

        assertEquals("The table already counts 2154 as SRID.", res)
    }

    @Test
    void testChangeSRID2() {

        SHPRead.readShape(connection, TestGeometricTools.getResource("roads.shp").getPath())

        String res = new Change_SRID().exec(connection,
                ["newSRID": "4326",
                 "tableName": "roads"])

        assertEquals("SRID changed from 2154 to 4326.", res)
    }



    @Test
    void testTruncateScreens() {

        String screen1 = "LINESTRING (224146.48 6758063.29, 224164.4 6757986.29, 224164.81 6757970.4) "
        String screen2 = "LINESTRING (224206.98 6757997.9, 224213.9 6757964.7, 224210.24 6757964.29, 224206.98 6757997.9)"
        def sql = new Sql(connection)
        sql.execute("CREATE TABLE SCREENS(pk serial, the_geom geometry, height double)")
        sql.executeInsert("INSERT INTO SCREENS(pk, THE_GEOM, HEIGHT) VALUES (2001,?, 66), (2002,?, 99)", [screen1, screen2])
        SHPRead.readShape(connection, TestGeometricTools.getResource("buildings.shp").getPath())

        new Screen_to_building().exec(connection, ["tableBuilding": "BUILDINGS", "tableScreens" : "SCREENS"])

        //SHPWrite.exportTable(connection, "target/BUILDINGS_SCREENS.shp", "BUILDINGS_SCREENS")

        // Check new walls not intersecting with buildings
        assertEquals(0, sql.firstRow("SELECT COUNT(*) CPT FROM BUILDINGS B, BUILDINGS_SCREENS S WHERE B.the_geom && S.the_geom and (S.height = 66 OR S.height = 99) and ST_INTERSECTS(B.THE_GEOM, S.THE_GEOM)")[0] as Integer)
    }

    @Test
    void testSetHeight1() {
        SHPRead.readShape(connection, TestGeometricTools.getResource("receivers.shp").getPath())
        def sql = new Sql(connection)

        new Set_Height().exec(connection,
                ["height": 0.05,
                 "tableName": "receivers"])

        assertEquals(0.05, sql.firstRow("SELECT ST_Z(THE_GEOM) FROM RECEIVERS")[0])
    }



    @Test
    void testSetHeight2() {
        SHPRead.readShape(connection, TestGeometricTools.getResource("roads.shp").getPath())
        def sql = new Sql(connection)

        new Set_Height().exec(connection,
                ["height": 0.05,
                 "tableName": "roads"])

        assertEquals(0.05, sql.firstRow("SELECT ST_Z(THE_GEOM) FROM ROADS")[0])
    }

    @Test
    void testCleanBuildings() {
        SHPRead.readShape(connection, TestGeometricTools.getResource("buildings.shp").getPath())
        def sql = new Sql(connection)

        new CleanBuildingsTable().exec(connection,
                ["tableName": "buildings"])

        assertEquals('POLYGON ((223850.08378369396 6758172.390990572, 223852.979044039 6758185.902205516, 223862.97904403895 6758183.902205515, 223860.02095646886 6758170.097796855, 223850.08378369396 6758172.390990572))', sql.firstRow("SELECT THE_GEOM FROM buildings")[0] as String)
    }


}
