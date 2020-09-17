package org.noise_planet.noisemodelling.wps.matsim

import groovy.sql.Sql
import org.h2gis.functions.io.dbf.DBFRead
import org.h2gis.functions.io.geojson.GeoJsonRead
import org.h2gis.functions.io.geojson.GeoJsonWrite
import org.h2gis.functions.io.shp.SHPRead
import org.h2gis.functions.io.shp.SHPWrite
import org.noise_planet.noisemodelling.wps.Database_Manager.Add_Primary_Key
import org.noise_planet.noisemodelling.wps.JdbcTestCase
import org.noise_planet.noisemodelling.wps.Matsim.Receivers_From_Activities
import org.noise_planet.noisemodelling.wps.Receivers.Building_Grid
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TestReceversArroundBuildings extends JdbcTestCase {
    Logger logger = LoggerFactory.getLogger(TestReceversArroundBuildings.class);

    void testDefault() {
        GeoJsonRead.readGeoJson(connection, this.class.getResource("tables/buildings.geojson").getFile(), "BUILDINGS");

        String result = runWps();

        // GeoJsonWrite.writeGeoJson(connection, "all_receivers.geojson", "ALL_RECEIVERS");

        assertEquals("Process done. Table of receivers ALL_RECEIVERS created !", result);
    }

    private String runWps() {
        new Add_Primary_Key().exec(connection, [
                "pkName": "PK",
                "tableName": "BUILDINGS"
        ])
        return new Building_Grid().exec(connection, [
                "tableBuilding": "BUILDINGS",
                "receiversTableName": "ALL_RECEIVERS"
        ])
    }
}
