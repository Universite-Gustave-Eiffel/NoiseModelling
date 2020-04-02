package org.noise_planet.noisemodelling.wps

import groovy.sql.Sql
import org.h2gis.functions.io.shp.SHPRead
import org.h2gis.functions.io.shp.SHPWrite
import org.junit.Test
import org.noise_planet.noisemodelling.wps.Others_Tools.Screen_to_building

class TestScreenToBuildings extends JdbcTestCase {

    @Test
    void testTruncateScreens() {
        String screen1 = "LINESTRING (224146.48 6758063.29, 224164.4 6757986.29, 224164.81 6757970.4) "
        String screen2 = "LINESTRING (224206.98 6757997.9, 224213.9 6757964.7, 224210.24 6757964.29, 224206.98 6757997.9)"
        def sql = new Sql(connection)
        sql.execute("CREATE TABLE SCREENS(pk serial, the_geom geometry, height double)")
        sql.executeInsert("INSERT INTO SCREENS(pk, THE_GEOM, HEIGHT) VALUES (2001,?, 66), (2002,?, 99)", [screen1, screen2])
        SHPRead.readShape(connection, TestScreenToBuildings.getResource("buildings.shp").getPath())

        new Screen_to_building().exec(connection, ["buildingTableName": "BUILDINGS", "screenTableName" : "SCREENS"])

        SHPWrite.exportTable(connection, "target/BUILDINGS_SCREENS.shp", "BUILDINGS_SCREENS")

        // Check new walls not intersecting with buildings
        assertEquals(0, sql.firstRow("SELECT COUNT(*) CPT FROM BUILDINGS B, BUILDINGS_SCREENS S WHERE B.the_geom && S.the_geom and (S.height = 66 OR S.height = 99) and ST_INTERSECTS(B.THE_GEOM, S.THE_GEOM)")[0] as Integer)


    }
}
