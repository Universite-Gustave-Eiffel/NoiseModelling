package org.noise_planet.noisemodelling.wps.matsim

import org.h2gis.functions.io.dbf.DBFWrite
import org.h2gis.functions.io.geojson.GeoJsonWrite
import org.h2gis.functions.io.shp.SHPWrite
import org.noise_planet.noisemodelling.wps.JdbcTestCase
import org.noise_planet.noisemodelling.wps.Others_Tools.Osm_Pbf_to_Buildings
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TestOsmPbfToBuidings extends JdbcTestCase {
    Logger logger = LoggerFactory.getLogger(TestOsmPbfToBuidings.class);

    void testDefault() {
        String result = runWps();
        GeoJsonWrite.writeGeoJson(connection, "buildings.geojson", "BUILDINGS");
    }

    private String runWps() {
        String omsFile = TestOsmPbfToBuidings.getResource("nantes_very_small.osm.pbf").getFile();

        return new Osm_Pbf_to_Buildings().exec(connection, [
                "pathFile": omsFile,
                "targetSRID": "2154"
        ])
    }
}
