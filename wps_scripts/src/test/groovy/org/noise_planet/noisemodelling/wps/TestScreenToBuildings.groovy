package org.noise_planet.noisemodelling.wps

import groovy.sql.Sql
import org.h2gis.functions.io.shp.SHPRead
import org.h2gis.functions.io.shp.SHPWrite
import org.junit.Test
import org.noise_planet.noisemodelling.wps.Others_Tools.Screen_to_building

class TestScreenToBuildings extends JdbcTestCase {

    @Test
    public void testTruncateScreens() {
        String screen1 = "LINESTRING (224146.48 6758063.29, 224164.4 6757986.29, 224164.81 6757970.4) "
        String screen2 = "LINESTRING (224206.98 6757997.9, 224213.9 6757964.7, 224210.24 6757964.29, 224206.98 6757997.9)"
        String fence = "POLYGON ((224163.73 6757984.27, 224163.67 6757985.37, 224166.23 6757985.4, 224166.18 6757984.29, 224163.73 6757984.27))"
        def sql = new Sql(connection)
        sql.execute("CREATE TABLE SCREENS(pk serial, the_geom geometry, height double)")
        sql.executeInsert("INSERT INTO SCREENS(pk, THE_GEOM, HEIGHT) VALUES (2001,?, 4), (2002,?, 3.5)", [screen1, screen2])
        SHPRead.readShape(connection, TestScreenToBuildings.getResource("buildings.shp").getPath())

        new Screen_to_building().exec(connection, ["buildingTableName": "BUILDINGS", "screenTableName" : "SCREENS"])

        assertEquals(2001, sql.firstRow("SELECT PK FROM SCREENS WHERE ST_INTERSECTS(SCREENS.THE_GEOM, ?::geometry)", [fence])[0] as Integer)
        assertEquals(0, sql.firstRow("SELECT COUNT(*) CPT FROM BUILDINGS_SCREENS WHERE ST_INTERSECTS(BUILDINGS_SCREENS.THE_GEOM, ?::geometry)", [fence]) [0] as Integer)
    }
}
