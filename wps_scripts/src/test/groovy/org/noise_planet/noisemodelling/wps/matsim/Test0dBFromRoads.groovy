package org.noise_planet.noisemodelling.wps.matsim

import org.h2gis.functions.io.geojson.GeoJsonRead
import org.h2gis.functions.io.geojson.GeoJsonWrite
import org.h2gis.functions.io.shp.SHPRead
import org.h2gis.functions.io.shp.SHPWrite
import org.noise_planet.noisemodelling.wps.JdbcTestCase
import org.noise_planet.noisemodelling.wps.Others_Tools.ZerodB_Source_From_Roads
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Test0dBFromRoads extends JdbcTestCase {
    Logger logger = LoggerFactory.getLogger(Test0dBFromRoads.class);

    void testDefault() {
        GeoJsonRead.readGeoJson(connection, this.class.getResource("tables/matsim_roads.geojson").getFile(), "MATSIM_ROADS")
        String result = runWps();

        GeoJsonWrite.writeGeoJson(connection, "sources_0db.geojson", "SOURCES_0DB")

        assertEquals("Process done. Table SOURCES_0DB created !", result);
    }

    private String runWps() {
        return new ZerodB_Source_From_Roads().exec(connection, [
                "roadsTableName": "MATSIM_ROADS",
                "sourcesTableName": "SOURCES_0DB"
        ])
    }
}
