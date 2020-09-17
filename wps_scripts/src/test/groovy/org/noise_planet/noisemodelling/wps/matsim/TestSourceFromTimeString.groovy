package org.noise_planet.noisemodelling.wps.matsim

import org.h2gis.functions.io.dbf.DBFRead
import org.h2gis.functions.io.geojson.GeoJsonRead
import org.noise_planet.noisemodelling.wps.JdbcTestCase
import org.noise_planet.noisemodelling.wps.Matsim.Sources_From_TimeString
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TestSourceFromTimeString extends JdbcTestCase {
    Logger logger = LoggerFactory.getLogger(TestSourceFromTimeString.class);

    void testDefault() {
        GeoJsonRead.readGeoJson(connection, this.class.getResource("tables/matsim_roads.geojson").getFile(), "MATSIM_ROADS");
        DBFRead.read(connection, this.class.getResource("tables/matsim_roads_stats.dbf").getFile(), "MATSIM_ROADS_STATS")
        String result = runWps();
        assertEquals("SOURCE_12_13 created.", result);
    }

    private String runWps() {
        return new Sources_From_TimeString().exec(connection, [
                roadsTableName: "MATSIM_ROADS",
                statsTableName: "MATSIM_ROADS_STATS",
                timeString: "12_13",
                outTableName: "SOURCE_12_13"
        ])
    }
}
