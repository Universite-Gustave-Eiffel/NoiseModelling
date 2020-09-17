package org.noise_planet.noisemodelling.wps.matsim

import org.h2gis.functions.io.dbf.DBFWrite
import org.h2gis.functions.io.geojson.GeoJsonWrite
import org.h2gis.functions.io.shp.SHPWrite;
import org.noise_planet.noisemodelling.wps.JdbcTestCase;
import org.noise_planet.noisemodelling.wps.Matsim.Traffic_From_Events;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TestTrafficFromEvents extends JdbcTestCase {
    Logger logger = LoggerFactory.getLogger(TestTrafficFromEvents.class);

    void testDefault() {
        String result = runWps("hour", true, "", false);

        GeoJsonWrite.writeGeoJson(connection, "matsim_roads.geojson", "MATSIM_ROADS");
        DBFWrite.exportTable(connection, "matsim_roads_stats.dbf", "MATSIM_ROADS_STATS");

        assertEquals("Roads stats imported from matsim traffic output", result);
    }

    void testPerVehicleLevel() {
        String result = runWps("hour", true, "", true);
        assertEquals("Roads stats imported from matsim traffic output", result);
    }

    void testDen() {
        String result = runWps("den", true, "", false);
        assertEquals("Roads stats imported from matsim traffic output", result);
    }

    void testQuarterHour() {
        String result = runWps("quarter", true, "", false);
        assertEquals("Roads stats imported from matsim traffic output", result);
    }

    void testIgnoreAgents() {
        String result = runWps("hour", true, "1960, 1961, 77869, 4230", false);
        assertEquals("Roads stats imported from matsim traffic output", result);
    }

    private String runWps(String timeSlice, boolean skipUnused, String ignoreAgents, boolean perVehicleLevel) {
        String matsim_folder = new File(TestTrafficFromEvents.getResource("output_events.xml.gz").getFile()).getParent();
        String networkCsv = TestTrafficFromEvents.getResource("network.csv").getFile();
        String outTableName = "MATSIM_ROADS";

        return new Traffic_From_Events().exec(connection, [
                "folder" : matsim_folder,
                "outTableName" : outTableName,
                "link2GeometryFile" : networkCsv,
                "timeSlice": timeSlice, // DEN, hour, quarter
                "skipUnused": skipUnused,
                "ignoreAgents": ignoreAgents,
                "perVehicleLevel": perVehicleLevel,
                "populationFactor": "0.001"
        ])
    }
}
