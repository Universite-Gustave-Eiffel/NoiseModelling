package org.noise_planet.noisemodelling.wps.matsim

import org.h2gis.functions.io.dbf.DBFWrite
import org.h2gis.functions.io.geojson.GeoJsonWrite
import org.h2gis.functions.io.shp.SHPWrite
import org.noise_planet.noisemodelling.wps.JdbcTestCase
import org.noise_planet.noisemodelling.wps.Matsim.Import_Activities
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TestImportActivities extends JdbcTestCase {
    Logger logger = LoggerFactory.getLogger(TestImportActivities.class);

    void testDefault() {
        String result = runWps();
        GeoJsonWrite.writeGeoJson(connection, "activities.geojson", "ACTIVITIES")
        assertEquals("Process done. Table of receivers ACTIVITIES created !", result);
    }

    private String runWps() {
        return new Import_Activities().exec(connection, [
                "facilitiesPath": this.class.getResource("output_facilities.xml.gz"),
                "outTableName": "ACTIVITIES"
        ])
    }
}
