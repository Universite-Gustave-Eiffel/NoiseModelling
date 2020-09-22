package org.noise_planet.noisemodelling.wps.matsim

import org.h2gis.functions.io.geojson.GeoJsonRead
import org.h2gis.functions.io.geojson.GeoJsonWrite
import org.noise_planet.noisemodelling.wps.Database_Manager.Add_Primary_Key
import org.noise_planet.noisemodelling.wps.JdbcTestCase
import org.noise_planet.noisemodelling.wps.Matsim.Import_Activities
import org.noise_planet.noisemodelling.wps.NoiseModelling.Noise_level_from_source
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TestCalculateAttenuationMatrix extends JdbcTestCase {
    Logger logger = LoggerFactory.getLogger(TestCalculateAttenuationMatrix.class);

    void testDefault() {
        GeoJsonRead.readGeoJson(connection, this.class.getResource("tables/buildings.geojson").getFile(), "BUILDINGS");
        GeoJsonRead.readGeoJson(connection, this.class.getResource("tables/receivers.geojson").getFile(), "RECEIVERS");
        GeoJsonRead.readGeoJson(connection, this.class.getResource("tables/sources_0db.geojson").getFile(), "SOURCES_0DB");

        String result = runWps();

        // GeoJsonWrite.writeGeoJson(connection, "att_matrix.geojson", "LDAY_GEOM")

        assertEquals("Calculation Done !  LDAY_GEOM table(s) have been created.", result);
    }

    private String runWps() {
        new Add_Primary_Key().exec(connection, [
                "pkName": "PK",
                "tableName": "BUILDINGS"
        ])
        new Add_Primary_Key().exec(connection, [
                "pkName": "PK",
                "tableName": "SOURCES_0DB"
        ])
        new Add_Primary_Key().exec(connection, [
                "pkName": "PK",
                "tableName": "RECEIVERS"
        ])
        return new Noise_level_from_source().exec(connection, [
                "tableBuilding": "BUILDINGS",
                "tableReceivers" : "RECEIVERS",
                "tableSources" : "SOURCES_0DB",
                "confMaxSrcDist": 150,
                "confMaxReflDist": 50,
                "confReflOrder": 0,
                "confSkipLevening": true,
                "confSkipLnight": true,
                "confSkipLden": true,
                "confExportSourceId": true,
                "confDiffVertical": true,
                "confDiffHorizontal": true
        ])
    }
}
