package org.noise_planet.noisemodelling.wps.matsim

import groovy.sql.Sql
import org.h2gis.functions.io.geojson.GeoJsonRead
import org.noise_planet.noisemodelling.wps.JdbcTestCase
import org.noise_planet.noisemodelling.wps.Matsim.Agent_Exposure
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TestAgentExposure extends JdbcTestCase {
    Logger logger = LoggerFactory.getLogger(TestAgentExposure.class);

    void testDefault() {
        GeoJsonRead.readGeoJson(connection, this.class.getResource("tables/noise_map.geojson").getFile(), "NOISE_MAP")
        GeoJsonRead.readGeoJson(connection, this.class.getResource("tables/receivers.geojson").getFile(), "RECEIVERS")

        Sql sql = new Sql(connection);
        sql.execute("ALTER TABLE RECEIVERS ALTER COLUMN PK INT NOT NULL;");
        sql.execute("ALTER TABLE RECEIVERS ADD PRIMARY KEY (PK)");
        sql.execute("CREATE INDEX ON RECEIVERS(FACILITY)");
        sql.execute("CREATE INDEX ON NOISE_MAP(TIMESTRING)");
        sql.execute("CREATE INDEX ON NOISE_MAP(IDRECEIVER)");

        String result = runWps();
        assertEquals("Process done. Table AGENTS created !", result);
    }

    private String runWps() {
        return new Agent_Exposure().exec(connection, [
                plansFile: this.class.getResource("output_plans.xml.gz").getFile(),
                dataTable: "NOISE_MAP",
                receiversTable: "RECEIVERS",
                timeSlice: "hour",
                outTableName: "AGENTS"
        ])
    }
}
