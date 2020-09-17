package org.noise_planet.noisemodelling.wps.matsim

import groovy.sql.Sql
import org.h2gis.functions.io.dbf.DBFRead
import org.h2gis.functions.io.dbf.DBFWrite
import org.h2gis.functions.io.geojson.GeoJsonRead
import org.h2gis.functions.io.geojson.GeoJsonWrite
import org.h2gis.functions.io.shp.SHPRead
import org.h2gis.functions.io.shp.SHPWrite
import org.noise_planet.noisemodelling.wps.JdbcTestCase
import org.noise_planet.noisemodelling.wps.Matsim.Import_Activities
import org.noise_planet.noisemodelling.wps.Matsim.Receivers_From_Activities
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TestReceiversFromActivities extends JdbcTestCase {
    Logger logger = LoggerFactory.getLogger(TestReceiversFromActivities.class);

    void testDefault() {
        GeoJsonRead.readGeoJson(connection, this.class.getResource("tables/activities.geojson").getFile(), "ACTIVITIES");
        GeoJsonRead.readGeoJson(connection, this.class.getResource("tables/all_receivers.geojson").getFile(), "ALL_RECEIVERS");

        String result = runWps();

        GeoJsonWrite.writeGeoJson(connection, "receivers.geojson", "RECEIVERS");

        assertEquals("Process done. Table of receivers RECEIVERS created !", result);
    }

    private String runWps() {
        return new Receivers_From_Activities().exec(connection, [
                "activitiesTable": "ACTIVITIES",
                "receiversTable": "ALL_RECEIVERS",
                "outTableName": "RECEIVERS"
        ])
    }
}
