package org.noise_planet.noisemodelling.wps

import groovy.sql.Sql
import org.h2gis.functions.io.shp.SHPRead
import org.h2gis.functions.io.shp.SHPWrite
import org.noise_planet.noisemodelling.wps.Receivers.Building_Grid

class TestReceivers extends JdbcTestCase {



    public void testRegularGrid() {
        SHPRead.readShape(connection, TestReceivers.getResource("buildings.shp").getPath())

        SHPRead.readShape(connection, TestReceivers.getResource("roads.shp").getPath())

        new Building_Grid().exec(connection,  ["tableBuilding" : "BUILDINGS",
                                               "delta" : 5,
                                               "sourcesTableName" : "ROADS",
                                               "fenceTableName" : "BUILDINGS"])
        SHPWrite.exportTable(connection, "target/receivers.shp", "RECEIVERS")
        //def sql = new Sql(connection)
        // sql.execute("CREATE SPATIAL INDEX ON RECEIVERS(THE_GEOM)")
        // Check if receivers minimal distance
        //assertEquals(50.0, sql.firstRow("SELECT MIN(ST_DISTANCE(R1.THE_GEOM, R2.THE_GEOM)) MINDIST FROM RECEIVERS R1, RECEIVERS R2 WHERE R1.PK != R2.PK AND R1.THE_GEOM && ST_EXPAND(R2.THE_GEOM, 100, 100)")[0] as Double);
    }
}
